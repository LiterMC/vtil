package com.github.litermc.vtil.api.connectivity;

import com.github.litermc.vtil.accessor.JointManagerAccessor;
import com.github.litermc.vtil.accessor.ShipObjectServerWorldAccessor;
import com.github.litermc.vtil.platform.PlatformHelper;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.internal.joints.VSJoint;
import org.valkyrienskies.core.internal.world.VsiPhysLevel;
import org.valkyrienskies.core.internal.world.VsiServerShipWorld;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Experimental API, use with caution
 */
public final class ShipConnectivityApi {
	private ShipConnectivityApi() {}

	public static boolean isConnectedToGround(final ServerShip ship) {
		final VsiServerShipWorld shipWorld = VSGameUtilsKt.getShipObjectWorld(PlatformHelper.get().getCurrentServer());
		final long dimId = shipWorld.getDimensionToGroundBodyIdImmutable().get(ship.getChunkClaimDimension());
		final JointManagerAccessor jointManager = ((ShipObjectServerWorldAccessor) shipWorld).vtil$getJointManager();
		return isConnectedTo(new JointManagerWrapper(jointManager), ship.getId(), dimId);
	}

	public static void getAllConnectedShips(final ServerShip ship, final LongSet result) {
		final VsiServerShipWorld shipWorld = VSGameUtilsKt.getShipObjectWorld(PlatformHelper.get().getCurrentServer());
		final JointManagerAccessor jointManager = ((ShipObjectServerWorldAccessor) shipWorld).vtil$getJointManager();
		getAllConnectedShips(new JointManagerWrapper(jointManager), ship.getId(), result);
	}

	public static Set<LoadedServerShip> getAllConnectedShipsAndSelf(final ServerShip ship) {
		final VsiServerShipWorld shipWorld = VSGameUtilsKt.getShipObjectWorld(PlatformHelper.get().getCurrentServer());
		final JointManagerAccessor jointManager = ((ShipObjectServerWorldAccessor) shipWorld).vtil$getJointManager();
		final Set<LoadedServerShip> ships = new HashSet<>();
		getAllConnectedShipsAndSelf(new JointManagerWrapper(jointManager), ship.getId()).longStream()
			.mapToObj(shipWorld.getLoadedShips()::getById)
			.filter(Objects::nonNull)
			.forEach(ships::add);
		return ships;
	}

	public static void getAllConnectedShipsAndSelf(final ServerShip ship, final LongSet result) {
		final VsiServerShipWorld shipWorld = VSGameUtilsKt.getShipObjectWorld(PlatformHelper.get().getCurrentServer());
		final JointManagerAccessor jointManager = ((ShipObjectServerWorldAccessor) shipWorld).vtil$getJointManager();
		getAllConnectedShipsAndSelf(new JointManagerWrapper(jointManager), ship.getId(), result);
	}

	public static boolean isConnectedToGround(final VsiPhysLevel world, final long shipId) {
		final VsiServerShipWorld shipWorld = VSGameUtilsKt.getShipObjectWorld(PlatformHelper.get().getCurrentServer());
		final long dimId = shipWorld.getDimensionToGroundBodyIdImmutable().get(world.getDimension());
		return isConnectedTo(new PhysLevelJointManager(world), shipId, dimId);
	}

	public static void getAllConnectedShips(final VsiPhysLevel world, final long shipId, final LongSet result) {
		getAllConnectedShips(new PhysLevelJointManager(world), shipId, result);
	}

	public static Set<PhysShip> getAllConnectedShipsAndSelf(final VsiPhysLevel world, final long shipId) {
		return getAllConnectedShipsAndSelf(new PhysLevelJointManager(world), shipId).longStream()
			.mapToObj(world::getShipById)
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());
	}

	@Deprecated
	public static void getAllConnectedShipsAndSelf(final VsiPhysLevel world, final long shipId, final Set<PhysShip> result) {
		getAllConnectedShipsAndSelf(new PhysLevelJointManager(world), shipId).longStream()
			.mapToObj(world::getShipById)
			.filter(Objects::nonNull)
			.forEach(result::add);
	}

	public static void getAllConnectedShipsAndSelf(final VsiPhysLevel world, final long shipId, final LongSet result) {
		getAllConnectedShipsAndSelf(new PhysLevelJointManager(world), shipId, result);
	}

	private static boolean isConnectedTo(final IJointManager manager, final long shipId, final long otherId) {
		final Collection<Integer> joints = manager.getShipJoints(shipId);
		for (final Integer jointId : joints) {
			final VSJoint joint = manager.getJointById(jointId);
			if (joint == null) {
				continue;
			}
			final Long id0 = joint.getShipId0();
			if (id0 != null && id0.longValue() == otherId) {
				return true;
			}
			final Long id1 = joint.getShipId1();
			if (id1 != null && id1.longValue() == otherId) {
				return true;
			}
		}
		return false;
	}

	private static void getAllConnectedShips(final IJointManager manager, final long shipId, final LongSet result) {
		final boolean hadSelf = result.contains(shipId);
		getAllConnectedShipsAndSelf(manager, shipId, result);
		if (!hadSelf) {
			result.remove(shipId);
		}
	}

	private static LongSet getAllConnectedShipsAndSelf(final IJointManager manager, final long shipId) {
		final LongSet ships = new LongOpenHashSet();
		getAllConnectedShipsAndSelf(manager, shipId, ships);
		return ships;
	}

	private static void getAllConnectedShipsAndSelf(final IJointManager manager, final long shipId, final LongSet result) {
		result.add(shipId);
		final Collection<Integer> joints = manager.getShipJoints(shipId);
		if (joints.isEmpty()) {
			return;
		}
		for (final Integer jointId : joints) {
			final VSJoint joint = manager.getJointById(jointId);
			if (joint == null) {
				continue;
			}
			final Long id0 = joint.getShipId0();
			if (id0 != null) {
				addConnectedShips(manager, id0, result);
			}
			final Long id1 = joint.getShipId1();
			if (id1 != null) {
				addConnectedShips(manager, id1, result);
			}
		}
	}

	private static void addConnectedShips(
		final IJointManager manager,
		final long shipId,
		final LongSet result
	) {
		if (!result.add(shipId)) {
			return;
		}
		final Collection<Integer> joints = manager.getShipJoints(shipId);
		for (final Integer jointId : joints) {
			final VSJoint joint = manager.getJointById(jointId);
			if (joint == null) {
				continue;
			}
			final Long id0 = joint.getShipId0();
			if (id0 != null) {
				addConnectedShips(manager, id0, result);
			}
			final Long id1 = joint.getShipId1();
			if (id1 != null) {
				addConnectedShips(manager, id1, result);
			}
		}
	}

	private interface IJointManager {
		VSJoint getJointById(int id);
		Collection<Integer> getShipJoints(long shipId);
	}

	private record JointManagerWrapper(JointManagerAccessor manager) implements IJointManager {
		@Override
		public VSJoint getJointById(int id) {
			return this.manager.vtil$getJointById(id);
		}

		@Override
		public Collection<Integer> getShipJoints(long shipId) {
			return this.manager.vtil$getShipJoints(shipId);
		}
	}

	private record PhysLevelJointManager(VsiPhysLevel world) implements IJointManager {
		@Override
		public VSJoint getJointById(int id) {
			return this.world.getJointById(id);
		}

		@Override
		public Collection<Integer> getShipJoints(long shipId) {
			return this.world.getJointsFromShip(shipId);
		}
	}
}
