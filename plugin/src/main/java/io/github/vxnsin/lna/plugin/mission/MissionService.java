package io.github.vxnsin.lna.plugin.mission;

import io.github.vxnsin.lna.plugin.Keys;
import io.github.vxnsin.lna.plugin.LnaPlugin;
import io.github.vxnsin.lna.plugin.fire.FireExtinguishedEvent;
import io.github.vxnsin.lna.plugin.fire.FireMistakeEvent;
import io.github.vxnsin.lna.plugin.furniture.FurnitureInteractEvent;
import io.github.vxnsin.lna.plugin.map.MapDefinition;
import io.github.vxnsin.lna.plugin.map.PlacedMaps.PlacedMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;

/** Starts and runs missions for groups, routes object clicks to the running mission and keeps the fire safety pass. */
public final class MissionService implements Listener {

    public static final String HUB_MAP = "hub";

    private final LnaPlugin plugin;
    private final PartyService parties;
    private final Map<String, MissionInfo> missions;
    private final List<MissionSession> sessions = new ArrayList<>();
    private Consumer<Player> hubArrival = player -> {};

    public MissionService(LnaPlugin plugin, PartyService parties, Map<String, MissionInfo> missions) {
        this.plugin = plugin;
        this.parties = parties;
        this.missions = missions;
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> List.copyOf(sessions).forEach(MissionSession::tick), 20L, 20L);
    }

    public Map<String, MissionInfo> missions() {
        return missions;
    }

    /** Starts a mission for the initiator and their group. Returns an error message or empty on success. */
    public Optional<String> startMission(Player initiator, String missionId) {
        MissionInfo info = missions.get(missionId);
        if (info == null) {
            return Optional.of("Unbekannte Mission: " + missionId);
        }
        if (!info.available()) {
            return Optional.of(info.label() + " ist noch in Arbeit.");
        }
        if (!parties.canStartMissions(initiator)) {
            return Optional.of("Nur die Gruppenleitung kann Missionen starten.");
        }
        List<Player> players = parties.onlineMembers(initiator);
        for (Player player : players) {
            if (sessionOf(player).isPresent()) {
                return Optional.of(player.getName() + " ist schon in einer Mission.");
            }
        }
        MapDefinition map = plugin.maps().get(info.map());
        Optional<PlacedMap> placed = plugin.placedMaps().get(info.map());
        if (map == null || placed.isEmpty() || placed.get().spawn().isEmpty()) {
            return Optional.of("Die Map '" + info.map() + "' ist noch nicht gebaut (/lna map build " + info.map() + ").");
        }
        if (sessions.stream().anyMatch(session -> info.map().equals(session.info().map()))) {
            return Optional.of("Die Map ist gerade belegt. Warte, bis die andere Gruppe fertig ist.");
        }
        MissionSession session = switch (info.type()) {
            case "hazard_hunt" -> new HazardHuntSession(this, info, players, map, placed.get(), plugin.furniture());
            case "fire_training" -> new FireTrainingSession(this, info, players, map, placed.get(), plugin.fires(),
                    plugin.items());
            default -> null;
        };
        if (session == null) {
            return Optional.of("Für " + info.label() + " gibt es noch keine Spiellogik.");
        }
        sessions.add(session);
        session.start();
        return Optional.empty();
    }

    public Optional<MissionSession> sessionOf(Player player) {
        return sessions.stream().filter(session -> session.involves(player)).findFirst();
    }

    /** Ends the player's mission for the whole group. */
    public boolean stop(Player player) {
        Optional<MissionSession> session = sessionOf(player);
        session.ifPresent(MissionSession::finish);
        return session.isPresent();
    }

    public void stopAll() {
        List.copyOf(sessions).forEach(MissionSession::finish);
    }

    /** Mission ids the player has a fire safety pass stamp for. */
    public List<String> passStamps(Player player) {
        List<String> stamps = player.getPersistentDataContainer().get(Keys.PASS, PersistentDataType.LIST.strings());
        return stamps == null ? List.of() : stamps;
    }

    /** Registers a callback for players arriving in the hub (e.g. to hand out the lobby items). */
    public void onHubArrival(Consumer<Player> callback) {
        hubArrival = callback;
    }

    public void sendToHub(Player player) {
        if (!player.isOnline()) {
            return;
        }
        plugin.placedMaps().get(HUB_MAP).flatMap(PlacedMap::spawn).ifPresent(player::teleport);
        hubArrival.accept(player);
    }

    void stampPass(Player player, MissionInfo info) {
        List<String> stamps = new ArrayList<>(passStamps(player));
        if (!stamps.contains(info.id())) {
            stamps.add(info.id());
            player.getPersistentDataContainer().set(Keys.PASS, PersistentDataType.LIST.strings(), stamps);
        }
    }

    void ended(MissionSession session) {
        sessions.remove(session);
    }

    void returnToHubLater(List<Player> players, long delayTicks) {
        scheduleLater(() -> players.forEach(this::sendToHub), delayTicks);
    }

    void scheduleLater(Runnable task, long delayTicks) {
        Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
    }

    @EventHandler
    public void onFurnitureInteract(FurnitureInteractEvent event) {
        sessionOf(event.getPlayer()).ifPresent(session -> session.onInteract(event.getPlayer(), event.hitbox()));
    }

    @EventHandler
    public void onFireExtinguished(FireExtinguishedEvent event) {
        sessionOf(event.getPlayer()).ifPresent(session -> session.onFireExtinguished(event.getPlayer(), event.fire()));
    }

    @EventHandler
    public void onFireMistake(FireMistakeEvent event) {
        sessionOf(event.getPlayer()).ifPresent(session ->
                session.onFireMistake(event.getPlayer(), event.fire(), event.mistake()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        sessionOf(event.getPlayer()).ifPresent(session -> session.removePlayer(event.getPlayer()));
        parties.leave(event.getPlayer());
    }
}
