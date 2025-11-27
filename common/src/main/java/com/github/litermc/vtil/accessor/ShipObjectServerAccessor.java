package com.github.litermc.vtil.accessor;

import com.github.litermc.vtil.api.attachment.IServerTickListener;

import org.valkyrienskies.core.impl.game.ships.ShipData;

import java.util.Collection;

public interface ShipObjectServerAccessor {
	static final String JDESC = "Lorg/valkyrienskies/core/impl/shadow/Eq;";
	static final String M_applyAttachmentInterfaces = "";

	void vtil$initDefaultAttachments();
	Collection<IServerTickListener> vtil$getServerTickListeners();
	ShipData vtil$getShipData();
	<T> T vtil$removeAttachment(Class<T> clazz);
}
