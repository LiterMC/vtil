package com.github.litermc.vtil.mixin.valkyrienskies;

import com.github.litermc.vtil.accessor.ShipObjectServerAccessor;
import com.github.litermc.vtil.api.attachment.IServerTickListener;

import org.valkyrienskies.core.impl.game.ships.ShipObjectServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.HashMap;

@Mixin(ShipObjectServer.class)
public class MixinShipObjectServer implements ShipObjectServerAccessor {
	@Unique
	private final HashMap<Class<?>, IServerTickListener> serverTickListeners = new HashMap<>();

	@Override
	public Collection<IServerTickListener> vtil$getServerTickListeners() {
		return this.serverTickListeners.values();
	}

	@Inject(method = "applyAttachmentInterfaces", at = @At("HEAD"), remap = false)
	private void applyAttachmentInterfaces(final Class<?> clazz, final Object value, final CallbackInfo ci) {
		if (value == null) {
			this.serverTickListeners.remove(clazz);
			return;
		}
		if (value instanceof final IServerTickListener listener) {
			this.serverTickListeners.put(clazz, listener);
			return;
		}
	}
}
