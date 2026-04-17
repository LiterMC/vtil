package com.github.litermc.vtil.accessor;

import org.valkyrienskies.core.internal.joints.VSJoint;

import java.util.Set;

public interface JointManagerAccessor {
	static final String JDESC = "Lorg/valkyrienskies/core/impl/shadow/Ek;";

	VSJoint vtil$getJointById(int id);
	Set<Integer> vtil$getShipJoints(long shipId);
}
