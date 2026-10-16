package io.github.vxnsin.lna.plugin.fire;

import java.util.Locale;
import java.util.Optional;

/** Fire classes according to DIN EN 2. Electrical fires have no class of their own but need special care. */
public enum FireClass {
    A("A", "feste Stoffe"),
    B("B", "Flüssigkeiten"),
    C("C", "Gase"),
    D("D", "Metalle"),
    F("F", "Speiseöle und -fette"),
    ELECTRIC("Elektro", "elektrische Anlagen");

    private final String label;
    private final String description;

    FireClass(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String label() {
        return label;
    }

    /** German display text, e.g. "Brandklasse F (Speiseöle und -fette)". */
    public String display() {
        return this == ELECTRIC ? "Brand an elektrischen Anlagen" : "Brandklasse " + label + " (" + description + ")";
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
