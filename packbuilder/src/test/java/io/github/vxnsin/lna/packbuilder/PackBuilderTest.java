package io.github.vxnsin.lna.packbuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PackBuilderTest {

    private static final String CREDITS_HEADER = """
            | Datei | Titel | Urheber*in | Quelle | Lizenz | Änderungen |
            |---|---|---|---|---|---|
            """;

    @TempDir
    Path root;
    Path content;

    @BeforeEach
    void setUp() throws IOException {
        content = root.resolve("content");
        Files.createDirectories(content.resolve("textures/item"));
        Files.createDirectories(content.resolve("textures/hud"));
        Files.createDirectories(content.resolve("sounds/test"));

        writePng(content.resolve("textures/item/loescher.png"), 16, 16, 10);
        writePng(content.resolve("textures/hud/icon.png"), 8, 8, 6);
        Files.write(content.resolve("sounds/test/spray.ogg"), fakeOgg(1));

        Files.writeString(content.resolve("items.yml"), """
                loescher:
                  base: minecraft:paper
                  name: "<red>Löscher"
                  texture: item/loescher.png
                  max_stack: 1
                  tags: [extinguisher]
                  properties:
                    capacity: 100
                """);
        Files.writeString(content.resolve("sounds.yml"), """
                test.spray:
                  files: [test/spray.ogg]
                  subtitle: "Sprühen"
                """);
        Files.writeString(content.resolve("glyphs.yml"), """
                icon:
                  texture: hud/icon.png
                  ascent: 7
                  height: 8
                """);
    }

    @Test
    void buildsPackWithItemSoundAndGlyph() throws IOException {
        writeCredits("""
                | textures/item/loescher.png | Löscher | Test | eigenes Werk | CC0 1.0 | – |
                | textures/hud/icon.png | Icon | Test | eigenes Werk | CC0 1.0 | – |
                | sounds/test/spray.ogg | Spray | Test | eigenes Werk | CC BY 4.0 | – |
                """);

        PackBuilder.Result result = build();

        assertFalse(result.problems().hasErrors(), () -> String.join("\n", result.problems().errors()));
        Set<String> entries = zipEntries(root.resolve("out/pack.zip"));
        assertTrue(entries.contains("pack.mcmeta"));
        assertTrue(entries.contains("assets/lna/items/loescher.json"));
        assertTrue(entries.contains("assets/lna/models/generated/loescher.json"));
        assertTrue(entries.contains("assets/lna/textures/item/loescher.png"));
        assertTrue(entries.contains("assets/lna/sounds/test/spray.ogg"));
        assertTrue(entries.contains("assets/lna/sounds.json"));
        assertTrue(entries.contains("assets/lna/font/hud.json"));
        assertTrue(entries.contains("assets/minecraft/textures/gui/sprites/boss_bar/white_progress.png"));

        JsonObject registry = JsonParser.parseString(Files.readString(root.resolve("out/registry.json"))).getAsJsonObject();
        JsonObject item = registry.getAsJsonObject("items").getAsJsonObject("loescher");
        assertEquals("lna:loescher", item.get("model").getAsString());
        assertEquals(100, item.getAsJsonObject("properties").get("capacity").getAsInt());
        JsonObject glyph = registry.getAsJsonObject("glyphs").getAsJsonObject("icon");
        assertEquals("", glyph.get("char").getAsString());
        // 6 opaque columns at scale 1 -> advance of 6 + 1 pixel spacing
        assertEquals(7, glyph.get("advance").getAsInt());
        assertEquals("", registry.getAsJsonObject("spaces").get("-1").getAsString());
    }

    @Test
    void failsWhenCreditIsMissing() throws IOException {
        writeCredits("""
                | textures/item/loescher.png | Löscher | Test | eigenes Werk | CC0 1.0 | – |
                | textures/hud/icon.png | Icon | Test | eigenes Werk | CC0 1.0 | – |
                """);

        PackBuilder.Result result = build();

        assertTrue(result.problems().errors().stream().anyMatch(e -> e.startsWith("sounds/test/spray.ogg")));
        assertFalse(Files.exists(root.resolve("out/pack.zip")));
    }

    @Test
    void rejectsNoDerivativesLicense() throws IOException {
        writeCredits("""
                | textures/item/loescher.png | Löscher | Jemand | https://example.org | CC BY-ND 4.0 | – |
                | textures/hud/icon.png | Icon | Test | eigenes Werk | CC0 1.0 | – |
                | sounds/test/spray.ogg | Spray | Test | eigenes Werk | CC0 1.0 | – |
                """);

        PackBuilder.Result result = build();

        assertTrue(result.problems().errors().stream().anyMatch(e -> e.contains("(ND)")));
    }

    @Test
    void warnsAboutStereoSound() throws IOException {
        Files.write(content.resolve("sounds/test/spray.ogg"), fakeOgg(2));
        writeCredits("""
                | textures/item/loescher.png | Löscher | Test | eigenes Werk | CC0 1.0 | – |
                | textures/hud/icon.png | Icon | Test | eigenes Werk | CC0 1.0 | – |
                | sounds/test/spray.ogg | Spray | Test | eigenes Werk | CC0 1.0 | – |
                """);

        PackBuilder.Result result = build();

        assertFalse(result.problems().hasErrors());
        assertTrue(result.problems().warnings().stream().anyMatch(w -> w.contains("2 Kanäle")));
    }

    @Test
    void switchesModelWhileItemIsUsed() throws IOException {
        Files.createDirectories(content.resolve("models/item"));
        Files.writeString(content.resolve("models/item/aim.json"), "{\"parent\":\"minecraft:item/handheld\"}");
        Files.writeString(content.resolve("items.yml"), """
                loescher:
                  base: minecraft:paper
                  name: "Löscher"
                  texture: item/loescher.png
                  use_models:
                    - { after: 4, model: item/aim.json }
                """);
        writeCredits("""
                | textures/item/loescher.png | Löscher | Test | eigenes Werk | CC0 1.0 | – |
                | textures/hud/icon.png | Icon | Test | eigenes Werk | CC0 1.0 | – |
                | sounds/test/spray.ogg | Spray | Test | eigenes Werk | CC0 1.0 | – |
                | models/item/aim.json | Zielen | Test | eigenes Werk | CC0 1.0 | – |
                """);

        PackBuilder.Result result = build();

        assertFalse(result.problems().hasErrors(), () -> String.join("\n", result.problems().errors()));
        Path zip = root.resolve("out/pack.zip");
        JsonObject model = JsonParser.parseString(zipEntry(zip, "assets/lna/items/loescher.json"))
                .getAsJsonObject().getAsJsonObject("model");
        assertEquals("minecraft:condition", model.get("type").getAsString());
        assertEquals("minecraft:using_item", model.get("property").getAsString());
        assertEquals("lna:generated/loescher", model.getAsJsonObject("on_false").get("model").getAsString());
        JsonObject entry = model.getAsJsonObject("on_true").getAsJsonArray("entries").get(0).getAsJsonObject();
        assertEquals(4, entry.get("threshold").getAsInt());
        assertEquals("lna:item/aim", entry.getAsJsonObject("model").get("model").getAsString());
        assertTrue(JsonParser.parseString(zipEntry(zip, "assets/lna/sounds.json")).getAsJsonObject().has("silent"));
    }

    @Test
    void sameContentGivesSameHash() throws IOException {
        writeCredits("""
                | textures/item/loescher.png | Löscher | Test | eigenes Werk | CC0 1.0 | – |
                | textures/hud/icon.png | Icon | Test | eigenes Werk | CC0 1.0 | – |
                | sounds/test/spray.ogg | Spray | Test | eigenes Werk | CC0 1.0 | – |
                """);

        String first = build().sha1();
        String second = build().sha1();

        assertNotNull(first);
        assertEquals(first, second);
    }

    private PackBuilder.Result build() throws IOException {
        return new PackBuilder(content, root.resolve("CREDITS.md"), root.resolve("out"), 97).build();
    }

    private void writeCredits(String rows) throws IOException {
        Files.writeString(root.resolve("CREDITS.md"), "# Credits\n\n" + CREDITS_HEADER + rows);
    }

    private static Set<String> zipEntries(Path zip) throws IOException {
        Set<String> names = new HashSet<>();
        try (ZipFile file = new ZipFile(zip.toFile())) {
            file.stream().forEach(entry -> names.add(entry.getName()));
        }
        return names;
    }

    private static String zipEntry(Path zip, String name) throws IOException {
        try (ZipFile file = new ZipFile(zip.toFile())) {
            return new String(file.getInputStream(file.getEntry(name)).readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** Image whose first {@code opaqueColumns} columns are opaque; the rest is transparent. */
    private static void writePng(Path path, int width, int height, int opaqueColumns) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < opaqueColumns; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, 0xFFCC2222);
            }
        }
        ImageIO.write(image, "png", path.toFile());
    }

    /** Minimal Ogg Vorbis header, sufficient for the builder's format check. */
    private static byte[] fakeOgg(int channels) {
        byte[] data = new byte[96];
        System.arraycopy("OggS".getBytes(), 0, data, 0, 4);
        int header = 28;
        data[header] = 0x01;
        System.arraycopy("vorbis".getBytes(), 0, data, header + 1, 6);
        data[header + 11] = (byte) channels;
        return data;
    }
}
