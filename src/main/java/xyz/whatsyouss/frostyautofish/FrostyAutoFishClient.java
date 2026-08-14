package xyz.whatsyouss.frostyautofish;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.whatsyouss.frostyautofish.config.ConfigManager;

public final class FrostyAutoFishClient implements ClientModInitializer {
    public static final String MOD_ID = "frosty_autofish";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private ConfigManager configManager;
    private AutoFishController controller;
    private KeyMapping toggleKey;
    private KeyMapping configKey;

    @Override
    public void onInitializeClient() {
        Minecraft minecraft = Minecraft.getInstance();
        configManager = new ConfigManager(LOGGER);
        configManager.load();

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "main")
        );
        toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.frosty_autofish.toggle",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F8,
                category
        ));
        configKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.frosty_autofish.config",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F9,
                category
        ));
        InputLockCoordinator inputLock = InputLockCoordinator.initialize(
                minecraft,
                toggleKey,
                configKey
        );
        controller = new AutoFishController(minecraft, configManager.config(), inputLock);

        ClientTickEvents.END_CLIENT_TICK.register(this::onEndClientTick);
        ClientEntityEvents.ENTITY_LOAD.register(controller::onEntityLoad);
        NamedTargetCommands.register(configManager);
        LOGGER.info("Frosty AutoFish initialized");
    }

    private void onEndClientTick(Minecraft minecraft) {
        while (toggleKey.consumeClick()) {
            controller.toggle();
        }
        while (configKey.consumeClick()) {
            if (!(minecraft.screen instanceof ConfigScreen)) {
                controller.disableForConfigScreen();
                minecraft.setScreen(new ConfigScreen(
                        minecraft.screen,
                        configManager,
                        controller
                ));
            }
        }
        controller.tick();
    }
}
