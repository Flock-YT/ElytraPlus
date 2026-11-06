package cn.ericcraft.elytraPlus;

import cn.ericcraft.elytraPlus.compat.ElytraAccess;
import cn.ericcraft.elytraPlus.config.FlightSettings;
import cn.ericcraft.elytraPlus.config.Messages;
import cn.ericcraft.elytraPlus.manager.FlightManager;
import cn.ericcraft.elytraPlus.listener.FlightListener;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FlightManagerTest {
    private Player player;
    private PlayerInventory inventory;
    private ItemStack elytra;
    private ElytraAccess items;
    private YamlConfiguration config;
    private Messages messages;
    private FlightManager flights;
    private AtomicBoolean allow;
    private AtomicBoolean flying;
    private double roll;

    @BeforeEach void setup() {
        player = mock(Player.class);
        inventory = mock(PlayerInventory.class);
        elytra = mock(ItemStack.class);
        items = mock(ElytraAccess.class);
        config = new YamlConfiguration();
        messages = new Messages(config, YamlConfiguration.loadConfiguration(new InputStreamReader(
                getClass().getResourceAsStream("/lang/zh_CN.yml"), StandardCharsets.UTF_8)));
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.isOnline()).thenReturn(true);
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
        when(player.hasPermission("elytraplus.use")).thenReturn(true);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getChestplate()).thenReturn(elytra);
        when(elytra.getType()).thenReturn(Material.ELYTRA);
        when(items.isUsable(elytra)).thenReturn(true);
        World world = mock(World.class);
        when(world.getName()).thenReturn("world");
        when(player.getWorld()).thenReturn(world);
        allow = new AtomicBoolean();
        flying = new AtomicBoolean();
        when(player.getAllowFlight()).thenAnswer(i -> allow.get());
        when(player.isFlying()).thenAnswer(i -> flying.get());
        doAnswer(i -> { allow.set(i.getArgument(0)); return null; }).when(player).setAllowFlight(anyBoolean());
        doAnswer(i -> { flying.set(i.getArgument(0)); return null; }).when(player).setFlying(anyBoolean());
        rebuild();
    }

    private void rebuild() {
        flights = new FlightManager(new FlightSettings(config), messages, items,
                uuid -> player, () -> roll, Logger.getAnonymousLogger());
    }
    private void enable() { flights.toggle(player); assertTrue(flights.hasSession(player.getUniqueId())); }
    private void assertStopped() { assertFalse(flights.hasSession(player.getUniqueId())); assertFalse(allow.get()); assertFalse(flying.get()); }

    @Test void enablingDoesNotForceTakeoffAndStandingDoesNotDamage() {
        enable(); assertTrue(allow.get()); assertFalse(flying.get()); flights.tick();
        verify(items, never()).damageOne(any());
    }
    @Test void toggleOffWorksAfterAllRequirementsAreLost() {
        enable(); when(player.hasPermission("elytraplus.use")).thenReturn(false);
        when(inventory.getChestplate()).thenReturn(null);
        flights.toggle(player); assertStopped();
    }
    @Test void refusesExternalAndNativeFlight() {
        allow.set(true); flights.toggle(player); assertTrue(allow.get());
        assertFalse(flights.hasSession(player.getUniqueId()));
        allow.set(false); when(player.getGameMode()).thenReturn(GameMode.CREATIVE);
        flights.toggle(player); assertFalse(flights.hasSession(player.getUniqueId()));
        when(player.getGameMode()).thenReturn(GameMode.SPECTATOR);
        flights.toggle(player); assertFalse(flights.hasSession(player.getUniqueId()));
    }
    @Test void permissionRevocationStopsFlight() {
        enable(); flying.set(true); when(player.hasPermission("elytraplus.use")).thenReturn(false);
        flights.tick(); assertStopped();
    }
    @Test void worldChangeStopsImmediatelyAndBypassAllowsIt() {
        config.set("settings.world-list.type", "BLACKLIST");
        config.set("settings.world-list.worlds", Collections.singletonList("blocked")); rebuild(); enable();
        when(player.getWorld().getName()).thenReturn("blocked");
        new FlightListener(flights, messages).onWorldChange(new PlayerChangedWorldEvent(player, mock(World.class)));
        assertStopped();
        when(player.hasPermission("elytraplus.bypass.world")).thenReturn(true); enable();
    }
    @Test void disabledWorldRestrictionsAllowListedWorldAndWorldChangesWithoutBypass() {
        config.set("settings.world-list.type", false);
        config.set("settings.world-list.worlds", Collections.singletonList("blocked"));
        rebuild();
        assertFalse(player.hasPermission("elytraplus.bypass.world"));
        when(player.getWorld().getName()).thenReturn("blocked");
        enable();
        flying.set(true);
        FlightListener listener = new FlightListener(flights, messages);
        for (String name : new String[] {"other_world", "blocked"}) {
            when(player.getWorld().getName()).thenReturn(name);
            listener.onWorldChange(new PlayerChangedWorldEvent(player, mock(World.class)));
            flights.tick();
            assertTrue(flights.hasSession(player.getUniqueId()));
            assertTrue(allow.get());
            assertTrue(flying.get());
        }
        when(player.hasPermission("elytraplus.use")).thenReturn(false);
        flights.tick(); assertStopped();
        when(player.hasPermission("elytraplus.use")).thenReturn(true);
        enable();
        when(inventory.getChestplate()).thenReturn(null);
        flights.tick(); assertStopped();
    }
    @Test void invalidTakeoffIsCancelled() {
        enable(); when(inventory.getChestplate()).thenReturn(null);
        PlayerToggleFlightEvent event = new PlayerToggleFlightEvent(player, true);
        new FlightListener(flights, messages).onToggle(event);
        assertTrue(event.isCancelled()); assertStopped();
    }
    @Test void brokenElytraStopsEvenWhenDamageDisabled() {
        config.set("settings.durability.enabled", false); rebuild(); enable();
        when(items.isUsable(elytra)).thenReturn(false); flights.tick(); assertStopped();
    }
    @Test void bypassDoesNotAllowBrokenElytra() {
        enable(); when(player.hasPermission("elytraplus.bypass.durability")).thenReturn(true);
        when(items.isUsable(elytra)).thenReturn(false); flights.tick(); assertStopped();
    }
    @Test void exhaustedElytraRemainsEquipped() {
        enable(); flying.set(true);
        doAnswer(i -> { when(items.isUsable(elytra)).thenReturn(false); return null; }).when(items).damageOne(elytra);
        flights.tick(); assertStopped(); verify(inventory).setChestplate(elytra);
        verify(inventory, never()).setChestplate(isNull());
    }
    @Test void respectsUnbreakableAndBypass() {
        enable(); flying.set(true); when(items.isUnbreakable(elytra)).thenReturn(true);
        flights.tick(); verify(items, never()).damageOne(any());
        when(items.isUnbreakable(elytra)).thenReturn(false);
        when(player.hasPermission("elytraplus.bypass.durability")).thenReturn(true);
        flights.tick(); verify(items, never()).damageOne(any());
    }
    @Test void unbreakingProbabilityHasDeterministicBoundary() {
        enable(); flying.set(true); when(items.unbreakingLevel(elytra)).thenReturn(3);
        roll = 0.25; flights.tick(); verify(items, never()).damageOne(any());
        roll = 0.249; flights.tick(); verify(items).damageOne(elytra);
    }
    @Test void customZeroAndOneProbabilities() {
        config.set("settings.durability.use-vanilla-formula", false);
        config.set("settings.durability.custom-chance", 0); rebuild(); enable(); flying.set(true);
        flights.tick(); verify(items, never()).damageOne(any()); flights.stop(player, null);
        config.set("settings.durability.custom-chance", 1); rebuild(); enable(); flying.set(true);
        roll = 0.999; flights.tick(); verify(items).damageOne(elytra);
    }
    @Test void externalRevocationIsNeverReenabled() {
        enable(); allow.set(false); clearInvocations(player); flights.tick(); assertStopped();
        verify(player, never()).setAllowFlight(true);
    }
    @Test void shutdownPreservesCreativeFlightAndIsIdempotent() {
        enable(); when(player.getGameMode()).thenReturn(GameMode.CREATIVE); flying.set(true);
        flights.shutdown(); flights.shutdown(); assertTrue(allow.get()); assertTrue(flying.get());
        assertFalse(flights.hasSession(player.getUniqueId()));
    }
    @Test void nativeModeTransitionReleasesOwnership() {
        enable(); FlightListener listener = new FlightListener(flights, messages);
        listener.onGameMode(new PlayerGameModeChangeEvent(player, GameMode.SPECTATOR));
        assertFalse(flights.hasSession(player.getUniqueId())); assertTrue(allow.get());
        flights.shutdown(); assertTrue(allow.get());
    }
    @Test void deathRespawnAndQuitClearSessions() {
        enable(); FlightListener listener = new FlightListener(flights, messages);
        PlayerDeathEvent death = mock(PlayerDeathEvent.class); when(death.getEntity()).thenReturn(player);
        listener.onDeath(death); assertStopped();
        PlayerRespawnEvent respawn = new PlayerRespawnEvent(player, new org.bukkit.Location(player.getWorld(), 0, 64, 0), false);
        listener.onRespawn(respawn); assertStopped();
        enable(); listener.onQuit(new PlayerQuitEvent(player, "")); assertStopped();
        listener.onRespawn(respawn); assertStopped();
    }
    @Test void kickAndShutdownRevokeOwnedFlight() {
        enable(); new FlightListener(flights, messages).onKick(new PlayerKickEvent(player, "", "")); assertStopped();
        enable(); flights.shutdown(); assertStopped(); flights.shutdown();
    }
    @Test void offlineSessionIsCleaned() {
        enable(); when(player.isOnline()).thenReturn(false); flights.tick(); assertStopped();
    }
    @Test void missingPlayerIsForgotten() {
        flights = new FlightManager(new FlightSettings(config), messages, items, uuid -> null, () -> 0, Logger.getAnonymousLogger());
        enable(); flights.tick(); assertFalse(flights.hasSession(player.getUniqueId()));
    }
}
