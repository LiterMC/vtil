package com.github.litermc.vtil.accessor;

import java.util.Set;

public interface AttachmentHolderAccessor {
	static final String JDESC = "Lorg/valkyrienskies/core/impl/shadow/CA;";

	Set<Class<?>> vtil$getAttachmentKeys();
	void vtil$clear();
}
