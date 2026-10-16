package io.github.vxnsin.lna.plugin.fire;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/** Fired when a player completely extinguishes a simulated fire. */
public final class FireExtinguishedEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Fire fire;

    public FireExtinguishedEvent(Player player, Fire fire) {
        super(player);
        this.fire = fire;
    }

    public Fire fire() {
        return fire;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
