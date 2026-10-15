package io.github.vxnsin.lna.plugin.furniture;

import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/** Fired when a player right-clicks a placed object. Missions listen to this (e.g. call point, hazard hunt). */
public final class FurnitureInteractEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Interaction hitbox;
    private final String itemId;

    public FurnitureInteractEvent(Player player, Interaction hitbox, String itemId) {
        super(player);
        this.hitbox = hitbox;
        this.itemId = itemId;
    }

    /** The clickable hitbox entity of the object; carries the object's persistent data. */
    public Interaction hitbox() {
        return hitbox;
    }

    public String itemId() {
        return itemId;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
