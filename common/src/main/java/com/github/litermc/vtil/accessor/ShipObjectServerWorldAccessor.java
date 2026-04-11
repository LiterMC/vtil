package com.github.litermc.vtil.accessor;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.valkyrienskies.core.impl.api.ServerShipInternal;
import org.valkyrienskies.core.impl.game.ships.ShipData;
import org.valkyrienskies.core.internal.ships.VsiMutableQueryableShipData;
import org.valkyrienskies.core.internal.world.VsiPlayer;

public interface ShipObjectServerWorldAccessor {
	static final String JDESC = "Lorg/valkyrienskies/core/impl/shadow/Et;";
	static final String M_postTick = "i";

	VsiMutableQueryableShipData<ShipData> vtil$getAllShips();
	ImmutableMap<VsiPlayer, ImmutableSet<ServerShipInternal>> vtil$getPlayersToTrackedShips();
}
