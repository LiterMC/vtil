package com.github.litermc.vtil.util;

import net.minecraft.server.level.ServerLevel;

import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.concurrent.ConcurrentHashMap;

public final class LevelUtil {
	private LevelUtil() {}

	private static final ConcurrentHashMap<String, ServerLevel> ID_TO_LEVEL_CACHE = new ConcurrentHashMap<>();

	public static void onServerLevelLoad(final ServerLevel level) {
		ID_TO_LEVEL_CACHE.put(VSGameUtilsKt.getDimensionId(level), level);
	}

	public static void onServerLevelUnload(final ServerLevel level) {
		ID_TO_LEVEL_CACHE.remove(VSGameUtilsKt.getDimensionId(level), level);
	}

	public static ServerLevel getLevel(final String dimId) {
		final ServerLevel level = ID_TO_LEVEL_CACHE.get(dimId);
		if (level == null) {
			throw new IllegalStateException("Level " + dimId + " is not loaded.");
		}
		return level;
	}
}
