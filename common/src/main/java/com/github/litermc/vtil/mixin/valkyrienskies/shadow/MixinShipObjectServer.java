package com.github.litermc.vtil.mixin.valkyrienskies.shadow;

import com.github.litermc.vtil.accessor.ShipObjectServerAccessor;
import com.github.litermc.vtil.api.attachment.IServerTickListener;

import org.valkyrienskies.core.impl.game.ships.ShipData;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.HashMap;

@Mixin(org.valkyrienskies.core.impl.shadow.Eq.class)
public abstract class MixinShipObjectServer implements ShipObjectServerAccessor {
	@Unique
	private boolean initing = true;
	@Unique
	private final HashMap<Class<?>, IServerTickListener> serverTickListeners = new HashMap<>();

	@Override
	public void vtil$reinitDefaultAttachments() {
		final var wingManager = new org.valkyrienskies.core.impl.shadow.Ew();
		wingManager.createWingGroup(/* isContraption */ false);
		this.setAttachment(wingManager);
		// this.setAttachment(new org.valkyrienskies.core.impl.shadow.Eh()); // marked as permanent
	}

	@Override
	public Collection<IServerTickListener> vtil$getServerTickListeners() {
		return this.serverTickListeners.values();
	}

	@Shadow(remap = false)
	protected abstract ShipData c();

	@Override
	public ShipData vtil$getShipData() {
		return this.c();
	}

	@Shadow(remap = false)
	protected abstract <T> T setAttachment(T attachment);

	@Shadow(remap = false)
	protected abstract <T> T removeAttachment(Class<T> clazz);

	@Override
	public <T> T vtil$removeAttachment(final Class<T> clazz) {
		return this.removeAttachment(clazz);
	}

	@Inject(method = M_applyAttachmentInterfaces + "(Ljava/lang/Class;Ljava/lang/Object;)V", at = @At("HEAD"), remap = false)
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
