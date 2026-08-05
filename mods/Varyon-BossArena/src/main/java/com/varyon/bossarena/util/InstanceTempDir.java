package com.varyon.bossarena.util;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Resolves temp asset-pack directories that are unique per server installation.
 *
 * <p>The pack roots are wiped and recreated on startup, so a fixed name under the shared system
 * temp dir lets two servers on the same host delete each other's assets while running. Suffixing
 * with a hash of the install directory keeps each instance isolated without redirecting
 * {@code java.io.tmpdir}, which would move every other mod's temp files too.
 */
public final class InstanceTempDir {

    private static volatile String cachedSuffix;

    private InstanceTempDir() {}

    /** Returns {@code <system temp>/<name>-<install hash>}. */
    public static Path resolve(String name) {
        return Path.of(System.getProperty("java.io.tmpdir"), name + "-" + instanceSuffix());
    }

    /**
     * Short stable token derived from the working directory, so the same installation reuses its
     * directory across restarts instead of accumulating orphans.
     */
    public static String instanceSuffix() {
        String suffix = cachedSuffix;
        if (suffix != null) {
            return suffix;
        }
        synchronized (InstanceTempDir.class) {
            if (cachedSuffix == null) {
                cachedSuffix = computeSuffix();
            }
            return cachedSuffix;
        }
    }

    private static String computeSuffix() {
        String installPath;
        try {
            installPath = Path.of("").toAbsolutePath().normalize().toString();
        } catch (RuntimeException e) {
            installPath = String.valueOf(System.getProperty("user.dir"));
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(installPath.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(12);
            for (int i = 0; i < 6; i++) {
                out.append(Character.forDigit((hash[i] >> 4) & 0xF, 16));
                out.append(Character.forDigit(hash[i] & 0xF, 16));
            }
            return out.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(installPath.hashCode());
        }
    }
}
