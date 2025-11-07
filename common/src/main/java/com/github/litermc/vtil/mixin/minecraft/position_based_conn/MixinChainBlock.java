package com.github.litermc.vtil.mixin.minecraft.position_based_conn;

import com.github.litermc.vtil.api.connectivity.IBlockAnchor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import org.spongepowered.asm.mixin.Mixin;

import java.util.Collection;

@Mixin(ChainBlock.class)
public abstract class MixinChainBlock extends Block implements IBlockAnchor {
	protected MixinChainBlock() {
		super(null);
	}

	@Override
	public void getConnectableBlocks(
		final LevelAccessor level,
		final BlockPos pos,
		final BlockState state,
		final Collection<BlockPos> result
	) {
		switch (state.getValue(BlockStateProperties.AXIS)) {
			case X -> {
				result.add(pos.relative(Direction.WEST));
				result.add(pos.relative(Direction.EAST));
			}
			case Y -> {
				result.add(pos.relative(Direction.DOWN));
				result.add(pos.relative(Direction.UP));
			}
			case Z -> {
				result.add(pos.relative(Direction.NORTH));
				result.add(pos.relative(Direction.SOUTH));
			}
		}
	}

	@Override
	public boolean isBlockConnectable(
		final LevelAccessor level,
		final BlockPos pos,
		final BlockState state,
		final BlockPos targetPos,
		final BlockState targetState
	) {
		final int xDiff = targetPos.getX() - pos.getX();
		final int yDiff = targetPos.getY() - pos.getY();
		final int zDiff = targetPos.getZ() - pos.getZ();
		if (Math.abs(xDiff) + Math.abs(yDiff) + Math.abs(zDiff) != 1) {
			return false;
		}
		final Direction dir = Direction.fromDelta(xDiff, yDiff, zDiff);
		return dir.getAxis() == state.getValue(BlockStateProperties.AXIS);
	}

	@Override
	public boolean willConnectivityChange(
		final LevelAccessor level,
		final BlockPos pos,
		final BlockState oldState,
		final BlockState state
	) {
		if (oldState == state) {
			return false;
		}
		if (oldState.getBlock().getClass() != this.getClass()) {
			return true;
		}
		return oldState.getValue(BlockStateProperties.AXIS) != state.getValue(BlockStateProperties.AXIS);
	}
}
