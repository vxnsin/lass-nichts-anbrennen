package dev.vensin.lna.server.mission;

import dev.vensin.lna.server.fire.Fire;
import dev.vensin.lna.server.fire.FireListener;
import dev.vensin.lna.server.fire.FireService;
import dev.vensin.lna.server.furniture.Furniture;
import dev.vensin.lna.server.item.Items;
import dev.vensin.lna.server.lang.Messages;
import dev.vensin.lna.server.map.MapDefinition;
import dev.vensin.lna.server.map.PlacedMap;
import dev.vensin.lna.server.world.GameWorld;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.timer.TaskSchedule;

public final class Missions implements FireListener {

    private final Messages messages;
    private final Parties parties;
    private final Passes passes;
    private final Map<String, MissionInfo> missions;
    private final Map<String, MapDefinition> maps;
    private final GameWorld world;
    private final Furniture furniture;
    private final FireService fires;
    private final Items items;
    private final List<MissionSession> sessions = new ArrayList<>();

    public Missions(Messages messages, Parties parties, Passes passes, Map<String, MissionInfo> missions,
                    Map<String, MapDefinition> maps, GameWorld world, Furniture furniture, FireService fires,
                    Items items) {
        this.messages = messages;
        this.parties = parties;
        this.passes = passes;
        this.missions = missions;
        this.maps = maps;
        this.world = world;
        this.furniture = furniture;
        this.fires = fires;
        this.items = items;
    }

    public void register(GlobalEventHandler events) {
        furniture.onClick((player, hitbox) ->
                sessionOf(player).ifPresent(session -> session.onInteract(player, hitbox)));
        fires.listen(this);
        events.addListener(PlayerDisconnectEvent.class, event -> {
            sessionOf(event.getPlayer()).ifPresent(session -> session.removePlayer(event.getPlayer()));
            parties.leave(event.getPlayer());
        });
        MinecraftServer.getSchedulerManager().buildTask(() -> List.copyOf(sessions).forEach(MissionSession::tick))
                .delay(TaskSchedule.tick(20)).repeat(TaskSchedule.tick(20)).schedule();
    }

    /** The {@code <number>} and {@code <title>} placeholders of a mission. */
    public static TagResolver label(MissionInfo info) {
        return TagResolver.resolver(Placeholder.unparsed("number", String.valueOf(info.number())),
                Placeholder.unparsed("title", info.title()));
    }

    public Map<String, MissionInfo> missions() {
        return missions;
    }

    public Passes passes() {
        return passes;
    }

    /** Returns the error in the language of the initiator or empty on success. */
    public Optional<Component> startMission(Player initiator, String missionId) {
        MissionInfo info = missions.get(missionId);
        if (info == null) {
            return error(initiator, "mission.error.unknown", Placeholder.unparsed("id", missionId));
        }
        if (!info.available()) {
            return error(initiator, "mission.error.unavailable", label(info));
        }
        if (!parties.canStartMissions(initiator)) {
            return error(initiator, "mission.error.not-leader");
        }
        List<Player> players = parties.onlineMembers(initiator);
        for (Player player : players) {
            if (sessionOf(player).isPresent()) {
                return error(initiator, "mission.error.busy-player", Placeholder.unparsed("name", player.getUsername()));
            }
        }
        MapDefinition map = info.map() == null ? null : maps.get(info.map());
        Optional<PlacedMap> placed = info.map() == null ? Optional.empty() : world.map(info.map());
        if (map == null || placed.isEmpty()) {
            return error(initiator, "mission.error.no-map");
        }
        if (mapInUse(info.map())) {
            return error(initiator, "mission.error.map-busy");
        }
        MissionSession session = switch (info.type()) {
            case "hazard_hunt" -> new HazardHuntSession(this, messages, info, players, map, placed.get(),
                    world.instance(), furniture);
            case "fire_training" -> new FireTrainingSession(this, messages, info, players, map, placed.get(),
                    world.instance(), fires, items);
            default -> null;
        };
        if (session == null) {
            return error(initiator, "mission.error.no-logic", label(info));
        }
        sessions.add(session);
        session.start();
        return Optional.empty();
    }

    public Optional<MissionSession> sessionOf(Player player) {
        return sessions.stream().filter(session -> session.involves(player)).findFirst();
    }

    public boolean stop(Player player) {
        Optional<MissionSession> session = sessionOf(player);
        session.ifPresent(MissionSession::finish);
        return session.isPresent();
    }

    public boolean answer(Player player, int answer) {
        Optional<MissionSession> session = sessionOf(player);
        session.ifPresent(s -> s.onAnswer(player, answer));
        return session.isPresent();
    }

    @Override
    public void extinguished(Player player, Fire fire) {
        sessionOf(player).ifPresent(session -> session.onFireExtinguished(player, fire));
    }

    @Override
    public void mistake(Player player, Fire fire, Mistake mistake) {
        sessionOf(player).ifPresent(session -> session.onFireMistake(player, fire, mistake));
    }

    boolean mapInUse(String mapId) {
        return sessions.stream().anyMatch(session -> mapId.equals(session.info().map()));
    }

    void stampPass(Player player, MissionInfo info) {
        passes.stamp(player.getUuid(), info.id());
    }

    void ended(MissionSession session) {
        sessions.remove(session);
    }

    void returnToHubLater(List<Player> players, int delayTicks) {
        later(() -> players.forEach(player -> {
            if (player.isOnline() && sessionOf(player).isEmpty()) {
                player.teleport(world.spawn());
            }
        }), delayTicks);
    }

    void later(Runnable task, int delayTicks) {
        MinecraftServer.getSchedulerManager().buildTask(task).delay(TaskSchedule.tick(delayTicks)).schedule();
    }

    private Optional<Component> error(Player player, String key, TagResolver... placeholders) {
        return Optional.of(messages.text(player, key, placeholders));
    }
}
