package com.github.litermc.vtil.util;

import net.minecraft.server.MinecraftServer;

import org.joml.primitives.AABBd;
import org.joml.primitives.AABBdc;
import org.joml.primitives.AABBic;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.QueryableShipData;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.internal.world.VsiPhysLevel;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public final class ShipQuerier {
	private static final Map<Long, ShipData> SHIP_DATAS = new ConcurrentHashMap<>();

	private ShipQuerier() {}

	public static void getIntersecting(
		final VsiPhysLevel world,
		final String dimensionId,
		final AABBdc box,
		final Predicate<PhysShip> tester,
		final List<PhysShip> result
	) {
		for (final PhysShip other : world.getAllPhysShips()) {
			final long otherId = other.getId();
			final ShipData data = SHIP_DATAS.get(otherId);
			if (data == null) {
				continue;
			}
			if (!dimensionId.equals(data.dimensionId())) {
				continue;
			}
			if (!tester.test(other)) {
				continue;
			}
			// TODO: cache ship boxes
			final AABBd otherBox = getShipWorldBox(other, data);
			if (!box.intersectsAABB(otherBox)) {
				continue;
			}
			result.add(other);
		}
	}

	public static String getShipDimension(final PhysShip ship) {
		final ShipData data = SHIP_DATAS.get(ship.getId());
		if (data == null) {
			return null;
		}
		return data.dimensionId();
	}

	public static AABBd getShipWorldBox(final PhysShip ship) {
		return getShipWorldBox(ship, SHIP_DATAS.get(ship.getId()));
	}

	private static AABBd getShipWorldBox(final PhysShip ship, final ShipData data) {
		final ShipTransform transform = ship.getTransform();
		if (data == null) {
			return new AABBd(transform.getPositionInWorld(), transform.getPositionInWorld());
		}
		final AABBic box = data.box();
		final AABBd worldBox = new AABBd(box.minX(), box.minY(), box.minZ(), box.maxX() + 1, box.maxY() + 1, box.maxZ() + 1);
		return worldBox.transform(transform.getShipToWorld());
	}

	public static void postServerTick(final MinecraftServer server) {
		final QueryableShipData<LoadedServerShip> loadedShips = VSGameUtilsKt.getShipObjectWorld(server).getLoadedShips();
		for (final LoadedServerShip ship : loadedShips) {
			final long id = ship.getId();
			final AABBic box = ship.getShipAABB();
			if (box == null) {
				SHIP_DATAS.remove(id);
				continue;
			}
			SHIP_DATAS.put(id, new ShipData(ship.getChunkClaimDimension(), box));
		}
		for (final Long id : SHIP_DATAS.keySet()) {
			if (!loadedShips.contains(id)) {
				SHIP_DATAS.remove(id);
			}
		}
	}

	private record ShipData(String dimensionId, AABBic box) {}
}
