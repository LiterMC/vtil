package com.github.litermc.vtil;

import com.github.litermc.vtil.accessor.ShipObjectServerAccessor;
import com.github.litermc.vtil.api.assemble.MoveApi;
import com.github.litermc.vtil.api.attachment.IServerTickListener;
import com.github.litermc.vtil.util.LevelUtil;
import com.github.litermc.vtil.util.TaskUtil;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.List;

public final class VtilListeners {
	private VtilListeners() {}

	public static void onModInit() {
		VtilRegistry.register();
		MoveApi.registerDefaultMovers();
	}

	public static void onServerLevelLoad(final ServerLevel level) {
		LevelUtil.onServerLevelLoad(level);
	}

	public static void onServerLevelUnload(final ServerLevel level) {
		LevelUtil.onServerLevelUnload(level);
	}

	public static void preServerTick(final MinecraftServer server) {
		TaskUtil.preServerTick();
		for (final LoadedServerShip ship : VSGameUtilsKt.getShipObjectWorld(server).getLoadedShips()) {
			if (!(ship instanceof final ShipObjectServerAccessor shipAccessor)) {
				continue;
			}
			for (final IServerTickListener listener : List.copyOf(shipAccessor.vtil$getServerTickListeners())) {
				listener.onServerTick(LevelUtil.getLevel(ship.getChunkClaimDimension()), ship);
			}
		}
	}

	public static void postServerTick(final MinecraftServer server) {
		TaskUtil.postServerTick();
	}
}
