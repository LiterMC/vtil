package com.github.litermc.vtil.compat.create;

import com.github.litermc.vtil.accessor.ContraptionHolder;
import com.github.litermc.vtil.accessor.ControlledContraptionEntityAccessor;
import com.github.litermc.vtil.api.assemble.IMoveable;
import com.github.litermc.vtil.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import java.util.ArrayList;
import java.util.List;

public class MoveableIControlContraption implements IMoveable<List<AbstractContraptionEntity>> {
	public static final MoveableIControlContraption INSTANCE = new MoveableIControlContraption();

	private MoveableIControlContraption() {}

	@Override
	public List<AbstractContraptionEntity> beforeMove(final ServerLevel level, final BlockPos origin, final BlockPos target) {
		final BlockEntity be = level.getBlockEntity(origin);
		if (be instanceof final ContraptionHolder holder) {
			return holder.vtil$clearContraptions();
		}
		return null;
	}

	@Override
	public void afterMove(final ServerLevel level, final BlockPos origin, final BlockPos target, List<AbstractContraptionEntity> data) {
		if (data == null) {
			return;
		}
		final BlockEntity be = level.getBlockEntity(target);
		final Vec3i offset = target.subtract(origin);
		data = data.stream().map((entity) -> moveContraptionEntity(level, entity, offset)).toList();
		if (be instanceof final ContraptionHolder holder) {
			holder.vtil$restoreContraptions(data);
		}
	}

	public static AbstractContraptionEntity moveContraptionEntity(final ServerLevel level, final AbstractContraptionEntity entity, final Vec3i offset) {
		if (entity == null) {
			return null;
		}
		final Contraption contraption = entity.getContraption();

		final List<Pair<Entity, Integer>> passengers = new ArrayList<>(entity.getPassengers().size());
		entity.getPassengers().forEach((passenger) -> {
			final Integer seat = contraption.getSeatMapping().get(passenger.getUUID());
			if (seat != null) {
				passengers.add(new Pair<>(passenger, seat));
			}
		});
		entity.ejectPassengers();

		contraption.anchor = contraption.anchor.offset(offset);
		entity.setPos(entity.position().add(offset.getX(), offset.getY(), offset.getZ()));
		if (entity instanceof final ControlledContraptionEntityAccessor ccea) {
			ccea.vtil$setControllerPos(ccea.vtil$getControllerPos().offset(offset));
		}

		final AbstractContraptionEntity newEntity = (AbstractContraptionEntity) (entity.getType().create(level));
		newEntity.restoreFrom(entity);

		passengers.forEach((passenger) -> newEntity.addSittingPassenger(passenger.left(), passenger.right()));

		entity.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
		level.addFreshEntity(newEntity);
		return newEntity;
	}
}
