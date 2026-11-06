package cn.ericcraft.elytraPlus.config;

import org.bukkit.configuration.ConfigurationSection;

public final class FlightSettings extends cn.ericcraft.elytraPlus.core.FlightSettings {
    public FlightSettings(ConfigurationSection config) { super(new BukkitConfigView(config)); }
}
