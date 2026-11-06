package cn.ericcraft.elytraPlus.compat;

import org.bukkit.inventory.ItemStack;

/** The only item operations needed by flight rules. */
public interface ElytraAccess {
    boolean isUsable(ItemStack item);
    boolean isUnbreakable(ItemStack item);
    int unbreakingLevel(ItemStack item);
    void damageOne(ItemStack item);
}
