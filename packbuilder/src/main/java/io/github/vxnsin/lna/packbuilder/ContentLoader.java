package io.github.vxnsin.lna.packbuilder;

import io.github.vxnsin.lna.packbuilder.Content.GlyphDef;
import io.github.vxnsin.lna.packbuilder.Content.ItemDef;
import io.github.vxnsin.lna.packbuilder.Content.SoundDef;
import io.github.vxnsin.lna.packbuilder.Content.UseModel;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/** Loads and validates items.yml, sounds.yml and glyphs.yml. */
public final class ContentLoader {

    static final Pattern ITEM_ID = Pattern.compile("[a-z0-9_]+");
    static final Pattern SOUND_KEY = Pattern.compile("[a-z0-9_.]+");
    static final Pattern VANILLA_ITEM = Pattern.compile("minecraft:[a-z0-9_]+");

    private final Path contentDir;
    private final Problems problems;

    public ContentLoader(Path contentDir, Problems problems) {
        this.contentDir = contentDir;
        this.problems = problems;
    }

    public Content load() throws IOException {
        return new Content(loadItems(), loadSounds(), loadGlyphs());
    }

    private Map<String, ItemDef> loadItems() throws IOException {
        Map<String, ItemDef> items = new TreeMap<>();
        for (var entry : readYaml("items.yml").entrySet()) {
            String id = entry.getKey();
            String where = "items.yml → " + id;
            if (!ITEM_ID.matcher(id).matches()) {
                problems.error(where + ": ID darf nur a-z, 0-9 und _ enthalten.");
                continue;
            }
            Map<String, Object> map = section(entry.getValue(), where);
            if (map == null) {
                continue;
            }
            String base = string(map, "base", where, true);
            if (base != null && !VANILLA_ITEM.matcher(base).matches()) {
                problems.error(where + ": base muss ein Vanilla-Item sein, z. B. minecraft:paper (ist: " + base + ").");
            }
            String texture = string(map, "texture", where, false);
            String model = string(map, "model", where, false);
            if ((texture == null) == (model == null)) {
                problems.error(where + ": Genau eines von texture (2D-Bild) oder model (Blockbench-JSON) angeben.");
            }
            String display = map.getOrDefault("display", "handheld").toString();
            if (!display.equals("handheld") && !display.equals("generated")) {
                problems.error(where + ": display muss handheld oder generated sein.");
            }
            int maxStack = integer(map, "max_stack", 64, where);
            if (maxStack < 1 || maxStack > 99) {
                problems.error(where + ": max_stack muss zwischen 1 und 99 liegen.");
            }
            items.put(id, new ItemDef(id, base, string(map, "name", where, true), stringList(map, "lore", where),
                    texture, model, useModels(map, where), display, maxStack, stringList(map, "tags", where),
                    properties(map, where)));
        }
        return items;
    }

    private Map<String, SoundDef> loadSounds() throws IOException {
        Map<String, SoundDef> sounds = new TreeMap<>();
        for (var entry : readYaml("sounds.yml").entrySet()) {
            String key = entry.getKey();
            String where = "sounds.yml → " + key;
            if (!SOUND_KEY.matcher(key).matches()) {
                problems.error(where + ": Schlüssel darf nur a-z, 0-9, _ und . enthalten.");
                continue;
            }
            Map<String, Object> map = section(entry.getValue(), where);
            if (map == null) {
                continue;
            }
            List<String> files = stringList(map, "files", where);
            if (files.isEmpty()) {
                problems.error(where + ": files braucht mindestens eine .ogg-Datei.");
            }
            boolean stream = Boolean.parseBoolean(String.valueOf(map.getOrDefault("stream", false)));
            sounds.put(key, new SoundDef(key, files, string(map, "subtitle", where, false), stream));
        }
        return sounds;
    }

    private Map<String, GlyphDef> loadGlyphs() throws IOException {
        Map<String, GlyphDef> glyphs = new TreeMap<>();
        for (var entry : readYaml("glyphs.yml").entrySet()) {
            String id = entry.getKey();
            String where = "glyphs.yml → " + id;
            if (!ITEM_ID.matcher(id).matches()) {
                problems.error(where + ": ID darf nur a-z, 0-9 und _ enthalten.");
                continue;
            }
            Map<String, Object> map = section(entry.getValue(), where);
            if (map == null) {
                continue;
            }
            int ascent = integer(map, "ascent", 8, where);
            int height = integer(map, "height", 8, where);
            if (ascent > height) {
                problems.error(where + ": ascent darf nicht größer als height sein (Minecraft-Regel).");
            }
            glyphs.put(id, new GlyphDef(id, string(map, "texture", where, true), ascent, height));
        }
        return glyphs;
    }

    private Map<String, Object> readYaml(String fileName) throws IOException {
        Path file = contentDir.resolve(fileName);
        if (!Files.exists(file)) {
            return Map.of();
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            Object root = new Yaml(new SafeConstructor(new LoaderOptions())).load(reader);
            if (root == null) {
                return Map.of();
            }
            if (!(root instanceof Map<?, ?> map)) {
                problems.error(fileName + ": Oberste Ebene muss eine Liste von Schlüsseln sein.");
                return Map.of();
            }
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((k, v) -> result.put(String.valueOf(k), v));
            return result;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> section(Object value, String where) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        problems.error(where + ": Erwartet einen Block mit Feldern (base, name, …).");
        return null;
    }

    private String string(Map<String, Object> map, String key, String where, boolean required) {
        Object value = map.get(key);
        if (value == null) {
            if (required) {
                problems.error(where + ": Feld \"" + key + "\" fehlt.");
            }
            return null;
        }
        return value.toString();
    }

    private int integer(Map<String, Object> map, String key, int fallback, String where) {
        Object value = map.get(key);
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        problems.error(where + ": \"" + key + "\" muss eine ganze Zahl sein.");
        return fallback;
    }

    private List<String> stringList(Map<String, Object> map, String key, String where) {
        Object value = map.get(key);
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            problems.error(where + ": \"" + key + "\" muss eine Liste sein (- Eintrag).");
            return List.of();
        }
        List<String> result = new ArrayList<>();
        list.forEach(element -> result.add(String.valueOf(element)));
        return result;
    }

    private List<UseModel> useModels(Map<String, Object> map, String where) {
        Object value = map.get("use_models");
        if (value == null) {
            return List.of();
        }
        String context = where + " → use_models";
        if (!(value instanceof List<?> list)) {
            problems.error(context + ": muss eine Liste sein, z. B. - { after: 4, model: item/x_aim.json }");
            return List.of();
        }
        List<UseModel> result = new ArrayList<>();
        for (Object element : list) {
            Map<String, Object> entry = section(element, context);
            if (entry == null) {
                continue;
            }
            int after = integer(entry, "after", 0, context);
            String model = string(entry, "model", context, true);
            if (after < 0) {
                problems.error(context + ": after darf nicht negativ sein.");
            } else if (model != null) {
                result.add(new UseModel(after, model));
            }
        }
        result.sort(Comparator.comparingInt(UseModel::after));
        return List.copyOf(result);
    }

    private Map<String, Object> properties(Map<String, Object> map, String where) {
        Object value = map.get("properties");
        if (value == null) {
            return Map.of();
        }
        Map<String, Object> section = section(value, where + " → properties");
        return section == null ? Map.of() : new TreeMap<>(section);
    }
}
