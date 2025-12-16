package com.github.litermc.vtil.mixin.valkyrienskies.shadow;

import com.github.litermc.vtil.api.attachment.IPermanentAttachment;

import org.spongepowered.asm.mixin.Mixin;

@Mixin(org.valkyrienskies.core.impl.shadow.Eh.class)
public abstract class MixinDragManager implements IPermanentAttachment {
}
