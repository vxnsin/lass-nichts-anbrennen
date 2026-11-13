package dev.vensin.lna.server.command;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.ConsoleSender;
import net.minestom.server.entity.Player;

/** Operators are stored by name, one per line, so the file can be edited by hand. */
public final class Operators {

    private final Path file;
    private final Set<String> names = new TreeSet<>();

    public Operators(Path file) throws IOException {
        this.file = file;
        if (Files.exists(file)) {
            Files.readAllLines(file, StandardCharsets.UTF_8).stream()
                    .map(line -> line.strip().toLowerCase(Locale.ROOT))
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .forEach(names::add);
        }
    }

    public boolean allows(CommandSender sender) {
        return sender instanceof ConsoleSender
                || sender instanceof Player player && names.contains(player.getUsername().toLowerCase(Locale.ROOT));
    }

    public boolean add(String name) throws IOException {
        boolean added = names.add(name.toLowerCase(Locale.ROOT));
        save();
        return added;
    }

    public boolean remove(String name) throws IOException {
        boolean removed = names.remove(name.toLowerCase(Locale.ROOT));
        save();
        return removed;
    }

    public Set<String> names() {
        return Set.copyOf(names);
    }

    private void save() throws IOException {
        Files.write(file, names, StandardCharsets.UTF_8);
    }
}
