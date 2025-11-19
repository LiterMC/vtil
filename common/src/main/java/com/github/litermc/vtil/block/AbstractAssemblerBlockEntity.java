package com.github.litermc.vtil.block;

import com.github.litermc.vtil.api.assemble.AssembleApi;
import com.github.litermc.vtil.api.connectivity.BlockConnectivityApi;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.util.datastructures.DenseBlockPosSet;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public abstract class AbstractAssemblerBlockEntity extends BlockEntity {
	private final Direction facing;
	private volatile boolean assembling = false;
	protected final Set<BlockPos> blocks = new HashSet<>();
	protected final DenseBlockPosSet checked = new DenseBlockPosSet();
	protected final Queue<BlockPos> queueing = new ArrayDeque<>();
	private String shipSlug = null;

	protected AbstractAssemblerBlockEntity(
		final BlockEntityType<? extends AbstractAssemblerBlockEntity> type,
		final BlockPos pos,
		final BlockState state
	) {
		super(type, pos, state);
		this.facing = this.getBlockState().getValue(DirectionalBlock.FACING);
	}

	public boolean isAssembling() {
		return this.assembling;
	}

	protected void setAssembling(final boolean assembling) {
		this.assembling = assembling;
	}

	/**
	 * The maximum dimension can be assembled by the assembler
	 * @return maximum dimension can be assembled by the assembler
	 */
	public int getMaxAssembleDimension() {
		return 16 * 64;
	}

	/**
	 * The maximum blocks can be assembled by the assembler
	 * @return maximum blocks can be assembled by the assembler
	 */
	public int getMaxAssembleBlocks() {
		return 16 * 16 * 16 * 32 * 25;
	}

	/**
	 * The maximum blocks can be processed per tick.
	 * @return maximum blocks can be processed per tick.
	 */
	public int getBlockChecksPerTick() {
		return 16 * 16 * 16 * 16;
	}

	/**
	 * startAssemble should only be called from main thread.
	 */
	public boolean startAssemble(final String slug) {
		if (this.isAssembling()) {
			return false;
		}
		this.setAssembling(true);
		this.shipSlug = slug;
		this.blocks.clear();
		this.checked.clear();
		this.queueing.clear();
		final BlockPos facingBlockPos = this.getBlockPos().relative(this.facing);
		this.checked.add(facingBlockPos.getX(), facingBlockPos.getY(), facingBlockPos.getZ());
		this.queueing.add(facingBlockPos);
		return true;
	}

	protected void finishAssemble() {
		this.setAssembling(false);
		this.shipSlug = null;
		this.blocks.clear();
		this.checked.clear();
		this.queueing.clear();
	}

	protected void finishAssembleAsSuccess() {
		this.finishAssemble();
	}

	protected void finishAssembleAsAssembleSelf() {
		this.finishAssemble();
	}

	protected void finishAssembleAsTooManyBlocks() {
		this.finishAssemble();
	}

	protected void finishAssembleAsNoBlockToAssemble() {
		this.finishAssemble();
	}

	protected void finishAssembleAsConflicts() {
		this.finishAssemble();
	}

	public void serverTick() {
		if (this.isAssembling()) {
			this.assembleTick((ServerLevel) (this.getLevel()));
		}
	}

	private void assembleTick(final ServerLevel level) {
		final int maxAssembleBlocks = this.getMaxAssembleBlocks();
		final int blockChecksPerTick = this.getBlockChecksPerTick();
		final BlockPos selfPos = this.getBlockPos();
		int ticked = 0;
		while (true) {
			final BlockPos pos = this.queueing.poll();
			if (pos == null) {
				this.checked.clear();
				if (ticked > 0) {
					return;
				}
				break;
			}
			if (this.blocks.size() > maxAssembleBlocks) {
				this.finishAssembleAsTooManyBlocks();
				return;
			}
			if (pos.equals(selfPos)) {
				this.finishAssembleAsAssembleSelf();
				return;
			}
			this.addAssemblingBlock(pos);
			if (!this.isAssembling()) {
				return;
			}
			ticked++;
			if (ticked > blockChecksPerTick) {
				return;
			}
		}
		if (this.blocks.isEmpty()) {
			this.finishAssembleAsNoBlockToAssemble();
			return;
		}

		final ServerShip ship = this.createShip(level, this.blocks);
		if (ship == null) {
			return;
		}
		this.onAssembleSuccess(ship);
	}

	protected void addAssemblingBlock(final BlockPos pos) {
		if (BlockConnectivityApi.isAir(this.getLevel().getBlockState(pos))) {
			return;
		}
		this.blocks.add(pos);
		for (final BlockPos p : this.queryNextBlocks(pos)) {
			final BlockState targetState = level.getBlockState(p);
			if (level.getBlockEntity(p) instanceof final AbstractAssemblerBlockEntity otherAssembler) {
				if (this == otherAssembler) {
					if (this.getBlockPos().relative(this.facing).equals(pos)) {
						continue;
					}
					this.finishAssembleAsAssembleSelf();
					return;
				}
				if (otherAssembler.isAssembling()) {
					this.finishAssembleAsConflicts();
					return;
				}
				continue;
			}
			if (this.checked.add(p.getX(), p.getY(), p.getZ())) {
				this.queueing.add(p.immutable());
			}
		}
	}

	protected Iterable<BlockPos> queryNextBlocks(final BlockPos pos) {
		final Set<BlockPos> result = new HashSet<>(6);
		BlockConnectivityApi.getConnectableBlocks(this.getLevel(), pos, result);
		return result;
	}

	protected ServerShip createShip(final ServerLevel level, final Set<BlockPos> blocks) {
		final ServerShip ship = AssembleApi.createShip(level, blocks);
		if (ship == null) {
			this.finishAssembleAsNoBlockToAssemble();
			return null;
		}
		ship.setSlug(this.shipSlug);
		this.finishAssembleAsSuccess();
		return ship;
	}

	protected void onAssembleSuccess(final ServerShip ship) {}
}
