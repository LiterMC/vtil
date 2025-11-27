package com.github.litermc.vtil.mixin.valkyrienskies;

import com.github.litermc.vtil.api.assemble.ShipAllocator;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.internal.ships.VsiQueryableShipData;
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
			target = "Lorg/valkyrienskies/core/internal/ships/VsiQueryableShipData;getById(J)Lorg/valkyrienskies/core/api/ships/Ship;"
		),
		remap = false
	)
	public Ship dragEntitiesWithShips$getById(
		final VsiQueryableShipData query,
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
		}
		return operation.call(query, id);
	}
}
