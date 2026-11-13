package dev.vensin.lna.server.mission;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/** Stamps of the fire safety pass: player UUID to the ids of the missions they passed. */
public final class Passes {

    private static final Logger LOG = LoggerFactory.getLogger(Passes.class);

    private final Path file;
    private final Map<UUID, List<String>> stamps = new LinkedHashMap<>();

    public Passes(Path file) throws IOException {
        this.file = file;
        if (!Files.exists(file)) {
            return;
        }
        Object root;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            root = new Yaml(new SafeConstructor(new LoaderOptions())).load(reader);
        }
        if (!(root instanceof Map<?, ?> entries)) {
            return;
        }
        entries.forEach((id, missions) -> {
            try {
                List<String> list = new ArrayList<>();
                if (missions instanceof List<?> values) {
                    values.forEach(value -> list.add(String.valueOf(value)));
                }
                stamps.put(UUID.fromString(String.valueOf(id)), list);
            } catch (IllegalArgumentException e) {
                LOG.warn("{}: skipping {}, not a player UUID", file, id);
            }
        });
    }

    public List<String> of(UUID player) {
        return List.copyOf(stamps.getOrDefault(player, List.of()));
    }

    public void stamp(UUID player, String missionId) {
        List<String> list = stamps.computeIfAbsent(player, id -> new ArrayList<>());
        if (list.contains(missionId)) {
            return;
        }
        list.add(missionId);
        save();
    }

    private void save() {
        Map<String, List<String>> out = new LinkedHashMap<>();
        stamps.forEach((id, missions) -> out.put(id.toString(), missions));
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            new Yaml(options).dump(out, writer);
        } catch (IOException e) {
            LOG.warn("Could not save {}: {}", file, e.getMessage());
        }
    }
}
