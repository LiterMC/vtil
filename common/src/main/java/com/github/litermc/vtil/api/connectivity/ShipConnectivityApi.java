package com.github.litermc.vtil.api.connectivity;

import com.github.litermc.vtil.accessor.ShipObjectServerWorldAccessor;
import com.github.litermc.vtil.platform.PlatformHelper;

import org.valkyrienskies.core.api.ships.QueryableShipData;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.apigame.constraints.VSConstraint;
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore;
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

	public static Set<ServerShip> getAllConnectedShipsAndSelf(final long shipId) {
		final ServerShipWorldCore world = VSGameUtilsKt.getShipObjectWorld(PlatformHelper.get().getCurrentServer());
		final QueryableShipData<ServerShip> shipQuery = world.getAllShips();
		final ServerShip startShip = shipQuery.getById(shipId);
		final Collection<VSConstraint> constraints = ((ShipObjectServerWorldAccessor) (world)).vtil$getConstraints(shipId);
		if (constraints.isEmpty()) {
			return Set.of(startShip);
		}
		final Set<ServerShip> ships = new HashSet<>(constraints.size() + 1);
		ships.add(startShip);
		for (final VSConstraint constraint : constraints) {
			addConnectedShips(world, shipQuery, shipQuery.getById(constraint.getShipId0()), ships);
			addConnectedShips(world, shipQuery, shipQuery.getById(constraint.getShipId1()), ships);
		}
		return ships;
	}

	private static void addConnectedShips(
		final ServerShipWorldCore world,
		final QueryableShipData<ServerShip> shipQuery,
		final ServerShip ship,
		final Set<ServerShip> result
	) {
		if (!result.add(ship)) {
			return;
		}
		final Collection<VSConstraint> constraints = ((ShipObjectServerWorldAccessor) (world)).vtil$getConstraints(ship.getId());
		for (final VSConstraint constraint : constraints) {
			addConnectedShips(world, shipQuery, shipQuery.getById(constraint.getShipId0()), result);
			addConnectedShips(world, shipQuery, shipQuery.getById(constraint.getShipId1()), result);
		}
	}
}
