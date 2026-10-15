package io.github.vxnsin.lna.packbuilder;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Command line: {@code --content <dir> --credits <CREDITS.md> --out <dir> --pack-format <number>}. */
public final class Main {

    private Main() {}

    public static void main(String[] args) throws Exception {
        Map<String, String> options = new HashMap<>();
        for (int i = 0; i + 1 < args.length; i += 2) {
            options.put(args[i], args[i + 1]);
        }
        for (String required : new String[] {"--content", "--credits", "--out", "--pack-format"}) {
            if (!options.containsKey(required)) {
                System.err.println("Fehlender Parameter: " + required);
                System.exit(2);
            }
        }

        PackBuilder builder = new PackBuilder(
                Path.of(options.get("--content")),
                Path.of(options.get("--credits")),
                Path.of(options.get("--out")),
                Integer.parseInt(options.get("--pack-format")));
        PackBuilder.Result result = builder.build();

        result.problems().warnings().forEach(w -> System.out.println("WARNUNG: " + w));
        if (result.problems().hasErrors()) {
            result.problems().errors().forEach(e -> System.err.println("FEHLER: " + e));
            System.err.println("Pack wurde NICHT gebaut (" + result.problems().errors().size() + " Fehler).");
            System.exit(1);
        }
        System.out.printf("Pack gebaut: %d Items, %d Sounds, %d Glyphen, %d Dateien geprüft -> pack.zip (%.1f KB)%n",
                result.items(), result.sounds(), result.glyphs(), result.files(), result.packBytes() / 1024.0);
        System.out.println("SHA-1: " + result.sha1());
    }
}
