package com.github.litermc.vtil.mixin.valkyrienskies;

import com.github.litermc.vtil.api.assemble.ShipAllocator;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import org.valkyrienskies.core.api.ships.QueryableShipData;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.util.EntityDragger;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityDragger.class)
public class MixinEntityDragger {
	@WrapOperation(
		method = "dragEntitiesWithShips",
		at = @At(
			value = "INVOKE",
			target = "Lorg/valkyrienskies/core/api/ships/QueryableShipData;getById(J)Lorg/valkyrienskies/core/api/ships/Ship;"
		),
		remap = false
	)
	public Ship dragEntitiesWithShips$getById(
		final QueryableShipData query,
		final long id,
		final Operation<Ship> operation,
		final @Local Entity entity
	) {
		final Level level = entity.level();
		if (level instanceof final ServerLevel serverLevel) {
			final ShipAllocator allocator = ShipAllocator.get(serverLevel.getServer());
			if (allocator != null && allocator.contains(id)) {
				return null;
			}
			return operation.call(query, id);
		}
		final Ship ship = operation.call(query, id);
		final String slug = ship.getSlug();
		if (slug != null && slug.startsWith(ShipAllocator.REUSABLE_SHIP_SLUG_PREFIX)) {
			return null;
		}
		return ship;
	}
}
