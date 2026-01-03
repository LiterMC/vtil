package com.github.litermc.vtil.mixin.create;

import com.simibubi.create.content.contraptions.OrientedContraptionEntity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(OrientedContraptionEntity.class)
public abstract class MixinOrientedContraptionEntity extends MixinAbstractContraptionEntity {
	@WrapOperation(
		method = "stopRiding",
		at = @At(
			value = "INVOKE",
			target = "Lcom/simibubi/create/content/contraptions/OrientedContraptionEntity;disassemble()V",
			remap = false
		),
		remap = true
	)
	public void stopRiding$disassemble(final OrientedContraptionEntity entity, final Operation<Void> operation) {
		if (!this.isTeleporting) {
			operation.call(entity);
		}
	}
}
