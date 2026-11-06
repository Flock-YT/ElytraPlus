package cn.ericcraft.elytraPlus;

import cn.ericcraft.elytraPlus.command.FlyCommand;
import cn.ericcraft.elytraPlus.compat.ElytraAccess;
import cn.ericcraft.elytraPlus.config.FlightSettings;
import cn.ericcraft.elytraPlus.config.Messages;
import cn.ericcraft.elytraPlus.manager.FlightManager;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FlyCommandTest {
    @Test void consoleAndInvalidArgumentsHaveNoPlayerSideEffects() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("messages.prefix", ""); config.set("messages.only-player", "Players only");
        config.set("messages.usage", "/fly");
        Messages messages = new Messages(config, config);
        FlightManager flights = new FlightManager(new FlightSettings(config), messages, mock(ElytraAccess.class),
                id -> null, () -> 0, Logger.getAnonymousLogger());
        FlyCommand command = new FlyCommand(flights, messages);
        CommandSender console = mock(CommandSender.class);
        assertTrue(command.onCommand(console, null, "fly", new String[0]));
        verify(console).sendMessage("Players only");
        Player player = mock(Player.class);
        assertTrue(command.onCommand(player, null, "fly", new String[]{"other"}));
        verify(player).sendMessage("/fly");
        verify(player, never()).setAllowFlight(anyBoolean());
    }
}
