package com.github.litermc.vtil.command;

import com.github.litermc.vtil.Constants;
import com.github.litermc.vtil.api.assemble.AssembleApi;
import com.github.litermc.vtil.api.assemble.ShipAllocator;
import com.github.litermc.vtil.util.BlockSectionView;
import com.github.litermc.vtil.util.LevelUtil;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.command.ShipArgument;
import org.valkyrienskies.mod.mixinducks.feature.command.VSCommandSource;

import java.util.Set;

public final class VtilCommands {
	public static final String ROOT_LITERAL = Constants.MOD_ID;

	private VtilCommands() {}

	public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal(ROOT_LITERAL)
			.requires((source) -> source.hasPermission(2))
			.then(Commands.literal("assemble")
				.then(Commands.argument("from", BlockPosArgument.blockPos())
					.then(Commands.argument("to", BlockPosArgument.blockPos())
						.then(Commands.argument("slug", StringArgumentType.word())
							.executes((ctx) -> assemble(ctx, true))
						)
						.executes((ctx) -> assemble(ctx, false))
					)
				)
			)
			.then(Commands.literal("assemble-async")
				.then(Commands.argument("from", BlockPosArgument.blockPos())
					.then(Commands.argument("to", BlockPosArgument.blockPos())
						.then(Commands.argument("slug", StringArgumentType.word())
							.executes((ctx) -> assembleAsync(ctx, true))
						)
						.executes((ctx) -> assembleAsync(ctx, false))
					)
				)
			)
			.then(Commands.literal("delete")
				.then(Commands.argument("ships", ShipArgument.Companion.ships())
					.executes(VtilCommands::delete)
				)
			)
		);
	}

	private static int assemble(final CommandContext<CommandSourceStack> context, final boolean hasSlug) throws CommandSyntaxException {
		final CommandSourceStack source = context.getSource();
		final MinecraftServer server = source.getServer();
		final ServerLevel level = source.getLevel();
		final BlockPos from = BlockPosArgument.getLoadedBlockPos(context, "from");
		final BlockPos to = BlockPosArgument.getLoadedBlockPos(context, "to");
		final BlockSectionView blocks = new BlockSectionView(from, to);

		final ServerShip ship = AssembleApi.createShip(level, blocks);

		if (ship == null) {
			source.sendFailure(Component.translatable("vtil.command.assemble.empty"));
			return 0;
		}

		final String slug = hasSlug ? StringArgumentType.getString(context, "slug") : "+assemble+" + ship.getId();
		ship.setSlug(slug);
		source.sendSuccess(() -> Component.translatable("vtil.command.assemble.success", slug), true);
		return (int) (ship.getId());
	}

	private static int assembleAsync(final CommandContext<CommandSourceStack> context, final boolean hasSlug) throws CommandSyntaxException {
		final CommandSourceStack source = context.getSource();
		final MinecraftServer server = source.getServer();
		final ServerLevel level = source.getLevel();
		final BlockPos from = BlockPosArgument.getLoadedBlockPos(context, "from");
		final BlockPos to = BlockPosArgument.getLoadedBlockPos(context, "to");
		final BlockSectionView blocks = new BlockSectionView(from, to);

		AssembleApi.createShipAsync(level, blocks, Integer.MAX_VALUE)
			.thenAccept((ship) -> {
				if (ship == null) {
					source.sendFailure(Component.translatable("vtil.command.assemble.empty"));
					return;
				}

				final String slug = hasSlug ? StringArgumentType.getString(context, "slug") : "+assemble+" + ship.getId();
				ship.setSlug(slug);
				source.sendSuccess(() -> Component.translatable("vtil.command.assemble.success", slug), true);
			});
		return 1;
	}

	private static int delete(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		final CommandSourceStack source = context.getSource();
		final MinecraftServer server = source.getServer();
		final ShipAllocator allocator = ShipAllocator.get(server);
		final Set<Ship> ships = ShipArgument.Companion.getShips((CommandContext<VSCommandSource>) ((CommandContext<?>) (context)), "ships");
		int successCount = 0;
		for (final Ship ship : ships) {
			if (!(ship instanceof ServerShip serverShip)) {
				continue;
			}
			final ServerLevel level = LevelUtil.getLevel(serverShip.getChunkClaimDimension());
			if (level == null) {
				continue;
			}
			allocator.putShip(serverShip);
			successCount++;
		}
		final int finalSuccessCount = successCount;
		source.sendSuccess(() ->
			Component.translatable("command.valkyrienskies.delete.success", finalSuccessCount),
			true
		);
		return finalSuccessCount;
	}
}
