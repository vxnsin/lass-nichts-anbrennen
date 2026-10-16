package io.github.vxnsin.lna.plugin.lobby;

import io.github.vxnsin.lna.plugin.Keys;
import io.github.vxnsin.lna.plugin.LnaPlugin;
import io.github.vxnsin.lna.plugin.furniture.FurnitureInteractEvent;
import io.github.vxnsin.lna.plugin.map.MapDefinition;
import io.github.vxnsin.lna.plugin.mission.MissionInfo;
import io.github.vxnsin.lna.plugin.mission.MissionService;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * Lobby in the hub: the campaign menu item, the mission table and the project author figure.
 * The menu item cannot be dropped or moved.
 */
public final class LobbyService implements Listener {

    public static final String MENU_ITEM = "kampagnen_menue";
    private static final int MENU_SLOT = 4;
    private static final String ABOUT_TEXT = "about_chat";

    private final LnaPlugin plugin;
    private final boolean teleportOnJoin;

    public LobbyService(LnaPlugin plugin) {
        this.plugin = plugin;
        this.teleportOnJoin = plugin.getConfig().getBoolean("lobby.teleport-on-join", true);
        plugin.missions().onHubArrival(this::giveMenuItem);
    }

    public void giveMenuItem(Player player) {
        for (ItemStack stack : player.getInventory().getContents()) {
            if (isLobbyItem(stack)) {
                return;
            }
        }
        plugin.items().get(MENU_ITEM).ifPresent(item -> {
            ItemStack stack = plugin.items().create(item);
            stack.editMeta(meta -> meta.getPersistentDataContainer().set(Keys.LOBBY_ITEM, PersistentDataType.STRING, MENU_ITEM));
            player.getInventory().setItem(MENU_SLOT, stack);
        });
    }

    public void showMission(Player player, MissionInfo info) {
        boolean stamped = plugin.missions().passStamps(player).contains(info.id());
        plugin.message(player, "<gold><bold><label></bold></gold><stamp>", Placeholder.unparsed("label", info.label()),
                Placeholder.parsed("stamp", stamped ? " <green>✔ Stempel</green>" : ""));
        plugin.message(player, "<gray><text></gray>", Placeholder.unparsed("text", info.description()));
        plugin.message(player, info.available()
                ? "<green><click:run_command:'/mission start " + info.id() + "'>[Mission starten]</click></green> "
                + "<gray>(startet für deine ganze Gruppe)</gray>"
                : "<dark_gray>Diese Mission ist noch in Arbeit.</dark_gray>");
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
    }

    public void showAbout(Player player) {
        MapDefinition hub = plugin.maps().get(MissionService.HUB_MAP);
        String text = hub != null ? hub.texts().get(ABOUT_TEXT) : null;
        plugin.message(player, text != null ? text
                : "<gold>Lass nichts anbrennen</gold> <gray>– Idee, Konzept und Code: vxnsin</gray>");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (teleportOnJoin && plugin.placedMaps().get(MissionService.HUB_MAP).isPresent()
                && plugin.missions().sessionOf(player).isEmpty()) {
            plugin.missions().sendToHub(player);
        } else {
            giveMenuItem(player);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getAction() == Action.PHYSICAL
                || event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK
                || !isLobbyItem(event.getItem())) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        player.openInventory(new CampaignMenu(plugin, this, player).getInventory());
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.6f, 1.3f);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (event.getView().getTopInventory().getHolder(false) instanceof CampaignMenu menu) {
            event.setCancelled(true);
            if (event.getClickedInventory() == event.getView().getTopInventory()) {
                menu.click(event.getSlot());
            }
            return;
        }
        boolean hotbarSwap = event.getClick() == ClickType.NUMBER_KEY
                && isLobbyItem(event.getWhoClicked().getInventory().getItem(event.getHotbarButton()));
        if (isLobbyItem(event.getCurrentItem()) || isLobbyItem(event.getCursor()) || hotbarSwap) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMenuDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder(false) instanceof CampaignMenu || isLobbyItem(event.getOldCursor())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (isLobbyItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        if (isLobbyItem(event.getMainHandItem()) || isLobbyItem(event.getOffHandItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onMissionCard(FurnitureInteractEvent event) {
        String missionId = event.hitbox().getPersistentDataContainer().get(Keys.MISSION, PersistentDataType.STRING);
        if (missionId != null) {
            MissionInfo info = plugin.missions().missions().get(missionId);
            if (info != null) {
                showMission(event.getPlayer(), info);
            }
        }
    }

    @EventHandler
    public void onFigure(PlayerInteractEntityEvent event) {
        if (event.getHand() == EquipmentSlot.HAND
                && event.getRightClicked().getPersistentDataContainer().has(Keys.NPC, PersistentDataType.STRING)) {
            event.setCancelled(true);
            showAbout(event.getPlayer());
        }
    }

    private static boolean isLobbyItem(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.hasItemMeta()
                && stack.getItemMeta().getPersistentDataContainer().has(Keys.LOBBY_ITEM, PersistentDataType.STRING);
    }
}
