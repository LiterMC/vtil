package com.github.litermc.vtil.mixin.minecraft;

import com.github.litermc.vtil.api.connectivity.BasicNeighbourBlockAnchor;

import net.minecraft.world.level.block.state.BlockBehaviour;

import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockBehaviour.class)
public abstract class MixinBlockBehaviour implements BasicNeighbourBlockAnchor {
}
