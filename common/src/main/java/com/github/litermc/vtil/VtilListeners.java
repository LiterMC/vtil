package com.github.litermc.vtil;

import com.github.litermc.vtil.accessor.ShipObjectServerAccessor;
import com.github.litermc.vtil.api.assemble.MoveApi;
import com.github.litermc.vtil.api.attachment.IServerTickListener;
import com.github.litermc.vtil.api.storage.ShipDataStorage;
import com.github.litermc.vtil.util.LevelUtil;
import com.github.litermc.vtil.util.ShipQuerier;
import com.github.litermc.vtil.util.TaskUtil;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.ValkyrienSkiesMod;

import java.util.List;

public final class VtilListeners {
	private VtilListeners() {}

	public static void onModInit() {
		VtilRegistry.register();
		MoveApi.registerDefaultMovers();
		ValkyrienSkiesMod.getApi().getPhysTickEvent().on((event) -> TaskUtil.onPhysTick(event.getWorld()));
	}

	public static void onServerLevelLoad(final ServerLevel level) {
		if (level.dimension() == Level.OVERWORLD) {
			ShipDataStorage.onOverworldLoad(level);
		}
		LevelUtil.onServerLevelLoad(level);
	}

	public static void onServerLevelUnload(final ServerLevel level) {
		if (level.dimension() == Level.OVERWORLD) {
			ShipDataStorage.onOverworldLoad(null);
		}
		LevelUtil.onServerLevelUnload(level);
		TaskUtil.onServerLevelUnload(level);
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
		ShipQuerier.postServerTick(server);
		TaskUtil.postServerTick();
	}
}
