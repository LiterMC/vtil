package com.github.litermc.vtil.api.assemble;

import com.github.litermc.vtil.compat.CompatMods;
import com.github.litermc.vtil.compat.create.MoveableIControlContraption;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import com.simibubi.create.content.contraptions.IControlContraption;
import com.simibubi.create.content.contraptions.piston.LinearActuatorBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;

import java.util.Map;
import java.util.HashMap;
import java.util.stream.Stream;

public final class MoveApi {
	private MoveApi() {}

	private static final Map<Class<?>, IMoveable<?>> DEFAULT_MOVERS = new HashMap<>();

	/**
	 * registerDefaultMover registers the mover for a class.
	 * Any mover registered before will the dropped and be returned.
	 *
	 * @param clazz The class to register.
	 * @param mover The mover. Pass {@code null} to remove old mover only.
	 * @return The old mover, if exists.
	 */
	public static IMoveable<?> registerDefaultMover(final Class<?> clazz, final IMoveable<?> mover) {
		if (mover == null) {
			return DEFAULT_MOVERS.remove(clazz);
		}
		return DEFAULT_MOVERS.put(clazz, mover);
	}

	/**
	 * getMover will check if the instance already implemented IMoveable.
	 * If not, then check if its class and subclass is registered,
	 * then check any mover registered on the class implemented interfaces
	 * and subclasses implemented interfaces.
	 *
	 * @param instance the instance object to check.
	 * @return The mover for the instance, {@code null} if not found or passed instance is a {@code null}.
	 */
	public static IMoveable<?> getMover(final Object instance) {
		if (instance == null) {
			return null;
		}
		if (instance instanceof final IMoveable<?> mover) {
			return mover;
		}
		for (Class<?> clazz = instance.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
			final IMoveable<?> mover = DEFAULT_MOVERS.get(clazz);
			if (mover != null) {
				return mover;
			}
		}
		for (Class<?> clazz = instance.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
			for (Class<?> intf : (Iterable<Class<?>>) (Stream.of(clazz.getInterfaces()).flatMap(MoveApi::streamClassAnsSubInterfaces)::iterator)) {
				final IMoveable<?> mover = DEFAULT_MOVERS.get(intf);
				if (mover != null) {
					return mover;
				}
			}
		}
		return null;
	}

	/**
	 * module-private
	 */
	public static void registerDefaultMovers() {
		if (CompatMods.CREATE.isLoaded()) {
			registerDefaultMover(IControlContraption.class, MoveableIControlContraption.INSTANCE);
		}
	}

	private static Stream<Class<?>> streamClassAnsSubInterfaces(Class<?> clazz) {
		return Stream.concat(Stream.of(clazz), Stream.of(clazz.getInterfaces()).flatMap(MoveApi::streamClassAnsSubInterfaces));
	}
}
