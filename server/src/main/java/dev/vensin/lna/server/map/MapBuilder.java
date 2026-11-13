package dev.vensin.lna.server.map;

import dev.vensin.lna.server.Tags;
import dev.vensin.lna.server.furniture.Furniture;
import dev.vensin.lna.server.furniture.Furniture.Placement;
import dev.vensin.lna.server.item.CustomItem;
import dev.vensin.lna.server.item.Items;
import dev.vensin.lna.server.lang.Messages;
import dev.vensin.lna.server.map.MapDefinition.Cell;
import dev.vensin.lna.server.map.MapDefinition.MapObject;
import dev.vensin.lna.server.mission.MissionInfo;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;

/** Places a map with its north-west corner at the given position; y is the floor layer. */
public final class MapBuilder {

    private final Furniture furniture;
    private final Items items;
    private final Messages messages;
    private final Map<String, MissionInfo> missions;
    private final Map<Entity, Map<String, Entity>> labels = new LinkedHashMap<>();
    private final List<String> warnings = new ArrayList<>();

    public MapBuilder(Furniture furniture, Items items, Messages messages, Map<String, MissionInfo> missions) {
        this.furniture = furniture;
        this.items = items;
        this.messages = messages;
        this.missions = missions;
    }

    public Map<Entity, Map<String, Entity>> labels() {
        return labels;
    }

    public List<String> warnings() {
        return warnings;
    }

    public PlacedMap build(Instance instance, MapDefinition map, int x0, int y0, int z0) {
        loadChunks(instance, x0, z0, map.width(), map.depth());
        Pos spawn = new Pos(x0 + 0.5, y0 + 1, z0 + 0.5);
        for (int z = 0; z < map.depth(); z++) {
            for (int x = 0; x < map.width(); x++) {
                Cell cell = map.cell(x, z);
                placeBlocks(instance, map, cell, x, z, x0 + x, y0, z0 + z);
                placeObjects(instance, map, cell, x, z, x0 + x, y0, z0 + z);
                if (cell.spawn() != null) {
                    spawn = new Pos(x0 + x + 0.5, y0 + 1, z0 + z + 0.5, cell.spawn().yaw(), 0);
                }
            }
        }
        return new PlacedMap(map.id(), x0, y0, z0, map.width(), map.depth(), map.height(), spawn);
    }

    private void placeBlocks(Instance instance, MapDefinition map, Cell cell, int x, int z, int wx, int y0, int wz) {
        instance.setBlock(wx, y0, wz, cell.floor() != null ? cell.floor() : map.floor());
        if (cell.isWall()) {
            for (int i = 1; i <= map.height(); i++) {
                instance.setBlock(wx, y0 + i, wz, cell.wall().get(Math.min(i - 1, cell.wall().size() - 1)));
            }
        } else {
            if (cell.block() != null) {
                instance.setBlock(wx, y0 + 1, wz, cell.block());
            }
            for (int i = 0; i < cell.pillar().size(); i++) {
                if (!cell.pillar().get(i).air()) {
                    instance.setBlock(wx, y0 + 1 + i, wz, cell.pillar().get(i));
                }
            }
        }
        if (cell.door() != null) {
            Block door = cell.door().withProperty("facing", cell.doorFacing().name().toLowerCase());
            instance.setBlock(wx, y0 + 1, wz, door.withProperty("half", "lower"));
            instance.setBlock(wx, y0 + 2, wz, door.withProperty("half", "upper"));
            for (int i = 3; i <= map.height(); i++) {
                instance.setBlock(wx, y0 + i, wz, map.frame());
            }
        }
        if (cell.roof() && map.ceiling() != null) {
            int spacing = map.lightSpacing();
            boolean lamp = map.light() != null && !cell.isWall()
                    && x % spacing == spacing / 2 && z % spacing == spacing / 2;
            instance.setBlock(wx, y0 + map.height() + 1, wz, lamp ? map.light() : map.ceiling());
        }
    }

