package io.github.vxnsin.lna.packbuilder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.vxnsin.lna.packbuilder.Content.GlyphDef;
import io.github.vxnsin.lna.packbuilder.Content.ItemDef;
import io.github.vxnsin.lna.packbuilder.Content.SoundDef;
import io.github.vxnsin.lna.packbuilder.Content.UseModel;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Stream;
import javax.imageio.ImageIO;

/**
 * Builds the resource pack from content/.
 *
 * <p>Writes to {@code outDir}: pack.zip, pack.sha1, registry.json (consumed by the plugin) and credits.json.
 */
public final class PackBuilder {

    /** Resource namespace, short for "Lass nichts anbrennen". */
    public static final String NAMESPACE = "lna";
    /** Font that holds all glyphs and spacing characters. */
    public static final String HUD_FONT = NAMESPACE + ":hud";
    /** First glyph code point (Unicode Private Use Area). */
    static final int GLYPH_START = 0xE000;
    /** Spacing characters: negative powers of two from U+F800, positive from U+F820. */
    static final int NEGATIVE_SPACE_START = 0xF800;
    static final int POSITIVE_SPACE_START = 0xF820;
    static final int SPACE_POWERS = 11; // 1 to 1024 pixels
    /** Built-in sound event without any sound files ({@code lna:silent}). */
    public static final String SILENT_SOUND = "silent";

    /** Asset directories in content/ that are copied into the pack (under assets/lna/). */
    private static final List<String> PACK_DIRS = List.of("textures", "models", "sounds");
    /** Asset directories that are validated but not packed (e.g. BetterModel sources). */
    private static final List<String> SOURCE_ONLY_DIRS = List.of("entities");

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final Gson PRETTY = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    public record Result(Problems problems, int items, int sounds, int glyphs, int files, long packBytes, String sha1) {}

    private final Path contentDir;
    private final Path creditsFile;
    private final Path outDir;
    private final int packFormat;

    public PackBuilder(Path contentDir, Path creditsFile, Path outDir, int packFormat) {
        this.contentDir = contentDir;
        this.creditsFile = creditsFile;
        this.outDir = outDir;
        this.packFormat = packFormat;
    }

    public Result build() throws IOException {
        Problems problems = new Problems();
        Credits credits = Credits.parse(creditsFile, problems);
        Content content = new ContentLoader(contentDir, problems).load();

        SortedMap<String, byte[]> pack = new TreeMap<>();
        Map<String, BufferedImage> images = new TreeMap<>();
        int files = collectAssets(credits, pack, images, problems);

        JsonObject registry = new JsonObject();
        registry.addProperty("namespace", NAMESPACE);
        registry.add("items", writeItems(content, pack, problems));
        registry.add("sounds", writeSounds(content, pack, problems));
        writeGlyphs(content, images, pack, registry, problems);
        hideWhiteBossBar(pack);
        pack.put("pack.mcmeta", json(packMeta()));

        if (problems.hasErrors()) {
            return new Result(problems, 0, 0, 0, files, 0, null);
        }

        byte[] zip = DeterministicZip.write(pack);
        String sha1 = sha1(zip);
        Files.createDirectories(outDir);
        Files.write(outDir.resolve("pack.zip"), zip);
        Files.writeString(outDir.resolve("pack.sha1"), sha1);
        Files.writeString(outDir.resolve("registry.json"), PRETTY.toJson(registry));
        Files.writeString(outDir.resolve("credits.json"), PRETTY.toJson(credits.entries().values()));
        return new Result(problems, content.items().size(), content.sounds().size(), content.glyphs().size(),
                files, zip.length, sha1);
    }

