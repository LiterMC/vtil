package com.github.litermc.vtil.api.assemble;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * IMoveable defines the specific behaviours when blocks is moving between world and ships.
 * When searching IMoveable for a block, it should use the following logic:
 * <ol>
 * <li>Check if the block's BlockEntity implemented the interface or has a mover.</li>
 * <li>Check if the block class implemented the interface or has a mover.</li>
 * </ol>
 *
 * @param <C> Block transfer context type, can be anything.
 * @see MoveApi#registerDefaultMover
 * @see MoveApi#getMover
 */
public interface IMoveable<C> {
	/**
	 * Invoked before save the block's status and NBT data for its block entity.
	 * Blocks may tweak its BlockStatus and/or BlockEntity in this method.
	 *
	 * @param level  The world the block is in
	 * @param origin The old block position
	 * @param target The new block position
	 */
	default void beforeSaveForMove(ServerLevel level, BlockPos origin, BlockPos target) {}

	/**
	 * Invoked before remove the origin block.
	 * Should do cleanup here if necessary.
	 * For example, blocks should remove their inventory contents here if they will drop during block removal.
	 * Any other unsafe contents during block movement (e.g. create contraption) should also be stored inside
	 * the context to recover them later in {@link afterMove}.
	 *
	 * @param level  The world the block is in.
	 * @param origin The old block position.
	 * @param target The new block position.
	 * @return Block transfer context, the exact value will immediately passes to {@link afterMove}, and will only be used once.
	 */
	C beforeMove(ServerLevel level, BlockPos origin, BlockPos target);

	/**
	 * Invoked after the origin block is moved.
	 * Should restore all things from the context if necessary.
	 *
	 * @param level   The world the block is in.
	 * @param origin  The old block position.
	 * @param target  The new block position.
	 * @param context The context {@link beforeMove} returns.
	 */
	void afterMove(ServerLevel level, BlockPos origin, BlockPos target, C context);
}
