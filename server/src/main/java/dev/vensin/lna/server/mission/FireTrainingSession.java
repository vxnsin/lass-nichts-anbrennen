package dev.vensin.lna.server.mission;

import dev.vensin.lna.server.Tags;
import dev.vensin.lna.server.fire.Cell;
import dev.vensin.lna.server.fire.Fire;
import dev.vensin.lna.server.fire.FireClass;
import dev.vensin.lna.server.fire.FireListener.Mistake;
import dev.vensin.lna.server.fire.FireService;
import dev.vensin.lna.server.item.CustomItem;
import dev.vensin.lna.server.item.Items;
import dev.vensin.lna.server.lang.Messages;
import dev.vensin.lna.server.map.MapDefinition;
import dev.vensin.lna.server.map.MapDefinition.FireSpot;
import dev.vensin.lna.server.map.PlacedMap;
import dev.vensin.lna.server.mission.MissionInfo.Question;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.sound.SoundEvent;

final class FireTrainingSession extends MissionSession {

    private static final double START_INTENSITY = 30;
    private static final int NEXT_STATION_DELAY = 60;
    private static final int NEXT_QUESTION_DELAY = 80;
    private static final int ALLOWED_MISTAKES = 2;
    private static final String LID = "topfdeckel";
    private static final String BLANKET = "loeschdecke";

    private record Spot(int x, int z, FireSpot spot) {}

    private record Station(Cell origin, FireClass fireClass, String title, String hint, String solution) {

        boolean fed() {
            return "lid".equals(solution) || "valve".equals(solution);
        }
    }

    private final PlacedMap placed;
    private final FireService fires;
    private final Items items;
    private final List<Station> stations = new ArrayList<>();
    private final Map<Mistake, Integer> mistakes = new EnumMap<>(Mistake.class);
    private final Set<String> countedMistakes = new HashSet<>();
    private final Map<UUID, Integer> answers = new HashMap<>();
    private final Map<UUID, Integer> quizScore = new HashMap<>();
    private final int duration;
    private int remaining;
    private int index = -1;
    private int question = -1;
    private int completed;
    private Fire current;

    FireTrainingSession(Missions missions, Messages messages, MissionInfo info, Collection<Player> players,
                        MapDefinition map, PlacedMap placed, Instance instance, FireService fires, Items items) {
        super(missions, messages, info, players, BossBar.Overlay.PROGRESS);
        this.placed = placed;
        this.fires = fires;
        this.items = items;
        this.duration = info.duration();
        this.remaining = duration;
        List<Spot> spots = new ArrayList<>();
        for (int z = 0; z < map.depth(); z++) {
            for (int x = 0; x < map.width(); x++) {
                FireSpot spot = map.cell(x, z).fire();
                if (spot != null) {
                    spots.add(new Spot(x, z, spot));
                }
            }
        }
        spots.sort(Comparator.comparingInt(spot -> spot.spot().order()));
        for (Spot spot : spots) {
            FireClass.parse(spot.spot().fireClass()).ifPresent(fireClass -> stations.add(new Station(
                    new Cell(instance, placed.x() + spot.x(), placed.y() + spot.spot().y(), placed.z() + spot.z()),
                    fireClass, spot.spot().title(), spot.spot().hint(), spot.spot().solution())));
        }
    }

    @Override
    protected void onStart() {
        for (Player player : onlinePlayers()) {
            player.teleport(placed.spawn());
            showTitle(player, messages.text(player, "mission.subtitle", Placeholder.unparsed("text", info.description())));
        }
        broadcast("mission.training.intro");
        updateBar();
        missions.later(this::nextStation, NEXT_STATION_DELAY);
    }

