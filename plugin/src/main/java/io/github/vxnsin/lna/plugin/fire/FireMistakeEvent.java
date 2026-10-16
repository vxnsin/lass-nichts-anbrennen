package io.github.vxnsin.lna.plugin.fire;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/** Fired when a player handles a fire wrongly. Missions use it for scoring and explanations. */
public final class FireMistakeEvent extends PlayerEvent {

    public enum Mistake {
        /** The extinguisher is not suitable for this fire class. */
        WRONG_CLASS,
        /** Water on burning fat or oil: fat explosion. */
        WATER_ON_FAT,
        /** Water on an electrical fire: electric shock. */
        WATER_ON_ELECTRIC
    }

    private static final HandlerList HANDLERS = new HandlerList();

    private final Fire fire;
    private final Mistake mistake;

    public FireMistakeEvent(Player player, Fire fire, Mistake mistake) {
        super(player);
        this.fire = fire;
        this.mistake = mistake;
    }

    public Fire fire() {
        return fire;
    }

    public Mistake mistake() {
        return mistake;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
