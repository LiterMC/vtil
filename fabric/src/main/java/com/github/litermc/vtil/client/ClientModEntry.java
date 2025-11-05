package com.github.litermc.vtil.client;

import com.github.litermc.vtil.VtilRegistry;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.RenderType;

public class ClientModEntry implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		VtilRegistry.Blocks.onRegisterRenderType(BlockRenderLayerMap.INSTANCE::putBlock);
	}
}
