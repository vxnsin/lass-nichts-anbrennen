package io.github.vxnsin.lna.plugin.lobby;

import io.github.vxnsin.lna.plugin.LnaPlugin;
import io.github.vxnsin.lna.plugin.item.CustomItem;
import io.github.vxnsin.lna.plugin.mission.MissionInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/** Campaign menu opened with the lobby item: missions, group, fire safety pass, hub and project info. */
final class CampaignMenu implements InventoryHolder {

    private static final int[] MISSION_SLOTS = {11, 12, 13, 14, 15};

    private final Inventory inventory;
    private final Map<Integer, Runnable> actions = new HashMap<>();

    CampaignMenu(LnaPlugin plugin, LobbyService lobby, Player player) {
        inventory = Bukkit.createInventory(this, 27, Component.text("Kampagne · Lass nichts anbrennen"));
        ItemStack filler = ItemStack.of(Material.GRAY_STAINED_GLASS_PANE);
        filler.editMeta(meta -> meta.setHideTooltip(true));
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }

        List<String> stamps = plugin.missions().passStamps(player);
        int index = 0;
        for (MissionInfo info : plugin.missions().missions().values()) {
            if (index >= MISSION_SLOTS.length) {
                break;
            }
            List<Component> lore = new ArrayList<>();
            lore.add(line(info.description(), NamedTextColor.GRAY));
            lore.add(Component.empty());
            if (stamps.contains(info.id())) {
                lore.add(line("✔ Stempel im Brandschutz-Pass", NamedTextColor.GREEN));
            }
            lore.add(info.available() ? line("Klicken zum Starten", NamedTextColor.YELLOW)
                    : line("Bald verfügbar", NamedTextColor.DARK_GRAY));
            set(MISSION_SLOTS[index++], icon(plugin, info.icon(), Component.text(info.label(),
                    info.available() ? NamedTextColor.GOLD : NamedTextColor.GRAY), lore), () -> {
                player.closeInventory();
                plugin.missions().startMission(player, info.id())
                        .ifPresent(error -> player.sendMessage(Component.text(error, NamedTextColor.RED)));
            });
        }

        set(21, icon(plugin, "handy", Component.text("Gruppe", NamedTextColor.AQUA),
                List.of(line("Mit anderen zusammen spielen", NamedTextColor.GRAY))), () -> {
            player.closeInventory();
            player.performCommand("gruppe");
        });
        set(22, icon(plugin, "klemmbrett", Component.text("Brandschutz-Pass", NamedTextColor.GREEN),
                List.of(line(stamps.size() + " von " + plugin.missions().missions().size() + " Stempeln", NamedTextColor.GRAY))),
                () -> {
                    player.closeInventory();
                    player.performCommand("mission");
                });
        set(23, icon(plugin, "fluchtplan", Component.text("Zum Hub", NamedTextColor.WHITE),
                List.of(line("Zurück ins Klassenzimmer", NamedTextColor.GRAY))), () -> {
            player.closeInventory();
            plugin.missions().sendToHub(player);
        });
        set(26, icon(plugin, "brandschutzhelfer_ausweis", Component.text("Über das Projekt", NamedTextColor.GOLD),
                List.of(line("Wer hat das gebaut?", NamedTextColor.GRAY))), () -> {
            player.closeInventory();
            lobby.showAbout(player);
        });
    }

    void click(int slot) {
        Runnable action = actions.get(slot);
        if (action != null) {
            action.run();
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    private void set(int slot, ItemStack item, Runnable action) {
        inventory.setItem(slot, item);
        actions.put(slot, action);
    }

    private static ItemStack icon(LnaPlugin plugin, String itemId, Component name, List<Component> lore) {
        Optional<CustomItem> item = plugin.items().get(itemId);
        ItemStack stack = item.map(plugin.items()::create).orElseGet(() -> ItemStack.of(Material.PAPER));
        stack.editMeta(meta -> {
            meta.itemName(name);
            meta.lore(lore);
        });
        return stack;
    }

    private static Component line(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }
}