    private void nextStation() {
        if (finished() || current != null) {
            return;
        }
        index++;
        if (index >= stations.size()) {
            broadcast("mission.training.all-out");
            if (info.quiz().isEmpty()) {
                finish();
            } else {
                broadcast("mission.quiz.intro");
                missions.later(this::nextQuestion, NEXT_STATION_DELAY);
            }
            return;
        }
        Station station = stations.get(index);
        current = fires.ignite(station.origin(), station.fireClass(), START_INTENSITY, station.fed());
        broadcastEmpty();
        for (Player player : onlinePlayers()) {
            player.sendMessage(messages.text(player, "mission.training.station",
                    Placeholder.unparsed("number", String.valueOf(index + 1)),
                    Placeholder.unparsed("total", String.valueOf(stations.size())),
                    Placeholder.unparsed("title", station.title()),
                    Placeholder.component("class", messages.text(player, station.fireClass().messageKey()))));
            if (station.hint() != null && !station.hint().isBlank()) {
                player.sendMessage(messages.text(player, "mission.training.hint",
                        Placeholder.unparsed("text", station.hint())));
            }
            player.playSound(Sound.sound(SoundEvent.BLOCK_FIRE_AMBIENT, Sound.Source.BLOCK, 1f, 1f));
        }
        updateBar();
    }

    @Override
    protected void tick() {
        remaining--;
        updateBar();
        if (remaining <= 0) {
            broadcast("mission.time-up");
            finish();
        }
    }

    @Override
    protected void onInteract(Player player, Entity hitbox) {
        // Objects that hand out an item are served by the lobby, here as in the hub.
        if (!placed.contains(hitbox.getPosition()) || hitbox.getTag(Tags.GIVE) != null) {
            return;
        }
        Station station = current != null ? stations.get(index) : null;
        if ("valve".equals(hitbox.getTag(Tags.ACTION))) {
            if (station != null && "valve".equals(station.solution())) {
                fires.smother(current, player);
                broadcast("mission.training.valve-right");
            } else {
                player.sendActionBar(messages.text(player, "mission.training.valve-closed"));
            }
            return;
        }
        if (station == null || hitbox.getPosition().blockX() != current.origin().x()
                || hitbox.getPosition().blockZ() != current.origin().z()) {
            return;
        }
        Optional<String> held = items.of(player.getItemInMainHand()).map(CustomItem::id);
        boolean lid = held.filter(LID::equals).isPresent();
        boolean blanket = held.filter(BLANKET::equals).isPresent();
        if (!lid && !blanket) {
            return;
        }
        FireClass fireClass = station.fireClass();
        if ("lid".equals(station.solution()) || fireClass == FireClass.F || (blanket && fireClass == FireClass.A)) {
            fires.smother(current, player);
            broadcast(fireClass == FireClass.F ? "mission.training.lid-right-fat" : "mission.training.smother-right");
        } else {
            player.sendActionBar(messages.text(player,
                    lid ? "mission.training.lid-useless" : "mission.training.blanket-useless"));
        }
    }

    @Override
    protected void onFireExtinguished(Player player, Fire fire) {
        if (fire != current) {
            return;
        }
        current = null;
        completed++;
        broadcast("mission.training.put-out", Placeholder.unparsed("name", player.getUsername()));
        updateBar();
        missions.later(this::nextStation, NEXT_STATION_DELAY);
    }

    @Override
    protected void onFireMistake(Player player, Fire fire, Mistake mistake) {
        if (fire == current && countedMistakes.add(index + ":" + mistake)) {
            mistakes.merge(mistake, 1, Integer::sum);
        }
    }

    private void nextQuestion() {
        if (finished()) {
            return;
        }
        answers.clear();
        question++;
        if (question >= info.quiz().size()) {
            finish();
            return;
        }
        Question q = info.quiz().get(question);
        broadcastEmpty();
        broadcast("mission.quiz.question", Placeholder.unparsed("number", String.valueOf(question + 1)),
                Placeholder.unparsed("total", String.valueOf(info.quiz().size())),
                Placeholder.unparsed("text", q.text()));
        for (int i = 0; i < q.answers().size(); i++) {
            int number = i + 1;
            broadcast("mission.quiz.answer",
                    Placeholder.styling("answer", ClickEvent.runCommand("/mission answer " + number)),
                    Placeholder.unparsed("number", String.valueOf(number)),
                    Placeholder.unparsed("text", q.answers().get(i)));
        }
        broadcast("mission.quiz.click");
    }

