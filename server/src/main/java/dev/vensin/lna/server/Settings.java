package dev.vensin.lna.server;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

public record Settings(int port, boolean onlineMode, String motd, Pack pack) {

    public record Pack(String host, int port, String publicUrl, boolean required, String prompt) {}

    private static final String DEFAULTS = "server.yml";

    /** Writes the commented default file on first start, so there is something to edit. */
    public static Settings load(Path file) throws IOException {
        if (!Files.exists(file)) {
            try (InputStream defaults = Settings.class.getClassLoader().getResourceAsStream(DEFAULTS)) {
                Files.copy(defaults, file);
            }
        }
        Map<String, Object> yaml = new Yaml(new SafeConstructor(new LoaderOptions()))
                .load(Files.readString(file, StandardCharsets.UTF_8));
        Map<String, Object> root = yaml == null ? Map.of() : yaml;
        Map<String, Object> pack = section(root.get("pack"));
        return new Settings(
                number(root, "port", 25565),
                !Boolean.FALSE.equals(root.get("online-mode")),
                text(root, "motd", "<gold>Lass nichts anbrennen</gold>"),
                new Pack(
                        text(pack, "host", ""),
                        number(pack, "port", 8164),
                        text(pack, "public-url", ""),
                        !Boolean.FALSE.equals(pack.get("required")),
                        text(pack, "prompt", "")));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> section(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private static int number(Map<String, Object> map, String key, int fallback) {
        return map.get(key) instanceof Number number ? number.intValue() : fallback;
    }

    private static String text(Map<String, Object> map, String key, String fallback) {
        Object value = map.get(key);
        return value == null ? fallback : String.valueOf(value);
    }
}
