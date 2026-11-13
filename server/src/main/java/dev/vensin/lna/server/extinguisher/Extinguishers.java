package dev.vensin.lna.server.extinguisher;

import com.google.gson.JsonArray;
import dev.vensin.lna.server.Tags;
import dev.vensin.lna.server.fire.Agent;
import dev.vensin.lna.server.fire.Effects;
import dev.vensin.lna.server.fire.FireClass;
import dev.vensin.lna.server.fire.FireService;
import dev.vensin.lna.server.hud.Glyphs;
import dev.vensin.lna.server.hud.Hud;
import dev.vensin.lna.server.item.CustomItem;
import dev.vensin.lna.server.item.Items;
import dev.vensin.lna.server.lang.Messages;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.item.PlayerFinishItemUseEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemAnimation;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.component.Consumable;
import net.minestom.server.item.component.UseEffects;
import net.minestom.server.particle.Particle;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.TaskSchedule;

/** Consumables that never complete: right click keeps the vanilla using state and the client swaps use_models itself. */
public final class Extinguishers {

    public static final String TAG = "extinguisher";
    private static final String HUD_LAYER = "extinguisher";
    private static final String DEFAULT_SOUND = "lna:feuerloescher.spray";
    private static final float USE_SECONDS = 3600f;
    /** Spraying starts once the aim model is shown (matches the use_models threshold in items.yml). */
    private static final int SPRAY_START_TICKS = 4;
    private static final int SPRAY_INTERVAL_TICKS = 3;
    /** Slightly shorter than the spray sample, so consecutive sounds overlap without gaps. */
    private static final int SOUND_INTERVAL_TICKS = 12;
    private static final int HUD_SEGMENTS = 10;

    private final Items items;
    private final Glyphs glyphs;
    private final Hud hud;
    private final FireService fires;
    private final Messages messages;
    private final Map<UUID, Long> lastSound = new HashMap<>();
    private long tick;

    public Extinguishers(Items items, Glyphs glyphs, Hud hud, FireService fires, Messages messages) {
        this.items = items;
        this.glyphs = glyphs;
        this.hud = hud;
        this.fires = fires;
        this.messages = messages;
        items.onCreate(TAG, (stack, item) -> withFill(usable(stack), item, capacity(item)));
    }

    public void register(GlobalEventHandler events) {
        events.addListener(PlayerDisconnectEvent.class, event -> lastSound.remove(event.getPlayer().getUuid()));
        events.addListener(PlayerFinishItemUseEvent.class, event -> {
            if (extinguisher(event.getItemStack()).isPresent()) {
                event.getPlayer().clearItemUse();
            }
        });
        MinecraftServer.getSchedulerManager().buildTask(this::tickSpraying).repeat(TaskSchedule.tick(1)).schedule();
        MinecraftServer.getSchedulerManager().buildTask(this::updateHud).repeat(TaskSchedule.tick(5)).schedule();
    }

    public static int capacity(CustomItem item) {
        return Math.max(1, item.intProperty("capacity", 100));
    }

    public int fill(ItemStack stack) {
        Integer fill = stack.getTag(Tags.FILL);
        return fill == null ? 0 : fill;
    }

    /** The durability bar mirrors the fill level (max damage = capacity + 1, so it never breaks). */
    public ItemStack withFill(ItemStack stack, CustomItem item, int fill) {
        int capacity = capacity(item);
        int clamped = Math.clamp(fill, 0, capacity);
        return stack.withTag(Tags.FILL, clamped)
                .with(DataComponents.MAX_DAMAGE, capacity + 1)
                .with(DataComponents.DAMAGE, capacity - clamped);
    }

    public boolean refill(Player player) {
        ItemStack stack = player.getEquipment(EquipmentSlot.MAIN_HAND);
        Optional<CustomItem> item = extinguisher(stack);
        item.ifPresent(found -> player.setEquipment(EquipmentSlot.MAIN_HAND, withFill(stack, found, capacity(found))));
        return item.isPresent();
    }

    private static ItemStack usable(ItemStack stack) {
        return stack
                .with(DataComponents.CONSUMABLE, new Consumable(USE_SECONDS, ItemAnimation.NONE,
                        SoundEvent.of("lna:silent", null), false, List.of()))
                .with(DataComponents.USE_EFFECTS, new UseEffects(true, false, 1.0f));
    }

    private Optional<CustomItem> extinguisher(ItemStack stack) {
        return items.of(stack).filter(item -> item.hasTag(TAG));
    }

