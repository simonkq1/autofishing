package xyz.whatsyouss.frostyautofish;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.network.chat.Component;
import xyz.whatsyouss.frostyautofish.config.AutoFishConfig;
import xyz.whatsyouss.frostyautofish.config.ConfigManager;
import xyz.whatsyouss.frostyautofish.config.TargetNameMatcher;

import java.util.Locale;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

final class NamedTargetCommands {
    private NamedTargetCommands() {
    }

    static void register(ConfigManager configManager) {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(literal("frostyautofish")
                        .then(literal("target")
                                .then(literal("add")
                                        .then(argument("name", StringArgumentType.greedyString())
                                                .executes(context -> add(
                                                        context.getSource(),
                                                        configManager,
                                                        StringArgumentType.getString(context, "name")
                                                ))))
                                .then(literal("remove")
                                        .then(argument("name", StringArgumentType.greedyString())
                                                .executes(context -> remove(
                                                        context.getSource(),
                                                        configManager,
                                                        StringArgumentType.getString(context, "name")
                                                ))))
                                .then(literal("list")
                                        .executes(context -> list(context.getSource(), configManager)))
                                .then(literal("clear")
                                        .executes(context -> clear(context.getSource(), configManager))))
                )
        );
    }

    private static int add(
            net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource source,
            ConfigManager configManager,
            String input
    ) {
        String name = TargetNameMatcher.normalize(input);
        if (name.isEmpty() || name.length() > TargetNameMatcher.MAX_RULE_LENGTH) {
            source.sendError(message("Name must contain 1-" + TargetNameMatcher.MAX_RULE_LENGTH + " characters"));
            return 0;
        }

        AutoFishConfig config = configManager.config();
        boolean duplicate = config.namedPlayerTargets.stream()
                .anyMatch(existing -> existing.equalsIgnoreCase(name));
        if (duplicate) {
            source.sendError(message("Target already exists: " + name));
            return 0;
        }

        config.namedPlayerTargets.add(name);
        configManager.save();
        source.sendFeedback(message("Added player-model target: " + name));
        source.sendFeedback(message("Matching ignores case/color codes and accepts name prefixes/suffixes"));
        return 1;
    }

    private static int remove(
            net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource source,
            ConfigManager configManager,
            String input
    ) {
        String name = TargetNameMatcher.normalize(input);
        boolean removed = configManager.config().namedPlayerTargets.removeIf(
                existing -> existing.toLowerCase(Locale.ROOT).equals(name.toLowerCase(Locale.ROOT))
        );
        if (!removed) {
            source.sendError(message("Target not found: " + name));
            return 0;
        }
        configManager.save();
        source.sendFeedback(message("Removed player-model target: " + name));
        return 1;
    }

    private static int list(
            net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource source,
            ConfigManager configManager
    ) {
        var targets = configManager.config().namedPlayerTargets;
        if (targets.isEmpty()) {
            source.sendFeedback(message("Player-model target list is empty"));
        } else {
            source.sendFeedback(message("Player-model targets (" + targets.size() + "): "
                    + String.join(", ", targets)));
        }
        return targets.size();
    }

    private static int clear(
            net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource source,
            ConfigManager configManager
    ) {
        int count = configManager.config().namedPlayerTargets.size();
        configManager.config().namedPlayerTargets.clear();
        configManager.save();
        source.sendFeedback(message("Cleared " + count + " player-model target(s)"));
        return count;
    }

    private static Component message(String text) {
        return Component.literal("§b[Frosty AutoFish] §f" + text);
    }
}
