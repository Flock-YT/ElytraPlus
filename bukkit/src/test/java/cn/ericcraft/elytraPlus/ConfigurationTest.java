package cn.ericcraft.elytraPlus;

import cn.ericcraft.elytraPlus.config.FlightSettings;
import cn.ericcraft.elytraPlus.config.Messages;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.bukkit.configuration.InvalidConfigurationException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfigurationTest {
    @Test void defaultsAndSnapshot() {
        YamlConfiguration config = new YamlConfiguration();
        FlightSettings settings = new FlightSettings(config);
        assertEquals(20, settings.interval); assertTrue(settings.durabilityEnabled);
        assertTrue(settings.metricsEnabled);
        config.set("bstats.enabled", false);
        assertFalse(new FlightSettings(config).metricsEnabled);
        assertTrue(settings.metricsEnabled);
        assertTrue(settings.allowsWorld("world")); assertTrue(settings.allowsWorld("example_world_name"));
        config.set("settings.world-list.worlds", Collections.singletonList("world"));
        assertTrue(settings.allowsWorld("world"));
    }
    @Test void whitelistIsCaseSensitiveAndModeIsCaseInsensitive() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("settings.world-list.type", "whitelist");
        config.set("settings.world-list.worlds", Arrays.asList("World", "World"));
        FlightSettings settings = new FlightSettings(config);
        assertTrue(settings.allowsWorld("World")); assertFalse(settings.allowsWorld("world"));
    }
    @Test void blacklistBlocksListedWorlds() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("settings.world-list.type", "blacklist");
        config.set("settings.world-list.worlds", Collections.singletonList("blocked"));
        FlightSettings settings = new FlightSettings(config);
        assertFalse(settings.allowsWorld("blocked")); assertTrue(settings.allowsWorld("world"));
    }
    @Test void falseWorldModeAllowsAllWorldsFromYaml() throws Exception {
        for (String mode : Arrays.asList("false", "False", "FALSE", "\"false\"", "\"False\"", "'fAlSe'")) {
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString("settings:\n  world-list:\n    type: " + mode + "\n    worlds: [world]\n");
            FlightSettings settings = new FlightSettings(config);
            assertTrue(settings.allowsWorld("world"), mode);
            assertTrue(settings.allowsWorld("other_world"), mode);
        }
    }
    @Test void disabledWorldRestrictionsStillValidateWorldList() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("settings.world-list.type", false);
        for (Object names : Arrays.asList("world", Arrays.asList("world", 7))) {
            config.set("settings.world-list.worlds", names);
            assertTrue(assertThrows(IllegalArgumentException.class, () -> new FlightSettings(config))
                    .getMessage().contains("settings.world-list.worlds"));
        }
    }
    @Test void rejectsMalformedValuesWithTheirPaths() {
        invalid("bstats.enabled", "true"); invalid("bstats.enabled", 1);
        invalid("bstats", false);
        invalid("settings.check-interval", 0); invalid("settings.check-interval", -1);
        invalid("settings.check-interval", 1.5); invalid("settings.check-interval", "20");
        invalid("settings.durability.custom-chance", Double.NaN);
        invalid("settings.durability.custom-chance", Double.POSITIVE_INFINITY);
        invalid("settings.durability.custom-chance", -0.1); invalid("settings.durability.custom-chance", 1.1);
        invalid("settings.durability.enabled", "true");
        invalid("settings.world-list.type", "ALLOW");
        invalid("settings.world-list.type", true);
        invalid("settings.world-list.type", "true");
        invalid("settings.world-list.type", 0);
        invalid("settings.world-list.worlds", "world");
        invalid("settings.world-list.worlds", Arrays.asList("world", 7));
        invalid("settings.durability", false);
    }
    private void invalid(String key, Object value) {
        YamlConfiguration config = new YamlConfiguration(); config.set(key, value);
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new FlightSettings(config)).getMessage().contains(key));
    }
    @Test void messageDefaultsColorAndExplicitSilence() {
        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set("messages.prefix", "&bPrefix "); defaults.set("messages.test", "&aText");
        YamlConfiguration config = new YamlConfiguration(); CommandSender sender = mock(CommandSender.class);
        new Messages(config, defaults).send(sender, "test"); verify(sender).sendMessage("§bPrefix §aText");
        reset(sender); config.set("messages.test", ""); new Messages(config, defaults).send(sender, "test");
        verifyNoInteractions(sender);
        config.set("messages.test", 1); assertThrows(IllegalArgumentException.class, () -> new Messages(config, defaults));
    }

    @TempDir Path data;

    private Messages load(YamlConfiguration config) throws Exception {
        return Messages.load(data.toFile(), config, path -> getClass().getResourceAsStream("/" + path));
    }

    @Test void selectsLanguageAndIgnoresOldConfigMessages() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.set("messages.flight-enabled", "Old override");
        CommandSender sender = mock(CommandSender.class);
        load(config).send(sender, "flight-enabled");
        verify(sender).sendMessage("§b[飞行系统] §a鞘翅飞行模式已开启！");
        assertTrue(Files.exists(data.resolve("lang/zh_CN.yml")));
        assertTrue(Files.exists(data.resolve("lang/en_US.yml")));
        reset(sender);
        config.set("language", "en_US");
        load(config).send(sender, "flight-enabled");
        verify(sender).sendMessage("§b[ElytraPlus] §aElytra flight enabled!");
    }

    @Test void preservesCustomFilesAndUsesSelectedLanguageFallback() throws Exception {
        YamlConfiguration config = new YamlConfiguration(); config.set("language", "en_US");
        load(config);
        Path file = data.resolve("lang/en_US.yml");
        byte[] custom = "messages:\n  prefix: '&6Custom '\n  flight-disabled: ''\n".getBytes(StandardCharsets.UTF_8);
        Files.write(file, custom);
        Messages messages = load(config);
        assertArrayEquals(custom, Files.readAllBytes(file));
        // Rendering is a startup snapshot, unaffected by later file edits.
        Files.write(file, "messages: invalid".getBytes(StandardCharsets.UTF_8));
        CommandSender sender = mock(CommandSender.class);
        messages.send(sender, "flight-enabled");
        verify(sender).sendMessage("§6Custom §aElytra flight enabled!");
        reset(sender); messages.send(sender, "flight-disabled"); verifyNoInteractions(sender);
    }

    @Test void rejectsInvalidLanguageAndLanguageFile() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        for (Object language : Arrays.asList("fr_FR", "../en_US", "", 7, false)) {
            config.set("language", language);
            assertTrue(assertThrows(IllegalArgumentException.class, () -> load(config)).getMessage().contains("language"));
        }
        config.set("language", "en_US"); load(config);
        Path file = data.resolve("lang/en_US.yml");
        for (String invalid : Arrays.asList("messages: invalid", "messages:\n  prefix: 1", "messages:\n  usage: false")) {
            Files.write(file, invalid.getBytes(StandardCharsets.UTF_8));
            assertTrue(assertThrows(IllegalArgumentException.class, () -> load(config)).getMessage().contains("lang/en_US.yml"));
        }
        Files.write(file, "messages: [".getBytes(StandardCharsets.UTF_8));
        assertTrue(assertThrows(InvalidConfigurationException.class, () -> load(config)).getMessage().contains("lang/en_US.yml"));
    }

    @Test void bundledLanguagesHaveMatchingKeys() throws Exception {
        YamlConfiguration chinese = new YamlConfiguration(), english = new YamlConfiguration();
        try (InputStreamReader zh = new InputStreamReader(getClass().getResourceAsStream("/lang/zh_CN.yml"), StandardCharsets.UTF_8);
             InputStreamReader en = new InputStreamReader(getClass().getResourceAsStream("/lang/en_US.yml"), StandardCharsets.UTF_8)) {
            chinese.load(zh); english.load(en);
        }
        assertEquals(chinese.getKeys(true), english.getKeys(true));
        new Messages(new YamlConfiguration(), chinese);
        new Messages(new YamlConfiguration(), english);
    }
}
