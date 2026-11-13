package dev.vensin.lna.server.pack;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public record ResourcePack(byte[] data, String sha1) {

    public static ResourcePack of(byte[] data) {
        try {
            return new ResourcePack(data, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(data)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
