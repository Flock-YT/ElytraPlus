package cn.ericcraft.elytraPlus.core;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class MapConfig implements ConfigView {
    private final Map<?, ?> values;
    public MapConfig(Map<?, ?> values) { this.values = values; }
    @Override public Object get(String path) {
        Object value = values;
        for (String part : path.split("\\.")) {
            if (!(value instanceof Map)) return null;
            value = ((Map<?, ?>) value).get(part);
        }
        return value;
    }
    @Override public boolean contains(String path) {
        Object value = values;
        String[] parts = path.split("\\.");
        for (int i = 0; i < parts.length; i++) {
            if (!(value instanceof Map) || !((Map<?, ?>) value).containsKey(parts[i])) return false;
            if (i == parts.length - 1) return true;
            value = ((Map<?, ?>) value).get(parts[i]);
        }
        return false;
    }
    @Override public boolean isConfigurationSection(String path) { return get(path) instanceof Map; }
    @Override public Set<String> keys(String path) {
        Object value = get(path);
        if (!(value instanceof Map)) return Collections.emptySet();
        Set<String> keys = new HashSet<>();
        for (Object key : ((Map<?, ?>) value).keySet()) {
            if (!(key instanceof String)) throw new IllegalArgumentException(path + " must have string keys");
            keys.add((String) key);
        }
        return keys;
    }
}
