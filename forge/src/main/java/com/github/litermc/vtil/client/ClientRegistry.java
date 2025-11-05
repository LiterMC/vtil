package com.github.litermc.vtil.client;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import com.github.litermc.vtil.Constants;
import com.github.litermc.vtil.VtilRegistry;

@Mod.EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientRegistry {
	@SubscribeEvent
	public static void clientSetup(final FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			VtilRegistry.Blocks.onRegisterRenderType(ItemBlockRenderTypes::setRenderLayer);
		});
	}
}
