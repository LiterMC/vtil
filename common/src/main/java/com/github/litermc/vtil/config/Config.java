package com.github.litermc.vtil.config;

public final class Config {
	/**
	 * TODO
	 * Not defined in the regular config file.
	 * Enable unstable shadow mixins. This has to be true for all unstable features to work.
	 */
	public static boolean enableShadowMixins = !false;

	/**
	 * Reuse deleted ship's chunks to assemble new ships when possible.
	 * Warn: Compatible issues may occur if enabled, only report issues when this is set to false.
	 */
	public static boolean reuseShipChunks = false;

	/**
	 * Recycle ships that no longer contains any blocks.
	 */
	public static boolean recycleEmptyShips = true;

	private Config() {}
}
