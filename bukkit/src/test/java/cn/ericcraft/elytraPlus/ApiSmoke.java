package cn.ericcraft.elytraPlus;

import cn.ericcraft.elytraPlus.compat.BukkitElytraAccess;
import cn.ericcraft.elytraPlus.config.FlightSettings;
import cn.ericcraft.elytraPlus.config.Messages;
import cn.ericcraft.elytraPlus.manager.FlightManager;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.Collections;
import java.util.UUID;
import java.util.jar.JarFile;
import java.util.logging.Logger;

/** Runs unchanged against each API, without pretending to be a real server test. */
public final class ApiSmoke {
    private static int damage;
    private static int maximum = 432;
    private static boolean unbreakable;
    private static boolean allow;
    private static boolean flying;

    public static void main(String[] args) throws Exception {
        try (JarFile jar = new JarFile(args[0])) {
            org.bukkit.plugin.PluginDescriptionFile descriptor = new org.bukkit.plugin.PluginDescriptionFile(
                    jar.getInputStream(jar.getJarEntry("plugin.yml")));
            check(descriptor.getMain().equals("cn.ericcraft.elytraPlus.ElytraPlus"), "plugin descriptor loads");
            java.util.Enumeration<java.util.jar.JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.endsWith(".class")) {
                    Class<?> type = Class.forName(name.substring(0, name.length() - 6).replace('/', '.'), false, ApiSmoke.class.getClassLoader());
                    type.getDeclaredMethods();
                    type.getDeclaredConstructors();
                }
            }
        }
        BukkitElytraAccess access = new BukkitElytraAccess();
        Class<?> metaType;
        try { metaType = Class.forName("org.bukkit.inventory.meta.Damageable"); }
        catch (ClassNotFoundException e) { metaType = ItemMeta.class; }
        if (!ItemMeta.class.isAssignableFrom(metaType)) metaType = combinedMeta();
        ItemMeta meta = (ItemMeta) mock(metaType, (p, m, a) -> {
            switch (m.getName()) {
                case "getDamage": return damage;
                case "setDamage": damage = (Integer) a[0]; return null;
                case "hasMaxDamage": return true;
                case "getMaxDamage": return maximum;
                case "isUnbreakable": return unbreakable;
                case "getEnchants": return Collections.emptyMap();
                case "spigot": return new LegacyUnbreakable();
                default: return defaultValue(m.getReturnType());
            }
        });
        FixtureItem item = new FixtureItem(meta);
        check(access.isUsable(item), "fresh elytra usable");
        damage = 430;
        access.damageOne(item);
        check(damage == 431 && !access.isUsable(item), "elytra stops at one durability");
        if (hasMethod(metaType, "hasMaxDamage")) {
            maximum = 10; damage = 8; access.damageOne(item);
            check(damage == 9 && !access.isUsable(item), "custom max damage supported");
        }
        unbreakable = true;
        check(access.isUnbreakable(item), "unbreakable supported");
        check(access.unbreakingLevel(item) == 0, "unenchanted item supported");
        check(!access.isUsable(null), "missing item rejected");
        maximum = 432; damage = 0; unbreakable = false;
        World world = mock(World.class, (p, m, a) -> m.getName().equals("getName") ? "world" : defaultValue(m.getReturnType()));
        PlayerInventory inventory = mock(PlayerInventory.class, (p, m, a) -> m.getName().equals("getChestplate") ? item : defaultValue(m.getReturnType()));
        UUID uuid = UUID.randomUUID();
        Player player = mock(Player.class, (p, m, a) -> {
            switch (m.getName()) {
                case "getUniqueId": return uuid;
                case "isOnline": return true;
                case "getGameMode": return GameMode.SURVIVAL;
                case "hasPermission": return "elytraplus.use".equals(a[0]);
                case "getInventory": return inventory;
                case "getWorld": return world;
                case "getAllowFlight": return allow;
                case "setAllowFlight": allow = (Boolean) a[0]; return null;
                case "isFlying": return flying;
                case "setFlying": flying = (Boolean) a[0]; return null;
                default: return defaultValue(m.getReturnType());
            }
        });
        YamlConfiguration config = new YamlConfiguration();
        YamlConfiguration defaults = new YamlConfiguration();
        defaults.set("messages.prefix", "");
        FlightManager flights = new FlightManager(new FlightSettings(config), new Messages(config, defaults), access,
                id -> player, () -> 0, Logger.getAnonymousLogger());
        flights.toggle(player); check(allow && !flying, "enable allows but does not force flight");
        flying = true; flights.tick(); check(damage == 1, "flying consumes durability");
        flights.shutdown(); check(!allow && !flying && !flights.hasSession(uuid), "shutdown clears owned flight");
        System.out.println("PASS: unchanged JAR class loading, item adapters and flight lifecycle (API fixture, not server gameplay)");
    }

    /** Early Damageable did not extend ItemMeta and both declare clone().
     * A covariant test interface makes the fixture assignable to both without
     * conflicting clone return types. Only this test fixture is compiled at runtime.
     */
    private static Class<?> combinedMeta() throws Exception {
        java.nio.file.Path directory = java.nio.file.Files.createTempDirectory("elytraplus-api-meta-");
        java.nio.file.Path source = directory.resolve("MixedMeta.java");
        String code = "public interface MixedMeta extends org.bukkit.inventory.meta.ItemMeta, "
                + "org.bukkit.inventory.meta.Damageable { MixedMeta clone(); }";
        java.nio.file.Files.write(source, code.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        javax.tools.JavaCompiler compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
        check(compiler != null, "API harness requires a JDK");
        int result = compiler.run(null, null, null, "-classpath", System.getProperty("java.class.path"),
                "-d", directory.toString(), source.toString());
        check(result == 0, "combined metadata fixture compiles");
        try (java.net.URLClassLoader loader = new java.net.URLClassLoader(
                new java.net.URL[]{directory.toUri().toURL()}, ApiSmoke.class.getClassLoader())) {
            return Class.forName("MixedMeta", true, loader);
        } finally {
            java.nio.file.Files.deleteIfExists(directory.resolve("MixedMeta.class"));
            java.nio.file.Files.deleteIfExists(source);
            java.nio.file.Files.deleteIfExists(directory);
        }
    }

    private static boolean hasMethod(Class<?> type, String name) {
        try { type.getMethod(name); return true; } catch (NoSuchMethodException e) { return false; }
    }
    /** JDK 17 dynamic proxies initialize method-signature API types, including registries that require a live server. */
    private static <T> T mock(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return org.mockito.Mockito.mock(type, invocation -> handler.invoke(
                invocation.getMock(), invocation.getMethod(), invocation.getArguments()));
    }
    private static void check(boolean value, String description) {
        if (!value) throw new AssertionError(description);
    }
    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0f;
        if (type == double.class) return 0d;
        if (type == short.class) return (short) 0;
        if (type == byte.class) return (byte) 0;
        if (type == char.class) return (char) 0;
        return null;
    }
    public static final class FixtureItem extends ItemStack {
        private ItemMeta meta;
        FixtureItem(ItemMeta meta) { super(); this.meta = meta; }
        @Override public Material getType() { return Material.ELYTRA; }
        @Override public ItemMeta getItemMeta() { return meta; }
        @Override public boolean setItemMeta(ItemMeta meta) { this.meta = meta; return true; }
        @Override public short getDurability() { return (short) damage; }
        @Override public void setDurability(short value) { damage = value; }
    }
    // Isolated nested class is only loaded when the pre-1.11 API requests it.
    public static final class LegacyUnbreakable extends ItemMeta.Spigot {
        @Override public boolean isUnbreakable() { return unbreakable; }
    }
}
