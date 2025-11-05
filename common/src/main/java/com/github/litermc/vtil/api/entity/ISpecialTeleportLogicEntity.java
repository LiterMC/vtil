package com.github.litermc.vtil.api.entity;

/**
 * ISpecialTeleportLogicEntity provides special logic for entity that switching dimension with Ship.
 */
public interface ISpecialTeleportLogicEntity {
	/**
	 * beforeDimentionalTeleport is invoked before the entity starts teleport to another dimension.
	 * It should cleanup any dimensional related stuff here.
	 * 
	 * {@link afterDimentionalTeleport} will always be invoked after, no matter if the teleportation succeed or not
	 */
	void beforeDimentionalTeleport();

	/**
	 * afterDimentionalTeleport is invoked after the teleport is finished.
	 * 
	 * @param oldEntity The old entity instance, or {@code null} if teleport has failed.
	 */
	void afterDimentionalTeleport(ISpecialTeleportLogicEntity oldEntity);
}
