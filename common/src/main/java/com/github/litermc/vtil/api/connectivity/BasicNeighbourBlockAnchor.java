package com.github.litermc.vtil.api.connectivity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collection;

/**
 * A simple block anchor implemention that consider any direct neighbour as connectable.
 * It is by default implemented on {@link BlockBehaviour}.
 */
public interface BasicNeighbourBlockAnchor extends IBlockAnchor {
	@Override
	default void getConnectableBlocks(
		final LevelAccessor level,
		final BlockPos pos,
		final BlockState state,
		final Collection<BlockPos> result
	) {
		for (final Direction dir : Direction.values()) {
			final BlockPos p = pos.relative(dir);
			if (this.isBlockConnectable(level, pos, state, p, level.getBlockState(p))) {
				result.add(p);
			}
		}
	}

	@Override
	default boolean isBlockConnectable(
		LevelAccessor level,
		BlockPos pos,
		BlockState state,
		BlockPos targetPos,
		BlockState targetState
	) {
		return pos.distManhattan(targetPos) == 1 && state.canSurvive(level, targetPos);
	}

	@Override
	default boolean willConnectivityChange(
		final LevelAccessor level,
		final BlockPos pos,
		final BlockState oldState,
		final BlockState state
	) {
		if (oldState == state) {
			return false;
		}
		final BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		for (final Direction dir : Direction.values()) {
			p.setWithOffset(pos, dir);
			final BlockState s = level.getBlockState(p);
			if (this.isBlockConnectable(level, pos, oldState, p, s) != this.isBlockConnectable(level, pos, state, p, s)) {
				return true;
			}
		}
		return false;
	}
}
