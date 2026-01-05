package com.github.litermc.vtil.mixin.create;

import com.github.litermc.vtil.accessor.AbstractContraptionEntityAccessor;
import com.github.litermc.vtil.api.entity.ISpecialTeleportLogicEntity;

import net.minecraft.world.entity.Entity;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Map;
import java.util.UUID;

@Pseudo
@Mixin(AbstractContraptionEntity.class)
public abstract class MixinAbstractContraptionEntity extends Entity implements AbstractContraptionEntityAccessor, ISpecialTeleportLogicEntity {
	@Unique
	protected boolean isTeleporting = false;
	@Unique
	private Map<UUID, Integer> seatMapping = null;

	protected MixinAbstractContraptionEntity() {
		super(null, null);
	}

	@Shadow(remap = false)
	public abstract Contraption getContraption();

	@Shadow(remap = false)
	public abstract void addSittingPassenger(Entity passenger, int seatIndex);

	@Override
	public Map<UUID, Integer> vtil$getSeatMapping() {
		return this.seatMapping;
	}

	@Override
	public void beforeDimentionalTeleport() {
		this.isTeleporting = true;
		final Contraption contraption = this.getContraption();
		this.seatMapping = contraption == null ? null : Map.copyOf(contraption.getSeatMapping());
	}

	@Override
	public void afterDimentionalTeleport(final ISpecialTeleportLogicEntity old) {
		this.isTeleporting = false;
		if (old == null) {
			this.seatMapping = null;
			return;
		}
		final Map<UUID, Integer> seatMapping = ((AbstractContraptionEntityAccessor) (old)).vtil$getSeatMapping();
		this.seatMapping = null;
		if (seatMapping == null) {
			return;
		}
		this.getPassengers().forEach((passenger) -> {
			final Integer seat = seatMapping.get(passenger.getUUID());
			if (seat != null) {
				this.addSittingPassenger(passenger, seat);
			}
		});
	}
}
