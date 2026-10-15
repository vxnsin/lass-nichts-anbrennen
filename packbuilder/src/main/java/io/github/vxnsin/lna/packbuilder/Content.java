package io.github.vxnsin.lna.packbuilder;

import java.util.List;
import java.util.Map;

/** Content definitions loaded from content/*.yml. */
public record Content(Map<String, ItemDef> items, Map<String, SoundDef> sounds, Map<String, GlyphDef> glyphs) {

    /**
     * A custom item.
     *
     * @param texture path relative to content/textures (flat item, model is generated), or {@code null}
     * @param model   path relative to content/models (e.g. a Blockbench export), or {@code null}
     * @param useModels models shown while the item is being used (right click held), sorted by start tick
     */
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

    /**
     * A model the client switches to while the item is in use.
     *
     * @param after use duration in ticks from which this model is shown
     * @param model path relative to content/models
     */
    public record UseModel(int after, String model) {}

    /** A sound event. {@code files} are paths relative to content/sounds. */
    public record SoundDef(String key, List<String> files, String subtitle, boolean stream) {}

    /**
     * An image rendered as a font character (HUD elements, GUI backgrounds, chat icons).
     *
     * @param texture path relative to content/textures
     * @param ascent  pixels above the baseline; negative values move the image down
     * @param height  rendered height in pixels; the image is scaled proportionally
     */
    public record GlyphDef(String id, String texture, int ascent, int height) {}
}
