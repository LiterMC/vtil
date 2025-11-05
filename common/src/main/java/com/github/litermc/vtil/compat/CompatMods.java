package com.github.litermc.vtil.compat;

import com.github.litermc.vtil.platform.PlatformHelper;

public enum CompatMods {
	CREATE("create");

	private final String modId;
	private Boolean loaded = null;

	private CompatMods(final String modId) {
		this.modId = modId;
	}

	public String getId() {
		return this.modId;
	}

	public boolean isLoaded() {
		if (this.loaded == null) {
			this.loaded = PlatformHelper.get().isModLoaded(this.getId());
		}
		return this.loaded;
	}
}
