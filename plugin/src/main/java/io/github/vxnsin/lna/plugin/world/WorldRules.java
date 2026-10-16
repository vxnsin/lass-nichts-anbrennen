package io.github.vxnsin.lna.plugin.world;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

/**
 * Applies the game rules the campaign relies on. Vanilla fire spread is always disabled in managed worlds,
 * because the fire simulation places static fire blocks and controls them itself.
 */
public final class WorldRules implements Listener {

    private final ConfigurationSection config;

    public WorldRules(ConfigurationSection config) {
        this.config = config;
    }

    public void applyToLoadedWorlds() {
        Bukkit.getWorlds().forEach(this::apply);
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        apply(event.getWorld());
    }

    public boolean manages(World world) {
        List<String> worlds = config.getStringList("worlds");
        return worlds.isEmpty() || worlds.contains(world.getName());
    }

    private void apply(World world) {
        if (!manages(world)) {
            return;
        }
        world.setGameRule(GameRule.DO_FIRE_TICK, false);
        world.setGameRule(GameRule.MOB_GRIEFING, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, config.getBoolean("mob-spawning", false));
        boolean weather = config.getBoolean("weather-cycle", false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, weather);
        if (!weather) {
            world.setStorm(false);
            world.setThundering(false);
        }
        boolean daylight = config.getBoolean("daylight-cycle", false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, daylight);
        // Since 26.x only dimensions with a world clock (the overworld) have a time of day.
        if (!daylight && world.getEnvironment() == World.Environment.NORMAL) {
            try {
                world.setTime(config.getLong("time", 6000L));
            } catch (IllegalArgumentException noWorldClock) {
                // Custom dimension without a clock: nothing to set.
            }
        }
    }
}
