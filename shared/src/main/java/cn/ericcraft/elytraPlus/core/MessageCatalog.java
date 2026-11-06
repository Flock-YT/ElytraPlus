package cn.ericcraft.elytraPlus.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Resolves fallback and silence before platform text rendering. */
public final class MessageCatalog {
    private final Map<String, String> messages;
    public MessageCatalog(ConfigView config, ConfigView defaults) {
        if (config.contains("messages") && !config.isConfigurationSection("messages")) {
            throw new IllegalArgumentException("messages must be a section");
        }
        String prefix = read(config, defaults, "prefix");
        Map<String, String> resolved = new HashMap<>();
        for (String key : defaults.keys("messages")) {
            String text = read(config, defaults, key);
            resolved.put(key, text.isEmpty() ? "" : colors(prefix + text));
        }
        messages = Collections.unmodifiableMap(resolved);
    }

    public void send(FlightPlayer player, String key) {
        String text = text(key);
        if (!text.isEmpty()) player.message(text);
    }
    public String text(String key) { return messages.containsKey(key) ? messages.get(key) : ""; }
    private static String colors(String value) {
        char[] chars = value.toCharArray();
        for (int i = 0; i + 1 < chars.length; i++) {
            if (chars[i] == '&' && "0123456789abcdefklmnorABCDEFKLMNOR".indexOf(chars[i + 1]) >= 0) {
                chars[i] = '\u00a7'; chars[i + 1] = Character.toLowerCase(chars[i + 1]);
            }
        }
        return new String(chars);
    }
    private static String read(ConfigView config, ConfigView defaults, String key) {
        String path = "messages." + key;
        Object value = config.contains(path) ? config.get(path) : defaults.get(path);
        if (!(value instanceof String)) throw new IllegalArgumentException(path + " must be a string");
        return (String) value;
    }
}
