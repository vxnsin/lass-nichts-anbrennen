package dev.vensin.lna.server.lobby;

import dev.vensin.lna.server.Tags;
import dev.vensin.lna.server.furniture.Furniture;
import dev.vensin.lna.server.item.Items;
import dev.vensin.lna.server.lang.Messages;
import dev.vensin.lna.server.map.MapDefinition;
import dev.vensin.lna.server.mission.MissionInfo;
import dev.vensin.lna.server.mission.Passes;
import dev.vensin.lna.server.world.GameWorld;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.entity.metadata.other.InteractionMeta;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.sound.SoundEvent;
import net.minestom.server.timer.TaskSchedule;

public final class Lobby {

    private static final String ABOUT_TEXT = "about_chat";
    private static final double HOVER_DISTANCE = 6;

    private final Items items;
    private final Messages messages;
    private final Map<String, MissionInfo> missions;
    private final Passes passes;
    private final Map<String, MapDefinition> maps;
    /** Hitbox to its titles by language; a title only shows while a player looks at the hitbox. */
    private final Map<Entity, Map<String, Entity>> labels;
    private final Map<UUID, Entity> hovered = new HashMap<>();

    public Lobby(Items items, Messages messages, Map<String, MissionInfo> missions, Passes passes,
                 Map<String, MapDefinition> maps, Map<Entity, Map<String, Entity>> labels) {
        this.items = items;
        this.messages = messages;
        this.missions = missions;
        this.passes = passes;
        this.maps = maps;
        this.labels = labels;
    }

    public void register(GlobalEventHandler events, Furniture furniture) {
        furniture.onClick(this::onFurnitureClick);
        events.addListener(PlayerEntityInteractEvent.class, event -> {
            if (event.getHand() == PlayerHand.MAIN && event.getTarget().getTag(Tags.NPC) != null) {
                showAbout(event.getPlayer());
            }
        });
        events.addListener(PlayerDisconnectEvent.class, event -> hovered.remove(event.getPlayer().getUuid()));
        MinecraftServer.getSchedulerManager().buildTask(this::updateHoverLabels)
                .repeat(TaskSchedule.tick(4)).schedule();
    }

    private void onFurnitureClick(Player player, Entity hitbox) {
        String give = hitbox.getTag(Tags.GIVE);
        if (give != null) {
            take(player, give);
            return;
        }
        String missionId = hitbox.getTag(Tags.MISSION);
        if (missionId != null && missions.containsKey(missionId)) {
            showMission(player, missions.get(missionId));
        }
    }

    private void take(Player player, String itemId) {
        for (ItemStack stack : player.getInventory().getItemStacks()) {
            if (items.of(stack).filter(item -> item.id().equals(itemId)).isPresent()) {
                player.sendActionBar(messages.text(player, "lobby.have-already"));
                return;
            }
        }
        items.get(itemId).ifPresent(item -> {
            player.getInventory().addItemStack(items.create(item));
            player.playSound(Sound.sound(SoundEvent.ITEM_ARMOR_EQUIP_IRON, Sound.Source.PLAYER, 0.8f, 1.2f));
            player.sendActionBar(messages.text(player, "lobby.taken",
                    Placeholder.component("item", MiniMessage.miniMessage().deserialize(item.name()))));
        });
    }

    private void showMission(Player player, MissionInfo info) {
        player.sendMessage(Component.empty());
        player.sendMessage(messages.text(player, "mission.label",
                Placeholder.unparsed("number", String.valueOf(info.number())),
                Placeholder.unparsed("title", info.title())));
        player.sendMessage(messages.text(player, "mission.description", Placeholder.unparsed("text", info.description())));
        if (passes.of(player.getUuid()).contains(info.id())) {
            player.sendMessage(messages.text(player, "mission.stamped"));
        }
        player.sendMessage(info.available()
                ? messages.text(player, "mission.start",
                        Placeholder.styling("start", ClickEvent.runCommand("/mission start " + info.id())))
                : messages.text(player, "mission.unavailable"));
        player.playSound(Sound.sound(SoundEvent.ITEM_BOOK_PAGE_TURN, Sound.Source.PLAYER, 1f, 1f));
    }

    private void showAbout(Player player) {
        MapDefinition hub = maps.get(GameWorld.HUB);
        String text = hub != null ? hub.texts().get(ABOUT_TEXT) : null;
        player.sendMessage(text != null ? MiniMessage.miniMessage().deserialize(text)
                : messages.text(player, "lobby.about-fallback"));
    }

    private void updateHoverLabels() {
        for (Player player : MinecraftServer.getConnectionManager().getOnlinePlayers()) {
            Entity label = lookedAt(player);
            Entity previous = hovered.get(player.getUuid());
            if (label == previous) {
                continue;
            }
            if (previous != null) {
                previous.removeViewer(player);
            }
            if (label == null) {
                hovered.remove(player.getUuid());
            } else {
                label.addViewer(player);
                hovered.put(player.getUuid(), label);
            }
        }
    }

    private Entity lookedAt(Player player) {
        Pos eye = player.getPosition().add(0, player.getEyeHeight(), 0);
        Vec direction = eye.direction();
        Entity nearest = null;
        double nearestDistance = HOVER_DISTANCE;
        for (Map.Entry<Entity, Map<String, Entity>> entry : labels.entrySet()) {
            Entity hitbox = entry.getKey();
            if (hitbox.getInstance() != player.getInstance()) {
                continue;
            }
            InteractionMeta box = (InteractionMeta) hitbox.getEntityMeta();
            double half = box.getWidth() / 2.0;
            Pos base = hitbox.getPosition();
            double distance = Rays.distanceToBox(eye, direction,
                    base.x() - half, base.y(), base.z() - half,
                    base.x() + half, base.y() + box.getHeight(), base.z() + half);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = entry.getValue().get(Messages.languageOf(player));
            }
        }
        return nearest;
    }
}
