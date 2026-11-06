package cn.ericcraft.elytraPlus.core;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Validated startup snapshot; no configuration reads in the flight loop. */
public class FlightSettings {
    public final boolean metricsEnabled;
    public final long interval;
    public final boolean durabilityEnabled;
    public final boolean vanillaFormula;
    public final double customChance;
    private final boolean worldRestrictionsDisabled;
    private final boolean whitelist;
    private final Set<String> worlds;

    public FlightSettings(ConfigView config) {
        metricsEnabled = bool(config, "bstats.enabled", true);
        Object intervalValue = value(config, "settings.check-interval", 20);
        if (!(intervalValue instanceof Number) ||
                !(intervalValue instanceof Integer || intervalValue instanceof Long) ||
                ((Number) intervalValue).longValue() <= 0) {
            throw invalid("settings.check-interval", "must be a positive integer");
        }
        interval = ((Number) intervalValue).longValue();
        durabilityEnabled = bool(config, "settings.durability.enabled", true);
        vanillaFormula = bool(config, "settings.durability.use-vanilla-formula", true);
        Object chance = value(config, "settings.durability.custom-chance", 0.5);
        if (!(chance instanceof Number)) throw invalid("settings.durability.custom-chance", "must be a number in [0, 1]");
        customChance = ((Number) chance).doubleValue();
        if (Double.isNaN(customChance) || Double.isInfinite(customChance) || customChance < 0 || customChance > 1) {
            throw invalid("settings.durability.custom-chance", "must be finite and in [0, 1]");
        }
        Object type = value(config, "settings.world-list.type", false);
        String mode = type instanceof String ? ((String) type).toUpperCase(Locale.ROOT) : "";
        worldRestrictionsDisabled = Boolean.FALSE.equals(type) || mode.equals("FALSE");
        if (!worldRestrictionsDisabled && !mode.equals("BLACKLIST") && !mode.equals("WHITELIST")) {
            throw invalid("settings.world-list.type", "must be BLACKLIST, WHITELIST or false");
        }
        whitelist = mode.equals("WHITELIST");
        Object names = value(config, "settings.world-list.worlds", Collections.singletonList("example_world_name"));
        if (!(names instanceof List)) throw invalid("settings.world-list.worlds", "must be a list of world names");
        Set<String> copy = new HashSet<>();
        for (Object name : (List<?>) names) {
            if (!(name instanceof String)) throw invalid("settings.world-list.worlds", "must contain only strings");
            copy.add((String) name);
        }
        worlds = Collections.unmodifiableSet(copy);
    }

    public boolean allowsWorld(String name) { return worldRestrictionsDisabled || whitelist == worlds.contains(name); }

    private static Object value(ConfigView config, String path, Object fallback) {
        // Reject malformed parent sections instead of silently ignoring their values.
        int dot = path.indexOf('.');
        while (dot >= 0) {
            String parent = path.substring(0, dot);
            if (config.contains(parent) && !config.isConfigurationSection(parent)) {
                throw invalid(parent, "must be a section");
            }
            dot = path.indexOf('.', dot + 1);
        }
        return config.contains(path) ? config.get(path) : fallback;
    }

    private static boolean bool(ConfigView config, String path, boolean fallback) {
        Object value = value(config, path, fallback);
        if (!(value instanceof Boolean)) throw invalid(path, "must be true or false");
        return (Boolean) value;
    }

    private static IllegalArgumentException invalid(String path, String detail) {
        return new IllegalArgumentException(path + " " + detail);
    }
}
