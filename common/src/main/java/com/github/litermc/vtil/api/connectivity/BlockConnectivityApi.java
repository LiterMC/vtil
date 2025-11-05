package com.github.litermc.vtil.api.connectivity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import java.util.Collection;

public final class BlockConnectivityApi {
	private BlockConnectivityApi() {}

	/**
	 * Returns if the block is not possible to have any connection.
	 * E.g. air, flowing liquid.
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
		final IBlockAnchor anchor = (IBlockAnchor) (state.getBlock());
		anchor.getConnectableBlocks(level, pos, state, result);
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
			return true;
		}
		final boolean wasAir = isAir(oldState);
		if (wasAir != isAir(newState)) {
			return true;
		}
		if (!wasAir) {
			return true;
		}
		final IBlockAnchor anchor = (IBlockAnchor) (newState.getBlock());
		return anchor.willConnectivityChange(level, pos, oldState, newState);
	}

}
