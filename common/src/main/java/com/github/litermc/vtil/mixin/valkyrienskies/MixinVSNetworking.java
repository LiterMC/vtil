package com.github.litermc.vtil.mixin.valkyrienskies;

import com.github.litermc.vtil.accessor.VSNetworkingAccessor;

import org.valkyrienskies.core.impl.networking.VSNetworking;
import org.valkyrienskies.core.impl.networking.simple.SimplePacketNetworking;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(VSNetworking.class)
public class MixinVSNetworking implements VSNetworkingAccessor {
	@Shadow(remap = false)
	@Final
	private SimplePacketNetworking simplePackets;

	@Override
	public SimplePacketNetworking vtil$getSimplePackets() {
		return this.simplePackets;
	}
}
