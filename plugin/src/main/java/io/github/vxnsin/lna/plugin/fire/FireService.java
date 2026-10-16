package io.github.vxnsin.lna.plugin.fire;

import io.github.vxnsin.lna.plugin.fire.FireMistakeEvent.Mistake;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

/**
 * Simulates fires with fire classes. Each fire grows over time and is shown as static vanilla fire blocks
 * (vanilla fire spread is disabled by WorldRules) plus smoke. Extinguishers report their spray here:
 * suitable agents reduce the intensity, unsuitable ones barely work, and water on fat or on electrical
 * fires is punished the way it would be in reality.
 */
public final class FireService {

    private static final long TICK_PERIOD = 10L;
    /** Intensity gained per cycle (every 0.5 s). */
    private static final double GROWTH = 1.5;
    /** Fraction of the spray power that still works when the agent is unsuitable. */
    private static final double UNSUITABLE_FACTOR = 0.1;
    private static final int SMOLDER_CYCLES = 10;
    private static final double REIGNITE_INTENSITY = 25;
    private static final double FLARE_INTENSITY = 35;
    private static final long FLARE_COOLDOWN_MS = 2500;
    private static final long HINT_COOLDOWN_MS = 4000;

    private final Map<UUID, Fire> fires = new LinkedHashMap<>();
    private final Set<Block> managedFlames = new HashSet<>();
    private final Map<String, Long> lastHint = new HashMap<>();

