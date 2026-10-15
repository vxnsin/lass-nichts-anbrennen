package io.github.vxnsin.lna.plugin.hud;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

/**
 * Glyphs (images rendered as characters of the {@code lna:hud} font) and pixel-precise horizontal spacing.
 *
 * <p>MiniMessage placeholders provided by {@link #tags()}:
 * <ul>
 *   <li>{@code <glyph:id>} inserts the glyph with that id from content/glyphs.yml</li>
 *   <li>{@code <shift:n>} moves the cursor by {@code n} pixels (negative values move left)</li>
 * </ul>
 * Together they allow custom HUDs (boss bar title) and custom GUI backgrounds (inventory titles).
 */
public final class Glyphs {

    public record Glyph(String id, String character, int advance) {}

    private final Key font;
    private final Map<String, Glyph> glyphs;
    /** Spacing characters keyed by their pixel advance (powers of two, positive and negative). */
    private final Map<Integer, String> spaces;
    private final int largestSpace;

    private Glyphs(Key font, Map<String, Glyph> glyphs, Map<Integer, String> spaces) {
        this.font = font;
        this.glyphs = Collections.unmodifiableMap(glyphs);
        this.spaces = Collections.unmodifiableMap(spaces);
        this.largestSpace = spaces.keySet().stream().mapToInt(Integer::intValue).max().orElse(1);
    }

    public static Glyphs load(JsonObject registry) {
        Map<String, Glyph> glyphs = new TreeMap<>();
        for (Map.Entry<String, JsonElement> entry : registry.getAsJsonObject("glyphs").entrySet()) {
            JsonObject json = entry.getValue().getAsJsonObject();
            glyphs.put(entry.getKey(), new Glyph(entry.getKey(), json.get("char").getAsString(), json.get("advance").getAsInt()));
        }
        Map<Integer, String> spaces = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : registry.getAsJsonObject("spaces").entrySet()) {
            spaces.put(Integer.parseInt(entry.getKey()), entry.getValue().getAsString());
        }
        return new Glyphs(Key.key(registry.get("font").getAsString()), glyphs, spaces);
    }

    public Optional<Glyph> get(String id) {
        return Optional.ofNullable(glyphs.get(id));
    }

    public boolean has(String id) {
        return glyphs.containsKey(id);
    }

    /** Renders a glyph. White text color keeps the original image colors; the text shadow is disabled. */
    public Component glyph(String id) {
        Glyph glyph = get(id).orElseThrow(() -> new IllegalArgumentException("Unknown glyph: " + id));
        return Component.text(glyph.character(), NamedTextColor.WHITE).font(font).shadowColor(ShadowColor.none());
    }

    /** Horizontal offset in pixels, composed from power-of-two spacing characters. */
    public Component shift(int pixels) {
        if (pixels == 0) {
            return Component.empty();
        }
        int sign = Integer.signum(pixels);
        int remaining = Math.abs(pixels);
        StringBuilder chars = new StringBuilder();
        for (int step = largestSpace; step > 0; step >>= 1) {
            while (remaining >= step) {
                chars.append(spaces.get(sign * step));
                remaining -= step;
            }
        }
        return Component.text(chars.toString()).font(font);
    }

    /**
     * Places {@code content} at horizontal offset {@code x} and moves the cursor back afterwards,
     * so independent HUD elements do not shift each other.
     */
    public Component positioned(int x, Component content, int width) {
        return Component.textOfChildren(shift(x), content, shift(-(x + width)));
    }

    public int size() {
        return glyphs.size();
    }

    public int advance(String id) {
        return get(id).map(Glyph::advance).orElse(0);
    }

    public TagResolver tags() {
        return TagResolver.resolver(
                TagResolver.resolver("glyph", (args, context) -> {
                    String id = args.popOr("<glyph> requires an id, e.g. <glyph:hud_feuerloescher>").value();
                    if (!has(id)) {
                        throw context.newException("Unknown glyph: " + id, args);
                    }
                    return Tag.selfClosingInserting(glyph(id));
                }),
                TagResolver.resolver("shift", (args, context) -> {
                    int pixels = args.popOr("<shift> requires a pixel value, e.g. <shift:-8>").asInt()
                            .orElseThrow(() -> context.newException("<shift> expects an integer", args));
                    return Tag.selfClosingInserting(shift(pixels));
                }));
    }
}
