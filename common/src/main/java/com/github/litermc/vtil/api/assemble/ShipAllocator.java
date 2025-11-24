package com.github.litermc.vtil.api.assemble;

import com.github.litermc.vtil.Constants;
import com.github.litermc.vtil.accessor.ShipObjectServerWorldAccessor;
import com.github.litermc.vtil.config.Config;
import com.github.litermc.vtil.platform.PlatformHelper;
import com.github.litermc.vtil.util.LevelUtil;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MutableClassToInstanceMap;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.joml.primitives.AABBic;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.QueryableShipData;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.apigame.ShipTeleportData;
import org.valkyrienskies.core.apigame.world.IPlayer;
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore;
import org.valkyrienskies.core.impl.game.ShipTeleportDataImpl;
import org.valkyrienskies.core.impl.game.ships.ShipData;
import org.valkyrienskies.core.impl.game.ships.ShipObjectServer;
import org.valkyrienskies.core.impl.game.ships.ShipObjectServerWorld;
import org.valkyrienskies.core.impl.networking.impl.PacketShipRemove;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.LongStream;

public final class ShipAllocator extends SavedData {
	private static final String DATA_NAME = Constants.MOD_ID + "_AllocatedShips";
	private static final String ALL_CACHED_SHIPS_TAG = "AllCachedShips";
	private static final String CACHED_SHIPS_TAG = "CachedShips";
	private static final Vector3dc SECURE_SHIP_STORAGE = new Vector3d(1e8, -1e8, 1e8);
	private static final Vector3ic SECURE_SHIP_STORAGEI = new Vector3i((int) (1e8), (int) (-1e8), (int) (1e8));
	private static final double SECURE_SHIP_SCALE = 1e-6;
	private static final Quaterniondc ZERO_QUATD = new Quaterniond();
	private static final Vector3dc ZERO_VEC3D = new Vector3d();
	public static final String REUSABLE_SHIP_SLUG_PREFIX = "+reuse+";

	private final MinecraftServer server;
	private final ServerShipWorldCore shipWorld;
	private final LongOpenHashSet avaliableShips = new LongOpenHashSet();
	private final LongOpenHashSet pendingShips = new LongOpenHashSet();

	private ShipAllocator(final MinecraftServer server) {
		this.server = server;
		this.shipWorld = VSGameUtilsKt.getShipObjectWorld(server);
	}

	/**
	 * Get the allocator instance of a server.
	 *
	 * @param server A minecraft server, must not be null.
	 * @return allocator instance for the server
	 */
	public static ShipAllocator get(final MinecraftServer server) {
		final ServerLevel overworld = server.overworld();
		return overworld.getDataStorage().computeIfAbsent((data) -> load(server, data), () -> new ShipAllocator(server), DATA_NAME);
	}

	/**
	 * Get the allocator instance for the current server.
	 *
	 * @return allocator instance for the current server, or {@code null} if there is no active server.
	 */
	public static ShipAllocator getCurrent() {
		final MinecraftServer server = PlatformHelper.get().getCurrentServer();
		return server == null ? null : get(server);
	}

	public static ShipAllocator load(final MinecraftServer server, final CompoundTag data) {
		final ShipAllocator allocator = new ShipAllocator(server);
		final QueryableShipData<ServerShip> shipStorage = allocator.shipWorld.getAllShips();
		for (final long id : data.getLongArray(CACHED_SHIPS_TAG)) {
			final ServerShip ship = shipStorage.getById(id);
			if (ship != null) {
				allocator.avaliableShips.add(id);
			}
		}
		return allocator;
	}

	@Override
	public CompoundTag save(final CompoundTag data) {
		data.putLongArray(
			CACHED_SHIPS_TAG,
			LongStream.concat(this.avaliableShips.longStream(), this.pendingShips.longStream())
				.filter(this.shipWorld.getAllShips()::contains)
				.toArray()
		);
		return data;
	}

	/**
	 * Check if a ship is in reallocate cache
	 *
	 * @param shipId The ship's ID
	 * @return whether or not the ship is stored in the cache
	 */
	public boolean contains(final long shipId) {
		return this.avaliableShips.contains(shipId) || this.pendingShips.contains(shipId);
	}

