package com.github.litermc.vtil.mixin.minecraft.position_based_conn;

import com.github.litermc.vtil.api.connectivity.BasicNeighbourBlockAnchor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;

import org.spongepowered.asm.mixin.Mixin;

@Mixin(TrapDoorBlock.class)
public abstract class MixinTrapDoorBlock extends Block implements BasicNeighbourBlockAnchor {
	protected MixinTrapDoorBlock() {
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
		final boolean isOpen = state.getValue(BlockStateProperties.OPEN);
		return switch (dir) {
			case UP -> isOpen || state.getValue(BlockStateProperties.HALF) == Half.TOP;
			case DOWN -> isOpen || state.getValue(BlockStateProperties.HALF) == Half.BOTTOM;
			default -> !isOpen || state.getValue(HorizontalDirectionalBlock.FACING) != dir;
		};
	}

	@Override
	public boolean willConnectivityChange(
		final LevelAccessor level,
		final BlockPos pos,
		final BlockState oldState,
		final BlockState state
	) {
		return oldState != state;
	}
}
