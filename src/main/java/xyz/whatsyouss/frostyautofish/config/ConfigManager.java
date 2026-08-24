package xyz.whatsyouss.frostyautofish.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Logger logger;
    private final Path path;
    private AutoFishConfig config = new AutoFishConfig();

    public ConfigManager(Logger logger) {
        this(logger, FabricLoader.getInstance().getConfigDir().resolve("frosty-autofish.json"));
    }

    ConfigManager(Logger logger, Path path) {
        this.logger = logger;
        this.path = path;
    }

    public AutoFishConfig config() {
        return config;
    }

    public void load() {
        if (!Files.exists(path)) {
            config = new AutoFishConfig();
            save();
            return;
        }

        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            config = parse(json);
        } catch (IOException exception) {
            logger.warn("Could not read AutoFish config; using defaults", exception);
            config = new AutoFishConfig();
        }
    }

    public void save() {
        config.normalize();
        try {
            Files.createDirectories(path.getParent());
            Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
            Files.writeString(temporary, serialize(config), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException unsupportedAtomicMove) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            logger.error("Could not save AutoFish config", exception);
        }
    }

    public void setHighValueEnabled(boolean enabled) {
        config.highValueEnabled = enabled;
        save();
    }

    public static AutoFishConfig parse(String json) {
        try {
            AutoFishConfig parsed = GSON.fromJson(json, AutoFishConfig.class);
            if (parsed == null) {
                parsed = new AutoFishConfig();
            }
            parsed.normalize();
            return parsed;
        } catch (RuntimeException exception) {
            return new AutoFishConfig();
        }
    }

    static String serialize(AutoFishConfig config) {
        return GSON.toJson(config);
    }
}
