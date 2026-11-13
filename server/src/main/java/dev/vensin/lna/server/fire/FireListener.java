package dev.vensin.lna.server.fire;

import net.minestom.server.entity.Player;

public interface FireListener {

    enum Mistake {
        WRONG_CLASS,
        WATER_ON_FAT,
        WATER_ON_ELECTRIC
    }

    default void extinguished(Player player, Fire fire) {}

    default void mistake(Player player, Fire fire, Mistake mistake) {}
}
