package cn.ericcraft.elytraPlus.config;

import cn.ericcraft.elytraPlus.core.ConfigView;
import org.bukkit.configuration.ConfigurationSection;
import java.util.Collections;
import java.util.Set;

public final class BukkitConfigView implements ConfigView {
    private final ConfigurationSection config;
    public BukkitConfigView(ConfigurationSection config) { this.config = config; }
    @Override public Object get(String path) { return config.get(path); }
    @Override public boolean contains(String path) { return config.contains(path); }
    @Override public boolean isConfigurationSection(String path) { return config.isConfigurationSection(path); }
    @Override public Set<String> keys(String path) {
        ConfigurationSection section = config.getConfigurationSection(path);
        return section == null ? Collections.emptySet() : section.getKeys(false);
    }
}
