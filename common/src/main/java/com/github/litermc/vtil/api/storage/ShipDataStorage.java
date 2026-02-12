package com.github.litermc.vtil.api.storage;

import com.github.litermc.vtil.Constants;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import org.valkyrienskies.core.api.ships.PhysShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class ShipDataStorage extends SavedData {
	private static final String DATA_NAME = Constants.MOD_ID + "_ShipAdditionalDatas";
	private static volatile ShipDataStorage INSTANCE = null;

	private final Map<Long, Holder> holders = new ConcurrentHashMap<>();

	private ShipDataStorage() {}

	private ShipDataStorage(final CompoundTag shipsData) {
		for (final String shipIdHex : shipsData.getAllKeys()) {
			final long shipId = Long.parseUnsignedLong(shipIdHex, 16);
			this.holders.put(shipId, this.new Holder(shipId, shipsData.getCompound(shipIdHex)));
		}
	}

	public static Holder get(final Ship ship) {
		if (ship instanceof ServerShip || ship instanceof PhysShip) {
			return get(ship.getId());
		}
		throw new IllegalArgumentException("Unexpected ship " + ship.getClass().getName() + " must be either ServerShip or PhysShip");
	}

	public static Holder get(final long shipId) {
		final ShipDataStorage instance = INSTANCE;
		if (instance == null) {
			throw new IllegalStateException("ShipDataStorage is not initialized. Is overworld loaded?");
		}
		return instance.holders.computeIfAbsent(shipId, (shipId0) -> instance.new Holder(shipId0, new CompoundTag()));
	}

	/**
	 * module-private
	 */
	public static void onShipRemoved(final long shipId) {
		final ShipDataStorage instance = INSTANCE;
		if (instance == null) {
			return;
		}
		instance.holders.remove(shipId);
	}

	/**
	 * module-private
	 */
	public static void onOverworldLoad(final ServerLevel overworld) {
		if (overworld == null) {
			INSTANCE = null;
			return;
		}
		INSTANCE = overworld.getDataStorage().computeIfAbsent(
			(data) -> new ShipDataStorage(data),
			() -> new ShipDataStorage(),
			DATA_NAME
		);
	}

	@Override
	public boolean isDirty() {
		return true;
	}

	@Override
	public CompoundTag save(final CompoundTag data) {
		this.holders.forEach((shipId, holder) -> {
			final CompoundTag tag = new CompoundTag();
			holder.save(tag);
			if (!tag.isEmpty()) {
				data.put(Long.toHexString(shipId), tag);
			}
		});
		return data;
	}

	public final class Holder {
		private final long shipId;
		private final CompoundTag oldData;
		private final Map<Class<? extends IShipAdditionalData>, IShipAdditionalData> storages = new ConcurrentHashMap<>();

		private Holder(final long shipId, final CompoundTag oldData) {
			this.shipId = shipId;
			this.oldData = oldData;
		}

		public <T extends IShipAdditionalData> T get(final Class<T> clazz) {
			final T storage = (T) this.storages.get(clazz);
			if (storage != null) {
				return storage;
			}
			if (!this.oldData.contains(clazz.getName())) {
				return null;
			}
			validateDataClass(clazz);
			return (T) this.storages.computeIfAbsent(clazz, (clazz0) -> {
				final T newStorage = construct((Class<T>) clazz0);
				newStorage.load(this.oldData.getCompound(clazz0.getName()));
				return newStorage;
			});
		}

		public <T extends IShipAdditionalData> T getOrCreate(final Class<T> clazz) {
			final T storage = (T) this.storages.get(clazz);
			if (storage != null) {
				return storage;
			}
			validateDataClass(clazz);
			return (T) this.storages.computeIfAbsent(clazz, (clazz0) -> {
				final T newStorage = construct((Class<T>) clazz0);
				final String className = clazz0.getName();
				if (this.oldData.contains(className)) {
					newStorage.load(this.oldData.getCompound(className));
				}
				return newStorage;
			});
		}

		public <T extends IShipAdditionalData> T getOrCreate(final Class<T> clazz, final Supplier<T> supplier) {
			final T storage = (T) this.storages.get(clazz);
			if (storage != null) {
				return storage;
			}
			validateDataClass(clazz);
			return (T) this.storages.computeIfAbsent(clazz, (clazz0) -> {
				final T newStorage;
				final String className = clazz0.getName();
				if (this.oldData.contains(className)) {
					newStorage = construct((Class<T>) clazz0);
					newStorage.load(this.oldData.getCompound(className));
				} else {
					newStorage = supplier.get();
				}
				return newStorage;
			});
		}

		public <T extends IShipAdditionalData> void put(final T storage) {
			validateDataClass(storage.getClass());
			this.storages.put(storage.getClass(), storage);
		}

		public <T extends IShipAdditionalData> T remove(final Class<T> clazz) {
			return (T) this.storages.remove(clazz);
		}

		public void save(final CompoundTag data) {
			for (final String className : this.oldData.getAllKeys()) {
				data.put(className, this.oldData.get(className));
			}
			this.storages.forEach((clazz, d) -> {
				final CompoundTag tag = new CompoundTag();
				d.save(tag);
				data.put(clazz.getName(), tag);
			});
		}
	}

	public static <T extends IShipAdditionalData> T construct(final Class<T> clazz) {
			try {
				return clazz.getConstructor().newInstance();
			} catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
				throw new AssertionError(e);
			}
	}

	private static void validateDataClass(final Class<? extends IShipAdditionalData> clazz) throws IllegalArgumentException {
		if (clazz.getEnclosingClass() != null) {
			throw new IllegalArgumentException("Storage class " + clazz.getName() + " must be a top-level class");
		}
		if (!Modifier.isFinal(clazz.getModifiers())) {
			throw new IllegalArgumentException("Storage class " + clazz.getName() + " must be a final class");
		}
		try {
			clazz.getConstructor();
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException("Storage class " + clazz.getName() + " missing public no‑argument constructor!", e);
		}
	}
}
