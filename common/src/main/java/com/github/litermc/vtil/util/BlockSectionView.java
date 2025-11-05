package com.github.litermc.vtil.util;

import net.minecraft.core.BlockPos;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;

public final class BlockSectionView extends AbstractSet<BlockPos> {
	private final BlockPos min;
	private final BlockPos max;
	private final int count;
	private final Iterable<BlockPos> iterable;

	public BlockSectionView(final BlockPos pos1, final BlockPos pos2) {
		final int minX = Math.min(pos1.getX(), pos2.getX());
		final int minY = Math.min(pos1.getY(), pos2.getY());
		final int minZ = Math.min(pos1.getZ(), pos2.getZ());
		final int maxX = Math.max(pos1.getX(), pos2.getX());
		final int maxY = Math.max(pos1.getY(), pos2.getY());
		final int maxZ = Math.max(pos1.getZ(), pos2.getZ());
		this.min = new BlockPos(
			Math.min(pos1.getX(), pos2.getX()),
			Math.min(pos1.getY(), pos2.getY()),
			Math.min(pos1.getZ(), pos2.getZ())
		);
		this.max = new BlockPos(
			Math.max(pos1.getX(), pos2.getX()),
			Math.max(pos1.getY(), pos2.getY()),
			Math.max(pos1.getZ(), pos2.getZ())
		);
		this.count = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
		this.iterable = BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ);
	}

	@Override
	public int size() {
		return this.count;
	}

	@Override
	public Iterator<BlockPos> iterator() {
		return this.iterable.iterator();
	}

	@Override
	public boolean contains(Object obj) {
		if (!(obj instanceof final BlockPos pos)) {
			return false;
		}
		return
			this.min.getX() <= pos.getX() && pos.getX() <= this.max.getX() &&
			this.min.getY() <= pos.getY() && pos.getY() <= this.max.getY() &&
			this.min.getZ() <= pos.getZ() && pos.getZ() <= this.max.getZ();
	}

	@Override
	public boolean containsAll(final Collection<?> collections) {
		if (!(collections instanceof final BlockSectionView view)) {
			return super.containsAll(collections);
		}
		return
			this.min.getX() <= view.min.getX() && this.min.getY() <= view.min.getY() && this.min.getZ() <= view.min.getZ() &&
			this.max.getX() >= view.max.getX() && this.max.getY() >= view.max.getY() && this.max.getZ() >= view.max.getZ();
	}
}
