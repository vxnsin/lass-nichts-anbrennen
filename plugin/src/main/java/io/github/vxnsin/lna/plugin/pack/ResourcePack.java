package io.github.vxnsin.lna.plugin.pack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Holds the resource pack served to clients.
 *
 * <p>On startup the pack embedded in the plugin jar is extracted to {@code plugins/LassNichtsAnbrennen/pack.zip}.
 * {@link #reload()} re-reads that file, which allows swapping in a freshly built pack without a restart.
 */
public final class ResourcePack {

    private static final String EMBEDDED_PATH = "lna/pack.zip";

    private final Path file;
    private volatile byte[] data;
    private volatile String sha1;

    private ResourcePack(Path file) {
        this.file = file;
    }

    public static ResourcePack extract(JavaPlugin plugin) throws IOException {
        Path file = plugin.getDataFolder().toPath().resolve("pack.zip");
        Files.createDirectories(file.getParent());
        try (InputStream embedded = plugin.getResource(EMBEDDED_PATH)) {
            if (embedded == null) {
                throw new IOException(EMBEDDED_PATH + " is missing from the plugin jar. Build with Gradle.");
            }
            Files.copy(embedded, file, StandardCopyOption.REPLACE_EXISTING);
        }
        ResourcePack pack = new ResourcePack(file);
        pack.reload();
        return pack;
    }

    public void reload() throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        String hash = sha1(bytes);
        this.data = bytes;
        this.sha1 = hash;
    }

    public byte[] data() {
        return data;
    }

    public String sha1() {
        return sha1;
    }

    private static String sha1(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
