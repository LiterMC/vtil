package com.github.litermc.vtil.api.connectivity;

import com.github.litermc.vtil.platform.PlatformHelper;

import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.internal.joints.VSJoint;
import org.valkyrienskies.core.internal.world.VsiPhysLevel;
import org.valkyrienskies.core.internal.world.VsiServerShipWorld;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Experimental API, use with caution
 */
public final class ShipConnectivityApi {
	private ShipConnectivityApi() {}

	public static boolean isConnectedToGround(final VsiPhysLevel world, final long shipId) {
		final VsiServerShipWorld shipWorld = VSGameUtilsKt.getShipObjectWorld(PlatformHelper.get().getCurrentServer());
		final Collection<Integer> joints = world.getJointsFromShip(shipId);
		final long dimId = shipWorld.getDimensionToGroundBodyIdImmutable().get(world.getDimension());
		for (final Integer jointId : joints) {
			final VSJoint joint = world.getJointById(jointId);
			final Long id0 = joint.getShipId0();
			if (id0 != null && id0.longValue() == dimId) {
				return true;
			}
			final Long id1 = joint.getShipId1();
			if (id1 != null && id0.longValue() == dimId) {
				return true;
			}
		}
		return false;
	}

	public static Set<PhysShip> getAllConnectedShipsAndSelf(final VsiPhysLevel world, final long shipId) {
		final PhysShip startShip = world.getShipById(shipId);
		if (startShip == null) {
			return Set.of();
		}
		final Collection<Integer> joints = world.getJointsFromShip(shipId);
		if (joints.isEmpty()) {
			return Set.of(startShip);
		}
		final Set<PhysShip> ships = new HashSet<>(joints.size() + 1);
		ships.add(startShip);
		for (final Integer jointId : joints) {
			final VSJoint joint = world.getJointById(jointId);
			final Long id0 = joint.getShipId0();
			if (id0 != null) {
				addConnectedShips(world, world.getShipById(id0), ships);
			}
			final Long id1 = joint.getShipId1();
			if (id1 != null) {
				addConnectedShips(world, world.getShipById(id1), ships);
			}
		}
		return ships;
	}

	private static void addConnectedShips(
		final VsiPhysLevel world,
		final PhysShip ship,
		final Set<PhysShip> result
	) {
		if (ship == null || !result.add(ship)) {
			return;
		}
		final Collection<Integer> joints = world.getJointsFromShip(ship.getId());
		for (final Integer jointId : joints) {
			final VSJoint joint = world.getJointById(jointId);
			final Long id0 = joint.getShipId0();
			if (id0 != null) {
				addConnectedShips(world, world.getShipById(id0), result);
			}
			final Long id1 = joint.getShipId1();
			if (id1 != null) {
				addConnectedShips(world, world.getShipById(id1), result);
			}
		}
	}
}
