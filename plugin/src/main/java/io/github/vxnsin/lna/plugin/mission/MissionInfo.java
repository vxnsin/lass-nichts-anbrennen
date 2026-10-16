package io.github.vxnsin.lna.plugin.mission;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/** One campaign mission as described in missions.yml. */
public record MissionInfo(String id, int number, String title, String description, String map, String icon,
                          String type, int duration, boolean available, List<Question> quiz) {

    /** A quiz question with answer options; {@code correct} is the 1-based number of the right answer. */
    public record Question(String text, List<String> answers, int correct, String explanation) {}

    @SuppressWarnings("unchecked")
    private static List<Question> quiz(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Question> questions = new ArrayList<>();
        for (Object element : list) {
            Map<String, Object> q = (Map<String, Object>) element;
            questions.add(new Question(
                    String.valueOf(q.get("question")),
                    ((List<Object>) q.get("answers")).stream().map(String::valueOf).toList(),
                    ((Number) q.get("correct")).intValue(),
                    String.valueOf(q.getOrDefault("explanation", ""))));
        }
        return List.copyOf(questions);
    }

    /** Loads missions.yml, sorted by mission number. */
    @SuppressWarnings("unchecked")
    public static Map<String, MissionInfo> load(InputStream in) throws IOException {
        Map<String, Object> root;
        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            root = new Yaml(new SafeConstructor(new LoaderOptions())).load(reader);
        }
        Map<String, MissionInfo> missions = new LinkedHashMap<>();
        root.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> m = (Map<String, Object>) entry.getValue();
                    return new MissionInfo(
                            entry.getKey(),
                            ((Number) m.getOrDefault("number", 0)).intValue(),
                            String.valueOf(m.getOrDefault("title", entry.getKey())),
                            String.valueOf(m.getOrDefault("description", "")),
                            (String) m.get("map"),
                            String.valueOf(m.getOrDefault("icon", "klemmbrett")),
                            String.valueOf(m.getOrDefault("type", "")),
                            ((Number) m.getOrDefault("duration", 300)).intValue(),
                            Boolean.TRUE.equals(m.get("available")),
                            quiz(m.get("quiz")));
                })
                .sorted(Comparator.comparingInt(MissionInfo::number))
                .forEach(mission -> missions.put(mission.id(), mission));
        return missions;
    }

    /** "Mission 2: Gefahren-Detektiv" */
    public String label() {
        return "Mission " + number + ": " + title;
    }
}
