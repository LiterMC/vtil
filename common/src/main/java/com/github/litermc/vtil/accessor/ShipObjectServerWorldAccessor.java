package com.github.litermc.vtil.accessor;

import org.valkyrienskies.core.apigame.constraints.VSConstraint;
import org.valkyrienskies.core.impl.networking.simple.SimplePacketNetworking;

import java.util.Collection;

public interface ShipObjectServerWorldAccessor {
	SimplePacketNetworking vtil$getSimplePackets();
	Collection<Integer> vtil$getConstraintIds(long shipId);
	Collection<VSConstraint> vtil$getConstraints(long shipId);	
}
