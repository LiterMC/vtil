package com.github.litermc.vtil.api.assemble;

import com.github.litermc.vtil.compat.CompatMods;
import com.github.litermc.vtil.util.TaskUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Mth;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3i;
import org.joml.primitives.AABBd;
import org.joml.primitives.AABBic;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.ServerShipTransformProvider;
import org.valkyrienskies.core.api.ships.properties.ShipTransform;
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore;
import org.valkyrienskies.core.impl.game.ShipTeleportDataImpl;
import org.valkyrienskies.core.impl.game.ships.ShipTransformImpl;
import org.valkyrienskies.core.util.datastructures.DenseBlockPosSet;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import com.simibubi.create.content.contraptions.actors.seat.SeatEntity;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class AssembleApi {
	private static final Quaterniondc ZERO_QUATD = new Quaterniond();
	private static final Vector3dc ZERO_VEC3D = new Vector3d();

	private AssembleApi() {}

	/**
	 * Assemble a ship with given blockset.
	 * If target block set is on a ship, the rotation, velocity, scale, etc. will be extended.
	 *
	 * @param level    World the blocks are in.
	 * @param blocks   Block set to assemble, must contains at least one non-air block,
	 *                 and must all in the world or on the same ship.
	 * @return The instance for created ship.
	 */
	public static ServerShip createShip(
		final ServerLevel level,
		final Set<BlockPos> blocks
	) {
		final ShipAllocator allocator = ShipAllocator.get(level.getServer());
		final ServerShipWorldCore shipWorld = VSGameUtilsKt.getShipObjectWorld(level);
		final String levelId = VSGameUtilsKt.getDimensionId(level);
		final ServerShip rootShip;

		final AABBd blocksBox = new AABBd();
		{
			final BlockPos pos = blocks.iterator().next();
			final ServerShip rootShipLoaded = VSGameUtilsKt.getShipObjectManagingPos(level, pos);
			rootShip = rootShipLoaded != null ? rootShipLoaded : VSGameUtilsKt.getShipManagingPos(level, pos);
			blocksBox.setMin(pos.getX(), pos.getY(), pos.getZ()).setMax(pos.getX(), pos.getY(), pos.getZ());
		}
		for (final BlockPos pos : blocks) {
			blocksBox.union(pos.getX(), pos.getY(), pos.getZ());
		}
		blocksBox.maxX += 1;
		blocksBox.maxY += 1;
		blocksBox.maxZ += 1;

		final Vector3d worldCenterD = blocksBox.center(new Vector3d());
		final Vector3i worldCenter = new Vector3i(Mth.floor(worldCenterD.x()), Mth.floor(worldCenterD.y()), Mth.floor(worldCenterD.z()));
		final ServerShip ship = allocator.allocShip()
			.consume(new ShipTeleportDataImpl(worldCenterD, ZERO_QUATD, ZERO_VEC3D, ZERO_VEC3D, levelId, 1.0));
		final Vector3i shipCenter = ship.getChunkClaim().getCenterBlockCoordinates(VSGameUtilsKt.getYRange(level), new Vector3i());
		final Vector3i offset = shipCenter.sub(worldCenter, new Vector3i());
		final Map<BlockPos, BlockState> blockStates = new HashMap<>(blocks.size());
		final List<Entity> attachableEntities = new ArrayList<>();

		// get attachable entities
		for (final Entity entity : level.getEntities(null, new AABB(blocksBox.minX - 1, blocksBox.minY - 1, blocksBox.minZ - 1, blocksBox.maxX + 2, blocksBox.maxY + 2, blocksBox.maxZ + 2))) {
			if (entity instanceof final HangingEntity he) {
				final BlockPos hanging = he.getPos().relative(he.getDirection().getOpposite());
				if (blocks.contains(hanging)) {
					attachableEntities.add(entity);
				}
			} else if (CompatMods.CREATE.isLoaded()) {
				if (entity instanceof final SeatEntity seat) {
					if (blocks.contains(seat.blockPosition())) {
						attachableEntities.add(entity);
					}
				} else if (entity instanceof final SuperGlueEntity glue) {
					final AABB bb = glue.getBoundingBox();
					if (BlockPos.betweenClosedStream(bb).anyMatch(blocks::contains)) {
						attachableEntities.add(entity);
					}
				}
			}
		}

		moveBlocks(level, blocks, offset, blockStates);

		if (blockStates.isEmpty()) {
			// No block present
			allocator.putShip(ship);
			return null;
		}

		// move attachable entities
		for (final Entity entity : attachableEntities) {
			final Vec3 pos = entity.position();
			entity.setPos(pos.x + offset.x, pos.y + offset.y, pos.z + offset.z);
		}

		sendBlockUpdates(level, offset, blockStates);
		fixShipStatus(shipWorld, ship, new Vector3d(shipCenter), new Vector3d(worldCenter), rootShip);
		return ship;
	}

	/**
	 * Assemble a ship with given blockset.
	 * Async version will create ship across multiple ticks, but still do all operations on main thread.
	 * This method still need be invoked from main thread.
	 *
	 * @param level    World the blocks are in.
	 * @param blocks   Block set to assemble, must contains at least one non-air block,
	 *                 and must all in the world or on the same ship.
	 * @param maxDelay The max ticks to delay the ship assembly. When reached the timeout,
	 *                 ship chunks will forcibly be waited in the main server thread.
	 * @return A CompletableFuture that provides the instance for created ship.
	 */
	public static CompletableFuture<ServerShip> createShipAsync(
		final ServerLevel level,
		final Set<BlockPos> blocks,
		final int maxDelay
	) {
		final BlockState AIR = Blocks.AIR.defaultBlockState();
		final ShipAllocator allocator = ShipAllocator.get(level.getServer());
		final ServerShipWorldCore shipWorld = VSGameUtilsKt.getShipObjectWorld(level);
		final String levelId = VSGameUtilsKt.getDimensionId(level);
		final int chunkLevel = ChunkLevel.byStatus(ChunkStatus.EMPTY);
		final ServerChunkCache chunkCache = level.getChunkSource();
		final ServerShip rootShip;

		final AABBd blocksBox = new AABBd();
		{
			final BlockPos pos = blocks.iterator().next();
			final ServerShip rootShipLoaded = VSGameUtilsKt.getShipObjectManagingPos(level, pos);
			rootShip = rootShipLoaded != null ? rootShipLoaded : VSGameUtilsKt.getShipManagingPos(level, pos);
			blocksBox.setMin(pos.getX(), pos.getY(), pos.getZ()).setMax(pos.getX(), pos.getY(), pos.getZ());
		}
		for (final BlockPos pos : blocks) {
			blocksBox.union(pos.getX(), pos.getY(), pos.getZ());
		}
		blocksBox.maxX += 1;
		blocksBox.maxY += 1;
		blocksBox.maxZ += 1;

		final Vector3d worldCenterD = blocksBox.center(new Vector3d());
		final Vector3i worldCenter = new Vector3i(Mth.floor(worldCenterD.x()), Mth.floor(worldCenterD.y()), Mth.floor(worldCenterD.z()));
		final ShipAllocator.ServerShipHolder shipHolder = allocator.allocShip();
		final Vector3i shipCenter = shipHolder.getShipData().getChunkClaim().getCenterBlockCoordinates(VSGameUtilsKt.getYRange(level), new Vector3i());
		final Vector3i offset = shipCenter.sub(worldCenter, new Vector3i());

		final ChunkPos
			minChunk = new ChunkPos(
				SectionPos.posToSectionCoord(shipCenter.x - blocksBox.lengthX() / 2),
				SectionPos.posToSectionCoord(shipCenter.z - blocksBox.lengthZ() / 2)
			),
			maxChunk = new ChunkPos(
				SectionPos.posToSectionCoord(shipCenter.x + blocksBox.lengthX() / 2),
				SectionPos.posToSectionCoord(shipCenter.z + blocksBox.lengthZ() / 2)
			);
		final List<ChunkPos> neededChunks = ChunkPos.rangeClosed(minChunk, maxChunk).toList();
		// TODO: this may cause newly created ship missing collision
		for (final ChunkPos chunkPos : neededChunks) {
			chunkCache.updateChunkForced(chunkPos, true);
		}

		final CompletableFuture future = new CompletableFuture();

		final Runnable callback = new Runnable() {
			private int timeout = maxDelay;

			@Override
			public void run() {
				boolean allLoaded = true;
				for (final ChunkPos chunkPos : neededChunks) {
					if (chunkCache.getChunkNow(chunkPos.x, chunkPos.z) == null) {
						allLoaded = false;
						break;
					}
				}
				if (!allLoaded && this.timeout > 0) {
					this.timeout--;
					TaskUtil.queueTickEnd(this);
					return;
				}
				final ServerShip ship = shipHolder.consume(new ShipTeleportDataImpl(worldCenterD, ZERO_QUATD, ZERO_VEC3D, ZERO_VEC3D, levelId, 1.0));

				final Map<BlockPos, BlockState> blockStates = new HashMap<>(blocks.size());
				final List<Entity> attachableEntities = new ArrayList<>();

				// get attachable entities
				for (final Entity entity : level.getEntities(null, new AABB(blocksBox.minX - 1, blocksBox.minY - 1, blocksBox.minZ - 1, blocksBox.maxX + 2, blocksBox.maxY + 2, blocksBox.maxZ + 2))) {
					if (entity instanceof final HangingEntity he) {
						final BlockPos hanging = he.getPos().relative(he.getDirection().getOpposite());
						if (blocks.contains(hanging)) {
							attachableEntities.add(entity);
						}
					} else if (CompatMods.CREATE.isLoaded()) {
						if (entity instanceof final SeatEntity seat) {
							if (blocks.contains(seat.blockPosition())) {
								attachableEntities.add(entity);
							}
						} else if (entity instanceof final SuperGlueEntity glue) {
							final AABB bb = glue.getBoundingBox();
							if (BlockPos.betweenClosedStream(bb).anyMatch(blocks::contains)) {
								attachableEntities.add(entity);
							}
						}
					}
				}

				moveBlocks(level, blocks, offset, blockStates);

				if (blockStates.isEmpty()) {
					// No block present
					future.complete(null);
					return;
				}

				// move attachable entities
				for (final Entity entity : attachableEntities) {
					final Vec3 pos = entity.position();
					entity.setPos(pos.x + offset.x, pos.y + offset.y, pos.z + offset.z);
				}

				sendBlockUpdates(level, offset, blockStates);
				fixShipStatus(shipWorld, ship, new Vector3d(shipCenter), new Vector3d(worldCenter), rootShip);
				future.complete(ship);
			}
		};
		TaskUtil.queueTickEnd(callback);
		return future;
	}

	private static void moveBlocks(final ServerLevel level, final Set<BlockPos> blocks, final Vector3i offset, final Map<BlockPos, BlockState> blockStates) {
		final BlockState AIR = Blocks.AIR.defaultBlockState();
		for (final BlockPos pos : blocks) {
			final BlockPos target = pos.offset(offset.x, offset.y, offset.z);
			final BlockState oldState = level.getBlockState(pos);
			final boolean wasAir = oldState.isAir();

			final BlockEntity be = level.getBlockEntity(pos);
			IMoveable<?> moveableOld = MoveApi.getMover(be);
			if (moveableOld == null) {
				moveableOld = MoveApi.getMover(oldState.getBlock());
			}
			if (moveableOld != null) {
				moveableOld.beforeSaveForMove(level, pos, target);
			}

			final BlockState state = level.getBlockState(pos);
			if (wasAir && moveableOld == null && state == oldState) {
				continue;
			}

			blockStates.put(pos.immutable(), state);
			final CompoundTag nbt = level.getChunkAt(pos).getBlockEntityNbtForSaving(pos);
			if (nbt != null) {
				final BlockPos targetPos = pos.offset(offset.x, offset.y, offset.z);
				nbt.putInt("x", targetPos.getX());
				nbt.putInt("y", targetPos.getY());
				nbt.putInt("z", targetPos.getZ());
				level.getChunkAt(targetPos).setBlockEntityNbt(nbt);
			}

			final Object moveData = moveableOld != null ? moveableOld.beforeMove(level, pos, target) : null;

			level.removeBlockEntity(pos);

			// Note: Block.UPDATE_SUPPRESS_DROPS only works for Level.destroyBlock which drop the block's item form,
			// and it does not prevent contents from dropping.
			level.setBlock(pos, AIR, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_MOVE_BY_PISTON);

			level.setBlock(target, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_MOVE_BY_PISTON);
			IMoveable<?> moveableNew = MoveApi.getMover(level.getBlockEntity(target));
			if (moveableNew == null) {
				moveableNew = MoveApi.getMover(state.getBlock());
			}
			if (moveableNew != null) {
				((IMoveable) (moveableNew)).afterMove(level, pos, target, moveData);
			}
		}
	}

	private static void sendBlockUpdates(final ServerLevel level, final Vector3i offset, final Map<BlockPos, BlockState> blockStates) {
		final int MAX_BLOCK_UPDATE = 512 - 1;
		final int BLOCK_UPDATE_FLAGS = Block.UPDATE_NEIGHBORS | Block.UPDATE_MOVE_BY_PISTON;
		final BlockState AIR = Blocks.AIR.defaultBlockState();

		final DenseBlockPosSet tickedAirs = new DenseBlockPosSet();
		final BlockPos.MutableBlockPos airPos = new BlockPos.MutableBlockPos();
		for (final Map.Entry<BlockPos, BlockState> entry : blockStates.entrySet()) {
			final BlockPos pos = entry.getKey();
			final BlockState state = entry.getValue();
			final Block block = state.getBlock();
			final BlockPos targetPos = pos.offset(offset.x, offset.y, offset.z);
			level.blockUpdated(pos, block);
			state.updateIndirectNeighbourShapes(level, pos, BLOCK_UPDATE_FLAGS, MAX_BLOCK_UPDATE);
			AIR.updateNeighbourShapes(level, pos, BLOCK_UPDATE_FLAGS, MAX_BLOCK_UPDATE);
			level.onBlockStateChange(pos, state, AIR);

			level.blockUpdated(targetPos, block);
			state.updateNeighbourShapes(level, targetPos, BLOCK_UPDATE_FLAGS, MAX_BLOCK_UPDATE);
			state.updateIndirectNeighbourShapes(level, targetPos, BLOCK_UPDATE_FLAGS, MAX_BLOCK_UPDATE);
			level.onBlockStateChange(targetPos, AIR, state);

			for (final Direction dir : Direction.values()) {
				airPos.setWithOffset(targetPos, dir);
				if (blockStates.containsKey(airPos) || !tickedAirs.add(airPos.getX(), airPos.getY(), airPos.getZ())) {
					continue;
				}
				AIR.updateNeighbourShapes(level, airPos, BLOCK_UPDATE_FLAGS, MAX_BLOCK_UPDATE);
			}
		}
	}

	private static void fixShipStatus(
		final ServerShipWorldCore shipWorld,
		final ServerShip ship,
		final Vector3dc shipAnchor,
		final Vector3dc targetAnchor,
		final ServerShip rootShip
	) {
		// fix new ship's velocity and omega
		final String dimension = ship.getChunkClaimDimension();
		final Vector3d position = new Vector3d().set(ship.getTransform().getPositionInShip()).sub(shipAnchor).add(targetAnchor);
		final Quaterniond rotation = new Quaterniond();
		final Vector3d velocity = new Vector3d();
		final Vector3d omega = new Vector3d();
		final Vector3d scaling = new Vector3d(1);

		if (rootShip == null) {
			shipWorld.teleportShip(ship, new ShipTeleportDataImpl(position, rotation, velocity, omega, dimension, 1.0));
			return;
		}

		final ShipTransform selfTransform = rootShip.getTransform();
		selfTransform.getShipToWorld().transformPosition(position);
		rotation.set(selfTransform.getShipToWorldRotation());
		velocity.set(rootShip.getVelocity());
		omega.set(rootShip.getOmega());
		scaling.set(selfTransform.getShipToWorldScaling());

		// TODO: for some reason the reposition can only be correct after 3 physics ticks. Investigate why and find a solution.

		final ServerShipTransformProvider oldProvider = ship.getTransformProvider();
		ship.setTransformProvider(new ServerShipTransformProvider() {
			private int count = 0;

			@Override
			public NextTransformAndVelocityData provideNextTransformAndVelocity(final ShipTransform transform, final ShipTransform nextTransform) {
				this.count++;
				if (this.count <= 3) {
					return null;
				}
				ship.setTransformProvider(oldProvider);
				position.set(nextTransform.getPositionInShip()).sub(shipAnchor).add(targetAnchor);
				if (rootShip != null) {
					final ShipTransform selfTransform2 = rootShip.getTransform();
					selfTransform2.getShipToWorld().transformPosition(position);
					rotation.set(selfTransform2.getShipToWorldRotation());
					velocity.set(rootShip.getVelocity());
					omega.set(rootShip.getOmega());
					scaling.set(selfTransform2.getShipToWorldScaling());
				}
				final ShipTransform newTransform = new ShipTransformImpl(position, shipAnchor, rotation, scaling);
				return new NextTransformAndVelocityData(newTransform, velocity, omega);
			}
		});
	}
}
