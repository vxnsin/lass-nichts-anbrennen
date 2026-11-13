package dev.vensin.lna.server.fire;

import dev.vensin.lna.server.fire.FireListener.Mistake;
import dev.vensin.lna.server.lang.Messages;
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
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.title.Title;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.damage.DamageType;
import net.minestom.server.instance.block.Block;
import net.minestom.server.particle.Particle;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.TaskSchedule;

public final class FireService {

    private static final int TICK_PERIOD = 10;
    /** Intensity gained per cycle (every 0.5 s). */
    private static final double GROWTH = 1.5;
    private static final double UNSUITABLE_FACTOR = 0.1;
    private static final int SMOLDER_CYCLES = 10;
    private static final double REIGNITE_INTENSITY = 25;
    private static final double FLARE_INTENSITY = 35;
    private static final long FLARE_COOLDOWN_MS = 2500;
    private static final long HINT_COOLDOWN_MS = 4000;
    private static final Title.Times TITLE_TIMES =
            Title.Times.times(Duration.ofMillis(100), Duration.ofSeconds(2), Duration.ofMillis(500));

    private final Messages messages;
    private final Map<UUID, Fire> fires = new LinkedHashMap<>();
    private final Set<Cell> managedFlames = new HashSet<>();
    private final Map<String, Long> lastHint = new HashMap<>();
    private final List<FireListener> listeners = new ArrayList<>();

    public FireService(Messages messages) {
        this.messages = messages;
    }

    public void start() {
        MinecraftServer.getSchedulerManager().buildTask(this::tick).repeat(TaskSchedule.tick(TICK_PERIOD)).schedule();
    }

    public void listen(FireListener listener) {
        listeners.add(listener);
    }

    public Fire ignite(Cell origin, FireClass fireClass, double intensity) {
        return ignite(origin, fireClass, intensity, false);
    }

    public Fire ignite(Cell origin, FireClass fireClass, double intensity, boolean fed) {
        Fire fire = new Fire(origin, fireClass, intensity, fed);
        fires.put(fire.id(), fire);
        updateFlames(fire);
        return fire;
    }

    public Collection<Fire> fires() {
        return List.copyOf(fires.values());
    }

    public boolean isManagedFlame(Cell cell) {
        return managedFlames.contains(cell);
    }

    public void clear() {
        new ArrayList<>(fires.values()).forEach(this::remove);
    }

    public void remove(Fire fire) {
        fire.flames().forEach(this::removeFlame);
        fire.flames(new HashSet<>());
        fires.remove(fire.id());
    }

    public void smother(Fire fire, Player player) {
        extinguish(fire, player, "fire.smothered");
    }

