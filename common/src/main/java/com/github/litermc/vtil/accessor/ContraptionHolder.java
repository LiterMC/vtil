package com.github.litermc.vtil.accessor;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;

import java.util.List;

public interface ContraptionHolder {
	List<AbstractContraptionEntity> vtil$clearContraptions();

	void vtil$restoreContraptions(List<AbstractContraptionEntity> contraptions);
}