	/**
	 * Put a ship into the reuse cache.
	 * The ship instance should not be used after.
	 *
	 * @return {@code true} if the ship is successfully put into the cache, or {@code false} if ship is directly deleted.
	 */
	public boolean putShip(final ServerShip ship) {
		final long shipId = ship.getId();
		final ServerLevel level = LevelUtil.getLevel(ship.getChunkClaimDimension());
		ship.setSlug(REUSABLE_SHIP_SLUG_PREFIX + shipId);
		ship.setStatic(true);

		clearShip(this.shipWorld, level, ship);

		if (!Config.reuseShipChunks) {
			this.shipWorld.deleteShip(ship);
			return false;
		}

		final ServerShip shipData = this.shipWorld.getAllShips().getById(shipId);
		if (shipData != null) {
			final ArrayList<IPlayer> players = new ArrayList<>(8);
			((ShipObjectServerWorld) (this.shipWorld)).getPlayersToTrackedShips().forEach((player, tracking) -> {
				if (tracking.contains(shipData)) {
					players.add(player);
				}
			});
			if (!players.isEmpty()) {
				((ShipObjectServerWorldAccessor) (this.shipWorld)).vtil$getSimplePackets().sendToClients(
					new PacketShipRemove(List.of(shipId)),
					players.toArray(new IPlayer[players.size()])
				);
			}
		}

		final Vector3i center = ship.getChunkClaim().getCenterBlockCoordinates(VSGameUtilsKt.getYRange(level), new Vector3i());
		level.setBlock(new BlockPos(center.x, center.y, center.z), Blocks.BARRIER.defaultBlockState(), Block.UPDATE_NONE);

		final ShipTeleportData teleportData = new ShipTeleportDataImpl(SECURE_SHIP_STORAGE, ZERO_QUATD, ZERO_VEC3D, ZERO_VEC3D, null, SECURE_SHIP_SCALE);
		this.shipWorld.teleportShip(ship, teleportData);

		Constants.LOG.debug("ShipAllocator: Caching ship {} in {}", shipId, ship.getChunkClaimDimension());
		this.avaliableShips.add(shipId);
		this.pendingShips.remove(shipId);
		this.setDirty();
		return true;
	}

	private ServerShip getShip(final long shipId) {
		final ServerShip ship = this.shipWorld.getLoadedShips().getById(shipId);
		if (ship != null) {
			return ship;
		}
		return this.shipWorld.getAllShips().getById(shipId);
	}

	/**
	 * Remove a ship from avaliableShips and return it
	 *
	 * @return the ship, or {@code null} if no reusable ship.
	 */
	private ServerShip pollCachedShip() {
		if (!Config.reuseShipChunks) {
			return null;
		}
		final LongIterator iterator = this.avaliableShips.longIterator();
		while (iterator.hasNext()) {
			this.setDirty();
			final long shipId = iterator.nextLong();
			iterator.remove();
			final ServerShip ship = this.getShip(shipId);
			if (ship != null) {
				Constants.LOG.debug("ShipAllocator: Reusing ship {} from {}", shipId, ship.getChunkClaimDimension());
				return ship;
			}
			Constants.LOG.debug("ShipAllocator: Ignored not existing ship {}", shipId);
		}
		return null;
	}

	/**
	 * Allocate a reusable ship from storage or create a new one.
	 *
	 * @return The allocated ship
	 */
	public ServerShipHolder allocShip() {
		final ServerShip ship = this.pollCachedShip();
		if (ship == null) {
			final ServerLevel level = this.server.overworld();
			final ServerShip newShip = this.shipWorld.createNewShipAtBlock(SECURE_SHIP_STORAGEI, false, SECURE_SHIP_SCALE, VSGameUtilsKt.getDimensionId(level));
			final Vector3i center = newShip.getChunkClaim().getCenterBlockCoordinates(VSGameUtilsKt.getYRange(level), new Vector3i());
			level.setBlock(new BlockPos(center.x, center.y, center.z), Blocks.BARRIER.defaultBlockState(), Block.UPDATE_NONE);
			return this.new ServerShipHolder(newShip);
		}
		this.pendingShips.add(ship.getId());
		return this.new ServerShipHolder(ship);
	}

	private static void clearShip(final ServerShipWorldCore world, final ServerLevel level, final ServerShip ship) {
		final BlockState AIR = Blocks.AIR.defaultBlockState();
		ship.setTransformProvider(null);
		MutableClassToInstanceMap<Object> attachments = null;
		if (ship instanceof final ShipData shipData) {
			attachments = shipData.getPersistentAttachedData();
		} else if (ship instanceof final ShipObjectServer shipObject) {
			attachments = shipObject.getShipData().getPersistentAttachedData();
		}
		if (attachments != null) {
			for (final Class<?> clazz : List.copyOf(attachments.keySet())) {
				ship.saveAttachment(clazz, null);
			}
		}
		for (final Integer cid : Set.copyOf(((ShipObjectServerWorldAccessor) (world)).vtil$getConstraintIds(ship.getId()))) {
			world.removeConstraint(cid);
		}
		// TODO: remove disabledCollisionPairs but it is obfuscated
		final AABBic box = ship.getShipAABB();
		if (box == null) {
			return;
		}
		for (final BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
			level.setBlock(pos, AIR, Block.UPDATE_KNOWN_SHAPE);
		}
	}

