package cn.ericcraft.elytraPlus;

import cn.ericcraft.elytraPlus.compat.BukkitElytraAccess;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BukkitElytraAccessTest {
    private final BukkitElytraAccess access = new BukkitElytraAccess();
    private ItemStack item;
    private ItemMeta meta;
    private AtomicInteger damage;

    @BeforeEach void setup() {
        item = mock(ItemStack.class); meta = mock(ItemMeta.class); damage = new AtomicInteger();
        when(item.getType()).thenReturn(Material.ELYTRA);
        when(item.getItemMeta()).thenReturn(meta);
        when(item.getDurability()).thenAnswer(i -> (short) damage.get());
        doAnswer(i -> { damage.set((Short) i.getArgument(0)); return null; }).when(item).setDurability(anyShort());
    }
    @Test void legacyDurabilityStopsAtOneRemaining() {
        damage.set(430); assertTrue(access.isUsable(item));
        access.damageOne(item); assertEquals(431, damage.get()); assertFalse(access.isUsable(item));
        access.damageOne(item); assertEquals(431, damage.get());
    }
    @Test void rejectsMissingWrongAndAlreadyOverdamagedItems() {
        assertFalse(access.isUsable(null));
        when(item.getType()).thenReturn(Material.DIAMOND_CHESTPLATE); assertFalse(access.isUsable(item));
        when(item.getType()).thenReturn(Material.ELYTRA); damage.set(432); assertFalse(access.isUsable(item));
    }
    @Test void legacyUnbreakableAndEnchantment() {
        ItemMeta.Spigot legacy = mock(ItemMeta.Spigot.class);
        when(meta.spigot()).thenReturn(legacy); when(legacy.isUnbreakable()).thenReturn(true);
        assertTrue(access.isUnbreakable(item));
        Enchantment enchantment = mock(Enchantment.class); when(enchantment.getName()).thenReturn("DURABILITY");
        when(meta.getEnchants()).thenReturn(Collections.singletonMap(enchantment, 3));
        assertEquals(3, access.unbreakingLevel(item));
        when(enchantment.getName()).thenReturn("MENDING"); assertEquals(0, access.unbreakingLevel(item));
    }
}