    /** Copies textures, models and sounds into the pack and validates each file, including its credits entry. */
    private int collectAssets(Credits credits, SortedMap<String, byte[]> pack, Map<String, BufferedImage> images,
                              Problems problems) throws IOException {
        int count = 0;
        for (String dir : Stream.concat(PACK_DIRS.stream(), SOURCE_ONLY_DIRS.stream()).toList()) {
            Path root = contentDir.resolve(dir);
            if (!Files.isDirectory(root)) {
                continue;
            }
            List<Path> paths;
            try (Stream<Path> walk = Files.walk(root)) {
                paths = walk.filter(Files::isRegularFile).sorted().toList();
            }
            for (Path file : paths) {
                String rel = contentDir.relativize(file).toString().replace('\\', '/');
                String fileName = file.getFileName().toString();
                if (fileName.equals(".gitkeep")) {
                    continue;
                }
                byte[] data = Files.readAllBytes(file);
                count++;
                if (!fileName.endsWith(".mcmeta") && !credits.contains(rel)) {
                    problems.error(rel + ": Kein Eintrag in CREDITS.md. Jede Datei braucht Urheber*in und Lizenz, "
                            + "auch eigene Werke.");
                }
                boolean copy = switch (dir) {
                    case "textures" -> {
                        if (fileName.endsWith(".png")) {
                            BufferedImage image = AssetChecks.png(rel, data, problems);
                            if (image != null) {
                                images.put(rel, image);
                            }
                            yield true;
                        }
                        if (fileName.endsWith(".png.mcmeta")) {
                            yield true;
                        }
                        problems.warn(rel + ": Unbekannter Dateityp in textures/, wird ignoriert.");
                        yield false;
                    }
                    case "models" -> {
                        if (!fileName.endsWith(".json")) {
                            problems.warn(rel + ": Modelle müssen .json sein (Blockbench → Export → Java Block/Item).");
                            yield false;
                        }
                        for (String ref : AssetChecks.modelTextureRefs(rel, data, problems)) {
                            checkTextureRef(rel, ref, problems);
                        }
                        yield true;
                    }
                    case "sounds" -> {
                        if (!fileName.endsWith(".ogg")) {
                            problems.warn(rel + ": Sounds müssen .ogg sein, wird ignoriert.");
                            yield false;
                        }
                        AssetChecks.ogg(rel, data, problems);
                        yield true;
                    }
                    default -> false; // source files only, not packed
                };
                if (copy) {
                    pack.put("assets/" + NAMESPACE + "/" + rel, data);
                }
            }
        }
        return count;
    }

    private void checkTextureRef(String where, String ref, Problems problems) {
        if (!ref.startsWith(NAMESPACE + ":")) {
            return; // minecraft: or no namespace: vanilla texture
        }
        String path = "textures/" + ref.substring(NAMESPACE.length() + 1) + ".png";
        if (!Files.exists(contentDir.resolve(path))) {
            problems.error(where + ": Verweist auf Textur \"" + ref + "\", aber content/" + path + " fehlt.");
        }
    }

    private JsonObject writeItems(Content content, SortedMap<String, byte[]> pack, Problems problems) {
        JsonObject items = new JsonObject();
        for (ItemDef item : content.items().values()) {
            String where = "items.yml → " + item.id();
            String modelRef;
            if (item.texture() != null) {
                String texturePath = "textures/" + item.texture();
                if (!texturePath.endsWith(".png") || !Files.exists(contentDir.resolve(texturePath))) {
                    problems.error(where + ": Textur content/" + texturePath + " fehlt (muss .png sein).");
                    continue;
                }
                modelRef = NAMESPACE + ":generated/" + item.id();
                JsonObject model = new JsonObject();
                model.addProperty("parent", "minecraft:item/" + item.display());
                JsonObject textures = new JsonObject();
                textures.addProperty("layer0", NAMESPACE + ":" + stripExtension(item.texture()));
                model.add("textures", textures);
                pack.put("assets/" + NAMESPACE + "/models/generated/" + item.id() + ".json", json(model));
            } else if (item.model() != null) {
                String modelPath = "models/" + item.model();
                if (!modelPath.endsWith(".json") || !Files.exists(contentDir.resolve(modelPath))) {
                    problems.error(where + ": Modell content/" + modelPath + " fehlt (muss .json sein).");
                    continue;
                }
                modelRef = NAMESPACE + ":" + stripExtension(item.model());
            } else {
                continue;
            }

            JsonObject definition = new JsonObject();
            definition.add("model", itemModel(item, modelRef, problems));
            pack.put("assets/" + NAMESPACE + "/items/" + item.id() + ".json", json(definition));

            JsonObject entry = new JsonObject();
            entry.addProperty("base", item.base());
            entry.addProperty("model", NAMESPACE + ":" + item.id());
            entry.addProperty("name", item.name());
            entry.add("lore", GSON.toJsonTree(item.lore()));
            entry.addProperty("maxStack", item.maxStack());
            entry.add("tags", GSON.toJsonTree(item.tags()));
            entry.add("properties", GSON.toJsonTree(item.properties()));
            items.add(item.id(), entry);
        }
        return items;
    }

