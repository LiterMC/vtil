package com.github.litermc.vtil.mixin.valkyrienskies.shadow;

import com.github.litermc.vtil.accessor.ShipObjectServerWorldAccessor;
import com.github.litermc.vtil.api.assemble.ShipAllocator;
import com.github.litermc.vtil.api.storage.ShipDataStorage;
import com.github.litermc.vtil.config.Config;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.valkyrienskies.core.api.ships.QueryableShipData;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.impl.api.ServerShipInternal;
import org.valkyrienskies.core.impl.game.ships.ShipData;
import org.valkyrienskies.core.internal.joints.VSJoint;
import org.valkyrienskies.core.internal.ships.VsiMutableQueryableShipData;
import org.valkyrienskies.core.internal.world.VsiPlayer;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@Mixin(org.valkyrienskies.core.impl.shadow.Er.class)
public abstract class MixinShipObjectServerWorld implements ShipObjectServerWorldAccessor {
	@Shadow(remap = false)
	protected abstract VsiMutableQueryableShipData<ShipData> a();

	@Override
	public VsiMutableQueryableShipData<ShipData> vtil$getAllShips() {
		return this.a();
	}

	@Shadow(remap = false)
	protected abstract ImmutableMap<VsiPlayer, ImmutableSet<ServerShipInternal>> c();

	@Override
	public ImmutableMap<VsiPlayer, ImmutableSet<ServerShipInternal>> vtil$getPlayersToTrackedShips() {
		return this.c();
	}

	@Inject(method = "deleteShip(Lorg/valkyrienskies/core/api/ships/ServerShip;)V", at = @At("HEAD"), remap = false)
	public void deleteShip$head(final ServerShip ship, final CallbackInfo ci) {
		ShipDataStorage.onShipRemoved(ship.getId());
	}

	@WrapOperation(
		method = M_postTick + "()V",
		at = @At(
			value = "INVOKE",
			target = JDESC + "deleteShip(Lorg/valkyrienskies/core/api/ships/ServerShip;)V"
		),
		remap = false
	)
	public void postTick$deleteShip(final org.valkyrienskies.core.impl.shadow.Er self, final ServerShip ship, final Operation<Void> operation) {
		// Hope VS won't have two deleteShip invoke sites in the future
		if (!Config.recycleEmptyShips) {
			operation.call(self, ship);
			return;
		}
		final ShipAllocator allocator = ShipAllocator.getCurrent();
		allocator.putShip(ship);
	}

	@WrapOperation(
		method = M_postTick + "()V",
		at = @At(
			value = "INVOKE",
			target = "Ljava/lang/Iterable;iterator()Ljava/util/Iterator;",
			ordinal = 0
		),
		slice = @Slice(
			from = @At(
				value = "FIELD",
				target = JDESC + "allShips:Lorg/valkyrienskies/core/internal/ships/VsiMutableQueryableShipData;",
				opcode = Opcodes.GETFIELD,
				ordinal = 1
			)
		),
		remap = false
	)
	public Iterator<ServerShip> postTick$createLoadedShips(
		final Iterable<ServerShip> ships,
		final Operation<Iterator<ServerShip>> operation
	) {
		return new ShipAllocator.SafeShipIterator(operation.call(ships));
	}
}
