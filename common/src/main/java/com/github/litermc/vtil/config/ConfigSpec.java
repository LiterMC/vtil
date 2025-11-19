package com.github.litermc.vtil.config;

import com.github.litermc.vtil.platform.PlatformHelper;

import java.nio.file.Path;

public final class ConfigSpec {
	public static final ConfigFile serverSpec;

	public static final ConfigFile.Value<Boolean> REUSE_SHIP_CHUNKS;
	public static final ConfigFile.Value<Boolean> RECYCLE_EMPTY_SHIPS;

	private ConfigSpec() {}

	static {
		final ConfigFile.Builder builder = PlatformHelper.get().createConfigBuilder();
		{
			builder
				.comment("Ship recycle settings")
				.push("recycle");

			REUSE_SHIP_CHUNKS = builder
				.comment("Reuse deleted ship's chunks to assemble new ships when possible.")
				.define("reuse_ship_chunks", Config.reuseShipChunks);

			RECYCLE_EMPTY_SHIPS = builder
				.comment("Recycle ships that no longer contains any blocks.")
				.define("recycle_empty_ships", Config.recycleEmptyShips);

			builder.pop();
		}

		serverSpec = builder.build(ConfigSpec::syncServer);
	}

	public static void syncServer(Path path) {
		Config.reuseShipChunks = REUSE_SHIP_CHUNKS.get();
		Config.recycleEmptyShips = RECYCLE_EMPTY_SHIPS.get();
	}

	public static void syncClient(Path path) {
	}
}
