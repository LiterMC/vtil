package com.github.litermc.vtil.util;

import org.valkyrienskies.core.api.world.PhysLevel;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import net.minecraft.server.level.ServerLevel;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Consumer;

public final class TaskUtil {
	private static final Queue<Task> TICK_START_QUEUE = new PriorityBlockingQueue<>();
	private static final Queue<Task> TICK_END_QUEUE = new PriorityBlockingQueue<>();
	private static final Map<String, Queue<Consumer<PhysLevel>>> PHYSTICK_QUEUES = new ConcurrentHashMap<>();
	private static volatile long tick = 0;

	private TaskUtil() {}

	public static void preServerTick() {
		tick++;
		final long t = tick;
		for (int i = TICK_START_QUEUE.size(); i > 0; i--) {
			final Task task = TICK_START_QUEUE.element();
			if (task.tick() > t) {
				return;
			}
			TICK_START_QUEUE.remove();
			task.task().run();
		}
	}

	public static void postServerTick() {
		final long t = tick;
		for (int i = TICK_END_QUEUE.size(); i > 0; i--) {
			final Task task = TICK_END_QUEUE.element();
			if (task.tick() > t) {
				return;
			}
			TICK_END_QUEUE.remove();
			task.task().run();
		}
	}

	public static void onPhysTick(final PhysLevel world) {
		final Queue<Consumer<PhysLevel>> queue = PHYSTICK_QUEUES.get(world.getDimension());
		if (queue == null) {
			return;
		}
		for (int i = queue.size(); i > 0; i--) {
			final Consumer<PhysLevel> task = queue.remove();
			task.accept(world);
		}
	}

	public static void onServerLevelUnload(final ServerLevel level) {
		PHYSTICK_QUEUES.remove(VSGameUtilsKt.getDimensionId(level));
	}

	public static void queueTickStart(final Runnable task) {
		queueTickStart(0, task);
	}

	public static void queueTickStart(final int delay, final Runnable task) {
		TICK_START_QUEUE.add(new Task(tick + delay, task));
	}

	public static void queueTickEnd(final Runnable task) {
		queueTickEnd(0, task);
	}

	public static void queueTickEnd(final int delay, final Runnable task) {
		TICK_END_QUEUE.add(new Task(tick + delay, task));
	}

	public static void queuePhysicsTick(final ServerLevel level, final Consumer<PhysLevel> task) {
		queuePhysicsTick(VSGameUtilsKt.getDimensionId(level), task);
	}

	// TODO: is string version necessary to expose?
	private static void queuePhysicsTick(final String dimId, final Consumer<PhysLevel> task) {
		final Queue<Consumer<PhysLevel>> queue = PHYSTICK_QUEUES.computeIfAbsent(dimId, (dimId0) -> new ConcurrentLinkedQueue<>());
		queue.add(task);
	}

	record Task(long tick, Runnable task) implements Comparable<Task> {
		@Override
		public int compareTo(final Task other) {
			return Long.compare(this.tick, other.tick);
		}
	}
}
