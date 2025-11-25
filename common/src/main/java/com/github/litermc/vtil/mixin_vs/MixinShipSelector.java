package com.github.litermc.vtil.mixin.valkyrienskies;

import com.github.litermc.vtil.api.assemble.ShipAllocator;

import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.command.ShipSelector;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ShipSelector.class)
public class MixinShipSelector {
	@ModifyArg(
		method = "select",
		at = @At(
			value = "INVOKE",
			target = "Lkotlin/collections/CollectionsKt;asSequence(Ljava/lang/Iterable;)Lkotlin/sequences/Sequence;"
		)
	)
	private Iterable<Ship> select$iterable(Iterable<Ship> ships) {
		return new ShipAllocator.SafeShipIterable(ships);
	}
}
