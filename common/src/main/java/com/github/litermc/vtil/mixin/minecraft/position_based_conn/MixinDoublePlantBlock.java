package com.github.litermc.vtil.mixin.minecraft.position_based_conn;

import com.github.litermc.vtil.api.connectivity.IBlockAnchor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

import org.spongepowered.asm.mixin.Mixin;

import java.util.Collection;

@Mixin(DoublePlantBlock.class)
public abstract class MixinDoublePlantBlock extends Block implements IBlockAnchor {
	protected MixinDoublePlantBlock() {
		super(null);
	}

	@Override
	public void getConnectableBlocks(
		final LevelAccessor level,
		final BlockPos pos,
		final BlockState state,
		final Collection<BlockPos> result
	) {
		result.add(pos.below());
		if (state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER) {
			result.add(pos.above());
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
		return
			pos.below().equals(targetPos) ||
			state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER && pos.above().equals(targetPos);
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
		return oldState.getValue(DoublePlantBlock.HALF) != state.getValue(DoublePlantBlock.HALF);
	}
}