    /**
     * Item model definition. With use_models, the client shows the default model normally and switches
     * by use duration (ticks) while the item is being used, like the vanilla bow.
     */
    private JsonObject itemModel(ItemDef item, String modelRef, Problems problems) {
        JsonObject idle = modelReference(modelRef);
        if (item.useModels().isEmpty()) {
            return idle;
        }
        JsonArray entries = new JsonArray();
        for (UseModel use : item.useModels()) {
            String path = "models/" + use.model();
            if (!path.endsWith(".json") || !Files.exists(contentDir.resolve(path))) {
                problems.error("items.yml → " + item.id() + " → use_models: content/" + path + " fehlt (muss .json sein).");
                continue;
            }
            JsonObject entry = new JsonObject();
            entry.addProperty("threshold", use.after());
            entry.add("model", modelReference(NAMESPACE + ":" + stripExtension(use.model())));
            entries.add(entry);
        }
        JsonObject byDuration = new JsonObject();
        byDuration.addProperty("type", "minecraft:range_dispatch");
        byDuration.addProperty("property", "minecraft:use_duration");
        byDuration.add("entries", entries);
        byDuration.add("fallback", idle);

        JsonObject condition = new JsonObject();
        condition.addProperty("type", "minecraft:condition");
        condition.addProperty("property", "minecraft:using_item");
        condition.add("on_true", byDuration);
        condition.add("on_false", idle);
        return condition;
    }

    private static JsonObject modelReference(String modelRef) {
        JsonObject model = new JsonObject();
        model.addProperty("type", "minecraft:model");
        model.addProperty("model", modelRef);
        return model;
    }

    private JsonArray writeSounds(Content content, SortedMap<String, byte[]> pack, Problems problems) {
        JsonObject soundsJson = new JsonObject();
        JsonObject lang = new JsonObject();
        JsonArray keys = new JsonArray();
        // Built-in empty sound event, e.g. for "usable" items whose vanilla use sound must stay quiet.
        JsonObject silent = new JsonObject();
        silent.add("sounds", new JsonArray());
        soundsJson.add(SILENT_SOUND, silent);
        keys.add(NAMESPACE + ":" + SILENT_SOUND);
        if (content.sounds().containsKey(SILENT_SOUND)) {
            problems.error("sounds.yml → " + SILENT_SOUND + ": Der Name ist für den eingebauten stillen Sound reserviert.");
        }
        for (SoundDef sound : content.sounds().values()) {
            JsonArray variants = new JsonArray();
            for (String file : sound.files()) {
                String path = "sounds/" + file;
                if (!file.endsWith(".ogg") || !Files.exists(contentDir.resolve(path))) {
                    problems.error("sounds.yml → " + sound.key() + ": content/" + path + " fehlt (muss .ogg sein).");
                    continue;
                }
                JsonObject variant = new JsonObject();
                variant.addProperty("name", NAMESPACE + ":" + stripExtension(file));
                if (sound.stream()) {
                    variant.addProperty("stream", true);
                }
                variants.add(variant);
            }
            JsonObject event = new JsonObject();
            event.add("sounds", variants);
            if (sound.subtitle() != null) {
                String subtitleKey = "subtitles." + NAMESPACE + "." + sound.key();
                event.addProperty("subtitle", subtitleKey);
                lang.addProperty(subtitleKey, sound.subtitle());
            }
            soundsJson.add(sound.key(), event);
            keys.add(NAMESPACE + ":" + sound.key());
        }
        pack.put("assets/" + NAMESPACE + "/sounds.json", json(soundsJson));
        if (!lang.isEmpty()) {
            // German is the game language; en_us acts as the fallback for every other client locale.
            pack.put("assets/" + NAMESPACE + "/lang/de_de.json", json(lang));
            pack.put("assets/" + NAMESPACE + "/lang/en_us.json", json(lang));
        }
        return keys;
    }

