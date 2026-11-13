package dev.vensin.lna.server.map;

import dev.vensin.lna.server.Tags;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.avatar.MannequinMeta;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.network.player.ResolvableProfile;

/** The non-block parts of a map: floating texts, flat icons and standing figures. */
final class MapObjects {

    private static final float[] LYING_FLAT = {-0.70710678f, 0f, 0f, 0.70710678f};

    private MapObjects() {}

    static Entity text(Instance instance, Pos position, Component text, float scale, int lineWidth, int background,
                       boolean hidden, String mapId) {
        Entity display = new Entity(EntityType.TEXT_DISPLAY);
        display.setNoGravity(true);
        display.setAutoViewable(!hidden);
        display.editEntityMeta(TextDisplayMeta.class, meta -> {
            meta.setText(text);
            meta.setBillboardRenderConstraints(AbstractDisplayMeta.BillboardConstraints.FIXED);
            meta.setAlignment(TextDisplayMeta.Alignment.CENTER);
            meta.setLineWidth(lineWidth);
            meta.setBackgroundColor(background);
            meta.setShadow(true);
            meta.setScale(new Vec(scale, scale, scale));
        });
        display.setTag(Tags.MAP, mapId);
        display.setInstance(instance, position);
        return display;
    }

    static void flatIcon(Instance instance, Pos position, ItemStack icon, float scale, String mapId) {
        Entity display = new Entity(EntityType.ITEM_DISPLAY);
        display.setNoGravity(true);
        display.editEntityMeta(ItemDisplayMeta.class, meta -> {
            meta.setItemStack(icon);
            meta.setDisplayContext(ItemDisplayMeta.DisplayContext.NONE);
            meta.setLeftRotation(LYING_FLAT);
            meta.setScale(new Vec(scale, scale, scale));
        });
        display.setTag(Tags.MAP, mapId);
        display.setInstance(instance, position);
    }

    /** A standing figure with the skin of the given player name, resolved by the client. */
    static void figure(Instance instance, Pos position, String name, Component description, String mapId) {
        Entity figure = new Entity(EntityType.MANNEQUIN);
        figure.setNoGravity(true);
        figure.editEntityMeta(MannequinMeta.class, meta -> {
            meta.setProfile(new ResolvableProfile(new ResolvableProfile.Partial(name, null, List.of())));
            meta.setImmovable(true);
            meta.setDescription(description);
        });
        figure.set(DataComponents.CUSTOM_NAME, Component.text(name, NamedTextColor.GOLD));
        figure.setCustomNameVisible(true);
        figure.setTag(Tags.MAP, mapId);
        figure.setTag(Tags.NPC, name);
        figure.setInstance(instance, position);
    }

    static int argb(int alpha, int red, int green, int blue) {
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

}
