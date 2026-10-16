package io.github.vxnsin.lna.plugin.fire;

import java.util.Locale;

/** Extinguishing agent of an extinguisher (item property "agent"). Decides particles and dangerous combinations. */
public enum Agent {
    POWDER,
    CO2,
    FOAM,
    WATER,
    WET_CHEMICAL,
    METAL_POWDER;

    public static Agent parse(String value) {
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return POWDER;
        }
    }
}
