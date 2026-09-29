package com.pvpkits;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;

import java.util.List;

@SuppressWarnings("unused")
public class PvPKits implements ModInitializer {

	private static final SuggestionProvider<CommandSourceStack> KIT_NAME_SUGGESTIONS = (context, builder) -> {
		CommandSourceStack source = context.getSource();
		if (source.getEntity() instanceof ServerPlayer player) {
			List<String> names = KitStorage.listKits(player);
			return SharedSuggestionProvider.suggest(names, builder);
		}
		return builder.buildFuture();
	};

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, _, _) ->
				dispatcher.register(Commands.literal("pk")
						.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
						.then(Commands.literal("save")
								.then(Commands.argument("name", StringArgumentType.word())
										.executes(PvPKits::saveKit)))
						.then(Commands.literal("load")
								.then(Commands.argument("name", StringArgumentType.word())
										.suggests(KIT_NAME_SUGGESTIONS)
										.executes(PvPKits::loadKit)))
						.then(Commands.literal("delete")
								.then(Commands.argument("name", StringArgumentType.word())
										.suggests(KIT_NAME_SUGGESTIONS)
										.executes(PvPKits::deleteKit)))
						.then(Commands.literal("list")
								.executes(PvPKits::listKits))
				));
	}

	private static ServerPlayer requirePlayer(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		return context.getSource().getPlayerOrException();
	}

	private static int saveKit(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = requirePlayer(context);
		String name = StringArgumentType.getString(context, "name");

		boolean ok = KitStorage.saveKit(player, name);
		if (ok) {
			context.getSource().sendSuccess(() -> Component.literal("Saved current inventory as kit '" + name + "'."), false);
			return 1;
		} else {
			context.getSource().sendFailure(Component.literal("Failed to save kit '" + name + "'. Check server logs for details."));
			return 0;
		}
	}

	private static int loadKit(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = requirePlayer(context);
		String name = StringArgumentType.getString(context, "name");

		boolean ok = KitStorage.loadKit(player, name);
		if (ok) {
			context.getSource().sendSuccess(() -> Component.literal("Loaded kit '" + name + "'. Your previous inventory has been replaced."), false);
			return 1;
		} else {
			context.getSource().sendFailure(Component.literal("No kit named '" + name + "' was found."));
			return 0;
		}
	}

	private static int deleteKit(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = requirePlayer(context);
		String name = StringArgumentType.getString(context, "name");

		boolean ok = KitStorage.deleteKit(player, name);
		if (ok) {
			context.getSource().sendSuccess(() -> Component.literal("Deleted kit '" + name + "'."), false);
			return 1;
		} else {
			context.getSource().sendFailure(Component.literal("No kit named '" + name + "' was found."));
			return 0;
		}
	}

	private static int listKits(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
		ServerPlayer player = requirePlayer(context);
		List<String> names = KitStorage.listKits(player);

		if (names.isEmpty()) {
			context.getSource().sendSuccess(() -> Component.literal("You have no saved kits."), false);
		} else {
			context.getSource().sendSuccess(() -> Component.literal("Saved kits: " + String.join(", ", names)), false);
		}
		return 1;
	}
}