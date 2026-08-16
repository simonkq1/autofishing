package xyz.whatsyouss.frostyautofish;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.network.chat.Component;
import xyz.whatsyouss.frostyautofish.config.AutoFishConfig;
import xyz.whatsyouss.frostyautofish.config.ConfigManager;
import xyz.whatsyouss.frostyautofish.config.TargetListEditor;
import xyz.whatsyouss.frostyautofish.config.TargetNameMatcher;

import java.util.List;
import java.util.function.Function;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

final class NamedTargetCommands {
    private NamedTargetCommands() {
    }

    static void register(ConfigManager configManager) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(literal("frostyautofish")
                        .then(listCommand("target", TargetListKind.PLAYER_MODEL, configManager))
                        .then(listCommand("highvalue", TargetListKind.HIGH_VALUE, configManager))
                )
        );
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> listCommand(
            String command,
            TargetListKind kind,
            ConfigManager configManager
    ) {
        return literal(command)
                .then(literal("add")
                        .then(argument("name", StringArgumentType.greedyString())
                                .executes(context -> add(
                                        context.getSource(),
                                        configManager,
                                        kind,
                                        StringArgumentType.getString(context, "name")
                                ))))
                .then(literal("remove")
                        .then(argument("name", StringArgumentType.greedyString())
                                .executes(context -> remove(
                                        context.getSource(),
                                        configManager,
                                        kind,
                                        StringArgumentType.getString(context, "name")
                                ))))
                .then(literal("list")
                        .executes(context -> list(context.getSource(), configManager, kind)))
                .then(literal("clear")
                        .executes(context -> clear(context.getSource(), configManager, kind)));
    }

    private static int add(
            FabricClientCommandSource source,
            ConfigManager configManager,
            TargetListKind kind,
            String input
    ) {
        TargetListEditor.Result result = TargetListEditor.add(kind.targets(configManager.config()), input);
        if (result.status() == TargetListEditor.Status.EMPTY_NAME
                || result.status() == TargetListEditor.Status.NAME_TOO_LONG) {
            source.sendError(message("Name must contain 1-" + TargetNameMatcher.MAX_RULE_LENGTH + " characters"));
            return 0;
        }
        if (!result.success()) {
            source.sendError(message("Target already exists: " + result.normalizedName()));
            return 0;
        }

        configManager.save();
        source.sendFeedback(message("Added " + kind.itemLabel() + ": " + result.normalizedName()));
        source.sendFeedback(message("Matching ignores case/color codes and accepts name prefixes/suffixes"));
        return 1;
    }

    private static int remove(
            FabricClientCommandSource source,
            ConfigManager configManager,
            TargetListKind kind,
            String input
    ) {
        TargetListEditor.Result result = TargetListEditor.remove(kind.targets(configManager.config()), input);
        if (!result.success()) {
            source.sendError(message("Target not found: " + result.normalizedName()));
            return 0;
        }
        configManager.save();
        source.sendFeedback(message("Removed " + kind.itemLabel() + ": " + result.normalizedName()));
        return 1;
    }

    private static int list(
            FabricClientCommandSource source,
            ConfigManager configManager,
            TargetListKind kind
    ) {
        var targets = kind.targets(configManager.config());
        if (targets.isEmpty()) {
            source.sendFeedback(message(kind.titleLabel() + " list is empty"));
        } else {
            source.sendFeedback(message(kind.titleLabel() + " (" + targets.size() + "): "
                    + String.join(", ", targets)));
        }
        return targets.size();
    }

    private static int clear(
            FabricClientCommandSource source,
            ConfigManager configManager,
            TargetListKind kind
    ) {
        TargetListEditor.Result result = TargetListEditor.clear(kind.targets(configManager.config()));
        configManager.save();
        source.sendFeedback(message("Cleared " + result.count() + " " + kind.itemLabel() + "(s)"));
        return result.count();
    }

    private static Component message(String text) {
        return Component.literal("§b[Frosty AutoFish] §f" + text);
    }

    private enum TargetListKind {
        PLAYER_MODEL(
                "player-model target",
                "Player-model targets",
                config -> config.namedPlayerTargets
        ),
        HIGH_VALUE(
                "high-value target",
                "High-value targets",
                config -> config.highValueTargets
        );

        private final String itemLabel;
        private final String titleLabel;
        private final Function<AutoFishConfig, List<String>> targets;

        TargetListKind(
                String itemLabel,
                String titleLabel,
                Function<AutoFishConfig, List<String>> targets
        ) {
            this.itemLabel = itemLabel;
            this.titleLabel = titleLabel;
            this.targets = targets;
        }

        private String itemLabel() {
            return itemLabel;
        }

        private String titleLabel() {
            return titleLabel;
        }

        private List<String> targets(AutoFishConfig config) {
            return targets.apply(config);
        }
    }
}
