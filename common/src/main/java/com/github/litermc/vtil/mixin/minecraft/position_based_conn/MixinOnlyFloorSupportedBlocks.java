package com.github.litermc.vtil.mixin.minecraft.position_based_conn;

import com.github.litermc.vtil.api.connectivity.IBlockAnchor;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.BambooSaplingBlock;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.BaseCoralPlantTypeBlock;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.TurtleEggBlock;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;

import java.util.Collection;

@Mixin({
	AbstractCandleBlock.class,
	BambooSaplingBlock.class,
	BambooStalkBlock.class,
	BannerBlock.class,
	BaseCoralPlantTypeBlock.class,
	BasePressurePlateBlock.class,
	BushBlock.class,
	CakeBlock.class,
	DiodeBlock.class,
	RedStoneWireBlock.class,
	TorchBlock.class,
	TurtleEggBlock.class
})
public abstract class MixinOnlyFloorSupportedBlocks extends Block implements IBlockAnchor {
	protected MixinOnlyFloorSupportedBlocks() {
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
	}

	@Override
	public boolean isBlockConnectable(
		final LevelAccessor level,
		final BlockPos pos,
		final BlockState state,
		final BlockPos targetPos,
		final BlockState targetState
	) {
		return pos.below().equals(targetPos);
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
