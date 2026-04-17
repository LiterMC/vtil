package com.github.litermc.vtil.mixin.valkyrienskies.shadow;

import com.github.litermc.vtil.accessor.JointManagerAccessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.valkyrienskies.core.internal.joints.VSJoint;

import java.util.Set;

@Mixin(org.valkyrienskies.core.impl.shadow.Ek.class)
public abstract class MixinJointManager implements JointManagerAccessor {
	@Shadow(remap = false)
	protected abstract VSJoint c(int id);

	@Override
	public VSJoint vtil$getJointById(int id) {
		return this.c(id);
	}

	@Shadow(remap = false)
	protected abstract Set<Integer> b(long shipId);

	@Override
	public Set<Integer> vtil$getShipJoints(long shipId) {
	    return this.b(shipId);
	}
}
