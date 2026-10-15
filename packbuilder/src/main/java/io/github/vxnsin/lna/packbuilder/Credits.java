package io.github.vxnsin.lna.packbuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Parses the asset attribution table in CREDITS.md.
 *
 * <p>Expected format (Markdown table, paths relative to {@code content/}):
 * <pre>
 * | Datei | Titel | Urheber*in | Quelle | Lizenz | Änderungen |
 * |---|---|---|---|---|---|
 * | textures/item/beispiel.png | Beispiel | Klasse WEC10A | eigenes Werk | CC BY 4.0 | – |
 * </pre>
 */
public final class Credits {

    public record Entry(String file, String title, String author, String source, String license, String changes) {}

    private final Map<String, Entry> entries;

    private Credits(Map<String, Entry> entries) {
        this.entries = Collections.unmodifiableMap(entries);
    }

    public static Credits parse(Path creditsFile, Problems problems) throws IOException {
        Map<String, Entry> entries = new LinkedHashMap<>();
        if (!Files.exists(creditsFile)) {
            problems.error("CREDITS.md fehlt: " + creditsFile);
            return new Credits(entries);
        }
        List<String> lines = Files.readAllLines(creditsFile);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).strip();
            if (!line.startsWith("|")) {
                continue;
            }
            List<String> cells = splitRow(line);
            if (cells.isEmpty() || cells.getFirst().equalsIgnoreCase("Datei") || cells.getFirst().startsWith("---")) {
                continue;
            }
            int lineNo = i + 1;
            if (cells.size() < 6) {
                problems.error("CREDITS.md Zeile " + lineNo + ": Es werden 6 Spalten erwartet "
                        + "(Datei | Titel | Urheber*in | Quelle | Lizenz | Änderungen).");
                continue;
            }
            Entry entry = new Entry(cells.get(0).replace("`", ""), cells.get(1), cells.get(2),
                    cells.get(3), cells.get(4), cells.get(5));
            if (entry.author().isBlank() || entry.license().isBlank()) {
                problems.error("CREDITS.md Zeile " + lineNo + ": Urheber*in und Lizenz dürfen nicht leer sein ("
                        + entry.file() + ").");
            }
            checkLicense(entry, lineNo, problems);
            if (entries.put(entry.file(), entry) != null) {
                problems.error("CREDITS.md Zeile " + lineNo + ": Datei doppelt eingetragen: " + entry.file());
            }
        }
        return new Credits(entries);
    }

    private static void checkLicense(Entry entry, int lineNo, Problems problems) {
        String license = entry.license().toUpperCase(Locale.ROOT);
        if (license.contains("-ND") || license.contains(" ND")) {
            problems.error("CREDITS.md Zeile " + lineNo + ": Lizenz \"" + entry.license() + "\" verbietet Bearbeitungen (ND) "
                    + "und darf nicht verwendet werden: " + entry.file());
        } else if (license.contains("-NC") || license.contains(" NC")) {
            problems.warn("CREDITS.md Zeile " + lineNo + ": Lizenz \"" + entry.license() + "\" ist nur nicht-kommerziell (NC). "
                    + "Besser eine CC0-/CC-BY-Alternative suchen: " + entry.file());
        }
    }

    private static List<String> splitRow(String line) {
        String inner = line.substring(1);
        if (inner.endsWith("|")) {
            inner = inner.substring(0, inner.length() - 1);
        }
        List<String> cells = new ArrayList<>();
        for (String cell : inner.split("\\|", -1)) {
            cells.add(cell.strip());
        }
        return cells;
    }

    public boolean contains(String file) {
        return entries.containsKey(file);
    }

    public Map<String, Entry> entries() {
        return entries;
    }
}