    /**
     * Generates the {@code lna:hud} font: one bitmap character per glyph plus spacing characters
     * that the plugin uses to position glyphs with pixel precision (HUDs, GUI backgrounds).
     */
    private void writeGlyphs(Content content, Map<String, BufferedImage> images, SortedMap<String, byte[]> pack,
                             JsonObject registry, Problems problems) {
        JsonArray providers = new JsonArray();
        JsonObject glyphs = new JsonObject();
        int next = GLYPH_START;
        for (GlyphDef glyph : content.glyphs().values()) {
            String path = "textures/" + glyph.texture();
            BufferedImage image = images.get(path);
            if (image == null) {
                problems.error("glyphs.yml → " + glyph.id() + ": Textur content/" + path + " fehlt oder ist kein PNG.");
                continue;
            }
            String character = Character.toString(next++);
            JsonObject provider = new JsonObject();
            provider.addProperty("type", "bitmap");
            provider.addProperty("file", NAMESPACE + ":" + glyph.texture());
            provider.addProperty("ascent", glyph.ascent());
            provider.addProperty("height", glyph.height());
            JsonArray chars = new JsonArray();
            chars.add(character);
            provider.add("chars", chars);
            providers.add(provider);

            JsonObject entry = new JsonObject();
            entry.addProperty("char", character);
            entry.addProperty("advance", AssetChecks.glyphAdvance(image, glyph.height()));
            entry.addProperty("ascent", glyph.ascent());
            entry.addProperty("height", glyph.height());
            glyphs.add(glyph.id(), entry);
        }

        JsonObject advances = new JsonObject();
        JsonObject spaces = new JsonObject();
        for (int power = 0; power < SPACE_POWERS; power++) {
            int pixels = 1 << power;
            String negative = Character.toString(NEGATIVE_SPACE_START + power);
            String positive = Character.toString(POSITIVE_SPACE_START + power);
            advances.addProperty(negative, -pixels);
            advances.addProperty(positive, pixels);
            spaces.addProperty(String.valueOf(-pixels), negative);
            spaces.addProperty(String.valueOf(pixels), positive);
        }
        JsonObject spaceProvider = new JsonObject();
        spaceProvider.addProperty("type", "space");
        spaceProvider.add("advances", advances);
        providers.add(spaceProvider);

        JsonObject font = new JsonObject();
        font.add("providers", providers);
        pack.put("assets/" + NAMESPACE + "/font/hud.json", json(font));

        registry.addProperty("font", HUD_FONT);
        registry.add("glyphs", glyphs);
        registry.add("spaces", spaces);
    }

    /**
     * Makes the white boss bar invisible so the plugin can use it as a HUD line where only the
     * title (glyphs) is rendered. Boss bars in other colors keep their vanilla look.
     */
    private void hideWhiteBossBar(SortedMap<String, byte[]> pack) throws IOException {
        byte[] transparent = transparentPng(182, 5);
        String dir = "assets/minecraft/textures/gui/sprites/boss_bar/";
        pack.put(dir + "white_background.png", transparent);
        pack.put(dir + "white_progress.png", transparent);
    }

    private JsonObject packMeta() {
        JsonObject meta = new JsonObject();
        JsonObject packSection = new JsonObject();
        packSection.addProperty("description", "Lass nichts anbrennen – Brandschutz-Kampagne");
        packSection.addProperty("min_format", packFormat);
        packSection.addProperty("max_format", packFormat);
        meta.add("pack", packSection);
        return meta;
    }

    private static byte[] transparentPng(int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private static byte[] json(Object value) {
        return GSON.toJson(value).getBytes(StandardCharsets.UTF_8);
    }

    private static String stripExtension(String path) {
        int dot = path.lastIndexOf('.');
        return dot < 0 ? path : path.substring(0, dot);
    }

    private static String sha1(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
