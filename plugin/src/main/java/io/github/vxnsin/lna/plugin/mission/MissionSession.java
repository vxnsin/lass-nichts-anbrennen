package io.github.vxnsin.lna.plugin.mission;

import io.github.vxnsin.lna.plugin.fire.Fire;
import io.github.vxnsin.lna.plugin.fire.FireMistakeEvent.Mistake;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;

/**
 * A running mission for one group of players. Players are switched to adventure mode for the duration
 * and get their previous game mode back when the mission ends or they leave.
 */
public abstract class MissionSession {

    protected final MissionService service;
    protected final MissionInfo info;
    private final List<UUID> players = new ArrayList<>();
    private final Map<UUID, GameMode> previousModes = new HashMap<>();
    private boolean finished;

    protected MissionSession(MissionService service, MissionInfo info, Collection<Player> players) {
        this.service = service;
        this.info = info;
        players.forEach(player -> this.players.add(player.getUniqueId()));
    }

    public MissionInfo info() {
        return info;
    }

    public boolean involves(Player player) {
        return players.contains(player.getUniqueId());
    }

    public List<Player> onlinePlayers() {
        return players.stream().map(Bukkit::getPlayer).filter(Objects::nonNull).toList();
    }

    final void start() {
        for (Player player : onlinePlayers()) {
            previousModes.put(player.getUniqueId(), player.getGameMode());
            player.setGameMode(GameMode.ADVENTURE);
        }
        onStart();
    }

    /** Ends the mission (results, return to the hub) exactly once. */
    public final void finish() {
        if (finished) {
            return;
        }
        finished = true;
        onFinish();
        onlinePlayers().forEach(this::restore);
        service.ended(this);
    }

    final void removePlayer(Player player) {
        restore(player);
        players.remove(player.getUniqueId());
        if (players.isEmpty()) {
            finish();
        }
    }

    protected void broadcast(Component message) {
        onlinePlayers().forEach(player -> player.sendMessage(message));
    }

    /** Called once after the players have been registered. */
    protected abstract void onStart();

    /** Called every second while the mission runs. */
    protected void tick() {}

    /** A player of this mission right-clicked a placed object. */
    protected void onInteract(Player player, Interaction hitbox) {}

    /** A player of this mission completely extinguished a simulated fire. */
    protected void onFireExtinguished(Player player, Fire fire) {}

    /** A player of this mission handled a simulated fire wrongly. */
    protected void onFireMistake(Player player, Fire fire, Mistake mistake) {}

    /** Called once when the mission ends, before game modes are restored. */
    protected void onFinish() {}

    private void restore(Player player) {
        GameMode mode = previousModes.remove(player.getUniqueId());
        if (mode != null) {
            player.setGameMode(mode);
        }
    }
}
