package com.github.litermc.vtil.api.connectivity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;

import java.util.Collection;

public final class BlockConnectivityApi {
	private BlockConnectivityApi() {}

	/**
	 * Returns if the block is not possible to have any connection.
	 * E.g. air, flowing liquid.
	 *
	 * @param state The block state
	 * @return {@code true} if the block is not possible to have connection, {@code false} otherwise.
	 */
	public static final boolean isAir(final BlockState state) {
		if (state.isAir()) {
			return true;
		}
		if (state.getBlock() instanceof LiquidBlock) {
			final FluidState fluidState = state.getFluidState();
			if (fluidState.isEmpty() || !fluidState.isSource()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if a block contains a fluid source (e.g. water logged).
	 * If so, then the block may be connected from all direction.
	 *
	 * @param state The block state
	 * @return if a block contains a fluid source.
	 */
	public static final boolean isFluidLogged(final BlockState state) {
		return state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED);
	}

	/**
	 * Get any possible connectable blocks from a block.
	 * Should not be invoked if {@link isAir} returns {@code true}.
	 * All results should be add into the result collection.
	 *
	 * @param level  World the block is in.
	 * @param pos    Position of the block.
	 * @param state  The block state, should not be any air.
	 * @param result The result holder.
	 */
	public static final void getPossibleConnectableBlocks(
		final LevelAccessor level,
		final BlockPos pos,
		final BlockState state,
		final Collection<BlockPos> result
	) {
		if (isFluidLogged(state)) {
			// TODO: what if getConnectableBlocks provides more than direct neighbors?
			for (final Direction dir : Direction.values()) {
				result.add(pos.relative(dir));
			}
			return;
		}
		final IBlockAnchor anchor = (IBlockAnchor) (state.getBlock());
		anchor.getConnectableBlocks(level, pos, state, result);
	}

	/**
	 * Get all connectable blocks from a block.
	 * All results should be add into the result collection.
	 *
	 * @param level  World the block is in.
	 * @param pos    Position of the block.
	 * @param result The result holder.
	 */
	public static final void getConnectableBlocks(final LevelAccessor level, final BlockPos pos, final Collection<BlockPos> result) {
		final BlockState state = level.getBlockState(pos);
		if (isAir(state)) {
			return;
		}
		getConnectableBlocks(level, pos, state, result);
	}

	/**
	 * Get all connectable blocks from a block.
	 * Should not be invoked if {@link isAir} returns {@code true}.
	 * All results should be add into the result collection.
	 *
	 * @param level  World the block is in.
	 * @param pos    Position of the block.
	 * @param state  The block state, should not be any air.
	 * @param result The result holder.
	 */
	public static final void getConnectableBlocks(
		final LevelAccessor level,
		final BlockPos pos,
		final BlockState state,
		final Collection<BlockPos> result
	) {
		getPossibleConnectableBlocks(level, pos, state, result);
		result.removeIf((p) -> {
			final BlockState s = level.getBlockState(p);
			return isAir(s) || !isBlockConnectable0(level, p, s, pos, state);
		});
	}

	/**
	 * Check if two blocks are connectable.
	 *
	 * @param level       World the block is in.
	 * @param pos         Position of the block.
	 * @param state       The block state, will never be any air.
	 * @param targetPos   Position of target block.
	 * @param targetState Target block state, will never be any air.
	 */
	public static final boolean isBlockConnectable(
		final LevelAccessor level,
		final BlockPos pos,
		final BlockPos targetPos
	) {
		final BlockState state = level.getBlockState(pos);
		if (isAir(state)) {
			return false;
		}
		final BlockState targetState = level.getBlockState(targetPos);
		if (isAir(targetState)) {
			return false;
		}
		return isBlockConnectable(level, pos, state, targetPos, targetState);
	}

	/**
	 * Check if two blocks are connectable.
	 * Should not be invoked if either block is air.
	 *
	 * @param level       World the block is in.
	 * @param pos         Position of the block.
	 * @param state       The block state, will never be any air.
	 * @param targetPos   Position of target block.
	 * @param targetState Target block state, will never be any air.
	 */
	public static final boolean isBlockConnectable(
		final LevelAccessor level,
		final BlockPos pos,
		final BlockState state,
		final BlockPos targetPos,
		final BlockState targetState
	) {
		return
			isBlockConnectable0(level, pos, state, targetPos, targetState) &&
			isBlockConnectable0(level, targetPos, targetState, pos, state);
	}

	private static final boolean isBlockConnectable0(
		final LevelAccessor level,
		final BlockPos pos,
		final BlockState state,
		final BlockPos targetPos,
		final BlockState targetState
	) {
		return
			isFluidLogged(state) ||
			((IBlockAnchor) (state.getBlock())).isBlockConnectable(level, pos, state, targetPos, targetState);
	}

	/**
	 * Check if connectable blocks will change.
	 *
	 * @param level    World the block is in.
	 * @param pos      Position of the block.
	 * @param oldState Previous block state.
	 * @param newState Current block state.
	 */
	public static boolean willConnectivityChange(
		final LevelAccessor level,
		final BlockPos pos,
		final BlockState oldState,
		final BlockState newState
	) {
		if (oldState == newState) {
			return false;
		}
		if (isFluidLogged(oldState) && isFluidLogged(newState)) {
			return false;
		}
		final boolean wasAir = isAir(oldState);
		if (wasAir != isAir(newState)) {
			return true;
		}
		if (wasAir) {
			return false;
		}
		final IBlockAnchor anchor = (IBlockAnchor) (newState.getBlock());
		return anchor.willConnectivityChange(level, pos, oldState, newState);
	}

}
