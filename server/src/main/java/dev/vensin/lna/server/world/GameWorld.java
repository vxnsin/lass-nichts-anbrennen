package dev.vensin.lna.server.world;

import dev.vensin.lna.server.map.MapBuilder;
import dev.vensin.lna.server.map.MapDefinition;
import dev.vensin.lna.server.map.PlacedMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.LightingChunk;

/** The world lives in memory only and is rebuilt from the map files on every start. */
public final class GameWorld {

    public static final String HUB = "hub";
    private static final int FLOOR_Y = 64;
    private static final int GAP = 48;

    private final InstanceContainer instance;
    private final Map<String, PlacedMap> placed = new LinkedHashMap<>();

    private GameWorld(InstanceContainer instance) {
        this.instance = instance;
    }

    public static GameWorld build(Map<String, MapDefinition> maps, MapBuilder builder) {
        InstanceContainer instance = MinecraftServer.getInstanceManager().createInstanceContainer();
        instance.setChunkSupplier(LightingChunk::new);
        instance.defaultClock().time(6000);
        instance.defaultClock().pause();

        GameWorld world = new GameWorld(instance);
        List<MapDefinition> order = new ArrayList<>(maps.values());
        order.sort(Comparator.comparing((MapDefinition map) -> !map.id().equals(HUB)).thenComparing(MapDefinition::id));
        int x = 0;
        for (MapDefinition map : order) {
            world.placed.put(map.id(), builder.build(instance, map, x, FLOOR_Y, 0));
            x += map.width() + GAP;
        }
        return world;
    }

    public InstanceContainer instance() {
        return instance;
    }

    public Optional<PlacedMap> map(String id) {
        return Optional.ofNullable(placed.get(id));
    }

    public Map<String, PlacedMap> maps() {
        return placed;
    }

    public Pos spawn() {
        return map(HUB).or(() -> placed.values().stream().findFirst()).map(PlacedMap::spawn)
                .orElse(new Pos(0.5, FLOOR_Y + 1, 0.5));
    }

    public int floorY() {
        return FLOOR_Y;
    }
}
