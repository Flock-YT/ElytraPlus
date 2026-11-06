package cn.ericcraft.elytraPlus.listener;

import cn.ericcraft.elytraPlus.config.Messages;
import cn.ericcraft.elytraPlus.manager.FlightManager;
import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class FlightListener implements Listener {
    private final FlightManager flights;
    private final Messages messages;
    private final Set<UUID> respawnMessages = new HashSet<>();

    public FlightListener(FlightManager flights, Messages messages) {
        this.flights = flights;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onToggle(PlayerToggleFlightEvent event) {
        if (event.isFlying() && flights.hasSession(event.getPlayer().getUniqueId()) && !flights.validate(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) { flights.validate(event.getPlayer()); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameMode(PlayerGameModeChangeEvent event) {
        if (event.getNewGameMode() == GameMode.CREATIVE || event.getNewGameMode() == GameMode.SPECTATOR) {
            flights.enteringNativeMode(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        if (flights.stop(event.getEntity(), null)) respawnMessages.add(event.getEntity().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        boolean stopped = flights.stop(event.getPlayer(), null);
        if (respawnMessages.remove(event.getPlayer().getUniqueId()) || stopped) {
            messages.send(event.getPlayer(), "respawn-disabled");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        flights.stop(event.getPlayer(), null);
        respawnMessages.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKick(PlayerKickEvent event) {
        flights.stop(event.getPlayer(), null);
        respawnMessages.remove(event.getPlayer().getUniqueId());
    }
}
