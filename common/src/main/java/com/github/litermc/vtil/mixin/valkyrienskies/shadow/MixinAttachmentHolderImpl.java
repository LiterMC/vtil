package com.github.litermc.vtil.mixin.valkyrienskies.shadow;

import com.github.litermc.vtil.accessor.AttachmentHolderAccessor;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

@Mixin(org.valkyrienskies.core.impl.shadow.CA.class)
public abstract class MixinAttachmentHolderImpl implements AttachmentHolderAccessor {
	@Shadow(remap = false)
	@Final
	private ReentrantLock b;
	@Shadow(remap = false)
	ObjectNode c;
	@Shadow(remap = false)
	@Final
	private Set<String> d;
	@Shadow(remap = false)
	@Final
	private Map<Class<?>, ?> e;
	@Shadow(remap = false)
	@Final
	private Map<String, String> f;

	@Unique
	private ReentrantLock getLock() {
		return this.b;
	}

	@Unique
	private ObjectNode getDataStorage() {
		return this.c;
	}

	@Unique
	private void setDataStorage(final ObjectNode dataStorage) {
		this.c = dataStorage;
	}

	@Unique
	private Set<String> getRemovedAttachments() {
		return this.d;
	}

	@Unique
	private Map<Class<?>, ?> getAttachments() {
		return this.e;
	}

	@Unique
	private Map<String, String> getInvaildAttachments() {
		return this.f;
	}

	@Override
	public Set<Class<?>> vtil$getAttachmentKeys() {
		this.getLock().lock();
		try {
			return Set.copyOf(this.getAttachments().keySet());
		} finally {
			this.getLock().unlock();
		}
	}

	@Override
	public void vtil$clear() {
		this.getLock().lock();
		try {
			this.setDataStorage(this.getDataStorage().deepCopy().removeAll()); // TODO: maybe inject <init> and save ObjectMapper instead?
			this.getRemovedAttachments().clear();
			this.getAttachments().clear();
			this.getInvaildAttachments().clear();
		} finally {
			this.getLock().unlock();
		}
	}
}
