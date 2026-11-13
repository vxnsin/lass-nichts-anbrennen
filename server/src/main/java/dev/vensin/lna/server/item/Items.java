package dev.vensin.lna.server.item;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.vensin.lna.server.Tags;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiFunction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minestom.server.component.DataComponents;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.slf4j.Logger;

public final class Items {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final Map<String, CustomItem> items;
    private final List<Map.Entry<String, BiFunction<ItemStack, CustomItem, ItemStack>>> initializers = new ArrayList<>();

    private Items(Map<String, CustomItem> items) {
        this.items = Collections.unmodifiableMap(items);
    }

    public static Items load(JsonObject registry, Logger logger) {
        Map<String, CustomItem> items = new TreeMap<>();
        for (Map.Entry<String, JsonElement> entry : registry.getAsJsonObject("items").entrySet()) {
            String id = entry.getKey();
            JsonObject json = entry.getValue().getAsJsonObject();
            Material base = Material.fromKey(json.get("base").getAsString());
            if (base == null) {
                logger.error("Item {}: unknown base item {}, skipped", id, json.get("base").getAsString());
                continue;
            }
            List<String> lore = new ArrayList<>();
            json.getAsJsonArray("lore").forEach(line -> lore.add(line.getAsString()));
            Set<String> tags = new LinkedHashSet<>();
            json.getAsJsonArray("tags").forEach(tag -> tags.add(tag.getAsString()));
            items.put(id, new CustomItem(id, base, json.get("model").getAsString(), json.get("name").getAsString(),
                    List.copyOf(lore), json.get("maxStack").getAsInt(), Set.copyOf(tags), json.getAsJsonObject("properties")));
        }
        return new Items(items);
    }

    public Optional<CustomItem> get(String id) {
        return Optional.ofNullable(items.get(id));
    }

    public Collection<CustomItem> all() {
        return items.values();
    }

    public Optional<CustomItem> of(ItemStack stack) {
        if (stack == null || stack.isAir()) {
            return Optional.empty();
        }
        String id = stack.getTag(Tags.ITEM_ID);
        return id == null ? Optional.empty() : get(id);
    }

    /** Runs for every new stack of an item with that tag, e.g. to make extinguishers usable and full. */
    public void onCreate(String tag, BiFunction<ItemStack, CustomItem, ItemStack> initializer) {
        initializers.add(Map.entry(tag, initializer));
    }

    public ItemStack create(CustomItem item) {
        List<Component> lore = item.lore().stream()
                .map(line -> MINI_MESSAGE.deserialize(line).decoration(TextDecoration.ITALIC, false))
                .toList();
        ItemStack stack = ItemStack.of(item.base())
                .with(DataComponents.ITEM_MODEL, item.model())
                .with(DataComponents.ITEM_NAME, MINI_MESSAGE.deserialize(item.name()))
                .with(DataComponents.LORE, lore)
                .with(DataComponents.MAX_STACK_SIZE, item.maxStack())
                .withTag(Tags.ITEM_ID, item.id());
        for (Map.Entry<String, BiFunction<ItemStack, CustomItem, ItemStack>> initializer : initializers) {
            if (item.hasTag(initializer.getKey())) {
                stack = initializer.getValue().apply(stack, item);
            }
        }
        return stack;
    }
}
