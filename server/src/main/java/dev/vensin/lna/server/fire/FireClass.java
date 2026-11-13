package dev.vensin.lna.server.fire;

import java.util.Locale;
import java.util.Optional;

/** DIN EN 2; electrical fires have no class of their own there. */
public enum FireClass {
    A,
    B,
    C,
    D,
    F,
    ELECTRIC;

    /** Key of the display name in the language files. */
    public String messageKey() {
        return "fire.class." + name().toLowerCase(Locale.ROOT);
    }

    public static Optional<FireClass> parse(String value) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.equals("ELECTRIC") || normalized.equals("ELEKTRO") || normalized.equals("E")) {
            return Optional.of(ELECTRIC);
        }
        try {
            return Optional.of(valueOf(normalized));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
