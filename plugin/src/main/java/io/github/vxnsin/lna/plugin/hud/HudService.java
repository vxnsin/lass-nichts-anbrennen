package io.github.vxnsin.lna.plugin.hud;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.ShadowColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Per-player HUD rendered as the title of a white boss bar. The resource pack makes the white boss bar
 * texture transparent, so only the title (glyphs and text) is visible.
 *
 * <p>The HUD consists of named layers rendered in insertion order. Layers should restore the cursor
 * position (see {@link Glyphs#positioned}) so they do not shift each other.
 */
public final class HudService implements Listener {

    private final Map<UUID, BossBar> bars = new HashMap<>();
    private final Map<UUID, Map<String, Component>> layers = new HashMap<>();

    public void set(Player player, String layer, Component content) {
        Map<String, Component> playerLayers = layers.computeIfAbsent(player.getUniqueId(), id -> new LinkedHashMap<>());
        if (content.equals(playerLayers.put(layer, content))) {
            return;
        }
        render(player, playerLayers);
    }

    public void remove(Player player, String layer) {
        Map<String, Component> playerLayers = layers.get(player.getUniqueId());
        if (playerLayers != null && playerLayers.remove(layer) != null) {
            render(player, playerLayers);
        }
    }

    public void clear() {
        bars.forEach((id, bar) -> {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                player.hideBossBar(bar);
            }
        });
        bars.clear();
        layers.clear();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        bars.remove(id);
        layers.remove(id);
    }

    private void render(Player player, Map<String, Component> playerLayers) {
        UUID id = player.getUniqueId();
        if (playerLayers.isEmpty()) {
            BossBar bar = bars.remove(id);
            if (bar != null) {
                player.hideBossBar(bar);
            }
            return;
        }
        Component title = Component.text()
                .shadowColor(ShadowColor.none())
                .append(playerLayers.values())
                .build();
        BossBar bar = bars.get(id);
        if (bar == null) {
            bar = BossBar.bossBar(title, 0f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
            bars.put(id, bar);
            player.showBossBar(bar);
        } else {
            bar.name(title);
        }
    }
}
