package io.github.vxnsin.lna.plugin.furniture;

import io.github.vxnsin.lna.plugin.item.CustomItem;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Building controls (permission lna.build):
 * <ul>
 *   <li>right click with a furniture or sign item: place it</li>
 *   <li>sneak + right click with any other custom item: place it as a decoration prop</li>
 *   <li>sneak + left click on a placed object: remove it</li>
 * </ul>
 * Right-clicking a placed object fires {@link FurnitureInteractEvent} for every player.
 */
public final class FurnitureListener implements Listener {

    public static final String BUILD_PERMISSION = "lna.build";

    private final FurnitureService furniture;

    public FurnitureListener(FurnitureService furniture) {
        this.furniture = furniture;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlace(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND
                || event.getClickedBlock() == null) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        Optional<CustomItem> item = furniture.items().of(hand);
        if (item.isEmpty() || !player.hasPermission(BUILD_PERMISSION)) {
            return;
        }
        boolean isFurniture = FurnitureService.isFurniture(item.get());
        boolean prop = !isFurniture && player.isSneaking();
        if (!isFurniture && !prop) {
            return;
        }
        event.setCancelled(true);
        Optional<String> error = furniture.place(player, item.get(), event.getClickedBlock(), event.getBlockFace(), prop);
        if (error.isPresent()) {
            player.sendActionBar(Component.text(error.get(), NamedTextColor.RED));
            return;
        }
        player.playSound(player.getLocation(), Sound.BLOCK_STONE_PLACE, 0.8f, 1.1f);
        if (player.getGameMode() != GameMode.CREATIVE) {
            hand.subtract();
        }
    }

    @EventHandler
    public void onAttack(PrePlayerAttackEntityEvent event) {
        if (!(event.getAttacked() instanceof Interaction hitbox) || furniture.idOf(hitbox).isEmpty()) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (!player.isSneaking() || !player.hasPermission(BUILD_PERMISSION)) {
            return;
        }
        String id = furniture.idOf(hitbox).get();
        furniture.remove(hitbox);
        player.playSound(player.getLocation(), Sound.BLOCK_STONE_BREAK, 0.8f, 1.1f);
        if (player.getGameMode() != GameMode.CREATIVE) {
            furniture.items().get(id).ifPresent(item -> player.getInventory().addItem(furniture.items().create(item)));
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !(event.getRightClicked() instanceof Interaction hitbox)) {
            return;
        }
        furniture.idOf(hitbox).ifPresent(id -> {
            event.setCancelled(true);
            Bukkit.getPluginManager().callEvent(new FurnitureInteractEvent(event.getPlayer(), hitbox, id));
        });
    }
}
