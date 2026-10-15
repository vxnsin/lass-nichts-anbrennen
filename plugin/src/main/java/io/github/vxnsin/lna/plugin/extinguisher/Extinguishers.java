package io.github.vxnsin.lna.plugin.extinguisher;

import io.github.vxnsin.lna.plugin.Keys;
import io.github.vxnsin.lna.plugin.hud.Glyphs;
import io.github.vxnsin.lna.plugin.hud.HudService;
import io.github.vxnsin.lna.plugin.item.CustomItem;
import io.github.vxnsin.lna.plugin.item.ItemRegistry;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.UseEffects;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Campfire;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

/**
 * Fire extinguisher behaviour.
 *
 * <p>Extinguishers are "usable" items (consumable component that never completes), so holding right click
 * puts the player into the vanilla using state. The client then switches to the draw and aim models
 * (content/items.yml, use_models) by itself, and the server sprays while the item is in active use.
 * The use_effects component removes the movement slowdown that vanilla applies while using an item.
 *
 * <p>Item properties (content/items.yml): {@code capacity}, {@code range}, {@code spray} (powder|co2),
 * {@code spray_sound}.
 */
public final class Extinguishers implements Listener {

    public static final String TAG = "extinguisher";

    private static final String HUD_LAYER = "extinguisher";
    private static final String DEFAULT_SOUND = "lna:feuerloescher.spray";
    private static final Key SILENT_SOUND = Key.key(Keys.NAMESPACE, "silent");
    /** Long enough to never finish; consumption is additionally cancelled in {@link #onConsume}. */
    private static final float USE_SECONDS = 3600f;
    /** Spraying starts once the aim model is shown (matches the use_models threshold in items.yml). */
    private static final int SPRAY_START_TICKS = 4;
    private static final int SPRAY_INTERVAL_TICKS = 3;
    /** Slightly shorter than the spray sample, so consecutive sounds overlap without gaps. */
    private static final int SOUND_INTERVAL_TICKS = 12;
    private static final int HUD_SEGMENTS = 10;
    private static final Particle.DustOptions POWDER = new Particle.DustOptions(Color.fromRGB(235, 238, 245), 1.5f);

    private final ItemRegistry items;
    private final Glyphs glyphs;
    private final HudService hud;
    private final Map<UUID, Integer> lastSound = new HashMap<>();

    public Extinguishers(ItemRegistry items, Glyphs glyphs, HudService hud) {
        this.items = items;
        this.glyphs = glyphs;
        this.hud = hud;
        items.onCreate(TAG, (stack, item) -> {
            makeUsable(stack);
            setFill(stack, item, capacity(item));
        });
    }

