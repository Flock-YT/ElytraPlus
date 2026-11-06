package cn.ericcraft.elytraPlus.config;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Function;

public final class Messages {
    private final cn.ericcraft.elytraPlus.core.MessageCatalog catalog;

    /** Loads language files once; config.yml messages are deliberately not consulted. */
    public static Messages load(File dataFolder, ConfigurationSection config,
                                Function<String, InputStream> resources)
            throws IOException, InvalidConfigurationException {
        Object language = config.contains("language") ? config.get("language") : "zh_CN";
        if (!"zh_CN".equals(language) && !"en_US".equals(language)) {
            throw new IllegalArgumentException("language must be zh_CN or en_US");
        }
        Path directory = dataFolder.toPath().resolve("lang");
        Files.createDirectories(directory);
        for (String name : new String[]{"zh_CN", "en_US"}) {
            Path file = directory.resolve(name + ".yml");
            if (!Files.exists(file)) {
                try (InputStream input = resource(resources, "lang/" + name + ".yml")) {
                    Files.copy(input, file);
                }
            }
        }
        String path = "lang/" + language + ".yml";
        YamlConfiguration defaults = new YamlConfiguration();
        try (InputStreamReader reader = new InputStreamReader(resource(resources, path), StandardCharsets.UTF_8)) {
            defaults.load(reader);
        }
        YamlConfiguration selected = new YamlConfiguration();
        try (InputStreamReader reader = new InputStreamReader(
                Files.newInputStream(dataFolder.toPath().resolve(path)), StandardCharsets.UTF_8)) {
            selected.load(reader);
        } catch (InvalidConfigurationException e) {
            throw new InvalidConfigurationException(path + ": " + e.getMessage(), e);
        }
        try {
            return new Messages(selected, defaults);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(path + ": " + e.getMessage(), e);
        }
    }

    private static InputStream resource(Function<String, InputStream> resources, String path) {
        return Objects.requireNonNull(resources.apply(path), "Missing bundled " + path);
    }

    public Messages(ConfigurationSection config, ConfigurationSection defaults) {
        catalog = new cn.ericcraft.elytraPlus.core.MessageCatalog(new BukkitConfigView(config), new BukkitConfigView(defaults));
    }

    public cn.ericcraft.elytraPlus.core.MessageCatalog catalog() { return catalog; }

    public void send(CommandSender sender, String key) {
        String text = catalog.text(key);
        if (!text.isEmpty()) sender.sendMessage(text);
    }
}
