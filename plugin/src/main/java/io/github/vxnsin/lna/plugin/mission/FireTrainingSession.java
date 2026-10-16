package io.github.vxnsin.lna.plugin.mission;

import io.github.vxnsin.lna.plugin.Keys;
import io.github.vxnsin.lna.plugin.fire.Fire;
import io.github.vxnsin.lna.plugin.fire.FireClass;
import io.github.vxnsin.lna.plugin.fire.FireMistakeEvent.Mistake;
import io.github.vxnsin.lna.plugin.fire.FireService;
import io.github.vxnsin.lna.plugin.item.CustomItem;
import io.github.vxnsin.lna.plugin.item.ItemRegistry;
import io.github.vxnsin.lna.plugin.map.MapDefinition;
import io.github.vxnsin.lna.plugin.map.MapDefinition.FireSpot;
import io.github.vxnsin.lna.plugin.map.PlacedMaps.PlacedMap;
import io.github.vxnsin.lna.plugin.mission.MissionInfo.Question;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * Fire training missions (mission 1 "Das Branddreieck", mission 4 "Fettbrand und Löscher-Training").
 *
 * <p>The fire spots of the map start one after another. Depending on the spot's solution, players use a
 * suitable extinguisher (taken from objects marked with give), smother the fire with the pot lid or fire
 * blanket ("lid"), or close the gas valve ("valve"). Lid and valve fires cannot be sprayed out, because the
 * lesson is to take away one side of the fire triangle. An optional quiz from missions.yml follows.
 */
final class FireTrainingSession extends MissionSession {

    private static final double START_INTENSITY = 30;
    private static final long NEXT_STATION_DELAY = 60L;
    private static final long NEXT_QUESTION_DELAY = 80L;
    private static final int ALLOWED_MISTAKES = 2;

    private record Station(Block origin, FireClass fireClass, String title, String hint, String solution) {

        boolean fed() {
            return "lid".equals(solution) || "valve".equals(solution);
        }
    }

    private final PlacedMap placed;
    private final FireService fires;
    private final ItemRegistry items;
    private final List<Station> stations = new ArrayList<>();
    private final Map<Mistake, Integer> mistakes = new EnumMap<>(Mistake.class);
    private final Set<String> countedMistakes = new HashSet<>();
    private final Map<UUID, Integer> answers = new HashMap<>();
    private final Map<UUID, Integer> quizScore = new HashMap<>();
    private final BossBar bar;
    private final int duration;
    private int remaining;
    private int index = -1;
    private int question = -1;
    private int completed;
    private Fire current;

