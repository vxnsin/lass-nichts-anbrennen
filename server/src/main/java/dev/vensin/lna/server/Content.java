package dev.vensin.lna.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Stream;

/** Reads bundled content; files in the override folder next to the server win over the bundled ones. */
public final class Content {

    private final Path overrides;

    public Content(Path overrides) {
        this.overrides = overrides;
    }

    public InputStream open(String path) throws IOException {
        Path file = overrides.resolve(path);
        if (Files.isRegularFile(file)) {
            return Files.newInputStream(file);
        }
        InputStream bundled = Content.class.getClassLoader().getResourceAsStream(path);
        if (bundled == null) {
            throw new IOException(path + " is missing");
        }
        return bundled;
    }

    public byte[] read(String path) throws IOException {
        try (InputStream in = open(path)) {
            return in.readAllBytes();
        }
    }

    public List<String> list(String directory, String suffix) {
        TreeSet<String> names = new TreeSet<>();
        try {
            URL url = Content.class.getClassLoader().getResource(directory);
            if (url != null) {
                URI uri = url.toURI();
                if ("jar".equals(uri.getScheme())) {
                    try (FileSystem jar = FileSystems.newFileSystem(uri, Map.of())) {
                        collect(jar.getPath(directory), suffix, names);
                    }
                } else {
                    collect(Path.of(uri), suffix, names);
                }
            }
            collect(overrides.resolve(directory), suffix, names);
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException("Cannot read folder " + directory, e);
        }
        return List.copyOf(names);
    }

    private static void collect(Path directory, String suffix, TreeSet<String> names) throws IOException {
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (Stream<Path> files = Files.list(directory)) {
            files.map(file -> file.getFileName().toString()).filter(name -> name.endsWith(suffix)).forEach(names::add);
        }
    }
}
