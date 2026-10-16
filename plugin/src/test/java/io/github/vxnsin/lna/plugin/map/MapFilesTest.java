package io.github.vxnsin.lna.plugin.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * Static checks for every map file, so broken plans fail the build instead of the server start:
 * consistent row widths, known legend characters and valid references to texts, hazards and items.
 * Block ids are validated by the server when the map is loaded.
 */
class MapFilesTest {

    private static final Path MAPS = Path.of("src/main/resources/maps");
    private static final Path ITEMS = Path.of("../content/items.yml");

    private static final Path MISSIONS = Path.of("src/main/resources/missions.yml");

    @TestFactory
    Stream<DynamicTest> mapsAreConsistent() throws IOException {
        Map<String, Object> items = load(ITEMS);
        Map<String, Object> missions = load(MISSIONS);
        try (Stream<Path> files = Files.list(MAPS)) {
            return files.filter(file -> file.toString().endsWith(".yml")).sorted().toList().stream()
                    .map(file -> DynamicTest.dynamicTest(file.getFileName().toString(),
                            () -> check(load(file), items, missions)));
        }
    }

    @SuppressWarnings("unchecked")
    private static void check(Map<String, Object> map, Map<String, Object> items, Map<String, Object> missions) {
        Map<String, Object> legend = (Map<String, Object>) map.get("legend");
        Map<String, Object> texts = (Map<String, Object>) map.getOrDefault("texts", Map.of());
        Map<String, Object> hazards = (Map<String, Object>) map.getOrDefault("hazards", Map.of());
        Map<String, Object> safe = (Map<String, Object>) map.getOrDefault("safe", Map.of());
        List<String> plan = (List<String>) map.get("plan");

        assertFalse(plan.isEmpty(), "plan is empty");
        int width = plan.getFirst().length();
        for (int row = 0; row < plan.size(); row++) {
            String line = plan.get(row);
            assertEquals(width, line.length(), "row " + (row + 1) + " width");
            for (char symbol : line.toCharArray()) {
                assertTrue(legend.containsKey(String.valueOf(symbol)), "row " + (row + 1) + ": '" + symbol + "' not in legend");
            }
        }
        long spawns = legend.entrySet().stream()
                .filter(entry -> entry.getValue() instanceof Map<?, ?> cell && cell.containsKey("spawn"))
                .mapToLong(entry -> plan.stream().mapToLong(line -> line.chars().filter(c -> c == entry.getKey().charAt(0)).count()).sum())
                .sum();
        assertEquals(1, spawns, "exactly one spawn cell");

        for (var entry : legend.entrySet()) {
            String where = "legend '" + entry.getKey() + "'";
            Map<String, Object> cell = entry.getValue() == null ? Map.of() : (Map<String, Object>) entry.getValue();
            if (cell.containsKey("text")) {
                assertTrue(texts.containsKey(cell.get("text")), where + ": unknown text " + cell.get("text"));
            }
            checkReference(where, cell, hazards, safe);
            if (cell.containsKey("mission")) {
                assertTrue(missions.containsKey(cell.get("mission")), where + ": unknown mission " + cell.get("mission"));
            }
            for (Object element : (List<Object>) cell.getOrDefault("objects", List.of())) {
                Map<String, Object> object = (Map<String, Object>) element;
                assertTrue(items.containsKey(object.get("id")), where + ": unknown item " + object.get("id"));
                checkReference(where, object, hazards, safe);
            }
        }
    }

    private static void checkReference(String where, Map<String, Object> entry, Map<String, Object> hazards,
                                       Map<String, Object> safe) {
        if (entry.containsKey("hazard")) {
            assertTrue(hazards.containsKey(entry.get("hazard")), where + ": unknown hazard " + entry.get("hazard"));
        }
        if (entry.containsKey("safe")) {
            assertTrue(safe.containsKey(entry.get("safe")), where + ": unknown safe entry " + entry.get("safe"));
        }
    }

    private static Map<String, Object> load(Path file) throws IOException {
        try (Reader reader = Files.newBufferedReader(file)) {
            return new Yaml(new SafeConstructor(new LoaderOptions())).load(reader);
        }
    }
}
