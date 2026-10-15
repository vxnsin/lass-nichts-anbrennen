package io.github.vxnsin.lna.plugin.item;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

/** A custom item defined in content/items.yml and loaded from the generated registry.json. */
public record CustomItem(
        String id,
        Material base,
        NamespacedKey model,
        String name,
        List<String> lore,
        int maxStack,
        Set<String> tags,
        JsonObject properties) {

    public boolean hasTag(String tag) {
        return tags.contains(tag);
    }

    public int intProperty(String key, int fallback) {
        JsonElement value = properties.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsInt() : fallback;
    }

    public double doubleProperty(String key, double fallback) {
        JsonElement value = properties.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsDouble() : fallback;
    }

    public String stringProperty(String key, String fallback) {
        JsonElement value = properties.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : fallback;
    }
}
