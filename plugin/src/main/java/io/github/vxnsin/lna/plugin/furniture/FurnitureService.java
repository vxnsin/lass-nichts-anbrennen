package io.github.vxnsin.lna.plugin.furniture;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import io.github.vxnsin.lna.plugin.Keys;
import io.github.vxnsin.lna.plugin.item.CustomItem;
import io.github.vxnsin.lna.plugin.item.ItemRegistry;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/**
 * Places custom models in the world as an {@link ItemDisplay} (the visible model, rendered 1:1 at block scale)
 * plus an {@link Interaction} hitbox that carries the object's data and receives clicks.
 * Floor objects with collision additionally get barrier blocks in the cells they occupy.
 *
 * <p>Item properties (content/items.yml): {@code mount} (floor|wall|ceiling), {@code hitbox} ([width, height]
 * in blocks), {@code collision} (true|false).
 */
public final class FurnitureService {

    public static final String TAG_FURNITURE = "furniture";
    public static final String TAG_SIGN = "sign";

    /** Placement rules resolved from the item properties. */
    public record Placement(Mount mount, float width, float height, boolean collision) {}

    private final ItemRegistry items;
    /** Extra yaw for all models, in case the client renders item displays turned compared to the model files. */
    private final float rotationOffset;

    public FurnitureService(ItemRegistry items, float rotationOffset) {
        this.items = items;
        this.rotationOffset = rotationOffset;
    }

    public static boolean isFurniture(CustomItem item) {
        return item.hasTag(TAG_FURNITURE) || item.hasTag(TAG_SIGN);
    }

    /** Props are any other custom items placed as decoration (small, floor only, no collision). */
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

    /** Places an object at the clicked block face. Returns an error message for the player if not possible. */
    public Optional<String> place(Player player, CustomItem item, Block clicked, BlockFace face, boolean prop) {
        Placement placement = placementOf(item, prop);
        Block target;
        BlockFace front;
        switch (placement.mount()) {
            case FLOOR -> {
                if (face != BlockFace.UP) {
                    return Optional.of("Auf den Boden stellen: Oberseite eines Blocks anklicken.");
                }
                target = clicked.getRelative(BlockFace.UP);
                front = player.getFacing().getOppositeFace();
            }
            case WALL -> {
                if (face == BlockFace.UP || face == BlockFace.DOWN || !face.isCartesian()) {
                    return Optional.of("An die Wand hängen: Seite eines Blocks anklicken.");
                }
                target = clicked.getRelative(face);
                front = face;
            }
            default -> {
                if (face != BlockFace.DOWN) {
                    return Optional.of("An die Decke hängen: Unterseite eines Blocks anklicken.");
                }
                target = clicked.getRelative(BlockFace.DOWN);
                front = player.getFacing().getOppositeFace();
            }
        }

        int cells = Math.max(1, (int) Math.ceil(placement.height()));
        for (int i = 0; i < cells; i++) {
            Block cell = placement.mount() == Mount.CEILING ? target : target.getRelative(BlockFace.UP, i);
            if (!cell.getType().isAir() && !cell.isReplaceable()) {
                return Optional.of("Hier ist kein Platz.");
            }
        }
        World world = target.getWorld();
        Location center = target.getLocation().add(0.5, 0.5, 0.5);
        if (!world.getNearbyEntitiesByType(Interaction.class, center, 0.45).isEmpty()) {
            return Optional.of("Hier steht schon etwas.");
        }

        float yaw = yawFor(front) + rotationOffset;
        ItemDisplay display = world.spawn(center, ItemDisplay.class, entity -> {
            entity.setItemStack(items.create(item));
            entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            entity.setTransformation(new Transformation(new Vector3f(),
                    new AxisAngle4f((float) Math.toRadians(yaw), 0, 1, 0), new Vector3f(1, 1, 1), new AxisAngle4f()));
            entity.setDisplayWidth(2f);
            entity.setDisplayHeight(placement.height() + 1f);
            entity.setPersistent(true);
        });

        double hitboxY = placement.mount() == Mount.CEILING ? target.getY() + 1 - placement.height() : target.getY();
        Location hitboxLocation = new Location(world, target.getX() + 0.5, hitboxY, target.getZ() + 0.5);
        int collisionCells = placement.collision() ? cells : 0;
        world.spawn(hitboxLocation, Interaction.class, entity -> {
            entity.setInteractionWidth(placement.width());
            entity.setInteractionHeight(placement.height());
            entity.setResponsive(true);
            entity.setPersistent(true);
            PersistentDataContainer data = entity.getPersistentDataContainer();
            data.set(Keys.FURNITURE_ID, PersistentDataType.STRING, item.id());
            data.set(Keys.FURNITURE_DISPLAY, PersistentDataType.STRING, display.getUniqueId().toString());
            data.set(Keys.FURNITURE_CELLS, PersistentDataType.INTEGER, collisionCells);
        });
        for (int i = 0; i < collisionCells; i++) {
            target.getRelative(BlockFace.UP, i).setType(Material.BARRIER);
        }
        return Optional.empty();
    }

    /** Removes the object belonging to this hitbox, including its model and barrier blocks. */
    public void remove(Interaction hitbox) {
        display(hitbox).ifPresent(Entity::remove);
        Integer cells = hitbox.getPersistentDataContainer().get(Keys.FURNITURE_CELLS, PersistentDataType.INTEGER);
        Block base = hitbox.getLocation().getBlock();
        for (int i = 0; i < (cells == null ? 0 : cells); i++) {
            Block cell = base.getRelative(BlockFace.UP, i);
            if (cell.getType() == Material.BARRIER) {
                cell.setType(Material.AIR);
            }
        }
        hitbox.remove();
    }

    public Optional<String> idOf(Entity entity) {
        if (!(entity instanceof Interaction)) {
            return Optional.empty();
        }
        return Optional.ofNullable(entity.getPersistentDataContainer().get(Keys.FURNITURE_ID, PersistentDataType.STRING));
    }

    public Optional<ItemDisplay> display(Interaction hitbox) {
        String id = hitbox.getPersistentDataContainer().get(Keys.FURNITURE_DISPLAY, PersistentDataType.STRING);
        if (id == null) {
            return Optional.empty();
        }
        return Bukkit.getEntity(UUID.fromString(id)) instanceof ItemDisplay display ? Optional.of(display) : Optional.empty();
    }

    /** All placed objects in a world, optionally restricted to one item id. */
    public List<Interaction> all(World world, String itemId) {
        return world.getEntitiesByClass(Interaction.class).stream()
                .filter(hitbox -> idOf(hitbox).filter(id -> itemId == null || id.equals(itemId)).isPresent())
                .toList();
    }

    public ItemRegistry items() {
        return items;
    }

    /** Yaw (degrees) that turns the model front (-z) towards {@code front}. */
    static float yawFor(BlockFace front) {
        return (float) Math.toDegrees(Math.atan2(-front.getModX(), -front.getModZ()));
    }
}
