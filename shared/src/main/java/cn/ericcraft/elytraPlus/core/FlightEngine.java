package cn.ericcraft.elytraPlus.core;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Owns plugin flight sessions. All calls must run on the server thread. */
public final class FlightEngine {
    private final Set<UUID> sessions = new HashSet<>();
    private final FlightSettings settings;
    private final MessageCatalog messages;
    private final Function<UUID, FlightPlayer> players;
    private final DoubleSupplier random;
    private final Logger logger;

    public FlightEngine(FlightSettings settings, MessageCatalog messages,
                         Function<UUID, FlightPlayer> players, DoubleSupplier random, Logger logger) {
        this.settings = settings;
        this.messages = messages;
        this.players = players;
        this.random = random;
        this.logger = logger;
    }

    public boolean hasSession(UUID uuid) { return sessions.contains(uuid); }

    public void toggle(FlightPlayer player) {
        if (stop(player, "flight-disabled")) return;
        if (nativeFlight(player) || player.allowFlight() || player.flying()) {
            messages.send(player, "already-can-fly");
            return;
        }
        String rejection = rejection(player, false);
        if (rejection != null) {
            messages.send(player, rejection);
            return;
        }
        if (player.dead() || !player.online()) return;
        player.allowFlight(true);
        sessions.add(player.id());
        messages.send(player, "flight-enabled");
    }

    /** Returns whether the player still owns a valid plugin session. */
    public boolean validate(FlightPlayer player) {
        if (!hasSession(player.id())) return false;
        if (!player.online() || player.dead() || nativeFlight(player)) {
            stop(player, null);
            return false;
        }
        if (!player.allowFlight()) {
            stop(player, "flight-revoked");
            return false;
        }
        String reason = rejection(player, true);
        if (reason != null) {
            stop(player, reason);
            return false;
        }
        return true;
    }

    public boolean stop(FlightPlayer player, String message) {
        if (!sessions.remove(player.id())) return false;
        if (!nativeFlight(player)) {
            player.flying(false);
            player.allowFlight(false);
        }
        if (message != null && player.online()) messages.send(player, message);
        return true;
    }

    /** A successful mode transition will let the server manage flight from here. */
    public void enteringNativeMode(FlightPlayer player) {
        sessions.remove(player.id());
    }

    public void tick() {
        for (UUID uuid : new HashSet<>(sessions)) {
            FlightPlayer player = players.apply(uuid);
            if (player == null) {
                sessions.remove(uuid);
                continue;
            }
            try {
                checkAndDamage(player);
            } catch (RuntimeException e) {
                logger.log(Level.SEVERE, "Flight check failed for " + uuid + "; closing session", e);
                stop(player, "flight-error");
            }
        }
    }

    private void checkAndDamage(FlightPlayer player) {
        if (!validate(player) || !player.flying() || !settings.durabilityEnabled ||
                player.permission("elytraplus.bypass.durability")) return;
        if (player.unbreakable()) return;
        double chance = settings.vanillaFormula ? 1.0 / (player.unbreaking() + 1.0) : settings.customChance;
        if (chance <= 0 || (chance < 1 && random.getAsDouble() >= chance)) return;
        player.damageOne();
        if (!player.usableElytra()) {
            stop(player, "elytra-broke-midair");
            player.breakSound();
        }
    }

    public void shutdown() {
        for (UUID uuid : new HashSet<>(sessions)) {
            FlightPlayer player = players.apply(uuid);
            if (player != null) stop(player, null);
        }
        sessions.clear();
    }

    private String rejection(FlightPlayer player, boolean active) {
        if (!player.permission("elytraplus.use")) return active ? "permission-revoked" : "no-permission";
        if (!settings.allowsWorld(player.world()) && !player.permission("elytraplus.bypass.world")) {
            return "world-disabled";
        }
        if (!player.wearingElytra()) return active ? "no-elytra-equipped" : "no-elytra";
        if (!player.usableElytra()) return active ? "elytra-broke-midair" : "elytra-broken";
        return null;
    }

    private static boolean nativeFlight(FlightPlayer player) {
        return player.nativeFlight();
    }
}
