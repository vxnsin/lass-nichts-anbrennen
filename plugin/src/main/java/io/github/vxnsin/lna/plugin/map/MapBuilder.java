package io.github.vxnsin.lna.plugin.map;

import io.github.vxnsin.lna.plugin.Keys;
import io.github.vxnsin.lna.plugin.furniture.FurnitureService;
import io.github.vxnsin.lna.plugin.furniture.FurnitureService.Placement;
import io.github.vxnsin.lna.plugin.hud.Glyphs;
import io.github.vxnsin.lna.plugin.item.CustomItem;
import io.github.vxnsin.lna.plugin.item.ItemRegistry;
import io.github.vxnsin.lna.plugin.map.MapDefinition.Cell;
import io.github.vxnsin.lna.plugin.map.MapDefinition.MapObject;
import io.github.vxnsin.lna.plugin.map.PlacedMaps.PlacedMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;

/**
 * Builds a {@link MapDefinition} into the world. The north-west corner of the plan is placed at the given
 * position, the floor one block below it. Existing blocks and objects in the area are replaced.
 */
public final class MapBuilder {

    public record Result(PlacedMap placed, List<String> warnings) {}

    private final FurnitureService furniture;
    private final ItemRegistry items;
    private final Glyphs glyphs;

    public MapBuilder(FurnitureService furniture, ItemRegistry items, Glyphs glyphs) {
        this.furniture = furniture;
        this.items = items;
        this.glyphs = glyphs;
    }

    public Result build(MapDefinition map, Location feet) {
        World world = feet.getWorld();
        int x0 = feet.getBlockX();
        int y0 = feet.getBlockY() - 1;
        int z0 = feet.getBlockZ();
        List<String> warnings = new ArrayList<>();
        clear(world, map, x0, y0, z0);

        // Pass 1: blocks.
        for (int z = 0; z < map.depth(); z++) {
            for (int x = 0; x < map.width(); x++) {
                Cell cell = map.cell(x, z);
                int wx = x0 + x;
                int wz = z0 + z;
                set(world, wx, y0, wz, cell.floor() != null ? cell.floor() : map.floor());
                if (cell.isWall()) {
                    for (int i = 1; i <= map.height(); i++) {
                        set(world, wx, y0 + i, wz, cell.wall().get(Math.min(i - 1, cell.wall().size() - 1)));
                    }
                } else {
                    if (cell.block() != null) {
                        set(world, wx, y0 + 1, wz, cell.block());
                    }
                    for (int i = 0; i < cell.pillar().size(); i++) {
                        set(world, wx, y0 + 1 + i, wz, cell.pillar().get(i));
                    }
                }
                if (cell.door() != null) {
                    for (int i = 3; i <= map.height(); i++) {
                        set(world, wx, y0 + i, wz, map.frame());
                    }
                }
                if (cell.roof() && map.ceiling() != null) {
                    int spacing = map.lightSpacing();
                    boolean lamp = map.light() != null && !cell.isWall()
                            && x % spacing == spacing / 2 && z % spacing == spacing / 2;
                    set(world, wx, y0 + map.height() + 1, wz, lamp ? map.light() : map.ceiling());
                }
            }
        }

        // Pass 2: doors, objects, markers, texts and the spawn point.
        Location spawn = feet.clone();
        for (int z = 0; z < map.depth(); z++) {
            for (int x = 0; x < map.width(); x++) {
                Cell cell = map.cell(x, z);
                int wx = x0 + x;
                int wz = z0 + z;
                if (cell.door() != null) {
                    placeDoor(world.getBlockAt(wx, y0 + 1, wz), cell.door(), cell.doorFacing());
                }
                if (cell.block() != null && (cell.hazard() != null || cell.safe() != null)) {
                    tag(furniture.placeMarker(world.getBlockAt(wx, y0 + 1, wz)), map.id(), cell.hazard(), cell.safe());
                }
                for (MapObject object : cell.objects()) {
                    Optional<CustomItem> item = items.get(object.id());
                    if (item.isEmpty()) {
                        warnings.add("Unbekanntes Item '" + object.id() + "' bei " + x + "/" + z);
                        continue;
                    }
                    Placement placement = furniture.placementOf(item.get(), object.prop());
                    if (object.collision() != null) {
                        placement = new Placement(placement.mount(), placement.width(), placement.height(),
                                object.collision());
                    }
                    Interaction hitbox = furniture.placeAt(item.get(), world.getBlockAt(wx, y0 + object.y(), wz),
                            object.facing(), placement);
                    tag(hitbox, map.id(), object.hazard(), object.safe());
                }
                if (cell.text() != null) {
                    spawnText(new Location(world, wx + 0.5, y0 + 2.2, wz + 0.5), map.texts().get(cell.text()), map.id());
                }
                if (cell.spawn() != null) {
                    spawn = new Location(world, wx + 0.5, y0 + 1, wz + 0.5, yaw(cell.spawn()), 0);
                }
            }
        }

        PlacedMap placed = new PlacedMap(map.id(), world.getName(), x0, y0, z0, map.width(), map.depth(), map.height(),
                spawn.getX(), spawn.getY(), spawn.getZ(), spawn.getYaw());
        return new Result(placed, warnings);
    }

