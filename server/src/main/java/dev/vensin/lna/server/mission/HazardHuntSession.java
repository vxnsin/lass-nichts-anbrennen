package dev.vensin.lna.server.mission;

import dev.vensin.lna.server.Tags;
import dev.vensin.lna.server.fire.Effects;
import dev.vensin.lna.server.furniture.Furniture;
import dev.vensin.lna.server.lang.Messages;
import dev.vensin.lna.server.map.MapDefinition;
import dev.vensin.lna.server.map.MapDefinition.Hazard;
import dev.vensin.lna.server.map.PlacedMap;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.particle.Particle;
import net.minestom.server.sound.SoundEvent;

final class HazardHuntSession extends MissionSession {

    private static final double PASS_SHARE = 0.8;

    private final MapDefinition map;
    private final PlacedMap placed;
    private final Instance instance;
    private final Furniture furniture;
    private final Set<String> found = new LinkedHashSet<>();
    private final Set<String> reportedSafe = new LinkedHashSet<>();
    private final int duration;
    private int remaining;

    HazardHuntSession(Missions missions, Messages messages, MissionInfo info, Collection<Player> players,
                      MapDefinition map, PlacedMap placed, Instance instance, Furniture furniture) {
        super(missions, messages, info, players, BossBar.Overlay.NOTCHED_10);
        this.map = map;
        this.placed = placed;
        this.instance = instance;
        this.furniture = furniture;
        this.duration = info.duration();
        this.remaining = duration;
    }

    @Override
    protected void onStart() {
        setGlowing(false);
        for (Player player : onlinePlayers()) {
            player.teleport(placed.spawn());
            showTitle(player, messages.text(player, "mission.hazard.subtitle",
                    Placeholder.unparsed("count", String.valueOf(map.hazards().size()))));
        }
        updateBar();
        broadcast("mission.hazard.intro");
    }

    @Override
    protected void tick() {
        remaining--;
        updateBar();
        if (remaining == 60) {
            broadcast("mission.one-minute");
        }
        if (remaining <= 0) {
            broadcast("mission.time-up");
            finish();
        }
    }

    @Override
    protected void onInteract(Player player, Entity hitbox) {
        if (!placed.contains(hitbox.getPosition())) {
            return;
        }
        String hazardId = hitbox.getTag(Tags.HAZARD);
        String safeId = hitbox.getTag(Tags.SAFE);
        if (hazardId != null) {
            reportHazard(player, hitbox, hazardId);
        } else if (safeId != null) {
            reportSafe(player, safeId);
        } else {
            player.sendActionBar(messages.text(player, "mission.hazard.all-fine"));
        }
    }

    private void reportHazard(Player player, Entity hitbox, String hazardId) {
        Hazard hazard = map.hazards().get(hazardId);
        if (hazard == null || !found.add(hazardId)) {
            player.sendActionBar(messages.text(player, "mission.hazard.already-found"));
            return;
        }
        markFound(hitbox);
        player.playSound(Sound.sound(SoundEvent.ENTITY_EXPERIENCE_ORB_PICKUP, Sound.Source.PLAYER, 1f, 1.2f));
        broadcast("mission.hazard.found", Placeholder.unparsed("name", player.getUsername()),
                Placeholder.unparsed("title", hazard.title()));
        broadcast("mission.hazard.explanation", Placeholder.unparsed("text", hazard.explanation()));
        updateBar();
        if (found.size() == map.hazards().size()) {
            broadcast("mission.hazard.all-found");
            finish();
        }
    }

    private void reportSafe(Player player, String safeId) {
        if (!reportedSafe.add(safeId)) {
            player.sendActionBar(messages.text(player, "mission.hazard.already-false-alarm"));
            return;
        }
        player.playSound(Sound.sound(SoundEvent.BLOCK_NOTE_BLOCK_BASS, Sound.Source.PLAYER, 1f, 0.6f));
        String reason = map.safe().get(safeId);
        for (Player member : onlinePlayers()) {
            member.sendMessage(messages.text(member, "mission.hazard.false-alarm",
                    Placeholder.unparsed("name", player.getUsername()),
                    Placeholder.component("text", reason != null ? Component.text(reason)
                            : messages.text(member, "mission.hazard.no-problem"))));
        }
    }

    @Override
    protected void onFinish() {
        int total = map.hazards().size();
        int needed = (int) Math.ceil(total * PASS_SHARE);
        String stars = found.size() == total && reportedSafe.isEmpty() ? "★★★"
                : found.size() >= needed ? "★★☆"
                : found.size() >= Math.ceil(total * 0.5) ? "★☆☆" : "☆☆☆";
        broadcastEmpty();
        broadcast("mission.hazard.result", Missions.label(info), Placeholder.unparsed("stars", stars));
        broadcast("mission.hazard.summary", Placeholder.unparsed("found", String.valueOf(found.size())),
                Placeholder.unparsed("total", String.valueOf(total)),
                Placeholder.unparsed("alarms", String.valueOf(reportedSafe.size())),
                Placeholder.unparsed("time", clock(duration - Math.max(0, remaining))));
        for (Map.Entry<String, Hazard> entry : map.hazards().entrySet()) {
            if (!found.contains(entry.getKey())) {
                broadcast("mission.hazard.missed", Placeholder.unparsed("title", entry.getValue().title()),
                        Placeholder.unparsed("text", entry.getValue().explanation()));
            }
        }
        boolean passed = found.size() >= needed;
        if (passed) {
            onlinePlayers().forEach(player -> missions.stampPass(player, info));
            broadcast("mission.stamp-earned");
        } else {
            broadcast("mission.hazard.failed", Placeholder.unparsed("needed", String.valueOf(needed)));
        }
        missions.returnToHubLater(onlinePlayers(), HUB_RETURN_TICKS);
        missions.later(() -> {
            if (!missions.mapInUse(info.map())) {
                setGlowing(false);
            }
        }, HUB_RETURN_TICKS);
    }

    private void updateBar() {
        updateBars(remaining, duration, "mission.hazard.bar",
                Placeholder.unparsed("found", String.valueOf(found.size())),
                Placeholder.unparsed("total", String.valueOf(map.hazards().size())));
    }

    private void markFound(Entity hitbox) {
        furniture.model(hitbox).ifPresent(model -> model.setGlowing(true));
        Effects.particles(instance, Particle.HAPPY_VILLAGER, hitbox.getPosition().add(0, 0.6, 0), 0.4, 0.4, 0.4, 0, 20);
    }

    private void setGlowing(boolean glowing) {
        for (Entity entity : instance.getEntities()) {
            if (info.map().equals(entity.getTag(Tags.MAP))) {
                furniture.model(entity).ifPresent(model -> model.setGlowing(glowing));
            }
        }
    }
}
