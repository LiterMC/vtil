package com.github.litermc.vtil.api.attachment;

/**
 * Experimental API, use with causion.
 *
 * The use for this interface is to mark an attachment to not be removed during ship reuse.
 */
public interface IPermanentAttachment {
	/**
	 * This method will be executed during a ship clean.
	 * The implementation should be idempotent, and should always produce same result.
	 */
	default void onShipClean() {}
}
