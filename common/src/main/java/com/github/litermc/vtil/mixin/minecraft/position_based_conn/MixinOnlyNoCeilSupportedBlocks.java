package com.github.litermc.vtil.mixin.minecraft.position_based_conn;

import com.github.litermc.vtil.api.connectivity.IBlockAnchor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;

import java.util.Collection;

@Mixin({
	CampfireBlock.class,
	CarpetBlock.class
})
public abstract class MixinOnlyNoCeilSupportedBlocks extends Block implements IBlockAnchor {
	protected MixinOnlyNoCeilSupportedBlocks() {
		super(null);
	}

	@Override
	public void getConnectableBlocks(
		final LevelAccessor level,
		final BlockPos pos,
		final BlockState state,
		final Collection<BlockPos> result
	) {
		for (final Direction dir : Direction.values()) {
			if (dir != Direction.UP) {
				result.add(pos.relative(dir));
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
		return dir != Direction.UP;
	}

	@Override
	public boolean willConnectivityChange(
		final LevelAccessor level,
		final BlockPos pos,
		final BlockState oldState,
		final BlockState state
	) {
		return oldState.getBlock().getClass() != this.getClass();
	}
}