	/**
	 * ServerShipHolder holds a server ship instance.
	 * User may not modify anything of the ship before invoke {@link ServerShipHolder#consume}.
	 * Otherwise, the change maybe resetted.
	 */
	public class ServerShipHolder {
		private final ServerShip ship;
		private boolean consumed = false;

		private ServerShipHolder(final ServerShip ship) {
			this.ship = ship;
		}

		/**
		 * Get underlying ship instance.
		 *
		 * @return the ship instance
		 */
		public ServerShip getShipData() {
			return this.ship;
		}

		/**
		 * Check if the ship is either consumed or canceled.
		 *
		 * @return if either {@link consume} or {@link cancel} has been called
		 */
		public boolean isConsumed() {
			return this.consumed;
		}

		public ServerShip consume(final ShipTeleportData target) {
			return this.consume(null, target);
		}

		/**
		 * Remove the ship from cache, and teleport the ship to target destination.
		 * The ship will also be set to non-static.
		 *
		 * @param slug New ship slug.
		 * @param target The target state of the ship. New dimension and scale must not be {@code null}.
		 * @return ship instance
		 * @throws IllegalArgumentException If either new dimension or new scale is {@code null}.
		 * @throws IllegalStateException If the holder is already consumed.
		 */
		public ServerShip consume(final String slug, final ShipTeleportData target) {
			if (target.getNewDimension() == null) {
				throw new IllegalArgumentException("New dimension cannot be null");
			}
			if (target.getNewScale() == null) {
				throw new IllegalArgumentException("New scale cannot be null");
			}
			if (this.consumed) {
				throw new IllegalStateException("ship is already consumed");
			}
			this.consumed = true;

			final long shipId = this.ship.getId();
			final ServerShip ship = ShipAllocator.this.getShip(shipId);

			clearShip(ShipAllocator.this.shipWorld, LevelUtil.getLevel(ship.getChunkClaimDimension()), ship);
			ship.setStatic(false);
			ship.setSlug(slug);

			ShipAllocator.this.shipWorld.teleportShip(ship, target);
			ShipAllocator.this.avaliableShips.remove(shipId);
			ShipAllocator.this.pendingShips.remove(shipId);
			ShipAllocator.this.setDirty();
			return ship;
		}

		/**
		 * Mark the ship as ready to be used in cache again.
		 *
		 * @throws IllegalStateException If the holder is already consumed.
		 */
		public void cancel() {
			if (this.consumed) {
				throw new IllegalStateException("ship is already consumed");
			}
			this.consumed = true;
			ShipAllocator.this.putShip(this.ship);
		}
	}

	/**
	 * SafeShipIterable can be used to wrap an {@link Iterable} to filter out recycled ships.
	 */
	public static class SafeShipIterable<T extends Ship> implements Iterable<T> {
		private final Iterable<T> ships;
		private final MinecraftServer server;

		public SafeShipIterable(final Iterable<T> ships) {
			this(ships, null);
		}

		public SafeShipIterable(final Iterable<T> ships, final MinecraftServer server) {
			this.ships = ships;
			this.server = server;
		}

		@Override
		public Iterator<T> iterator() {
			return new SafeShipIterator(this.ships.iterator(), this.server);
		}
	}

	/**
	 * SafeShipIterable can be used to wrap an {@link Iterator} to filter out recycled ships.
	 */
	public static class SafeShipIterator<T extends Ship> implements Iterator<T> {
		private final Iterator<T> ships;
		private T nextShip = null;
		private final ShipAllocator allocator;

		public SafeShipIterator(final Iterator<T> ships) {
			this(ships, null);
		}

		public SafeShipIterator(final Iterator<T> ships, final MinecraftServer server) {
			this.ships = ships;
			this.allocator = server == null ? ShipAllocator.getCurrent() : ShipAllocator.get(server);
		}

		protected boolean isShipVaild(final Ship ship) {
			if (this.allocator != null) {
				return !this.allocator.contains(ship.getId());
			}
			final String slug = ship.getSlug();
			if (slug != null && slug.startsWith(REUSABLE_SHIP_SLUG_PREFIX)) {
				return false;
			}
			return true;
		}

		@Override
		public boolean hasNext() {
			if (this.nextShip != null) {
				return true;
			}
			while (this.ships.hasNext()) {
				final T ship = this.ships.next();
				if (this.isShipVaild(ship)) {
					this.nextShip = ship;
					return true;
				}
			}
			return false;
		}

		@Override
		public T next() {
			if (this.nextShip == null) {
				throw new NoSuchElementException();
			}
			final T ship = this.nextShip;
			this.nextShip = null;
			return ship;
		}
	}
}
