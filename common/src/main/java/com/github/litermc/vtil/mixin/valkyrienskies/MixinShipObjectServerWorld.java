package com.github.litermc.vtil.mixin.valkyrienskies;

import com.github.litermc.vtil.accessor.ShipObjectServerWorldAccessor;
import com.github.litermc.vtil.accessor.VSNetworkingAccessor;
import com.github.litermc.vtil.api.assemble.ShipAllocator;
import com.github.litermc.vtil.config.Config;

import org.valkyrienskies.core.api.ships.QueryableShipData;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.impl.game.ships.ShipObjectServerWorld;
import org.valkyrienskies.core.impl.networking.VSNetworking;
import org.valkyrienskies.core.impl.networking.simple.SimplePacketNetworking;
import org.valkyrienskies.core.internal.joints.VSJoint;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

@Mixin(ShipObjectServerWorld.class)
public class MixinShipObjectServerWorld implements ShipObjectServerWorldAccessor {
	@Shadow
	@Final
	private Map<Integer, VSConstraint> constraints;

	@Shadow
	@Final
	private Map<Long, Set<Integer>> shipIdToConstraints;

	@Unique
	private VSNetworking networking;

	@Inject(method = "<init>", at = @At("RETURN"), remap = false)
	public void init(
		final @Coerce Object allShips,
		final @Coerce Object chunkAllocators,
		final @Coerce Object loadManager,
		final VSNetworking networking,
		final @Coerce Object blockTypes,
		final @Coerce Object dimensionInfo,
		final CallbackInfo ci
	) {
		this.networking = networking;
	}

	@Override
	public Collection<Integer> vtil$getConstraintIds(final long shipId) {
		final Set<Integer> conIds = this.shipIdToConstraints.get(shipId);
		if (conIds == null) {
			return Collections.emptySet();
		}
		return conIds;
	}

	@Override
	public Collection<VSConstraint> vtil$getConstraints(final long shipId) {
		final Set<Integer> conIds = this.shipIdToConstraints.get(shipId);
		if (conIds == null) {
			return Collections.emptyList();
		}
		return conIds.stream().map(this.constraints::get).toList();
	}

	@Override
	public SimplePacketNetworking vtil$getSimplePackets() {
		return ((VSNetworkingAccessor) ((Object) (this.networking))).vtil$getSimplePackets();
	}

	@WrapOperation(
		method = "postTick",
		at = @At(
			value = "INVOKE",
			target = "Lorg/valkyrienskies/core/impl/game/ships/ShipObjectServerWorld;deleteShip(Lorg/valkyrienskies/core/api/ships/ServerShip;)V"
		),
		remap = false
	)
	public void postTick$deleteShip(final ShipObjectServerWorld self, final ServerShip ship, final Operation<Void> operation) {
		// Hope VS won't have two deleteShip invoke sites in the future
		if (!Config.recycleEmptyShips) {
			operation.call(self, ship);
			return;
		}
		final ShipAllocator allocator = ShipAllocator.getCurrent();
		allocator.putShip(ship);
	}

	@WrapOperation(
		method = "postTick",
		at = @At(
			value = "INVOKE",
			target = "Ljava/lang/Iterable;iterator()Ljava/util/Iterator;",
			ordinal = 0
		),
		slice = @Slice(
			from = @At(
				value = "INVOKE",
				target = "Lorg/valkyrienskies/core/impl/game/ships/ShipObjectServerWorld;getAllShips()Lorg/valkyrienskies/core/api/ships/QueryableShipData;",
				ordinal = 0
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
