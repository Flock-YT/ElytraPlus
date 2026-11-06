package cn.ericcraft.elytraPlus.core;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ConfigurationTest {
    @Test void restrictionsAreCaseSensitiveAndSnapshotsAreImmutable() {
        List<String> names = new ArrayList<>(Arrays.asList("world"));
        Map<String, Object> list = new HashMap<>(); list.put("type", "whitelist"); list.put("worlds", names);
        FlightSettings config = new FlightSettings(new MapConfig(Collections.singletonMap("settings", Collections.singletonMap("world-list", list))));
        names.add("new"); assertTrue(config.allowsWorld("world")); assertFalse(config.allowsWorld("World")); assertFalse(config.allowsWorld("new"));
    }
    @Test void malformedParentsAndNonfiniteProbabilitiesFailWithPaths() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> new FlightSettings(new MapConfig(Collections.singletonMap("settings", true))));
        assertTrue(error.getMessage().contains("settings"));
        for (Object bad : Arrays.asList(-1, 1.01, Double.NaN, Double.POSITIVE_INFINITY, "0.5")) {
            error = assertThrows(IllegalArgumentException.class, () -> new FlightSettings(new MapConfig(Collections.singletonMap("settings", Collections.singletonMap("durability", Collections.singletonMap("custom-chance", bad))))));
            assertTrue(error.getMessage().contains("settings.durability.custom-chance"));
        }
    }
    @Test void falseModeIgnoresNamesButStillValidatesList() {
        FlightSettings settings = new FlightSettings(new MapConfig(Collections.emptyMap())); assertTrue(settings.allowsWorld("any"));
        assertThrows(IllegalArgumentException.class, () -> new FlightSettings(new MapConfig(Collections.singletonMap("settings", Collections.singletonMap("world-list", Collections.singletonMap("worlds", false))))));
    }
    @Test void selectedMessagesFallbackAndExplicitSilence() {
        Map<String, Object> values = new HashMap<>(); values.put("prefix", "&aP "); values.put("hello", "&lHi"); values.put("silent", "default");
        MapConfig defaults = new MapConfig(Collections.singletonMap("messages", values));
        MessageCatalog catalog = new MessageCatalog(new MapConfig(Collections.singletonMap("messages", Collections.singletonMap("silent", ""))), defaults);
        assertEquals("§aP §lHi", catalog.text("hello")); assertEquals("", catalog.text("silent"));
    }
    @Test void legacyColorsResetDecorationAndPreserveUnknownCodes() {
        List<LegacyText.Run> runs = LegacyText.parse("§aA§lB§cC§rD§zE");
        assertEquals(4, runs.size()); assertEquals("a", runs.get(0).formats); assertEquals("al", runs.get(1).formats);
        assertEquals("c", runs.get(2).formats); assertEquals("", runs.get(3).formats); assertEquals("D§zE", runs.get(3).text);
    }
}
