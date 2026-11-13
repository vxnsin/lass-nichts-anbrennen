package dev.vensin.lna.packbuilder;

import java.util.List;
import java.util.Map;

public record Content(Map<String, ItemDef> items, Map<String, SoundDef> sounds, Map<String, GlyphDef> glyphs) {

    /** {@code texture} is relative to content/textures (the model is generated), {@code model} to content/models; either may be null. */
    public record ItemDef(
            String id,
            String base,
            String name,
            List<String> lore,
            String texture,
            String model,
            List<UseModel> useModels,
            String display,
            int maxStack,
            List<String> tags,
            Map<String, Object> properties) {}

    /** {@code after} is the use duration in ticks from which {@code model} (relative to content/models) is shown. */
    public record UseModel(int after, String model) {}

    /** {@code files} are paths relative to content/sounds. */
    public record SoundDef(String key, List<String> files, String subtitle, boolean stream) {}

    /** {@code ascent}: pixels above the baseline, negative moves the image down. {@code height}: rendered, scaled proportionally. */
    public record GlyphDef(String id, String texture, int ascent, int height) {}
}
