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
	 */
	public static boolean reuseShipChunks = true;

	/**
	 * Recycle ships that no longer contains any blocks.
	 */
	public static boolean recycleEmptyShips = true;

	private Config() {}
}
