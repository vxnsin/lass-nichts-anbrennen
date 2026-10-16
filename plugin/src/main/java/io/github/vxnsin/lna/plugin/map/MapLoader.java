package io.github.vxnsin.lna.plugin.map;

import io.github.vxnsin.lna.plugin.map.MapDefinition.Cell;
import io.github.vxnsin.lna.plugin.map.MapDefinition.FireSpot;
import io.github.vxnsin.lna.plugin.map.MapDefinition.Hazard;
import io.github.vxnsin.lna.plugin.map.MapDefinition.MapObject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * Loads map definitions from maps/*.yml inside the plugin jar. Files with the same name in
 * plugins/LassNichtsAnbrennen/maps/ override them, additional files there add new maps.
 */
public final class MapLoader {

    private static final String DIRECTORY = "maps/";

    private final Logger logger;

    public MapLoader(Logger logger) {
        this.logger = logger;
    }

    public Map<String, MapDefinition> load(File pluginJar, File dataFolder) {
        Map<String, MapDefinition> maps = new TreeMap<>();
        try (JarFile jar = new JarFile(pluginJar)) {
            for (JarEntry entry : jar.stream().toList()) {
                String name = entry.getName();
                if (name.startsWith(DIRECTORY) && name.endsWith(".yml")) {
                    try (InputStream in = jar.getInputStream(entry)) {
                        read(idOf(name), in).ifPresent(map -> maps.put(map.id(), map));
                    }
                }
            }
        } catch (IOException e) {
            logger.severe("Could not read embedded maps: " + e.getMessage());
        }
        File[] overrides = new File(dataFolder, DIRECTORY).listFiles((dir, name) -> name.endsWith(".yml"));
        if (overrides != null) {
            for (File file : overrides) {
                try (InputStream in = Files.newInputStream(file.toPath())) {
                    read(idOf(file.getName()), in).ifPresent(map -> maps.put(map.id(), map));
                } catch (IOException e) {
                    logger.severe("Could not read map " + file + ": " + e.getMessage());
                }
            }
        }
        return maps;
    }

