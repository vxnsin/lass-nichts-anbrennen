package io.github.vxnsin.lna.packbuilder;

import java.util.ArrayList;
import java.util.List;

/** Collects errors and warnings so that all problems are reported in a single run. */
public final class Problems {

    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    public void error(String message) {
        errors.add(message);
    }

    public void warn(String message) {
        warnings.add(message);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public List<String> errors() {
        return List.copyOf(errors);
    }

    public List<String> warnings() {
        return List.copyOf(warnings);
    }
}
