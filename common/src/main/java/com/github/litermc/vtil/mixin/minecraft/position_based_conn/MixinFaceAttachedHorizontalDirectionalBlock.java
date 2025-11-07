package com.github.litermc.vtil.mixin.minecraft.position_based_conn;

import com.github.litermc.vtil.api.connectivity.IBlockAnchor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collection;

@Mixin(FaceAttachedHorizontalDirectionalBlock.class)
public abstract class MixinFaceAttachedHorizontalDirectionalBlock extends Block implements IBlockAnchor {
	protected MixinFaceAttachedHorizontalDirectionalBlock() {
		super(null);
	}

	@Shadow
	protected static Direction getConnectedDirection(BlockState state) {
		return null;
	}

	@Override
	public void getConnectableBlocks(
		final LevelAccessor level,
		final BlockPos pos,
		final BlockState state,
		final Collection<BlockPos> result
	) {
		result.add(pos.relative(getConnectedDirection(state).getOpposite()));
	}

	@Override
	public boolean isBlockConnectable(
		final LevelAccessor level,
		final BlockPos pos,
		final BlockState state,
		final BlockPos targetPos,
		final BlockState targetState
	) {
		return pos.relative(getConnectedDirection(state).getOpposite()).equals(targetPos);
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
		if (oldState.getBlock() != this) {
			return true;
		}
		return getConnectedDirection(oldState) != getConnectedDirection(state);
	}
}
