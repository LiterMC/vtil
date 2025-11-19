package com.github.litermc.vtil.config;

public final class Config {
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