    public boolean spray(Player player, Pos eye, Vec direction, double range, Agent agent, Set<FireClass> suitable,
                         double power) {
        boolean hit = false;
        for (Fire fire : fires()) {
            if (fire.origin().instance() != player.getInstance() || !hitByCone(fire, eye, direction, range)) {
                continue;
            }
            hit = true;
            if (fire.fed()) {
                if (hint(player, "fed")) {
                    player.sendActionBar(messages.text(player, "fire.fed"));
                }
            } else if (agent == Agent.WATER && fire.fireClass() == FireClass.F) {
                flare(fire, player);
            } else if (agent == Agent.WATER && fire.fireClass() == FireClass.ELECTRIC) {
                shock(fire, player);
            } else if (!suitable.contains(fire.fireClass())) {
                fire.intensity(fire.intensity() - power * UNSUITABLE_FACTOR);
                if (hint(player, "wrong_class")) {
                    player.sendActionBar(messages.text(player, "fire.wrong-class", Placeholder.component("class",
                            messages.text(player, fire.fireClass().messageKey()))));
                    listeners.forEach(listener -> listener.mistake(player, fire, Mistake.WRONG_CLASS));
                }
            } else if (fire.state() == Fire.State.SMOLDERING) {
                extinguish(fire, player, "fire.extinguished");
            } else {
                fire.intensity(fire.intensity() - power);
                if (fire.intensity() <= 0) {
                    if (fire.fireClass() == FireClass.A) {
                        smolder(fire, player);
                    } else {
                        extinguish(fire, player, "fire.extinguished");
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
        Set<Cell> desired = new HashSet<>();
        if (fire.state() == Fire.State.BURNING) {
            double radius = fire.radius();
            int reach = (int) Math.ceil(radius);
            for (int dx = -reach; dx <= reach; dx++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    if (dx * dx + dz * dz > radius * radius + 0.01 && (dx != 0 || dz != 0)) {
                        continue;
                    }
                    Cell cell = fire.origin().offset(dx, 0, dz);
                    if (canBurn(cell)) {
                        desired.add(cell);
                    }
                }
            }
        }
        for (Cell cell : fire.flames()) {
            if (!desired.contains(cell)) {
                removeFlame(cell);
            }
        }
        for (Cell cell : desired) {
            if (!fire.flames().contains(cell)) {
                cell.instance().setBlock(cell.x(), cell.y(), cell.z(), Block.FIRE);
                managedFlames.add(cell);
            }
        }
        fire.flames(desired);
    }

    private static boolean canBurn(Cell cell) {
        Block block = cell.instance().getBlock(cell.x(), cell.y(), cell.z());
        Block below = cell.instance().getBlock(cell.x(), cell.y() - 1, cell.z());
        return (block.air() || block.compare(Block.FIRE)) && below.solid();
    }

    private void removeFlame(Cell cell) {
        if (cell.instance().getBlock(cell.x(), cell.y(), cell.z()).compare(Block.FIRE)) {
            cell.instance().setBlock(cell.x(), cell.y(), cell.z(), Block.AIR);
        }
        managedFlames.remove(cell);
    }

    private static boolean hitByCone(Fire fire, Pos eye, Vec direction, double range) {
        List<Cell> targets = new ArrayList<>(fire.flames());
        targets.add(fire.origin());
        for (Cell cell : targets) {
            Vec toTarget = new Vec(cell.x() + 0.5 - eye.x(), cell.y() + 0.4 - eye.y(), cell.z() + 0.5 - eye.z());
            double along = toTarget.dot(direction);
            if (along < 0 || along > range + 0.5) {
                continue;
            }
            double lateral = toTarget.sub(direction.mul(along)).length();
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
        player.sendActionBar(messages.text(player, "fire.smoldering"));
    }

    private void reignite(Fire fire) {
        fire.state(Fire.State.BURNING);
        fire.intensity(REIGNITE_INTENSITY);
        updateFlames(fire);
        Pos center = fire.origin().center(0);
        for (Player player : fire.origin().instance().getPlayers()) {
            if (player.getPosition().distanceSquared(center) <= 16 * 16) {
                player.sendActionBar(messages.text(player, "fire.reignited"));
            }
        }
    }

    private void extinguish(Fire fire, Player player, String messageKey) {
        Pos center = fire.origin().center(0.3);
        remove(fire);
        Effects.particles(fire.origin().instance(), Particle.LARGE_SMOKE, center, 0.5, 0.3, 0.5, 0.02, 20);
        Effects.sound(fire.origin().instance(), SoundEvent.BLOCK_FIRE_EXTINGUISH, center, 1f, 0.9f);
        player.sendActionBar(messages.text(player, messageKey));
        listeners.forEach(listener -> listener.extinguished(player, fire));
    }

    private void flare(Fire fire, Player player) {
        long now = System.currentTimeMillis();
        if (now - fire.lastFlare() < FLARE_COOLDOWN_MS) {
            return;
        }
        fire.lastFlare(now);
        fire.intensity(fire.intensity() + FLARE_INTENSITY);
        updateFlames(fire);
        Pos center = fire.origin().center(0.5);
        Effects.particles(fire.origin().instance(), Particle.FLAME, center, 0.8, 1.2, 0.8, 0.12, 80);
        Effects.particles(fire.origin().instance(), Particle.EXPLOSION, center, 0.3, 0.3, 0.3, 0, 2);
        Effects.sound(fire.origin().instance(), SoundEvent.ENTITY_GENERIC_EXPLODE, center, 0.7f, 1.5f);
        Vec away = new Vec(player.getPosition().x() - center.x(), 0, player.getPosition().z() - center.z());
        // Minestom velocities are blocks per second, not per tick.
        player.setVelocity(away.lengthSquared() > 0 ? away.normalize().mul(16).withY(7) : new Vec(0, 8, 0));
        player.setFireTicks(40);
        player.showTitle(Title.title(messages.text(player, "fire.flare.title"),
                messages.text(player, "fire.flare.subtitle"), TITLE_TIMES));
        player.sendMessage(messages.text(player, "fire.flare.lesson"));
        listeners.forEach(listener -> listener.mistake(player, fire, Mistake.WATER_ON_FAT));
    }

    private void shock(Fire fire, Player player) {
        if (!hint(player, "shock")) {
            return;
        }
        player.damage(DamageType.LIGHTNING_BOLT, 4f);
        player.showTitle(Title.title(messages.text(player, "fire.shock.title"),
                messages.text(player, "fire.shock.subtitle"), TITLE_TIMES));
        player.sendMessage(messages.text(player, "fire.shock.lesson"));
        listeners.forEach(listener -> listener.mistake(player, fire, Mistake.WATER_ON_ELECTRIC));
    }

    private void burnEffects(Fire fire) {
        Pos top = fire.origin().center(0.9);
        double spread = 0.3 + fire.radius() * 0.4;
        Effects.particles(fire.origin().instance(), Particle.CAMPFIRE_COSY_SMOKE, top, spread, 0.1, spread, 0.01,
                1 + (int) (fire.intensity() / 25));
        if (fire.intensity() > 60) {
            Effects.particles(fire.origin().instance(), Particle.LARGE_SMOKE, top.add(0, 0.5, 0), spread, 0.3, spread, 0.02, 4);
        }
        if (ThreadLocalRandom.current().nextInt(6) == 0) {
            Effects.sound(fire.origin().instance(), SoundEvent.BLOCK_FIRE_AMBIENT, top,
                    0.8f + (float) fire.intensity() / 200f, 1f);
        }
    }

    private void smolderEffects(Fire fire) {
        Pos center = fire.origin().center(0.15);
        Effects.particles(fire.origin().instance(), Particle.SMOKE, center, 0.3, 0.05, 0.3, 0.01, 6);
        Effects.particles(fire.origin().instance(), Particle.SMALL_FLAME, center, 0.25, 0.02, 0.25, 0, 1);
    }

    private boolean hint(Player player, String type) {
        long now = System.currentTimeMillis();
        String key = player.getUuid() + ":" + type;
        if (now - lastHint.getOrDefault(key, 0L) < HINT_COOLDOWN_MS) {
            return false;
        }
        lastHint.put(key, now);
        return true;
    }
}
