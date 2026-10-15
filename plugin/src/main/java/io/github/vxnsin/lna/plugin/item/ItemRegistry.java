package io.github.vxnsin.lna.plugin.item;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.vxnsin.lna.plugin.Keys;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.logging.Logger;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/** Creates and identifies custom items defined in the generated registry.json. */
public final class ItemRegistry {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final Map<String, CustomItem> items;
    private final List<Map.Entry<String, BiConsumer<ItemStack, CustomItem>>> initializers = new ArrayList<>();

    private ItemRegistry(Map<String, CustomItem> items) {
        this.items = Collections.unmodifiableMap(items);
    }

    public static ItemRegistry load(JsonObject registry, Logger logger) {
        Map<String, CustomItem> items = new TreeMap<>();
        for (Map.Entry<String, JsonElement> entry : registry.getAsJsonObject("items").entrySet()) {
            String id = entry.getKey();
            JsonObject json = entry.getValue().getAsJsonObject();
            Material base = Material.matchMaterial(json.get("base").getAsString());
            if (base == null || !base.isItem()) {
                logger.severe("Item '" + id + "': unknown base item " + json.get("base").getAsString() + ", skipped.");
                continue;
            }
            List<String> lore = new ArrayList<>();
            json.getAsJsonArray("lore").forEach(line -> lore.add(line.getAsString()));
            Set<String> tags = new LinkedHashSet<>();
            json.getAsJsonArray("tags").forEach(tag -> tags.add(tag.getAsString()));
            items.put(id, new CustomItem(
                    id,
                    base,
                    NamespacedKey.fromString(json.get("model").getAsString()),
                    json.get("name").getAsString(),
                    List.copyOf(lore),
                    json.get("maxStack").getAsInt(),
                    Set.copyOf(tags),
                    json.getAsJsonObject("properties")));
        }
        return new ItemRegistry(items);
    }

    /** Registers a callback that runs on every newly created stack of an item carrying {@code tag}. */
    public void onCreate(String tag, BiConsumer<ItemStack, CustomItem> initializer) {
        initializers.add(Map.entry(tag, initializer));
    }

    public Optional<CustomItem> get(String id) {
        return Optional.ofNullable(items.get(id));
    }

    public Collection<CustomItem> all() {
        return items.values();
    }

    /** Resolves the custom item a stack represents, if any. */
    public Optional<CustomItem> of(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasItemMeta()) {
            return Optional.empty();
        }
        String id = stack.getItemMeta().getPersistentDataContainer().get(Keys.ITEM_ID, PersistentDataType.STRING);
        return id == null ? Optional.empty() : get(id);
    }

    public boolean isCustom(ItemStack stack) {
        return of(stack).isPresent();
    }

    public ItemStack create(CustomItem item) {
        ItemStack stack = ItemStack.of(item.base());
        stack.editMeta(meta -> {
            meta.setItemModel(item.model());
            meta.itemName(MINI_MESSAGE.deserialize(item.name()));
            meta.lore(item.lore().stream()
                    .map(line -> MINI_MESSAGE.deserialize(line).decoration(TextDecoration.ITALIC, false))
                    .toList());
            meta.setMaxStackSize(item.maxStack());
            meta.getPersistentDataContainer().set(Keys.ITEM_ID, PersistentDataType.STRING, item.id());
        });
        for (Map.Entry<String, BiConsumer<ItemStack, CustomItem>> initializer : initializers) {
            if (item.hasTag(initializer.getKey())) {
                initializer.getValue().accept(stack, item);
            }
        }
        return stack;
    }
}
