package dev.vensin.lna.server.hud;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.ShadowColor;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.PlayerDisconnectEvent;

/** The HUD is the title of a white boss bar; the resource pack makes that bar's texture transparent. */
public final class Hud {

    private final Map<UUID, BossBar> bars = new HashMap<>();
    private final Map<UUID, Map<String, Component>> layers = new HashMap<>();

    public void register(GlobalEventHandler events) {
        events.addListener(PlayerDisconnectEvent.class, event -> {
            bars.remove(event.getPlayer().getUuid());
            layers.remove(event.getPlayer().getUuid());
        });
    }

    public void set(Player player, String layer, Component content) {
        Map<String, Component> playerLayers = layers.computeIfAbsent(player.getUuid(), id -> new LinkedHashMap<>());
        if (content.equals(playerLayers.put(layer, content))) {
            return;
        }
        render(player, playerLayers);
    }

    public void remove(Player player, String layer) {
        Map<String, Component> playerLayers = layers.get(player.getUuid());
        if (playerLayers != null && playerLayers.remove(layer) != null) {
            render(player, playerLayers);
        }
    }

    private void render(Player player, Map<String, Component> playerLayers) {
        UUID id = player.getUuid();
        if (playerLayers.isEmpty()) {
            BossBar bar = bars.remove(id);
            if (bar != null) {
                player.hideBossBar(bar);
            }
            return;
        }
        Component title = Component.text().shadowColor(ShadowColor.none()).append(playerLayers.values()).build();
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
