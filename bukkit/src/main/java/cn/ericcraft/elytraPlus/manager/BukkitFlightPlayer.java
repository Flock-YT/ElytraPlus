package cn.ericcraft.elytraPlus.manager;

import cn.ericcraft.elytraPlus.compat.ElytraAccess;
import cn.ericcraft.elytraPlus.core.FlightPlayer;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.UUID;

final class BukkitFlightPlayer implements FlightPlayer {
    private final Player player;
    private final ElytraAccess items;
    BukkitFlightPlayer(Player player, ElytraAccess items) { this.player = player; this.items = items; }
    private ItemStack chest() { return player.getInventory().getChestplate(); }
    @Override public UUID id() { return player.getUniqueId(); }
    @Override public boolean online() { return player.isOnline(); }
    @Override public boolean dead() { return player.isDead(); }
    @Override public boolean nativeFlight() { return player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR; }
    @Override public boolean allowFlight() { return player.getAllowFlight(); }
    @Override public boolean flying() { return player.isFlying(); }
    @Override public void allowFlight(boolean value) { player.setAllowFlight(value); }
    @Override public void flying(boolean value) { player.setFlying(value); }
    @Override public boolean permission(String node) { return player.hasPermission(node); }
    @Override public String world() { return player.getWorld().getName(); }
    @Override public boolean wearingElytra() { return chest() != null && chest().getType() == Material.ELYTRA; }
    @Override public boolean usableElytra() { return items.isUsable(chest()); }
    @Override public boolean unbreakable() { return items.isUnbreakable(chest()); }
    @Override public int unbreaking() { return items.unbreakingLevel(chest()); }
    @Override public void damageOne() {
        ItemStack item = chest(); items.damageOne(item); player.getInventory().setChestplate(item);
    }
    @Override public void breakSound() { player.playSound(player.getLocation(), "entity.item.break", 1.0f, 1.0f); }
    @Override public void message(String text) { player.sendMessage(text); }
}
