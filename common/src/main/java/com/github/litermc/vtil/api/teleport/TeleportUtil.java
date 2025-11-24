package com.github.litermc.vtil.api.teleport;

import com.github.litermc.vtil.api.entity.ISpecialTeleportLogicEntity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import org.joml.Quaterniondc;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.ServerShipTransformProvider;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.impl.game.ShipTeleportDataImpl;
import org.valkyrienskies.core.internal.ShipTeleportData;
import org.valkyrienskies.core.internal.world.VsiServerShipWorld;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.List;

public class TeleportUtil {
	/**
	 * Teleport a ship with velocity and omega.
	 *
	 * @param ship Teleporting ship
	 * @param data Teleport data
	 */
	public static void teleportShip(final LoadedServerShip ship, final TeleportData data) {
		final ServerLevel level = data.level();
		final String dimension = VSGameUtilsKt.getDimensionId(level);
		final VsiServerShipWorld world = VSGameUtilsKt.getShipObjectWorld(level);

		final long id = ship.getId();

		final Vector3dc newPos = data.newPos();
		final Quaterniondc rotation = data.rotation();
		final Vector3dc velocity = data.velocity();
		final Vector3dc omega = data.omega();

		final ShipTeleportData teleportData = new ShipTeleportDataImpl(newPos, rotation, velocity, omega, dimension, null);
		world.teleportShip(ship, teleportData);
		if (velocity.lengthSquared() != 0 || omega.lengthSquared() != 0) {
			final ServerShipTransformProvider oldProvider = ship.getTransformProvider();
			ship.setTransformProvider(new ServerShipTransformProvider() {
				@Override
				public NextTransformAndVelocityData provideNextTransformAndVelocity(final ShipTransform prevTransform, final ShipTransform transform) {
					final LoadedServerShip ship2 = world.getLoadedShips().getById(id);
					if (!prevTransform.getPositionInWorld().equals(transform.getPositionInWorld()) || !prevTransform.getShipToWorldRotation().equals(transform.getShipToWorldRotation())) {
						ship2.setTransformProvider(oldProvider);
						return null;
					}
					if (ship2.getVelocity().lengthSquared() == 0 && ship2.getOmega().lengthSquared() == 0) {
						return new NextTransformAndVelocityData(transform, velocity, omega);
					}
					return null;
				}
			});
		}
	}

	/**
	 * Teleport an entity to another dimension with its passengers
	 *
	 * @param <T>      The entity's type
	 * @param entity   The entity going to teleport
	 * @param newLevel Target level
	 * @param newPos   Target position
	 * @return Teleported entity, or {@code null} if teleportation failed.
	 */
	public static <T extends Entity> T teleportEntity(final T entity, final ServerLevel newLevel, final Vec3 newPos) {
		final Vec3 oldPos = entity.position();
		final List<Entity> passengers = List.copyOf(entity.getPassengers());
		if (entity instanceof final ISpecialTeleportLogicEntity specialEntity) {
			specialEntity.beforeDimentionalTeleport();
		}
		for (final Entity p : passengers) {
			if (p instanceof final ISpecialTeleportLogicEntity specialEntity) {
				specialEntity.beforeDimentionalTeleport();
			}
		}
		final T newEntity;
		if (entity instanceof final ServerPlayer player) {
			player.teleportTo(newLevel, newPos.x, newPos.y, newPos.z, player.getYRot(), player.getXRot());
			newEntity = entity;
		} else {
			newEntity = (T) entity.getType().create(newLevel);
			if (newEntity == null) {
				if (entity instanceof final ISpecialTeleportLogicEntity specialEntity) {
					specialEntity.afterDimentionalTeleport(null);
				}
				return null;
			}
			entity.ejectPassengers();
			newEntity.restoreFrom(entity);
			newEntity.moveTo(newPos.x, newPos.y, newPos.z, newEntity.getYRot(), newEntity.getXRot());
			newEntity.setYHeadRot(entity.getYHeadRot());
			newEntity.setYBodyRot(entity.getVisualRotationYInDegrees());
			newLevel.addDuringTeleport(newEntity);
			entity.setRemoved(Entity.RemovalReason.CHANGED_DIMENSION);
		}
		for (final Entity p : passengers) {
			final Entity newPassenger = teleportEntity(p, newLevel, p.position().subtract(oldPos).add(newPos));
			if (newPassenger != null) {
				newPassenger.startRiding(newEntity, true);
			}
		}
		if (newEntity instanceof final ISpecialTeleportLogicEntity specialEntity) {
			specialEntity.afterDimentionalTeleport((ISpecialTeleportLogicEntity) (entity));
		}
		return newEntity;
	}

	public record TeleportData(ServerLevel level, Vector3dc newPos, Quaterniondc rotation, Vector3dc velocity, Vector3dc omega) {}
}
