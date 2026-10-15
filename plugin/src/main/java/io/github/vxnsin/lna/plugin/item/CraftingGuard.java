package io.github.vxnsin.lna.plugin.item;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Prevents custom items from being used as crafting ingredients. They share their base material
 * with vanilla items (e.g. paper) and would otherwise match vanilla recipes.
 */
public final class CraftingGuard implements Listener {

    private final ItemRegistry items;

    public CraftingGuard(ItemRegistry items) {
        this.items = items;
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        for (ItemStack ingredient : event.getInventory().getMatrix()) {
            if (items.isCustom(ingredient)) {
                event.getInventory().setResult(null);
                return;
            }
        }
    }
}
