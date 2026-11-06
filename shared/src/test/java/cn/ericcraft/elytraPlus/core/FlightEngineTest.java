package cn.ericcraft.elytraPlus.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;

class FlightEngineTest {
    private FakePlayer player;
    private FlightEngine engine;
    private Map<String, Object> durability;
    private double roll;
    private void rebuild() {
        Map<String, Object> values = new HashMap<>();
        values.put("settings", Collections.singletonMap("durability", durability));
        Map<String, Object> text = new HashMap<>();
        for (String key : Arrays.asList("prefix", "flight-enabled", "flight-disabled", "permission-revoked", "world-disabled", "elytra-broke-midair", "already-can-fly")) text.put(key, key.equals("prefix") ? "" : key);
        MapConfig messages = new MapConfig(Collections.singletonMap("messages", text));
        engine = new FlightEngine(new FlightSettings(new MapConfig(values)), new MessageCatalog(messages, messages),
                id -> player.online ? player : null, () -> roll, Logger.getAnonymousLogger());
    }
    @BeforeEach void setup() { player = new FakePlayer(); durability = new HashMap<>(); rebuild(); }
    private void enable() { engine.toggle(player); assertTrue(engine.hasSession(player.id)); }
    private void stopped() { assertFalse(engine.hasSession(player.id)); assertFalse(player.allow); assertFalse(player.flying); }
    @Test void enablingAndStandingNeverForceTakeoffOrDamage() { enable(); engine.tick(); assertTrue(player.allow); assertFalse(player.flying); assertEquals(0, player.damage); }
    @Test void togglingOffStillWorksAfterPermissionAndEquipmentAreLost() { enable(); player.use = false; player.equipped = false; engine.toggle(player); stopped(); }
    @Test void externalFlightIsNeverClaimed() { player.allow = true; engine.toggle(player); assertFalse(engine.hasSession(player.id)); assertTrue(player.allow); }
    @Test void nativeFlightIsNeverClaimed() { player.nativeMode = true; player.allow = true; engine.toggle(player); assertFalse(engine.hasSession(player.id)); assertTrue(player.allow); }
    @Test void revokedPermissionClosesTheSession() { enable(); player.use = false; engine.tick(); stopped(); }
    @Test void lostEquipmentClosesTheSession() { enable(); player.equipped = false; engine.tick(); stopped(); }
    @Test void externallyRevokedAbilitiesAreNotReenabled() { enable(); player.allow = false; engine.tick(); stopped(); }
    @Test void damageStopsAtBrokenThresholdAndKeepsTheItem() { enable(); player.damage = 430; player.flying = true; engine.tick(); stopped(); assertEquals(431, player.damage); assertTrue(player.equipped); assertEquals(1, player.sounds); }
    @Test void customizedMaximumDamageIsRespected() { enable(); player.maximum = 10; player.damage = 8; player.flying = true; engine.tick(); stopped(); assertEquals(9, player.damage); }
    @Test void unbreakableAndDurabilityBypassDoNotWear() { enable(); player.flying = true; player.unbreakable = true; engine.tick(); assertEquals(0, player.damage); player.unbreakable = false; player.bypass = true; engine.tick(); assertEquals(0, player.damage); }
    @Test void brokenItemsRemainInvalidWithDurabilityDisabled() { durability.put("enabled", false); rebuild(); player.damage = 431; engine.toggle(player); stopped(); }
    @Test void bypassStillRequiresUsableEquipment() { player.bypass = true; player.damage = 431; engine.toggle(player); stopped(); }
    @Test void unbreakingUsesExactProbabilityBoundary() { enable(); player.flying = true; player.unbreaking = 3; roll = .25; engine.tick(); assertEquals(0, player.damage); roll = .249; engine.tick(); assertEquals(1, player.damage); }
    @Test void customZeroAndOneProbabilities() { durability.put("use-vanilla-formula", false); durability.put("custom-chance", 0); rebuild(); enable(); player.flying = true; engine.tick(); assertEquals(0, player.damage); engine.shutdown(); durability.put("custom-chance", 1); rebuild(); enable(); player.flying = true; engine.tick(); assertEquals(1, player.damage); }
    @Test void shutdownPreservesCreativeFlightAndIsIdempotent() { enable(); player.nativeMode = true; player.flying = true; engine.shutdown(); engine.shutdown(); assertTrue(player.allow); assertTrue(player.flying); assertFalse(engine.hasSession(player.id)); }
    @Test void nativeTransitionReleasesOwnership() { enable(); engine.enteringNativeMode(player); assertFalse(engine.hasSession(player.id)); assertTrue(player.allow); }
    @Test void disconnectedPlayerIsForgotten() { enable(); player.online = false; engine.tick(); assertFalse(engine.hasSession(player.id)); }
    @Test void deadPlayersLoseFlight() { enable(); player.alive = false; engine.validate(player); stopped(); }

    static final class FakePlayer implements FlightPlayer {
        final UUID id = UUID.randomUUID();
        boolean online = true, alive = true, use = true, equipped = true;
        boolean nativeMode, allow, flying, unbreakable, bypass;
        int damage, unbreaking, sounds, maximum = 432;
        final List<String> messages = new ArrayList<>();
        @Override public UUID id() { return id; }
        @Override public boolean online() { return online; }
        @Override public boolean dead() { return !alive; }
        @Override public boolean nativeFlight() { return nativeMode; }
        @Override public boolean allowFlight() { return allow; }
        @Override public boolean flying() { return flying; }
        @Override public void allowFlight(boolean value) { allow = value; }
        @Override public void flying(boolean value) { flying = value; }
        @Override public boolean permission(String node) { return node.equals("elytraplus.use") ? use : bypass; }
        @Override public String world() { return "minecraft:overworld"; }
        @Override public boolean wearingElytra() { return equipped; }
        @Override public boolean usableElytra() { return equipped && damage < maximum - 1; }
        @Override public boolean unbreakable() { return unbreakable; }
        @Override public int unbreaking() { return unbreaking; }
        @Override public void damageOne() { damage = Math.min(damage + 1, maximum - 1); }
        @Override public void breakSound() { sounds++; }
        @Override public void message(String text) { messages.add(text); }
    }
}
