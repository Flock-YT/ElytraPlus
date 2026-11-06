package cn.ericcraft.elytraPlus.manager;

import cn.ericcraft.elytraPlus.compat.ElytraAccess;
import cn.ericcraft.elytraPlus.config.FlightSettings;
import cn.ericcraft.elytraPlus.config.Messages;
import cn.ericcraft.elytraPlus.core.FlightEngine;
import org.bukkit.entity.Player;
import java.util.UUID;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.logging.Logger;

/** Bukkit facade; all session decisions belong to the shared core. */
public final class FlightManager {
    private final FlightEngine engine;
    private final ElytraAccess items;
    public FlightManager(FlightSettings settings, Messages messages, ElytraAccess items,
                         Function<UUID, Player> players, DoubleSupplier random, Logger logger) {
        this.items = items;
        engine = new FlightEngine(settings, messages.catalog(), id -> {
            Player player = players.apply(id);
            return player == null ? null : wrap(player);
        }, random, logger);
    }
    private BukkitFlightPlayer wrap(Player player) { return new BukkitFlightPlayer(player, items); }
    public boolean hasSession(UUID id) { return engine.hasSession(id); }
    public void toggle(Player player) { engine.toggle(wrap(player)); }
    public boolean validate(Player player) { return engine.validate(wrap(player)); }
    public boolean stop(Player player, String message) { return engine.stop(wrap(player), message); }
    public void enteringNativeMode(Player player) { engine.enteringNativeMode(wrap(player)); }
    public void tick() { engine.tick(); }
    public void shutdown() { engine.shutdown(); }
}
