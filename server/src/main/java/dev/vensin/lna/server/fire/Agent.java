package dev.vensin.lna.server.fire;

import java.util.Locale;

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
