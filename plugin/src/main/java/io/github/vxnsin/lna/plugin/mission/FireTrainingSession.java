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
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
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
 * Mission 4 "Fettbrand und Löscher-Training": the fires of the map start one after another (fire spots in
 * map order). Players take extinguishers from the extinguisher wall and must pick the right one for each
 * fire class; a grease fire can also be smothered with the pot lid. Mistakes are counted per station.
 */
final class FireTrainingSession extends MissionSession {

    private static final double START_INTENSITY = 30;
    private static final long NEXT_STATION_DELAY = 60L;
    private static final int ALLOWED_MISTAKES = 2;

    private record Station(Block origin, FireClass fireClass, String title, String hint) {}

    private final PlacedMap placed;
    private final FireService fires;
    private final ItemRegistry items;
    private final List<Station> stations = new ArrayList<>();
    private final Map<Mistake, Integer> mistakes = new EnumMap<>(Mistake.class);
    private final Set<String> countedMistakes = new HashSet<>();
    private final BossBar bar;
    private final int duration;
    private int remaining;
    private int index = -1;
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
                    fireClass, spot.spot().title(), spot.spot().hint())));
        }
    }

    @Override
    protected void onStart() {
        Title title = Title.title(Component.text(info.label(), NamedTextColor.GOLD),
                Component.text(stations.size() + " Brände, der richtige Löscher entscheidet", NamedTextColor.WHITE),
                Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(3), Duration.ofMillis(700)));
        for (Player player : onlinePlayers()) {
            placed.spawn().ifPresent(player::teleport);
            player.showBossBar(bar);
            player.showTitle(title);
        }
        broadcast(Component.text("Nimm dir an der Löscherwand einen Löscher (Rechtsklick). Welcher passt zu welchem "
                + "Brand? Auf dem Etikett stehen die Brandklassen.", NamedTextColor.GRAY));
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
            finish();
            return;
        }
        Station station = stations.get(index);
        current = fires.ignite(station.origin(), station.fireClass(), START_INTENSITY);
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
        String give = hitbox.getPersistentDataContainer().get(Keys.GIVE, PersistentDataType.STRING);
        if (give != null) {
            take(player, give);
            return;
        }
        if (current == null || hitbox.getLocation().getBlockX() != current.origin().getX()
                || hitbox.getLocation().getBlockZ() != current.origin().getZ()) {
            return;
        }
        Optional<String> held = items.of(player.getInventory().getItemInMainHand()).map(CustomItem::id);
        boolean lid = held.filter("topfdeckel"::equals).isPresent();
        boolean blanket = held.filter("loeschdecke"::equals).isPresent();
        if (!lid && !blanket) {
            return;
        }
        FireClass fireClass = current.fireClass();
        if (fireClass == FireClass.F || (blanket && fireClass == FireClass.A)) {
            Fire fire = current;
            fires.smother(fire, player);
            if (fireClass == FireClass.F) {
                broadcast(Component.text("   Richtig: Deckel drauf und den Herd ausschalten, dann kann sich das Fett "
                        + "nicht wieder entzünden.", NamedTextColor.GRAY));
            }
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

    @Override
    protected void onFinish() {
        if (current != null) {
            fires.remove(current);
            current = null;
        }
        int total = mistakes.values().stream().mapToInt(Integer::intValue).sum();
        boolean passed = completed == stations.size() && total <= ALLOWED_MISTAKES;
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
            if (passed) {
                service.stampPass(player, info);
            }
        }
        broadcast(passed
                ? Component.text("Stempel für den Brandschutz-Pass erhalten! Zurück zum Hub in 10 Sekunden.", NamedTextColor.GREEN)
                : Component.text("Für den Stempel: alle Brände löschen mit höchstens " + ALLOWED_MISTAKES
                        + " Fehlern. Zurück zum Hub in 10 Sekunden.", NamedTextColor.GOLD));
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
