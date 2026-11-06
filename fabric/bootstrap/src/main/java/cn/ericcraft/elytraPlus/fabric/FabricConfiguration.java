package cn.ericcraft.elytraPlus.fabric;

import cn.ericcraft.elytraPlus.core.*;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Same YAML contract as Bukkit, independently decoded without a Bukkit dependency. */
public final class FabricConfiguration {
    public final FlightSettings settings;
    public final MessageCatalog messages;
    private FabricConfiguration(FlightSettings settings, MessageCatalog messages) { this.settings = settings; this.messages = messages; }
    public static FabricConfiguration load(Path directory) {
        try {
            Files.createDirectories(directory.resolve("lang"));
            for (String path : new String[]{"config.yml", "lang/zh_CN.yml", "lang/en_US.yml"}) {
                Path file = directory.resolve(path);
                if (!Files.exists(file)) {
                    try (InputStream input = resource(path)) { Files.copy(input, file); }
                }
            }
            Map<?, ?> decoded = read(Files.newInputStream(directory.resolve("config.yml")));
            MapConfig config = new MapConfig(decoded);
            Object language = config.contains("language") ? config.get("language") : "zh_CN";
            if (!"zh_CN".equals(language) && !"en_US".equals(language)) throw new IllegalArgumentException("language must be zh_CN or en_US");
            String path = "lang/" + language + ".yml";
            MessageCatalog messages;
            try {
                messages = new MessageCatalog(new MapConfig(read(Files.newInputStream(directory.resolve(path)))),
                        new MapConfig(read(resource(path))));
            } catch (IllegalArgumentException error) { throw new IllegalArgumentException(path + ": " + error.getMessage(), error); }
            // Normalize only Fabric dimension selectors, without changing Bukkit world-name matching.
            FlightSettings settings = new FlightSettings(new DimensionConfig(config));
            return new FabricConfiguration(settings, messages);
        } catch (IOException error) { throw new IllegalStateException("Cannot read ElytraPlus configuration", error); }
    }
    private static InputStream resource(String path) { return Objects.requireNonNull(FabricConfiguration.class.getResourceAsStream("/" + path), "Missing " + path); }
    private static Map<?, ?> read(InputStream input) throws IOException {
        LoaderOptions options = new LoaderOptions(); options.setAllowDuplicateKeys(false);
        try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            Object value = new Yaml(new SafeConstructor(options)).load(reader);
            if (value == null) return Collections.emptyMap();
            if (!(value instanceof Map)) throw new IllegalArgumentException("YAML root must be a mapping");
            return (Map<?, ?>) value;
        } catch (org.yaml.snakeyaml.error.YAMLException error) {
            throw new IllegalArgumentException("Invalid YAML: " + error.getMessage(), error);
        }
    }
    public static String dimension(String name) {
        if ("world".equals(name)) return "minecraft:overworld";
        if ("world_nether".equals(name)) return "minecraft:the_nether";
        if ("world_the_end".equals(name)) return "minecraft:the_end";
        if (!name.matches("[a-z0-9_.-]+:[a-z0-9/._-]+")) throw new IllegalArgumentException("settings.world-list.worlds: use namespace:path or world/world_nether/world_the_end: " + name);
        return name;
    }
    private static final class DimensionConfig implements ConfigView {
        private final ConfigView config;
        DimensionConfig(ConfigView config) { this.config = config; }
        @Override public Object get(String path) {
            Object value = config.get(path);
            if (!path.equals("settings.world-list.worlds") || !(value instanceof List)) return value;
            List<Object> normalized = new ArrayList<>();
            for (Object name : (List<?>) value) normalized.add(name instanceof String ? dimension((String) name) : name);
            return normalized;
        }
        @Override public boolean contains(String path) { return config.contains(path); }
        @Override public boolean isConfigurationSection(String path) { return config.isConfigurationSection(path); }
        @Override public Set<String> keys(String path) { return config.keys(path); }
    }
}
