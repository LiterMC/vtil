package com.github.litermc.vtil.accessor;

import org.valkyrienskies.core.apigame.constraints.VSConstraint;

import java.util.Collection;

public interface ShipObjectServerWorldAccessor {
	Collection<Integer> vtil$getConstraintIds(long shipId);
	Collection<VSConstraint> vtil$getConstraints(long shipId);	
}