    public void start(Plugin plugin) {
        Bukkit.getScheduler().runTaskTimer(plugin, this::tickSpraying, 1L, 1L);
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateHud, 20L, 5L);
    }

    public static int capacity(CustomItem item) {
        return Math.max(1, item.intProperty("capacity", 100));
    }

    public int fill(ItemStack stack) {
        Integer fill = stack.getItemMeta().getPersistentDataContainer().get(Keys.FILL, PersistentDataType.INTEGER);
        return fill == null ? 0 : fill;
    }

    /** Stores the fill level and mirrors it in the durability bar (max damage = capacity + 1, never breaks). */
    public void setFill(ItemStack stack, CustomItem item, int fill) {
        int capacity = capacity(item);
        int clamped = Math.clamp(fill, 0, capacity);
        stack.editMeta(meta -> {
            meta.getPersistentDataContainer().set(Keys.FILL, PersistentDataType.INTEGER, clamped);
            if (meta instanceof Damageable damageable) {
                damageable.setMaxDamage(capacity + 1);
                damageable.setDamage(capacity - clamped);
            }
        });
    }

    /** Refills the extinguisher in the player's main hand. Returns {@code false} if none is held. */
    public boolean refill(Player player) {
        ItemStack stack = player.getInventory().getItemInMainHand();
        Optional<CustomItem> item = extinguisher(stack);
        item.ifPresent(i -> setFill(stack, i, capacity(i)));
        return item.isPresent();
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        if (extinguisher(event.getItem()).isPresent()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastSound.remove(event.getPlayer().getUniqueId());
    }

    private static void makeUsable(ItemStack stack) {
        stack.setData(DataComponentTypes.CONSUMABLE, Consumable.consumable()
                .consumeSeconds(USE_SECONDS)
                .animation(ItemUseAnimation.NONE)
                .hasConsumeParticles(false)
                .sound(SILENT_SOUND)
                .build());
        stack.setData(DataComponentTypes.USE_EFFECTS, UseEffects.useEffects()
                .canSprint(true)
                .speedMultiplier(1.0f)
                .build());
    }

    private Optional<CustomItem> extinguisher(ItemStack stack) {
        return items.of(stack).filter(item -> item.hasTag(TAG));
    }

    private void tickSpraying() {
        int now = Bukkit.getCurrentTick();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.hasActiveItem()) {
                continue;
            }
            int used = player.getActiveItemUsedTime();
            if (used < SPRAY_START_TICKS || (used - SPRAY_START_TICKS) % SPRAY_INTERVAL_TICKS != 0) {
                continue;
            }
            EquipmentSlot hand = player.getActiveItemHand();
            ItemStack stack = player.getInventory().getItem(hand);
            extinguisher(stack).ifPresent(item -> spray(player, stack, item, now, used == SPRAY_START_TICKS));
        }
    }

    private void spray(Player player, ItemStack stack, CustomItem item, int now, boolean firstBurst) {
        int fill = fill(stack);
        if (fill <= 0) {
            if (firstBurst) {
                player.sendActionBar(MiniMessage.miniMessage().deserialize(
                        "<red>Leer!</red> <gray>Benutzte Feuerlöscher nicht zurückhängen, sondern melden.</gray>"));
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_DISPENSER_FAIL, 0.6f, 1.2f);
            }
            return;
        }
        setFill(stack, item, fill - 1);

        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection();
        double range = item.doubleProperty("range", 6.0);
        emitParticles(player.getWorld(), eye, direction, range, item.stringProperty("spray", "powder"));

        UUID id = player.getUniqueId();
        if (now - lastSound.getOrDefault(id, Integer.MIN_VALUE / 2) >= SOUND_INTERVAL_TICKS) {
            lastSound.put(id, now);
            float pitch = 0.95f + ThreadLocalRandom.current().nextFloat() * 0.1f;
            Sound sound = Sound.sound(Key.key(item.stringProperty("spray_sound", DEFAULT_SOUND)), Sound.Source.PLAYER, 1f, pitch);
            player.getWorld().playSound(sound, eye.getX(), eye.getY(), eye.getZ());
        }

        if (extinguish(player, eye, direction, range) > 0) {
            player.getWorld().playSound(eye, org.bukkit.Sound.BLOCK_FIRE_EXTINGUISH, 0.8f, 1.0f);
        }
    }

    private void emitParticles(World world, Location eye, Vector direction, double range, String spray) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Location nozzle = eye.clone().add(direction.clone().multiply(0.6)).add(0, -0.25, 0);
        Particle stream = "co2".equals(spray) ? Particle.SNOWFLAKE : Particle.CLOUD;
        for (int i = 0; i < 6; i++) {
            Vector velocity = direction.clone()
                    .add(new Vector(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).multiply(0.08))
                    .normalize();
            // count 0: offset is used as velocity, extra as speed
            world.spawnParticle(stream, nozzle, 0, velocity.getX(), velocity.getY(), velocity.getZ(),
                    0.35 + random.nextDouble() * 0.25);
        }
        if (!"co2".equals(spray)) {
            for (double distance = 1; distance <= range; distance += 1) {
                Location point = nozzle.clone().add(direction.clone().multiply(distance));
                double spread = 0.1 + distance * 0.1;
                world.spawnParticle(Particle.DUST, point, 2, spread, spread, spread, 0, POWDER);
            }
        }
    }

    /** Extinguishes vanilla fire, lit campfires and burning entities in a widening cone. Returns the number of hits. */
    private int extinguish(Player player, Location eye, Vector direction, double range) {
        int hits = 0;
        for (double distance = 0.5; distance <= range; distance += 0.5) {
            Location point = eye.clone().add(direction.clone().multiply(distance));
            double radius = 0.3 + distance * 0.12;
            hits += extinguishBlocks(point, radius);
            for (Entity entity : point.getWorld().getNearbyEntities(point, radius, radius, radius)) {
                if (!entity.equals(player) && entity.getFireTicks() > 0) {
                    entity.setFireTicks(0);
                    hits++;
                }
            }
            if (point.getBlock().getType().isOccluding()) {
                break;
            }
        }
        return hits;
    }

    private int extinguishBlocks(Location center, double radius) {
        int hits = 0;
        int reach = (int) Math.ceil(radius);
        double maxDistanceSquared = (radius + 0.5) * (radius + 0.5);
        for (int x = -reach; x <= reach; x++) {
            for (int y = -reach; y <= reach; y++) {
                for (int z = -reach; z <= reach; z++) {
                    Block block = center.getWorld().getBlockAt(
                            center.getBlockX() + x, center.getBlockY() + y, center.getBlockZ() + z);
                    if (block.getLocation().add(0.5, 0.5, 0.5).distanceSquared(center) > maxDistanceSquared) {
                        continue;
                    }
                    if (Tag.FIRE.isTagged(block.getType())) {
                        block.setType(Material.AIR);
                        center.getWorld().spawnParticle(Particle.SMOKE, block.getLocation().add(0.5, 0.3, 0.5), 6, 0.2, 0.1, 0.2, 0.02);
                        hits++;
                    } else if (block.getBlockData() instanceof Campfire campfire && campfire.isLit()) {
                        campfire.setLit(false);
                        block.setBlockData(campfire);
                        hits++;
                    }
                }
            }
        }
        return hits;
    }

    private void updateHud() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            ItemStack stack = player.getInventory().getItemInMainHand();
            Optional<CustomItem> item = extinguisher(stack);
            if (item.isPresent()) {
                hud.set(player, HUD_LAYER, render(fill(stack), capacity(item.get())));
            } else {
                hud.remove(player, HUD_LAYER);
            }
        }
    }

    private Component render(int fill, int capacity) {
        int percent = Math.round(100f * fill / capacity);
        Component label = Component.text(" " + percent + " %", fill == 0 ? NamedTextColor.RED : NamedTextColor.WHITE);
        if (!glyphs.has("hud_feuerloescher") || !glyphs.has("hud_bar_full") || !glyphs.has("hud_bar_empty")) {
            return Component.text("Füllstand:", NamedTextColor.GRAY).append(label);
        }
        int filled = (int) Math.ceil((double) fill * HUD_SEGMENTS / capacity);
        var bar = Component.text().append(glyphs.glyph("hud_feuerloescher"), glyphs.shift(2));
        for (int segment = 0; segment < HUD_SEGMENTS; segment++) {
            bar.append(glyphs.glyph(segment < filled ? "hud_bar_full" : "hud_bar_empty"));
        }
        return bar.append(label).build();
    }
}
