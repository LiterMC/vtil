package com.github.litermc.vtil.mixin.create;

import com.github.litermc.vtil.accessor.OrientedContraptionEntityAccessor;
import com.github.litermc.vtil.api.entity.ISpecialTeleportLogicEntity;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.OrientedContraptionEntity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;
import java.util.UUID;

@Pseudo
@Mixin(OrientedContraptionEntity.class)
public abstract class MixinOrientedContraptionEntity extends AbstractContraptionEntity implements OrientedContraptionEntityAccessor, ISpecialTeleportLogicEntity {
	@Unique
	private boolean isTeleporting = false;
	@Unique
	private Map<UUID, Integer> seatMapping = null;

	protected MixinOrientedContraptionEntity() {
		super(null, null);
	}

	@Override
	public Map<UUID, Integer> vtil$getSeatMapping() {
		return this.seatMapping;
	}

	@Override
	public void beforeDimentionalTeleport() {
		this.isTeleporting = true;
		this.seatMapping = Map.copyOf(this.getContraption().getSeatMapping());
	}

	@Override
	public void afterDimentionalTeleport(final ISpecialTeleportLogicEntity old) {
		this.isTeleporting = false;
		if (old == null) {
			this.seatMapping = null;
			return;
		}
		final Map<UUID, Integer> seatMapping = ((OrientedContraptionEntityAccessor)(old)).vtil$getSeatMapping();
		this.seatMapping = null;
		this.getPassengers().forEach((passenger) -> {
			final Integer seat = seatMapping.get(passenger.getUUID());
			if (seat != null) {
				this.addSittingPassenger(passenger, seat);
			}
		});
	}

	@WrapOperation(
		method = "stopRiding",
		at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/contraptions/OrientedContraptionEntity;disassemble()V", remap = false),
		remap = true
	)
	public void stopRiding$disassemble(final OrientedContraptionEntity entity, final Operation<Void> operation) {
		if (!this.isTeleporting) {
			operation.call(entity);
		}
	}
}
