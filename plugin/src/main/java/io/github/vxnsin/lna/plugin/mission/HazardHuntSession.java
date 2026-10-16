package io.github.vxnsin.lna.plugin.mission;

import io.github.vxnsin.lna.plugin.Keys;
import io.github.vxnsin.lna.plugin.furniture.FurnitureService;
import io.github.vxnsin.lna.plugin.map.MapDefinition;
import io.github.vxnsin.lna.plugin.map.MapDefinition.Hazard;
import io.github.vxnsin.lna.plugin.map.PlacedMaps.PlacedMap;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

/**
 * Mission 2 "Gefahren-Detektiv": find and report all fire hazards of a map before the time runs out.
 * Hazards and harmless objects come from the map file (hazards / safe); reporting a harmless object
 * counts as a false alarm. Found hazards glow for the whole group.
 */
final class HazardHuntSession extends MissionSession {

    private final MapDefinition map;
    private final PlacedMap placed;
    private final FurnitureService furniture;
    private final Set<String> found = new LinkedHashSet<>();
    private final Set<String> reportedSafe = new LinkedHashSet<>();
    private final BossBar bar;
    private final int duration;
    private int remaining;

    HazardHuntSession(MissionService service, MissionInfo info, Collection<Player> players, MapDefinition map,
                      PlacedMap placed, FurnitureService furniture) {
        super(service, info, players);
        this.map = map;
        this.placed = placed;
        this.furniture = furniture;
        this.duration = info.duration();
        this.remaining = duration;
        this.bar = BossBar.bossBar(Component.empty(), 1f, BossBar.Color.RED, BossBar.Overlay.NOTCHED_10);
    }

