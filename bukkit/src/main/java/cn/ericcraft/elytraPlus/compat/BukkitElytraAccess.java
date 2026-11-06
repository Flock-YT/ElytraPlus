package cn.ericcraft.elytraPlus.compat;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/** Only public API is reflected; modern classes are never linked on old servers. */
public final class BukkitElytraAccess implements ElytraAccess {
    private final Method getDamage;
    private final Method setDamage;
    private final Method hasMaxDamage;
    private final Method getMaxDamage;
    private final Method unbreakable;
    private final Method spigot;
    private final Method enchantmentKey;
    private final Method enchantmentName;

    public BukkitElytraAccess() {
        Class<?> damageable = optionalClass("org.bukkit.inventory.meta.Damageable");
        getDamage = optionalMethod(damageable, "getDamage");
        setDamage = optionalMethod(damageable, "setDamage", int.class);
        hasMaxDamage = optionalMethod(damageable, "hasMaxDamage");
        getMaxDamage = optionalMethod(damageable, "getMaxDamage");
        Method direct = optionalMethod(ItemMeta.class, "isUnbreakable");
        spigot = direct == null ? optionalMethod(ItemMeta.class, "spigot") : null;
        unbreakable = direct != null ? direct : optionalMethod(
                spigot == null ? null : spigot.getReturnType(), "isUnbreakable");
        enchantmentKey = optionalMethod(Enchantment.class, "getKey");
        enchantmentName = optionalMethod(Enchantment.class, "getName");
        if (enchantmentKey == null && enchantmentName == null) {
            throw new IllegalStateException("Bukkit API cannot identify enchantments");
        }
    }

    @Override
    public boolean isUsable(ItemStack item) {
        return item != null && item.getType() == Material.ELYTRA && damage(item) < maximum(item) - 1;
    }

    @Override
    public boolean isUnbreakable(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null || unbreakable == null) return false;
        Object receiver = spigot == null ? meta : invoke(spigot, meta);
        return (Boolean) invoke(unbreakable, receiver);
    }

    @Override
    public int unbreakingLevel(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0;
        for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
            String name = String.valueOf(invoke(enchantmentKey != null ? enchantmentKey : enchantmentName, entry.getKey()));
            if (name.equals("minecraft:unbreaking") || name.equals("DURABILITY") || name.equals("UNBREAKING")) {
                return Math.max(0, entry.getValue());
            }
        }
        return 0;
    }

    @Override
    public void damageOne(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        int next = Math.min(damage(item) + 1, maximum(item) - 1);
        if (setDamage != null && meta != null) {
            invoke(setDamage, meta, next);
            item.setItemMeta(meta);
        } else {
            item.setDurability((short) next);
        }
    }

    private int damage(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        return getDamage != null && meta != null ? (Integer) invoke(getDamage, meta) : item.getDurability();
    }

    private int maximum(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (hasMaxDamage != null && getMaxDamage != null && meta != null && (Boolean) invoke(hasMaxDamage, meta)) {
            return (Integer) invoke(getMaxDamage, meta);
        }
        int maximum = item.getType().getMaxDurability();
        // Bukkit 1.9-1.12 reports 431 for elytra, corrected to 432 in 1.13.
        // 431 is the broken damage threshold, not the item's full durability.
        return getDamage == null && maximum == 431 ? 432 : maximum;
    }

    private static Class<?> optionalClass(String name) {
        try { return Class.forName(name); }
        catch (ClassNotFoundException e) { return null; }
    }

    private static Method optionalMethod(Class<?> type, String name, Class<?>... args) {
        if (type == null) return null;
        try { return type.getMethod(name, args); }
        catch (NoSuchMethodException e) { return null; }
    }

    private static Object invoke(Method method, Object receiver, Object... args) {
        try { return method.invoke(receiver, args); }
        catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Bukkit item API call failed: " + method.getName(), e);
        }
    }
}
