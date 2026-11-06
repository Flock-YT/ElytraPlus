package cn.ericcraft.elytraPlus.fabric;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class FabricConfigurationTest {
    @TempDir Path directory;
    private void config(String value) throws Exception { Files.write(directory.resolve("config.yml"), value.getBytes(StandardCharsets.UTF_8)); }
    @Test void createsDefaultsWithoutOverwritingUserFiles() throws Exception {
        FabricConfiguration defaults = FabricConfiguration.load(directory);
        assertEquals(20, defaults.settings.interval);
        config("language: en_US\nsettings:\n  check-interval: 40\n");
        assertEquals(40, FabricConfiguration.load(directory).settings.interval);
        assertTrue(Files.exists(directory.resolve("lang/zh_CN.yml")));
    }
    @Test void aliasesAndCustomDimensionsAreMatched() throws Exception {
        config("settings:\n  world-list:\n    type: BLACKLIST\n    worlds: [world_nether, 'example:moon']\n");
        FabricConfiguration settings = FabricConfiguration.load(directory);
        assertFalse(settings.settings.allowsWorld("minecraft:the_nether")); assertFalse(settings.settings.allowsWorld("example:moon"));
        assertTrue(settings.settings.allowsWorld("minecraft:overworld"));
    }
    @Test void invalidDimensionAndDuplicateKeysAreRejected() throws Exception {
        config("settings:\n  world-list:\n    worlds: [Not_A_Dimension]\n");
        assertThrows(IllegalArgumentException.class, () -> FabricConfiguration.load(directory));
        config("language: en_US\nlanguage: zh_CN\n");
        assertThrows(IllegalArgumentException.class, () -> FabricConfiguration.load(directory));
    }
    @Test void invalidLanguageAndMessageTypesAreRejected() throws Exception {
        config("language: unsupported\n"); assertThrows(IllegalArgumentException.class, () -> FabricConfiguration.load(directory));
        config("language: en_US\n"); FabricConfiguration.load(directory);
        Files.write(directory.resolve("lang/en_US.yml"), "messages:\n  flight-enabled: false\n".getBytes(StandardCharsets.UTF_8));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> FabricConfiguration.load(directory)).getMessage().contains("lang/en_US.yml"));
    }
    @Test void explicitEmptyMessagesRemainSilent() throws Exception {
        FabricConfiguration.load(directory);
        Files.write(directory.resolve("lang/zh_CN.yml"), "messages:\n  flight-enabled: ''\n".getBytes(StandardCharsets.UTF_8));
        assertEquals("", FabricConfiguration.load(directory).messages.text("flight-enabled"));
    }
}