    private void placeObjects(Instance instance, MapDefinition map, Cell cell, int x, int z, int wx, int y0, int wz) {
        if (cell.block() != null && (cell.hazard() != null || cell.safe() != null || cell.action() != null
                || cell.fire() != null)) {
            Entity marker = furniture.placeMarker(instance, wx, y0 + 1, wz);
            tag(marker, map.id(), cell.hazard(), cell.safe());
            if (cell.action() != null) {
                marker.setTag(Tags.ACTION, cell.action());
            }
        }
        for (MapObject object : cell.objects()) {
            Optional<CustomItem> item = items.get(object.id());
            if (item.isEmpty()) {
                warnings.add(map.id() + ": unknown item " + object.id() + " at " + x + "/" + z);
                continue;
            }
            Placement placement = furniture.placementOf(item.get(), object.prop());
            if (object.collision() != null) {
                placement = new Placement(placement.mount(), placement.width(), placement.height(), object.collision());
            }
            Entity hitbox = furniture.placeAt(instance, item.get(), wx, y0 + object.y(), wz, object.facing(), placement);
            tag(hitbox, map.id(), object.hazard(), object.safe());
            if (object.give()) {
                hitbox.setTag(Tags.GIVE, object.id());
            }
        }
        if (cell.mission() != null) {
            placeMissionCard(instance, wx, y0, wz, cell.mission(), map.id());
        }
        Facing facing = cell.doorFacing();
        if (cell.npc() != null) {
            MapObjects.figure(instance, new Pos(wx + 0.5, y0 + 1, wz + 0.5, facing.yaw(), 0), cell.npc(),
                    messages.text(Messages.GERMAN, "npc.author"), map.id());
        }
        if (cell.text() != null) {
            // Without a figure the text hangs at the back of the cell, on the wall side.
            double back = cell.npc() != null ? 0 : 0.45;
            Pos position = new Pos(wx + 0.5 - facing.dx() * back, y0 + (cell.npc() != null ? 3.1 : 2.2),
                    wz + 0.5 - facing.dz() * back, facing.yaw(), 0);
            MapObjects.text(instance, position, MiniMessage.miniMessage().deserialize(map.texts().get(cell.text())),
                    1f, 260, MapObjects.argb(110, 0, 0, 0), false, map.id());
        }
    }

    private void placeMissionCard(Instance instance, int x, int y0, int z, String missionId, String mapId) {
        MissionInfo info = missions.get(missionId);
        if (info == null) {
            warnings.add(mapId + ": unknown mission " + missionId);
            return;
        }
        items.get(info.icon()).ifPresent(icon -> MapObjects.flatIcon(instance, new Pos(x + 0.5, y0 + 2.15, z + 0.5),
                items.create(icon), 0.55f, mapId));
        Map<String, Entity> titles = new LinkedHashMap<>();
        for (String language : List.of(Messages.GERMAN, Messages.ENGLISH)) {
            titles.put(language, MapObjects.text(instance, new Pos(x + 0.5, y0 + 2.5, z + 0.5),
                    messages.text(language, info.available() ? "mission.card-open" : "mission.card-closed",
                            Placeholder.unparsed("number", String.valueOf(info.number())),
                            Placeholder.unparsed("title", info.title())),
                    0.55f, 200, MapObjects.argb(150, 10, 10, 15), true, mapId));
        }
        Entity hitbox = furniture.placeMarker(instance, x, y0 + 1, z);
        hitbox.setTag(Tags.MAP, mapId);
        hitbox.setTag(Tags.MISSION, missionId);
        labels.put(hitbox, titles);
    }

    private static void tag(Entity hitbox, String mapId, String hazard, String safe) {
        hitbox.setTag(Tags.MAP, mapId);
        if (hazard != null) {
            hitbox.setTag(Tags.HAZARD, hazard);
        }
        if (safe != null) {
            hitbox.setTag(Tags.SAFE, safe);
        }
    }

    private static void loadChunks(Instance instance, int x0, int z0, int width, int depth) {
        for (int cx = Math.floorDiv(x0, 16); cx <= Math.floorDiv(x0 + width, 16); cx++) {
            for (int cz = Math.floorDiv(z0, 16); cz <= Math.floorDiv(z0 + depth, 16); cz++) {
                instance.loadChunk(cx, cz).join();
            }
        }
    }
}
