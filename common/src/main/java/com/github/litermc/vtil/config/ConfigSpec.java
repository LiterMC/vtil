package com.github.litermc.vtil.config;

import com.github.litermc.vtil.Constants;
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
				.comment(
					"Unstable features.\n" +
					"If you met any issues with VS, please disable unstable features first before reporting to VS or other mods' repo."
				)
				.push("unstable");

			{
				builder
					.comment("Ship recycle settings")
					.push("recycle");

				REUSE_SHIP_CHUNKS = builder
					.comment(
						"Reuse deleted ship's chunks to assemble new ships when possible.\n" +
						"Warn: Compatible issues may occur if enabled, only report issues when this is set to false."
					)
					.define("reuse_ship_chunks", Config.reuseShipChunks);

				RECYCLE_EMPTY_SHIPS = builder
					.comment("Recycle ships that no longer contains any blocks.")
					.define("recycle_empty_ships", Config.recycleEmptyShips);

				builder.pop();
			}

			builder.pop();
		}
		serverSpec = builder.build(ConfigSpec::syncServer);
	}

	public static void syncServer(Path path) {
		if (Config.enableShadowMixins) {
			Config.reuseShipChunks = REUSE_SHIP_CHUNKS.get();
			Constants.LOG.info("vtil.reuse_ship_chunks = {}", Config.reuse_ship_chunks);
			Config.recycleEmptyShips = RECYCLE_EMPTY_SHIPS.get();
		} else {
			Config.reuseShipChunks = false;
			Config.recycleEmptyShips = false;
		}
	}

	public static void syncClient(Path path) {
	}
}
