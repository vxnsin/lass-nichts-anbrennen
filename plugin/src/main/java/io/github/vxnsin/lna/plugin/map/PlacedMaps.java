package io.github.vxnsin.lna.plugin.map;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.util.TreeMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.BoundingBox;

/** Remembers where maps have been built (plugins/LassNichtsAnbrennen/placed-maps.yml), so missions find them. */
public final class PlacedMaps {

    /** A built map: north-west floor corner, size and spawn point. */
    public record PlacedMap(String id, String world, int x, int y, int z, int width, int depth, int height,
                            double spawnX, double spawnY, double spawnZ, float spawnYaw) {

        public Optional<World> bukkitWorld() {
            return Optional.ofNullable(Bukkit.getWorld(world));
        }

        public Optional<Location> spawn() {
            return bukkitWorld().map(w -> new Location(w, spawnX, spawnY, spawnZ, spawnYaw, 0));
        }

        public BoundingBox bounds() {
            return new BoundingBox(x, y, z, x + width, y + height + 2, z + depth);
        }

        public boolean contains(Location location) {
            return location.getWorld().getName().equals(world) && bounds().contains(location.toVector());
        }
    }

    private final File file;
    private final Map<String, PlacedMap> maps = new TreeMap<>();

    public PlacedMaps(File file) {
        this.file = file;
    }

    public void load() {
        maps.clear();
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String id : yaml.getKeys(false)) {
            ConfigurationSection map = yaml.getConfigurationSection(id);
            if (map == null) {
                continue;
            }
            maps.put(id, new PlacedMap(id, map.getString("world"), map.getInt("x"), map.getInt("y"), map.getInt("z"),
                    map.getInt("width"), map.getInt("depth"), map.getInt("height"), map.getDouble("spawn.x"),
                    map.getDouble("spawn.y"), map.getDouble("spawn.z"), (float) map.getDouble("spawn.yaw")));
        }
    }

    public void put(PlacedMap map) throws IOException {
        maps.put(map.id(), map);
        YamlConfiguration yaml = new YamlConfiguration();
        for (PlacedMap placed : maps.values()) {
            ConfigurationSection section = yaml.createSection(placed.id());
            section.set("world", placed.world());
            section.set("x", placed.x());
            section.set("y", placed.y());
            section.set("z", placed.z());
            section.set("width", placed.width());
            section.set("depth", placed.depth());
            section.set("height", placed.height());
            section.set("spawn.x", placed.spawnX());
            section.set("spawn.y", placed.spawnY());
            section.set("spawn.z", placed.spawnZ());
            section.set("spawn.yaw", placed.spawnYaw());
        }
        yaml.save(file);
    }

    public Optional<PlacedMap> get(String id) {
        return Optional.ofNullable(maps.get(id));
    }

    public Collection<PlacedMap> all() {
        return maps.values();
    }
}
