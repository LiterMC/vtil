package com.github.litermc.vtil.api.storage;

import net.minecraft.nbt.CompoundTag;

/**
 * Data classes must extends this interface to be use with {@link ShipDataStorage}.
 * Any data classes must also define a public no‑argument constructor.
 */
public interface IShipAdditionalData {
	/**
	 * load will be invoked when a new instance just created and have saved serialized data.
	 * @param data the serialized data, should never be modified.
	 */
	void load(CompoundTag data);

	/**
	 * save will be invoked when data needs to be serialized and saved.
	 * @param data the serialized data storage. Should be modified in the method only.
	 */
	void save(CompoundTag data);
}
