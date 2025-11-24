package com.github.litermc.vtil.accessor;

import com.github.litermc.vtil.api.attachment.IServerTickListener;

import java.util.Collection;

public interface ShipObjectServerAccessor {
	Collection<IServerTickListener> vtil$getServerTickListeners();
}
