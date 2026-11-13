package dev.vensin.lna.server.mission;

import dev.vensin.lna.server.fire.Fire;
import dev.vensin.lna.server.fire.FireListener.Mistake;
import dev.vensin.lna.server.lang.Messages;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.title.Title;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;

public abstract class MissionSession {

    protected static final int HUB_RETURN_TICKS = 200;
    private static final Title.Times TITLE_TIMES =
            Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(3), Duration.ofMillis(700));

    protected final Missions missions;
    protected final Messages messages;
    protected final MissionInfo info;
    private final List<UUID> players = new ArrayList<>();
    /** One bar per player, because the title is in that player's language. */
    private final Map<UUID, BossBar> bars = new HashMap<>();
    private final BossBar.Overlay overlay;
    private boolean finished;

    protected MissionSession(Missions missions, Messages messages, MissionInfo info, Collection<Player> players,
                             BossBar.Overlay overlay) {
        this.missions = missions;
        this.messages = messages;
        this.info = info;
        this.overlay = overlay;
        players.forEach(player -> this.players.add(player.getUuid()));
    }

    public MissionInfo info() {
        return info;
    }

    public boolean involves(Player player) {
        return players.contains(player.getUuid());
    }

    public List<Player> onlinePlayers() {
        return players.stream().map(MinecraftServer.getConnectionManager()::getOnlinePlayerByUuid)
                .filter(Objects::nonNull).toList();
    }

    final void start() {
        onStart();
    }

    public final void finish() {
        if (finished) {
            return;
        }
        finished = true;
        onFinish();
        for (Player player : onlinePlayers()) {
            BossBar bar = bars.remove(player.getUuid());
            if (bar != null) {
                player.hideBossBar(bar);
            }
        }
        missions.ended(this);
    }

    protected boolean finished() {
        return finished;
    }

    final void removePlayer(Player player) {
        players.remove(player.getUuid());
        bars.remove(player.getUuid());
        if (players.isEmpty()) {
            finish();
        }
    }

    protected void broadcast(String key, TagResolver... placeholders) {
        onlinePlayers().forEach(player -> player.sendMessage(messages.text(player, key, placeholders)));
    }

    protected void broadcastEmpty() {
        onlinePlayers().forEach(player -> player.sendMessage(Component.empty()));
    }

    protected void showTitle(Player player, Component subtitle) {
        player.showTitle(Title.title(messages.text(player, "mission.title", Missions.label(info)), subtitle, TITLE_TIMES));
    }

    protected void updateBars(int remaining, int duration, String key, TagResolver... placeholders) {
        float progress = Math.clamp((float) remaining / duration, 0f, 1f);
        TagResolver[] all = new TagResolver[placeholders.length + 1];
        System.arraycopy(placeholders, 0, all, 0, placeholders.length);
        all[placeholders.length] = Placeholder.unparsed("time", clock(remaining));
        for (Player player : onlinePlayers()) {
            Component name = messages.text(player, key, all);
            BossBar bar = bars.get(player.getUuid());
            if (bar == null) {
                bar = BossBar.bossBar(name, progress, BossBar.Color.RED, overlay);
                bars.put(player.getUuid(), bar);
                player.showBossBar(bar);
            } else {
                bar.progress(progress);
                bar.name(name);
            }
        }
    }

    protected static String clock(int seconds) {
        int clamped = Math.max(0, seconds);
        return clamped / 60 + ":" + String.format("%02d", clamped % 60);
    }

    protected abstract void onStart();

    protected void tick() {}

    protected void onInteract(Player player, Entity hitbox) {}

    /** {@code answer} is 1-based. */
    protected void onAnswer(Player player, int answer) {}

    protected void onFireExtinguished(Player player, Fire fire) {}

    protected void onFireMistake(Player player, Fire fire, Mistake mistake) {}

    protected void onFinish() {}
}
