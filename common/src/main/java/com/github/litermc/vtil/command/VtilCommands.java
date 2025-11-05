package com.github.litermc.vtil.command;

import com.github.litermc.vtil.Constants;
import com.github.litermc.vtil.api.assemble.AssembleApi;
import com.github.litermc.vtil.util.BlockSectionView;

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
import org.valkyrienskies.mod.common.VSGameUtilsKt;
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
		);
	}

	private static int assemble(final CommandContext<CommandSourceStack> context, final boolean hasSlug) throws CommandSyntaxException {
		final CommandSourceStack source = context.getSource();
		final MinecraftServer server = source.getServer();
		final ServerLevel level = source.getLevel();
		final BlockPos from = BlockPosArgument.getLoadedBlockPos(context, "from");
		final BlockPos to = BlockPosArgument.getLoadedBlockPos(context, "to");
		final BlockSectionView blocks = new BlockSectionView(from, to);

		final ServerShip ship = AssembleApi.createShip(level, blocks, VSGameUtilsKt.getShipManagingPos(level, from));

		if (ship == null) {
			source.sendFailure(Component.translatable("vtil.command.assemble.empty"));
			return 0;
		}

		final String slug = hasSlug ? StringArgumentType.getString(context, "slug") : "+assemble+" + ship.getId();
		ship.setSlug(slug);
		source.sendSuccess(() -> Component.translatable("vtil.command.assemble.success", slug), true);
		return (int) (ship.getId());
	}
}