    /** Removes blocks and objects of a previous build in the map area. */
    private void clear(World world, MapDefinition map, int x0, int y0, int z0) {
        BoundingBox box = new BoundingBox(x0, y0, z0, x0 + map.width(), y0 + map.height() + 2, z0 + map.depth());
        for (Entity entity : world.getNearbyEntities(box)) {
            if (entity instanceof Interaction hitbox && furniture.idOf(hitbox).isPresent()) {
                furniture.remove(hitbox);
            } else if (entity.getPersistentDataContainer().has(Keys.MAP)) {
                entity.remove();
            }
        }
        BlockData air = Material.AIR.createBlockData();
        for (int y = y0; y <= y0 + map.height() + 1; y++) {
            for (int z = z0; z < z0 + map.depth(); z++) {
                for (int x = x0; x < x0 + map.width(); x++) {
                    set(world, x, y, z, air);
                }
            }
        }
    }

    private static void tag(Interaction hitbox, String mapId, String hazard, String safe) {
        var data = hitbox.getPersistentDataContainer();
        data.set(Keys.MAP, PersistentDataType.STRING, mapId);
        if (hazard != null) {
            data.set(Keys.HAZARD, PersistentDataType.STRING, hazard);
        }
        if (safe != null) {
            data.set(Keys.SAFE, PersistentDataType.STRING, safe);
        }
    }

    private void spawnText(Location location, String text, String mapId) {
        location.getWorld().spawn(location, TextDisplay.class, display -> {
            display.text(MiniMessage.miniMessage().deserialize(text, glyphs.tags()));
            display.setBillboard(Display.Billboard.VERTICAL);
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setLineWidth(260);
            display.setBackgroundColor(Color.fromARGB(110, 0, 0, 0));
            display.setShadowed(true);
            display.setPersistent(true);
            display.getPersistentDataContainer().set(Keys.MAP, PersistentDataType.STRING, mapId);
        });
    }

    private static void placeDoor(Block bottom, BlockData door, BlockFace facing) {
        BlockData lower = door.clone();
        BlockData upper = door.clone();
        if (lower instanceof Directional directional) {
            directional.setFacing(facing);
            ((Directional) upper).setFacing(facing);
        }
        if (lower instanceof Bisected half) {
            half.setHalf(Bisected.Half.BOTTOM);
            ((Bisected) upper).setHalf(Bisected.Half.TOP);
            bottom.getRelative(BlockFace.UP).setBlockData(upper, false);
        }
        bottom.setBlockData(lower, false);
    }

    private static void set(World world, int x, int y, int z, BlockData data) {
        world.getBlockAt(x, y, z).setBlockData(data, false);
    }

    /** Minecraft yaw for a horizontal facing (south = 0, west = 90, north = 180, east = -90). */
    private static float yaw(BlockFace face) {
        return switch (face) {
            case WEST -> 90f;
            case NORTH -> 180f;
            case EAST -> -90f;
            default -> 0f;
        };
    }
}