    private Optional<MapDefinition> read(String id, InputStream in) throws IOException {
        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            Object root = new Yaml(new SafeConstructor(new LoaderOptions())).load(reader);
            return Optional.of(parse(id, map(root, "root")));
        } catch (RuntimeException e) {
            logger.severe("Map '" + id + "' skipped: " + e.getMessage());
            return Optional.empty();
        }
    }

    private static MapDefinition parse(String id, Map<String, Object> yaml) {
        Map<String, String> texts = strings(yaml.get("texts"), "texts");
        Map<String, Hazard> hazards = new LinkedHashMap<>();
        map(yaml.getOrDefault("hazards", Map.of()), "hazards").forEach((key, value) -> {
            Map<String, Object> hazard = map(value, "hazards." + key);
            hazards.put(key, new Hazard(string(hazard, "title", key), string(hazard, "explanation", "")));
        });
        Map<String, String> safe = strings(yaml.get("safe"), "safe");

        Map<Character, Cell> legend = new LinkedHashMap<>();
        map(yaml.get("legend"), "legend").forEach((key, value) -> {
            if (key.length() != 1) {
                throw new IllegalArgumentException("legend key '" + key + "' must be a single character");
            }
            Cell cell = cell(map(value == null ? Map.of() : value, "legend." + key), "legend '" + key + "'");
            check(cell.text() == null || texts.containsKey(cell.text()), "legend '" + key + "': unknown text " + cell.text());
            checkReferences(key, cell.hazard(), cell.safe(), hazards, safe);
            cell.objects().forEach(object -> checkReferences(key, object.hazard(), object.safe(), hazards, safe));
            legend.put(key.charAt(0), cell);
        });

        List<String> plan = list(yaml.get("plan"), "plan").stream().map(String::valueOf).toList();
        check(!plan.isEmpty(), "plan is empty");
        int width = plan.getFirst().length();
        for (int z = 0; z < plan.size(); z++) {
            String row = plan.get(z);
            check(row.length() == width, "plan row " + (z + 1) + " has " + row.length() + " columns, expected " + width);
            for (char symbol : row.toCharArray()) {
                check(legend.containsKey(symbol), "plan row " + (z + 1) + ": character '" + symbol + "' is not in the legend");
            }
        }

        Cell wall = legend.get('#');
        BlockData frame = yaml.containsKey("frame") ? block(yaml.get("frame"))
                : wall != null && wall.isWall() ? wall.wall().getFirst() : block("minecraft:white_concrete");
        return new MapDefinition(
                id,
                string(yaml, "name", id),
                ((Number) yaml.getOrDefault("height", 4)).intValue(),
                block(yaml.getOrDefault("floor", "minecraft:smooth_stone")),
                yaml.containsKey("ceiling") ? block(yaml.get("ceiling")) : null,
                yaml.containsKey("light") ? block(yaml.get("light")) : null,
                Math.max(1, ((Number) yaml.getOrDefault("light-spacing", 4)).intValue()),
                frame,
                plan,
                legend,
                texts,
                hazards,
                safe);
    }

    private static Cell cell(Map<String, Object> entry, String where) {
        List<MapObject> objects = new ArrayList<>();
        for (Object element : list(entry.get("objects"), where + " objects")) {
            Map<String, Object> object = map(element, where + " object");
            objects.add(new MapObject(
                    string(object, "id", null),
                    facing(object.getOrDefault("facing", "north")),
                    ((Number) object.getOrDefault("y", 1)).intValue(),
                    Boolean.TRUE.equals(object.get("prop")),
                    object.containsKey("collision") ? Boolean.TRUE.equals(object.get("collision")) : null,
                    string(object, "hazard", null),
                    string(object, "safe", null),
                    Boolean.TRUE.equals(object.get("give"))));
            check(objects.getLast().id() != null, where + ": object without id");
        }
        return new Cell(
                list(entry.get("wall"), where + " wall").stream().map(MapLoader::block).toList(),
                entry.containsKey("floor") ? block(entry.get("floor")) : null,
                !Boolean.FALSE.equals(entry.get("roof")),
                entry.containsKey("door") ? block(entry.get("door")) : null,
                facing(entry.getOrDefault("facing", "south")),
                entry.containsKey("block") ? block(entry.get("block")) : null,
                list(entry.get("pillar"), where + " pillar").stream().map(MapLoader::block).toList(),
                entry.containsKey("spawn") ? facing(entry.get("spawn")) : null,
                string(entry, "text", null),
                string(entry, "hazard", null),
                string(entry, "safe", null),
                string(entry, "mission", null),
                string(entry, "npc", null),
                entry.containsKey("fire") ? fireSpot(map(entry.get("fire"), where + " fire")) : null,
                List.copyOf(objects));
    }

    private static FireSpot fireSpot(Map<String, Object> fire) {
        return new FireSpot(
                string(fire, "class", "a"),
                ((Number) fire.getOrDefault("order", 1)).intValue(),
                ((Number) fire.getOrDefault("y", 1)).intValue(),
                string(fire, "title", "Brand"),
                string(fire, "hint", ""));
    }

    private static void checkReferences(String key, String hazard, String safe, Map<String, Hazard> hazards,
                                        Map<String, String> safeEntries) {
        check(hazard == null || hazards.containsKey(hazard), "legend '" + key + "': unknown hazard " + hazard);
        check(safe == null || safeEntries.containsKey(safe), "legend '" + key + "': unknown safe entry " + safe);
    }

    private static BlockData block(Object value) {
        try {
            return Bukkit.createBlockData(String.valueOf(value));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("invalid block '" + value + "'");
        }
    }

    private static BlockFace facing(Object value) {
        BlockFace face = BlockFace.valueOf(String.valueOf(value).toUpperCase(Locale.ROOT));
        check(face.isCartesian() && face != BlockFace.UP && face != BlockFace.DOWN,
                "facing must be north, east, south or west (is " + value + ")");
        return face;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value, String where) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            ((Map<Object, Object>) map).forEach((key, item) -> result.put(String.valueOf(key), item));
            return result;
        }
        throw new IllegalArgumentException(where + " must be a section");
    }

    private static List<?> list(Object value, String where) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            return list;
        }
        throw new IllegalArgumentException(where + " must be a list");
    }

    private static Map<String, String> strings(Object value, String where) {
        Map<String, String> result = new LinkedHashMap<>();
        if (value != null) {
            map(value, where).forEach((key, text) -> result.put(key, String.valueOf(text)));
        }
        return result;
    }

    private static String string(Map<String, Object> map, String key, String fallback) {
        Object value = map.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    private static String idOf(String fileName) {
        String name = fileName.substring(fileName.lastIndexOf('/') + 1);
        return name.substring(0, name.length() - ".yml".length());
    }
}
