package com.github.litermc.vtil.mixin.minecraft.position_based_conn;

import com.github.litermc.vtil.api.connectivity.IBlockAnchor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.Collection;

@Mixin(AttachedStemBlock.class)
public abstract class MixinAttachedStemBlock extends Block implements IBlockAnchor {
	protected MixinAttachedStemBlock() {
		super(null);
	}

	@Unique
	private static BlockPos getAttachingPos(final BlockPos pos, final BlockState state) {
		return pos.relative(state.getValue(HorizontalDirectionalBlock.FACING));
	}

	@Override
	public void getConnectableBlocks(
		final LevelAccessor level,
		final BlockPos pos,
		final BlockState state,
		final Collection<BlockPos> result
	) {
		result.add(pos.below());
		result.add(getAttachingPos(pos, state));
	}

	@Override
	public boolean isBlockConnectable(
		final LevelAccessor level,
		final BlockPos pos,
		final BlockState state,
		final BlockPos targetPos,
		final BlockState targetState
	) {
		return pos.below().equals(targetPos) || getAttachingPos(pos, state).equals(targetPos);
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
		return oldState.getValue(HorizontalDirectionalBlock.FACING) != state.getValue(HorizontalDirectionalBlock.FACING);
	}
}
