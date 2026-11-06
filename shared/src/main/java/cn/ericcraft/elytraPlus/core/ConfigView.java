package cn.ericcraft.elytraPlus.core;

import java.util.Set;

/** Platform-independent access to a decoded YAML mapping. */
public interface ConfigView {
    Object get(String path);
    boolean contains(String path);
    boolean isConfigurationSection(String path);
    Set<String> keys(String path);
}
