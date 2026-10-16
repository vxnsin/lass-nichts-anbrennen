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
import io.github.vxnsin.lna.plugin.mission.MissionInfo;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/**
 * Builds a {@link MapDefinition} into the world. The north-west corner of the plan is placed at the given
 * position, the floor one block below it. Existing blocks and objects in the area are replaced.
 */
public final class MapBuilder {

    public record Result(PlacedMap placed, List<String> warnings) {}

    private final FurnitureService furniture;
    private final ItemRegistry items;
    private final Glyphs glyphs;
    private final Supplier<Map<String, MissionInfo>> missions;

    public MapBuilder(FurnitureService furniture, ItemRegistry items, Glyphs glyphs,
                      Supplier<Map<String, MissionInfo>> missions) {
        this.furniture = furniture;
        this.items = items;
        this.glyphs = glyphs;
        this.missions = missions;
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
                if (cell.block() != null && (cell.hazard() != null || cell.safe() != null || cell.action() != null
                        || cell.fire() != null)) {
                    Interaction marker = furniture.placeMarker(world.getBlockAt(wx, y0 + 1, wz));
                    tag(marker, map.id(), cell.hazard(), cell.safe());
                    if (cell.action() != null) {
                        marker.getPersistentDataContainer().set(Keys.ACTION, PersistentDataType.STRING, cell.action());
                    }
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
                    if (object.give()) {
                        hitbox.getPersistentDataContainer().set(Keys.GIVE, PersistentDataType.STRING, object.id());
                    }
                }
                if (cell.mission() != null) {
                    placeMissionCard(world, wx, y0, wz, cell.mission(), map.id(), warnings);
                }
                if (cell.npc() != null) {
                    spawnFigure(new Location(world, wx + 0.5, y0 + 1, wz + 0.5, yaw(cell.doorFacing()), 0),
                            cell.npc(), map.id());
                }
                if (cell.text() != null) {
                    double height = cell.npc() != null ? 3.1 : 2.2;
                    spawnText(new Location(world, wx + 0.5, y0 + height, wz + 0.5), map.texts().get(cell.text()), map.id());
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

    /** A mission card lying on the table block of this cell: flat icon, floating title and a click box. */
    private void placeMissionCard(World world, int x, int y0, int z, String missionId, String mapId, List<String> warnings) {
        MissionInfo info = missions.get().get(missionId);
        if (info == null) {
            warnings.add("Unbekannte Mission '" + missionId + "'");
            return;
        }
        items.get(info.icon()).ifPresent(icon -> world.spawn(new Location(world, x + 0.5, y0 + 2.15, z + 0.5),
                ItemDisplay.class, display -> {
                    display.setItemStack(items.create(icon));
                    display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
                    display.setTransformation(new Transformation(new Vector3f(),
                            new AxisAngle4f((float) Math.toRadians(-90), 1, 0, 0), new Vector3f(0.55f, 0.55f, 0.55f),
                            new AxisAngle4f()));
                    display.setPersistent(true);
                    display.getPersistentDataContainer().set(Keys.MAP, PersistentDataType.STRING, mapId);
                }));
        String title = info.available()
                ? "<gold>Mission " + info.number() + "</gold>\n<white>" + info.title() + "</white>"
                : "<dark_gray>Mission " + info.number() + "</dark_gray>\n<gray>" + info.title() + "</gray>\n<dark_gray>bald verfügbar</dark_gray>";
        world.spawn(new Location(world, x + 0.5, y0 + 2.45, z + 0.5), TextDisplay.class, display -> {
            display.text(MiniMessage.miniMessage().deserialize(title));
            display.setBillboard(Display.Billboard.VERTICAL);
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
            display.setBackgroundColor(Color.fromARGB(90, 0, 0, 0));
            display.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(),
                    new Vector3f(0.6f, 0.6f, 0.6f), new AxisAngle4f()));
            display.setPersistent(true);
            display.getPersistentDataContainer().set(Keys.MAP, PersistentDataType.STRING, mapId);
        });
        Interaction hitbox = furniture.placeMarker(world.getBlockAt(x, y0 + 1, z));
        hitbox.getPersistentDataContainer().set(Keys.MAP, PersistentDataType.STRING, mapId);
        hitbox.getPersistentDataContainer().set(Keys.MISSION, PersistentDataType.STRING, missionId);
    }

    /** A standing player figure with the skin of the given player name (e.g. the project author). */
    private static void spawnFigure(Location location, String name, String mapId) {
        location.getWorld().spawn(location, Mannequin.class, figure -> {
            figure.setProfile(ResolvableProfile.resolvableProfile().name(name).build());
            figure.setImmovable(true);
            figure.setInvulnerable(true);
            figure.setSilent(true);
            figure.setPersistent(true);
            figure.setRemoveWhenFarAway(false);
            figure.customName(Component.text(name, NamedTextColor.GOLD));
            figure.setCustomNameVisible(true);
            figure.setDescription(Component.text("Idee, Konzept & Code", NamedTextColor.GRAY));
            figure.getPersistentDataContainer().set(Keys.MAP, PersistentDataType.STRING, mapId);
            figure.getPersistentDataContainer().set(Keys.NPC, PersistentDataType.STRING, name);
        });
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
