package com.github.litermc.vtil.mixin.valkyrienskies;

import com.github.litermc.vtil.accessor.ShipObjectServerWorldAccessor;
import com.github.litermc.vtil.api.assemble.ShipAllocator;
import com.github.litermc.vtil.config.Config;

import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.apigame.constraints.VSConstraint;
import org.valkyrienskies.core.impl.game.ships.ShipObjectServerWorld;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Mixin(ShipObjectServerWorld.class)
public class MixinShipObjectServerWorld implements ShipObjectServerWorldAccessor {
	@Shadow(remap = false)
	@Final
	private Map<Integer, VSConstraint> constraints;

	@Shadow(remap = false)
	@Final
	private Map<Long, Set<Integer>> shipIdToConstraints;

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
}
