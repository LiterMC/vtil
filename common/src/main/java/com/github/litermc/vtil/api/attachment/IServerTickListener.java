package com.github.litermc.vtil.api.attachment;

import net.minecraft.server.level.ServerLevel;

import org.valkyrienskies.core.api.ships.LoadedServerShip;

public interface IServerTickListener {
	void onServerTick(ServerLevel level, LoadedServerShip ship);
}