    @Override
    protected void onAnswer(Player player, int answer) {
        if (question < 0 || question >= info.quiz().size()) {
            return;
        }
        Question q = info.quiz().get(question);
        if (answer > q.answers().size()) {
            return;
        }
        if (answers.putIfAbsent(player.getUuid(), answer) != null) {
            player.sendActionBar(messages.text(player, "mission.quiz.already"));
            return;
        }
        if (answer == q.correct()) {
            quizScore.merge(player.getUuid(), 1, Integer::sum);
        }
        player.sendActionBar(messages.text(player, "mission.quiz.saved"));
        if (answers.size() >= onlinePlayers().size()) {
            int asked = question;
            broadcast("mission.quiz.solution", Placeholder.unparsed("number", String.valueOf(q.correct())),
                    Placeholder.unparsed("text", q.answers().get(q.correct() - 1)),
                    Placeholder.unparsed("explanation", q.explanation()));
            missions.later(() -> {
                if (question == asked) {
                    nextQuestion();
                }
            }, NEXT_QUESTION_DELAY);
        }
    }

    @Override
    protected void onFinish() {
        if (current != null) {
            fires.remove(current);
            current = null;
        }
        int total = mistakes.values().stream().mapToInt(Integer::intValue).sum();
        boolean firesDone = completed == stations.size() && total <= ALLOWED_MISTAKES;
        int questions = info.quiz().size();
        int neededAnswers = (int) Math.ceil(questions * 2 / 3.0);
        broadcastEmpty();
        broadcast("mission.training.result", Missions.label(info));
        broadcast("mission.training.summary", Placeholder.unparsed("done", String.valueOf(completed)),
                Placeholder.unparsed("total", String.valueOf(stations.size())),
                Placeholder.unparsed("mistakes", String.valueOf(total)),
                Placeholder.unparsed("time", clock(duration - Math.max(0, remaining))));
        for (Player player : onlinePlayers()) {
            mistakes.forEach((mistake, count) -> player.sendMessage(messages.text(player, "mission.training.mistake-line",
                    Placeholder.component("mistake", messages.text(player, "mission.training.mistake."
                            + mistake.name().toLowerCase(Locale.ROOT).replace('_', '-'))),
                    Placeholder.unparsed("count", String.valueOf(count)))));
            clearMissionItems(player);
            int correct = quizScore.getOrDefault(player.getUuid(), 0);
            if (questions > 0) {
                player.sendMessage(messages.text(player, "mission.quiz.result",
                        Placeholder.unparsed("correct", String.valueOf(correct)),
                        Placeholder.unparsed("total", String.valueOf(questions))));
            }
            if (firesDone && (questions == 0 || correct >= neededAnswers)) {
                missions.stampPass(player, info);
                player.sendMessage(messages.text(player, "mission.stamp-earned"));
            } else {
                player.sendMessage(messages.text(player,
                        questions > 0 ? "mission.training.failed-quiz" : "mission.training.failed",
                        Placeholder.unparsed("mistakes", String.valueOf(ALLOWED_MISTAKES)),
                        Placeholder.unparsed("answers", String.valueOf(neededAnswers))));
            }
        }
        missions.returnToHubLater(onlinePlayers(), HUB_RETURN_TICKS);
    }

    private void clearMissionItems(Player player) {
        ItemStack[] stacks = player.getInventory().getItemStacks();
        for (int slot = 0; slot < stacks.length; slot++) {
            if (items.of(stacks[slot]).isPresent()) {
                player.getInventory().setItemStack(slot, ItemStack.AIR);
            }
        }
    }

    private void updateBar() {
        updateBars(remaining, duration, "mission.training.bar",
                Placeholder.unparsed("done", String.valueOf(completed)),
                Placeholder.unparsed("total", String.valueOf(stations.size())));
    }
}