    public void start(Plugin plugin) {
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, TICK_PERIOD, TICK_PERIOD);
    }

    /** Starts a fire in the given cell (usually the air block above the burning object). */
    public Fire ignite(Block origin, FireClass fireClass, double intensity) {
        Fire fire = new Fire(origin, fireClass, intensity);
        fires.put(fire.id(), fire);
        updateFlames(fire);
        return fire;
    }

    public Collection<Fire> fires() {
        return List.copyOf(fires.values());
    }

    public boolean isManagedFlame(Block block) {
        return managedFlames.contains(block);
    }

    /** Removes all fires and their flame blocks (e.g. on plugin shutdown or mission reset). */
    public void clear() {
        new ArrayList<>(fires.values()).forEach(this::remove);
    }

    public void remove(Fire fire) {
        fire.flames().forEach(this::removeFlame);
        fire.flames(new HashSet<>());
        fires.remove(fire.id());
    }

    /**
     * Applies one spray burst. Returns true if any fire was hit.
     *
     * @param suitable fire classes the extinguisher is approved for
     * @param power    intensity removed per burst on a suitable fire
     */
    public boolean spray(Player player, Location eye, Vector direction, double range, Agent agent,
                         Set<FireClass> suitable, double power) {
        boolean hit = false;
        for (Fire fire : fires()) {
            if (!hitByCone(fire, eye, direction, range)) {
                continue;
            }
            hit = true;
            if (agent == Agent.WATER && fire.fireClass() == FireClass.F) {
                flare(fire, player);
            } else if (agent == Agent.WATER && fire.fireClass() == FireClass.ELECTRIC) {
                shock(fire, player);
            } else if (!suitable.contains(fire.fireClass())) {
                fire.intensity(fire.intensity() - power * UNSUITABLE_FACTOR);
                if (hint(player, "wrong_class")) {
                    player.sendActionBar(Component.text("Wirkt kaum: Dieser Löscher ist nicht geeignet für "
                            + fire.fireClass().display() + ".", NamedTextColor.GOLD));
                    Bukkit.getPluginManager().callEvent(new FireMistakeEvent(player, fire, Mistake.WRONG_CLASS));
                }
            } else if (fire.state() == Fire.State.SMOLDERING) {
                extinguish(fire, player);
            } else {
                fire.intensity(fire.intensity() - power);
                if (fire.intensity() <= 0) {
                    if (fire.fireClass() == FireClass.A) {
                        smolder(fire, player);
                    } else {
                        extinguish(fire, player);
                    }
                }
            }
        }
        return hit;
    }

    private void tick() {
        for (Fire fire : fires()) {
            if (fire.state() == Fire.State.BURNING) {
                fire.intensity(fire.intensity() + GROWTH);
                updateFlames(fire);
                burnEffects(fire);
            } else {
                smolderEffects(fire);
                fire.smolderCycles(fire.smolderCycles() - 1);
                if (fire.smolderCycles() <= 0) {
                    reignite(fire);
                }
            }
        }
    }

    private void updateFlames(Fire fire) {
        Set<Block> desired = new HashSet<>();
        if (fire.state() == Fire.State.BURNING) {
            double radius = fire.radius();
            int reach = (int) Math.ceil(radius);
            for (int dx = -reach; dx <= reach; dx++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    if (dx * dx + dz * dz > radius * radius + 0.01 && (dx != 0 || dz != 0)) {
                        continue;
                    }
                    Block cell = fire.origin().getRelative(dx, 0, dz);
                    if (canBurn(cell)) {
                        desired.add(cell);
                    }
                }
            }
        }
        for (Block block : fire.flames()) {
            if (!desired.contains(block)) {
                removeFlame(block);
            }
        }
        for (Block block : desired) {
            if (!fire.flames().contains(block)) {
                block.setType(Material.FIRE, false);
                managedFlames.add(block);
            }
        }
        fire.flames(desired);
    }

    private static boolean canBurn(Block cell) {
        Material type = cell.getType();
        return (type.isAir() || type == Material.FIRE) && cell.getRelative(BlockFace.DOWN).getType().isSolid();
    }

    private void removeFlame(Block block) {
        if (block.getType() == Material.FIRE) {
            block.setType(Material.AIR, false);
        }
        managedFlames.remove(block);
    }

    private static boolean hitByCone(Fire fire, Location eye, Vector direction, double range) {
        List<Block> targets = new ArrayList<>(fire.flames());
        targets.add(fire.origin());
        Vector start = eye.toVector();
        for (Block block : targets) {
            Vector toTarget = block.getLocation().add(0.5, 0.4, 0.5).toVector().subtract(start);
            double along = toTarget.dot(direction);
            if (along < 0 || along > range + 0.5) {
                continue;
            }
            double lateral = toTarget.clone().subtract(direction.clone().multiply(along)).length();
            if (lateral <= 0.7 + along * 0.2) {
                return true;
            }
        }
        return false;
    }

    private void smolder(Fire fire, Player player) {
        fire.state(Fire.State.SMOLDERING);
        fire.smolderCycles(SMOLDER_CYCLES);
        updateFlames(fire);
        player.sendActionBar(Component.text("Flammen aus – Glutnester nachlöschen!", NamedTextColor.YELLOW));
    }

    private void reignite(Fire fire) {
        fire.state(Fire.State.BURNING);
        fire.intensity(REIGNITE_INTENSITY);
        updateFlames(fire);
        Component warning = Component.text("Wiederentzündung! Nach dem Löschen auf Glutnester achten.", NamedTextColor.RED);
        nearbyPlayers(fire, 16).forEach(player -> player.sendActionBar(warning));
    }

    private void extinguish(Fire fire, Player player) {
        Location center = fire.origin().getLocation().add(0.5, 0.3, 0.5);
        remove(fire);
        center.getWorld().spawnParticle(Particle.LARGE_SMOKE, center, 20, 0.5, 0.3, 0.5, 0.02);
        center.getWorld().playSound(center, Sound.BLOCK_FIRE_EXTINGUISH, 1f, 0.9f);
        player.sendActionBar(Component.text("Brand gelöscht!", NamedTextColor.GREEN));
        Bukkit.getPluginManager().callEvent(new FireExtinguishedEvent(player, fire));
    }

    /** Water on burning fat evaporates explosively and throws burning fat around. */
    private void flare(Fire fire, Player player) {
        long now = System.currentTimeMillis();
        if (now - fire.lastFlare() < FLARE_COOLDOWN_MS) {
            return;
        }
        fire.lastFlare(now);
        fire.intensity(fire.intensity() + FLARE_INTENSITY);
        updateFlames(fire);
        Location center = fire.origin().getLocation().add(0.5, 0.5, 0.5);
        World world = center.getWorld();
        world.spawnParticle(Particle.FLAME, center, 80, 0.8, 1.2, 0.8, 0.12);
        world.spawnParticle(Particle.EXPLOSION, center, 2, 0.3, 0.3, 0.3, 0);
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.5f);
        Vector away = player.getLocation().toVector().subtract(center.toVector()).setY(0);
        player.setVelocity(away.lengthSquared() > 0 ? away.normalize().multiply(0.8).setY(0.35) : new Vector(0, 0.4, 0));
        player.setFireTicks(40);
        player.showTitle(Title.title(Component.text("Fettexplosion!", NamedTextColor.RED),
                Component.text("Wasser verdampft schlagartig im heißen Fett.", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(100), Duration.ofSeconds(2), Duration.ofMillis(500))));
        player.sendMessage(Component.text("Fettbrände nie mit Wasser löschen! Herd aus, Deckel drauf oder "
                + "Fettbrandlöscher (Brandklasse F) benutzen.", NamedTextColor.GOLD));
        Bukkit.getPluginManager().callEvent(new FireMistakeEvent(player, fire, Mistake.WATER_ON_FAT));
    }

    /** Water conducts electricity: the player takes damage and the fire is not affected. */
    private void shock(Fire fire, Player player) {
        if (!hint(player, "shock")) {
            return;
        }
        player.damage(4.0);
        player.showTitle(Title.title(Component.text("Stromschlag!", NamedTextColor.RED),
                Component.text("Wasser leitet Strom.", NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(100), Duration.ofSeconds(2), Duration.ofMillis(500))));
        player.sendMessage(Component.text("Brände an elektrischen Anlagen: Strom abschalten und einen "
                + "CO₂-Löscher benutzen, Abstand halten.", NamedTextColor.GOLD));
        Bukkit.getPluginManager().callEvent(new FireMistakeEvent(player, fire, Mistake.WATER_ON_ELECTRIC));
    }

    private void burnEffects(Fire fire) {
        Location top = fire.origin().getLocation().add(0.5, 0.9, 0.5);
        World world = top.getWorld();
        double spread = 0.3 + fire.radius() * 0.4;
        world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, top, 1 + (int) (fire.intensity() / 25), spread, 0.1, spread, 0.01);
        if (fire.intensity() > 60) {
            world.spawnParticle(Particle.LARGE_SMOKE, top.clone().add(0, 0.5, 0), 4, spread, 0.3, spread, 0.02);
        }
        if (ThreadLocalRandom.current().nextInt(6) == 0) {
            world.playSound(top, Sound.BLOCK_FIRE_AMBIENT, 0.8f + (float) fire.intensity() / 200f, 1f);
        }
    }

    private void smolderEffects(Fire fire) {
        Location center = fire.origin().getLocation().add(0.5, 0.15, 0.5);
        center.getWorld().spawnParticle(Particle.SMOKE, center, 6, 0.3, 0.05, 0.3, 0.01);
        center.getWorld().spawnParticle(Particle.SMALL_FLAME, center, 1, 0.25, 0.02, 0.25, 0);
    }

    private boolean hint(Player player, String type) {
        long now = System.currentTimeMillis();
        String key = player.getUniqueId() + ":" + type;
        if (now - lastHint.getOrDefault(key, 0L) < HINT_COOLDOWN_MS) {
            return false;
        }
        lastHint.put(key, now);
        return true;
    }

    private static List<Player> nearbyPlayers(Fire fire, double radius) {
        Location center = fire.origin().getLocation();
        return center.getWorld().getPlayers().stream()
                .filter(player -> player.getLocation().distanceSquared(center) <= radius * radius)
                .toList();
    }
}
