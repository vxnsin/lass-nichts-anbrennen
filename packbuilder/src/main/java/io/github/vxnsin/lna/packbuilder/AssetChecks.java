package io.github.vxnsin.lna.packbuilder;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/** Validates individual asset files before they are added to the pack. */
final class AssetChecks {

    private AssetChecks() {}

    /** Decodes a PNG and checks its dimensions. Returns {@code null} if the file is not a valid image. */
    static BufferedImage png(String path, byte[] data, Problems problems) {
        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(data));
        } catch (IOException e) {
            image = null;
        }
        if (image == null) {
            problems.error(path + ": Kein gültiges PNG-Bild.");
            return null;
        }
        int w = image.getWidth();
        int h = image.getHeight();
        if (path.startsWith("textures/item/") && w != h && h % w != 0) {
            problems.warn(path + ": Bild ist " + w + "×" + h + " Pixel. Item-Texturen sollten quadratisch sein "
                    + "(oder Höhe = Vielfaches der Breite für Animationen).");
        }
        return image;
    }

    /** Verifies that the file is Ogg Vorbis and warns about non-mono audio. */
    static void ogg(String path, byte[] data, Problems problems) {
        if (data.length < 64 || !new String(data, 0, 4, StandardCharsets.US_ASCII).equals("OggS")) {
            problems.error(path + ": Keine Ogg-Datei. Minecraft braucht .ogg (Vorbis), z. B. mit Audacity exportieren.");
            return;
        }
        // Vorbis identification header: 0x01 "vorbis" <version: 4 bytes> <channels: 1 byte>
        int header = indexOf(data, new byte[] {0x01, 'v', 'o', 'r', 'b', 'i', 's'});
        if (header < 0) {
            problems.error(path + ": Ogg-Datei ohne Vorbis-Ton (Opus/FLAC werden nicht unterstützt).");
            return;
        }
        int channels = data[header + 11] & 0xFF;
        if (channels != 1) {
            problems.warn(path + ": Sound hat " + channels + " Kanäle. Nur Mono-Sounds klingen im Spiel räumlich.");
        }
    }

    /** Returns all texture references ("textures": {...}) of an item or block model. */
    static List<String> modelTextureRefs(String path, byte[] data, Problems problems) {
        List<String> refs = new ArrayList<>();
        JsonObject root;
        try {
            root = JsonParser.parseString(new String(data, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (RuntimeException e) {
            problems.error(path + ": Ungültiges JSON (" + e.getMessage() + ").");
            return refs;
        }
        JsonElement textures = root.get("textures");
        if (textures != null && textures.isJsonObject()) {
            for (var entry : textures.getAsJsonObject().entrySet()) {
                String value = entry.getValue().getAsString();
                if (!value.startsWith("#")) {
                    refs.add(value);
                }
            }
        }
        return refs;
    }

    /**
     * Horizontal advance of a bitmap glyph, computed the same way as the client: rightmost
     * non-transparent column, scaled to the display height, plus one pixel of spacing.
     */
    static int glyphAdvance(BufferedImage image, int displayHeight) {
        int actualWidth = 0;
        for (int x = image.getWidth() - 1; x >= 0 && actualWidth == 0; x--) {
            for (int y = 0; y < image.getHeight(); y++) {
                if ((image.getRGB(x, y) >>> 24) != 0) {
                    actualWidth = x + 1;
                    break;
                }
            }
        }
        float scale = (float) displayHeight / image.getHeight();
        return (int) (0.5f + actualWidth * scale) + 1;
    }

    private static int indexOf(byte[] data, byte[] pattern) {
        outer:
        for (int i = 0; i <= Math.min(data.length - pattern.length, 512); i++) {
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}