    @Override
    protected void onStart() {
        setGlowing(false);
        updateBar();
        Title title = Title.title(Component.text(info.label(), NamedTextColor.GOLD),
                Component.text("Finde " + map.hazards().size() + " Brandgefahren", NamedTextColor.WHITE),
                Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(3), Duration.ofMillis(700)));
        for (Player player : onlinePlayers()) {
            placed.spawn().ifPresent(player::teleport);
            player.showBossBar(bar);
            player.showTitle(title);
        }
        broadcast(Component.text("Rechtsklick auf eine Gefahr, um sie zu melden. Aber Vorsicht: "
                + "Fehlalarme zählen auch!", NamedTextColor.GRAY));
    }

    @Override
    protected void tick() {
        remaining--;
        updateBar();
        if (remaining == 60) {
            broadcast(Component.text("Noch eine Minute!", NamedTextColor.GOLD));
        }
        if (remaining <= 0) {
            broadcast(Component.text("Die Zeit ist um!", NamedTextColor.RED));
            finish();
        }
    }

    @Override
    protected void onInteract(Player player, Interaction hitbox) {
        if (!placed.contains(hitbox.getLocation())) {
            return;
        }
        var data = hitbox.getPersistentDataContainer();
        String hazardId = data.get(Keys.HAZARD, PersistentDataType.STRING);
        String safeId = data.get(Keys.SAFE, PersistentDataType.STRING);
        if (hazardId != null) {
            reportHazard(player, hitbox, hazardId);
        } else if (safeId != null) {
            reportSafe(player, safeId);
        } else {
            player.sendActionBar(Component.text("Hier ist alles in Ordnung.", NamedTextColor.GRAY));
        }
    }

    private void reportHazard(Player player, Interaction hitbox, String hazardId) {
        Hazard hazard = map.hazards().get(hazardId);
        if (hazard == null || !found.add(hazardId)) {
            player.sendActionBar(Component.text("Diese Gefahr wurde schon gemeldet.", NamedTextColor.GRAY));
            return;
        }
        markFound(hitbox);
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
        broadcast(Component.text("✔ ", NamedTextColor.GREEN)
                .append(Component.text(player.getName() + " hat gefunden: ", NamedTextColor.GRAY))
                .append(Component.text(hazard.title(), NamedTextColor.WHITE)));
        broadcast(Component.text("   " + hazard.explanation(), NamedTextColor.GRAY));
        updateBar();
        if (found.size() == map.hazards().size()) {
            broadcast(Component.text("Alle Gefahren gefunden!", NamedTextColor.GREEN, TextDecoration.BOLD));
            finish();
        }
    }

    private void reportSafe(Player player, String safeId) {
        if (!reportedSafe.add(safeId)) {
            player.sendActionBar(Component.text("Das war schon ein Fehlalarm.", NamedTextColor.GRAY));
            return;
        }
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.6f);
        broadcast(Component.text("✘ Fehlalarm von " + player.getName() + ": ", NamedTextColor.RED)
                .append(Component.text(map.safe().getOrDefault(safeId, "Kein Problem."), NamedTextColor.GRAY)));
    }

    @Override
    protected void onFinish() {
        int total = map.hazards().size();
        int falseAlarms = reportedSafe.size();
        String stars = found.size() == total && falseAlarms == 0 ? "★★★"
                : found.size() >= Math.ceil(total * 0.8) ? "★★☆"
                : found.size() >= Math.ceil(total * 0.5) ? "★☆☆" : "☆☆☆";
        int seconds = duration - Math.max(0, remaining);
        broadcast(Component.empty());
        broadcast(Component.text(info.label() + " – Ergebnis " + stars, NamedTextColor.GOLD, TextDecoration.BOLD));
        broadcast(Component.text(found.size() + " von " + total + " Gefahren gefunden, " + falseAlarms
                + " Fehlalarm(e), Zeit " + seconds / 60 + ":" + String.format("%02d", seconds % 60), NamedTextColor.WHITE));
        for (Map.Entry<String, Hazard> entry : map.hazards().entrySet()) {
            if (!found.contains(entry.getKey())) {
                broadcast(Component.text("• Übersehen: ", NamedTextColor.RED)
                        .append(Component.text(entry.getValue().title(), NamedTextColor.WHITE))
                        .append(Component.text(" – " + entry.getValue().explanation(), NamedTextColor.GRAY)));
            }
        }
        boolean passed = found.size() >= Math.ceil(total * 0.8);
        for (Player player : onlinePlayers()) {
            player.hideBossBar(bar);
            if (passed) {
                service.stampPass(player, info);
            }
        }
        broadcast(passed
                ? Component.text("Stempel für den Brandschutz-Pass erhalten! Zurück zum Hub in 10 Sekunden.", NamedTextColor.GREEN)
                : Component.text("Für den Stempel brauchst du mindestens " + (int) Math.ceil(total * 0.8)
                        + " Gefahren. Versuch es noch einmal! Zurück zum Hub in 10 Sekunden.", NamedTextColor.GOLD));
        service.returnToHubLater(onlinePlayers(), 200L);
        service.scheduleLater(() -> setGlowing(false), 200L);
    }

    private void updateBar() {
        float progress = Math.clamp((float) remaining / duration, 0f, 1f);
        bar.progress(progress);
        bar.name(Component.text("Gefahren gefunden: " + found.size() + "/" + map.hazards().size() + "  ·  "
                + Math.max(0, remaining) / 60 + ":" + String.format("%02d", Math.max(0, remaining) % 60), NamedTextColor.WHITE));
    }

    private void markFound(Interaction hitbox) {
        furniture.display(hitbox).ifPresentOrElse(display -> display.setGlowing(true), () -> hitbox.getWorld()
                .spawnParticle(Particle.HAPPY_VILLAGER, hitbox.getLocation().add(0, 0.6, 0), 15, 0.4, 0.4, 0.4, 0));
        hitbox.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, hitbox.getLocation().add(0, 0.6, 0), 10, 0.3, 0.3, 0.3, 0);
    }

    private void setGlowing(boolean glowing) {
        placed.bukkitWorld().ifPresent((World world) -> world.getNearbyEntities(placed.bounds()).stream()
                .filter(entity -> entity instanceof Interaction)
                .map(entity -> (Interaction) entity)
                .forEach(hitbox -> furniture.display(hitbox).ifPresent(display -> display.setGlowing(glowing))));
    }
}
