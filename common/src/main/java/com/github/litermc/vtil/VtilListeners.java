package com.github.litermc.vtil;

import com.github.litermc.vtil.api.assemble.MoveApi;
import com.github.litermc.vtil.util.TaskUtil;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public final class VtilListeners {
	private VtilListeners() {}

	public static void onModInit() {
		VtilRegistry.register();
		MoveApi.registerDefaultMovers();
	}

	public static void onServerLevelLoad(final ServerLevel level) {
	}

	public static void onServerLevelUnload(final ServerLevel level) {
	}

	public static void preServerTick(final MinecraftServer server) {
		TaskUtil.preServerTick();
	}

	public static void postServerTick(final MinecraftServer server) {
		TaskUtil.postServerTick();
	}
}
