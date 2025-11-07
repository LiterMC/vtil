package com.github.litermc.vtil.mixin.minecraft.position_based_conn;

import com.github.litermc.vtil.api.connectivity.BasicNeighbourBlockAnchor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;

@Mixin(FenceGateBlock.class)
public abstract class MixinFenceGateBlock extends Block implements BasicNeighbourBlockAnchor {
	protected MixinFenceGateBlock() {
		super(null);
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
		final Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
		return switch (dir) {
			case UP, DOWN -> true;
			default -> dir != facing && dir != facing.getOpposite();
		};
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
		return oldState.getBlock().getClass() != this.getClass();
	}
}