    private void tickSpraying() {
        tick++;
        for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            if (!player.isUsingItem()) {
                continue;
            }
            long used = player.getCurrentItemUseTime();
            if (used < SPRAY_START_TICKS || (used - SPRAY_START_TICKS) % SPRAY_INTERVAL_TICKS != 0) {
                continue;
            }
            EquipmentSlot slot = player.getItemUseHand() == PlayerHand.OFF ? EquipmentSlot.OFF_HAND : EquipmentSlot.MAIN_HAND;
            ItemStack stack = player.getEquipment(slot);
            extinguisher(stack).ifPresent(item -> spray(player, slot, stack, item, used == SPRAY_START_TICKS));
        }
    }

    private void spray(Player player, EquipmentSlot slot, ItemStack stack, CustomItem item, boolean firstBurst) {
        int fill = fill(stack);
        if (fill <= 0) {
            if (firstBurst) {
                player.sendActionBar(messages.text(player, "extinguisher.empty"));
                player.playSound(Sound.sound(SoundEvent.BLOCK_DISPENSER_FAIL, Sound.Source.PLAYER, 0.6f, 1.2f));
            }
            return;
        }
        player.setEquipment(slot, withFill(stack, item, fill - 1));
        Pos eye = player.getPosition().add(0, player.getEyeHeight(), 0);
        Vec direction = eye.direction();
        double range = item.doubleProperty("range", 6.0);
        Agent agent = Agent.parse(item.stringProperty("agent", "powder"));
        emitParticles(player.getInstance(), eye, direction, range, agent);
        fires.spray(player, eye, direction, range, agent, suitableClasses(item), item.doubleProperty("power", 4.0));
        if (tick - lastSound.getOrDefault(player.getUuid(), Long.MIN_VALUE / 2) >= SOUND_INTERVAL_TICKS) {
            lastSound.put(player.getUuid(), tick);
            float pitch = 0.95f + ThreadLocalRandom.current().nextFloat() * 0.1f;
            player.getInstance().playSound(Sound.sound(Key.key(item.stringProperty("spray_sound", DEFAULT_SOUND)),
                    Sound.Source.PLAYER, 1f, pitch), eye.x(), eye.y(), eye.z());
        }
    }

    private static Set<FireClass> suitableClasses(CustomItem item) {
        Set<FireClass> classes = EnumSet.noneOf(FireClass.class);
        if (item.properties().get("fire_classes") instanceof JsonArray array) {
            array.forEach(value -> FireClass.parse(value.getAsString()).ifPresent(classes::add));
        }
        return classes;
    }

    private static void emitParticles(Instance instance, Pos eye, Vec direction, double range, Agent agent) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Pos nozzle = eye.add(direction.mul(0.6)).add(0, -0.25, 0);
        Particle stream = switch (agent) {
            case CO2 -> Particle.SNOWFLAKE;
            case WATER -> Particle.SPLASH;
            default -> Particle.CLOUD;
        };
        for (int i = 0; i < 6; i++) {
            Vec velocity = direction
                    .add(new Vec(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).mul(0.08))
                    .normalize();
            Effects.particles(instance, stream, nozzle, velocity.x(), velocity.y(), velocity.z(),
                    0.35 + random.nextDouble() * 0.25, 0);
        }
        Particle dust = switch (agent) {
            case FOAM -> Particle.DUST.withProperties(TextColor.color(245, 240, 215), 2.0f);
            case WET_CHEMICAL -> Particle.DUST.withProperties(TextColor.color(240, 230, 160), 1.6f);
            case METAL_POWDER -> Particle.DUST.withProperties(TextColor.color(150, 150, 160), 1.3f);
            case POWDER -> Particle.DUST.withProperties(TextColor.color(235, 238, 245), 1.5f);
            default -> null;
        };
        for (double distance = 1; distance <= range; distance += 1) {
            Pos point = nozzle.add(direction.mul(distance));
            double spread = 0.1 + distance * 0.1;
            if (dust != null) {
                Effects.particles(instance, dust, point, spread, spread, spread, 0, 2);
            } else if (agent == Agent.WATER) {
                Effects.particles(instance, Particle.FALLING_WATER, point, spread, spread, spread, 0, 3);
            }
        }
    }

    private void updateHud() {
        for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            ItemStack stack = player.getEquipment(EquipmentSlot.MAIN_HAND);
            Optional<CustomItem> item = extinguisher(stack);
            if (item.isPresent()) {
                hud.set(player, HUD_LAYER, render(player, fill(stack), capacity(item.get())));
            } else {
                hud.remove(player, HUD_LAYER);
            }
        }
    }

    private Component render(Player player, int fill, int capacity) {
        int percent = Math.round(100f * fill / capacity);
        Component label = Component.text(" " + percent + " %", fill == 0 ? NamedTextColor.RED : NamedTextColor.WHITE);
        if (!glyphs.has("hud_feuerloescher") || !glyphs.has("hud_bar_full") || !glyphs.has("hud_bar_empty")) {
            return messages.text(player, "extinguisher.fill").append(label);
        }
        int filled = (int) Math.ceil((double) fill * HUD_SEGMENTS / capacity);
        var bar = Component.text().append(glyphs.glyph("hud_feuerloescher"), glyphs.shift(2));
        for (int segment = 0; segment < HUD_SEGMENTS; segment++) {
            bar.append(glyphs.glyph(segment < filled ? "hud_bar_full" : "hud_bar_empty"));
        }
        return bar.append(label).build();
    }
}