    FireTrainingSession(MissionService service, MissionInfo info, Collection<Player> players, MapDefinition map,
                        PlacedMap placed, FireService fires, ItemRegistry items) {
        super(service, info, players);
        this.placed = placed;
        this.fires = fires;
        this.items = items;
        this.duration = info.duration();
        this.remaining = duration;
        this.bar = BossBar.bossBar(Component.empty(), 1f, BossBar.Color.RED, BossBar.Overlay.PROGRESS);
        World world = placed.bukkitWorld().orElseThrow();
        record Spot(int x, int z, FireSpot spot) {}
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
                    world.getBlockAt(placed.x() + spot.x(), placed.y() + spot.spot().y(), placed.z() + spot.z()),
                    fireClass, spot.spot().title(), spot.spot().hint(), spot.spot().solution())));
        }
    }

    @Override
    protected void onStart() {
        Title title = Title.title(Component.text(info.label(), NamedTextColor.GOLD),
                Component.text(info.description(), NamedTextColor.WHITE),
                Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(3), Duration.ofMillis(700)));
        for (Player player : onlinePlayers()) {
            placed.spawn().ifPresent(player::teleport);
            player.showBossBar(bar);
            player.showTitle(title);
        }
        broadcast(Component.text("Werkzeuge und Löscher nimmst du per Rechtsklick. Lies die Tipps zu jedem Brand!",
                NamedTextColor.GRAY));
        updateBar();
        service.scheduleLater(this::nextStation, NEXT_STATION_DELAY);
    }

    private void nextStation() {
        if (current != null) {
            return;
        }
        index++;
        if (index >= stations.size()) {
            broadcast(Component.text("Alle Brände gelöscht!", NamedTextColor.GREEN, TextDecoration.BOLD));
            if (info.quiz().isEmpty()) {
                finish();
            } else {
                broadcast(Component.text("Zum Abschluss ein kurzes Quiz:", NamedTextColor.GOLD));
                service.scheduleLater(this::nextQuestion, NEXT_STATION_DELAY);
            }
            return;
        }
        Station station = stations.get(index);
        current = fires.ignite(station.origin(), station.fireClass(), START_INTENSITY, station.fed());
        broadcast(Component.empty());
        broadcast(Component.text("🔥 Station " + (index + 1) + "/" + stations.size() + ": " + station.title(), NamedTextColor.GOLD)
                .append(Component.text(" (" + station.fireClass().display() + ")", NamedTextColor.GRAY)));
        if (!station.hint().isBlank()) {
            broadcast(Component.text("   Tipp: " + station.hint(), NamedTextColor.GRAY));
        }
        onlinePlayers().forEach(player -> player.playSound(player.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 1f, 1f));
        updateBar();
    }

    @Override
    protected void tick() {
        remaining--;
        updateBar();
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
        String give = data.get(Keys.GIVE, PersistentDataType.STRING);
        if (give != null) {
            take(player, give);
            return;
        }
        Station station = current != null ? stations.get(index) : null;
        if ("valve".equals(data.get(Keys.ACTION, PersistentDataType.STRING))) {
            if (station != null && "valve".equals(station.solution())) {
                fires.smother(current, player);
                broadcast(Component.text("   Richtig: Gasventil zu – ohne Brennstoff erlischt die Flamme.", NamedTextColor.GRAY));
            } else {
                player.sendActionBar(Component.text("Das Gasventil ist zu.", NamedTextColor.GRAY));
            }
            return;
        }
        if (station == null || hitbox.getLocation().getBlockX() != current.origin().getX()
                || hitbox.getLocation().getBlockZ() != current.origin().getZ()) {
            return;
        }
        Optional<String> held = items.of(player.getInventory().getItemInMainHand()).map(CustomItem::id);
        boolean lid = held.filter("topfdeckel"::equals).isPresent();
        boolean blanket = held.filter("loeschdecke"::equals).isPresent();
        if (!lid && !blanket) {
            return;
        }
        FireClass fireClass = station.fireClass();
        if ("lid".equals(station.solution()) || fireClass == FireClass.F || (blanket && fireClass == FireClass.A)) {
            fires.smother(current, player);
            broadcast(Component.text(fireClass == FireClass.F
                    ? "   Richtig: Deckel drauf und den Herd ausschalten, dann kann sich das Fett nicht wieder entzünden."
                    : "   Richtig: Ohne Sauerstoff erstickt das Feuer.", NamedTextColor.GRAY));
        } else {
            player.sendActionBar(Component.text((lid ? "Ein Topfdeckel" : "Eine Löschdecke")
                    + " hilft bei diesem Brand nicht.", NamedTextColor.GOLD));
        }
    }

    @Override
    protected void onFireExtinguished(Player player, Fire fire) {
        if (fire != current) {
            return;
        }
        current = null;
        completed++;
        broadcast(Component.text("✔ " + player.getName() + " hat den Brand gelöscht.", NamedTextColor.GREEN));
        updateBar();
        service.scheduleLater(this::nextStation, NEXT_STATION_DELAY);
    }

    @Override
    protected void onFireMistake(Player player, Fire fire, Mistake mistake) {
        if (fire == current && countedMistakes.add(index + ":" + mistake)) {
            mistakes.merge(mistake, 1, Integer::sum);
        }
    }

    private void nextQuestion() {
        answers.clear();
        question++;
        if (question >= info.quiz().size()) {
            finish();
            return;
        }
        Question q = info.quiz().get(question);
        broadcast(Component.empty());
        broadcast(Component.text("❓ Frage " + (question + 1) + "/" + info.quiz().size() + ": " + q.text(), NamedTextColor.GOLD));
        for (int i = 0; i < q.answers().size(); i++) {
            int number = i + 1;
            broadcast(Component.text("   [" + number + "] " + q.answers().get(i), NamedTextColor.YELLOW)
                    .clickEvent(ClickEvent.runCommand("/mission antwort " + number)));
        }
        broadcast(Component.text("   Antwort anklicken.", NamedTextColor.DARK_GRAY));
    }

    @Override
    protected void onAnswer(Player player, int answer) {
        if (question < 0 || question >= info.quiz().size()) {
            return;
        }
        if (answers.putIfAbsent(player.getUniqueId(), answer) != null) {
            player.sendActionBar(Component.text("Du hast schon geantwortet.", NamedTextColor.GRAY));
            return;
        }
        Question q = info.quiz().get(question);
        if (answer == q.correct()) {
            quizScore.merge(player.getUniqueId(), 1, Integer::sum);
        }
        player.sendActionBar(Component.text("Antwort gespeichert.", NamedTextColor.GRAY));
        if (answers.size() >= onlinePlayers().size()) {
            int asked = question;
            broadcast(Component.text("   Richtig ist [" + q.correct() + "] " + q.answers().get(q.correct() - 1)
                    + ". " + q.explanation(), NamedTextColor.GREEN));
            service.scheduleLater(() -> {
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
        int seconds = duration - Math.max(0, remaining);
        broadcast(Component.empty());
        broadcast(Component.text(info.label() + " – Ergebnis", NamedTextColor.GOLD, TextDecoration.BOLD));
        broadcast(Component.text(completed + " von " + stations.size() + " Bränden gelöscht, " + total
                + " Fehler, Zeit " + seconds / 60 + ":" + String.format("%02d", seconds % 60), NamedTextColor.WHITE));
        mistakes.forEach((mistake, count) -> broadcast(Component.text("• " + describe(mistake) + ": " + count + "×",
                NamedTextColor.RED)));
        for (Player player : onlinePlayers()) {
            player.hideBossBar(bar);
            clearMissionItems(player);
            int score = quizScore.getOrDefault(player.getUniqueId(), 0);
            if (questions > 0) {
                player.sendMessage(Component.text("Dein Quiz: " + score + " von " + questions + " richtig.", NamedTextColor.WHITE));
            }
            boolean passed = firesDone && (questions == 0 || score >= neededAnswers);
            if (passed) {
                service.stampPass(player, info);
            }
            player.sendMessage(passed
                    ? Component.text("Stempel für den Brandschutz-Pass erhalten! Zurück zum Hub in 10 Sekunden.", NamedTextColor.GREEN)
                    : Component.text("Für den Stempel: alle Brände mit höchstens " + ALLOWED_MISTAKES + " Fehlern löschen"
                            + (questions > 0 ? " und mindestens " + neededAnswers + " Quizfragen richtig" : "")
                            + ". Zurück zum Hub in 10 Sekunden.", NamedTextColor.GOLD));
        }
        service.returnToHubLater(onlinePlayers(), 200L);
    }

    private void take(Player player, String itemId) {
        Optional<CustomItem> item = items.get(itemId);
        if (item.isEmpty()) {
            return;
        }
        for (ItemStack stack : player.getInventory().getContents()) {
            if (items.of(stack).map(CustomItem::id).filter(itemId::equals).isPresent()) {
                player.sendActionBar(Component.text("Den hast du schon.", NamedTextColor.GRAY));
                return;
            }
        }
        player.getInventory().addItem(items.create(item.get()));
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 0.8f, 1.2f);
        player.sendActionBar(Component.text("Genommen: ", NamedTextColor.GRAY)
                .append(Component.text(itemId.replace('_', ' '), NamedTextColor.WHITE)));
    }

    private void clearMissionItems(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack stack = contents[slot];
            if (items.isCustom(stack) && !stack.getItemMeta().getPersistentDataContainer()
                    .has(Keys.LOBBY_ITEM, PersistentDataType.STRING)) {
                player.getInventory().setItem(slot, null);
            }
        }
    }

    private void updateBar() {
        bar.progress(Math.clamp((float) remaining / duration, 0f, 1f));
        bar.name(Component.text("Brände gelöscht: " + completed + "/" + stations.size() + "  ·  "
                + Math.max(0, remaining) / 60 + ":" + String.format("%02d", Math.max(0, remaining) % 60), NamedTextColor.WHITE));
    }

    private static String describe(Mistake mistake) {
        return switch (mistake) {
            case WRONG_CLASS -> "Ungeeigneter Löscher";
            case WATER_ON_FAT -> "Wasser auf brennendes Fett";
            case WATER_ON_ELECTRIC -> "Wasser auf eine elektrische Anlage";
        };
    }
}
