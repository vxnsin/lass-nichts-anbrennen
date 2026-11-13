package dev.vensin.lna.server.furniture;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import dev.vensin.lna.server.Tags;
import dev.vensin.lna.server.item.CustomItem;
import dev.vensin.lna.server.item.Items;
import dev.vensin.lna.server.map.Facing;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.entity.metadata.other.InteractionMeta;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.PlayerEntityInteractEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;

/** A placed object is an item display (the model, 1:1 at block scale) plus an interaction entity that takes the clicks. */
public final class Furniture {

    public static final String TAG_FURNITURE = "furniture";
    public static final String TAG_SIGN = "sign";
    /** Furniture id of clickable boxes without a model. */
    public static final String MARKER_ID = "marker";

    public record Placement(Mount mount, float width, float height, boolean collision) {}

    private final Items items;
    private final Map<Entity, Entity> models = new HashMap<>();
    private final List<BiConsumer<Player, Entity>> clickHandlers = new ArrayList<>();

    public Furniture(Items items) {
        this.items = items;
    }

    public void register(GlobalEventHandler events) {
        events.addListener(PlayerEntityInteractEvent.class, event -> {
            if (event.getHand() == PlayerHand.MAIN && idOf(event.getTarget()).isPresent()) {
                clickHandlers.forEach(handler -> handler.accept(event.getPlayer(), event.getTarget()));
            }
        });
    }

    public void onClick(BiConsumer<Player, Entity> handler) {
        clickHandlers.add(handler);
    }

    /** Props are other custom items put down as decoration: small, floor only, no collision. */
    public Placement placementOf(CustomItem item, boolean prop) {
        if (prop) {
            return new Placement(Mount.FLOOR, 0.6f, 0.6f, false);
        }
        String defaultMount = item.hasTag(TAG_SIGN) ? "wall" : "floor";
        Mount mount = Mount.valueOf(item.stringProperty("mount", defaultMount).toUpperCase(Locale.ROOT));
        float width = 1f;
        float height = 1f;
        JsonElement hitbox = item.properties().get("hitbox");
        if (hitbox instanceof JsonArray array && array.size() == 2) {
            width = array.get(0).getAsFloat();
            height = array.get(1).getAsFloat();
        }
        boolean collision = mount == Mount.FLOOR && Boolean.parseBoolean(item.stringProperty("collision", "true"));
        return new Placement(mount, width, height, collision);
    }

    /** {@code front} is the direction the model front faces; for wall mounts the wall is behind it. */
    public Entity placeAt(Instance instance, CustomItem item, int x, int y, int z, Facing front, Placement placement) {
        Entity model = new Entity(EntityType.ITEM_DISPLAY);
        model.setNoGravity(true);
        model.editEntityMeta(ItemDisplayMeta.class, meta -> {
            meta.setItemStack(items.create(item));
            meta.setDisplayContext(ItemDisplayMeta.DisplayContext.NONE);
            meta.setLeftRotation(yawRotation(front));
            meta.setWidth(2f);
            meta.setHeight(placement.height() + 1f);
        });
        model.setInstance(instance, new Pos(x + 0.5, y + 0.5, z + 0.5));

        int cells = placement.collision() ? Math.max(1, (int) Math.ceil(placement.height())) : 0;
        double hitboxY = placement.mount() == Mount.CEILING ? y + 1 - placement.height() : y;
        Entity hitbox = hitbox(instance, new Pos(x + 0.5, hitboxY, z + 0.5), placement.width(), placement.height(),
                item.id(), cells);
        models.put(hitbox, model);
        for (int i = 0; i < cells; i++) {
            instance.setBlock(x, y + i, z, Block.BARRIER);
        }
        return hitbox;
    }

    /** An invisible clickable box around a block, e.g. barrels blocking an escape route. */
    public Entity placeMarker(Instance instance, int x, int y, int z) {
        return hitbox(instance, new Pos(x + 0.5, y - 0.05, z + 0.5), 1.1f, 1.1f, MARKER_ID, 0);
    }

    public void remove(Entity hitbox) {
        Entity model = models.remove(hitbox);
        if (model != null) {
            model.remove();
        }
        Integer cells = hitbox.getTag(Tags.FURNITURE_CELLS);
        Instance instance = hitbox.getInstance();
        Pos base = hitbox.getPosition();
        for (int i = 0; i < (cells == null ? 0 : cells); i++) {
            if (instance.getBlock(base.blockX(), base.blockY() + i, base.blockZ()).compare(Block.BARRIER)) {
                instance.setBlock(base.blockX(), base.blockY() + i, base.blockZ(), Block.AIR);
            }
        }
        hitbox.remove();
    }

    public Optional<String> idOf(Entity entity) {
        return Optional.ofNullable(entity.getTag(Tags.FURNITURE_ID));
    }

    public Optional<Entity> model(Entity hitbox) {
        return Optional.ofNullable(models.get(hitbox));
    }

    private static Entity hitbox(Instance instance, Pos position, float width, float height, String id, int cells) {
        Entity hitbox = new Entity(EntityType.INTERACTION);
        hitbox.setNoGravity(true);
        hitbox.editEntityMeta(InteractionMeta.class, meta -> {
            meta.setWidth(width);
            meta.setHeight(height);
            meta.setResponse(true);
        });
        hitbox.setTag(Tags.FURNITURE_ID, id);
        hitbox.setTag(Tags.FURNITURE_CELLS, cells);
        hitbox.setInstance(instance, position);
        return hitbox;
    }

    /** Quaternion (x, y, z, w) that turns the model front, which points to -z, towards {@code front}. */
    private static float[] yawRotation(Facing front) {
        double angle = Math.atan2(-front.dx(), -front.dz());
        return new float[] {0f, (float) Math.sin(angle / 2), 0f, (float) Math.cos(angle / 2)};
    }
}
