package fr.varyon.vrpg.config;

import com.hypixel.hytale.logger.HytaleLogger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ReferenceTomlInstaller {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    public static final String CONFIG_DIR = "config";
    private static final String RESOURCE_PREFIX = CONFIG_DIR + "/";

    private ReferenceTomlInstaller() {}

    public static Path configDir(Path dataDir) {
        return dataDir.resolve(CONFIG_DIR);
    }

    public static Path ensureInstalled(Path dataDir, String fileName) {
        Path configDir = configDir(dataDir);
        Path file = configDir.resolve(fileName);
        if (Files.isRegularFile(file)) {
            return file;
        }
        Path legacy = dataDir.resolve(fileName);
        if (Files.isRegularFile(legacy)) {
            try {
                Files.createDirectories(configDir);
                Files.move(legacy, file);
                LOGGER.atInfo().log("[VaryonRPG] " + fileName + " déplacé vers config/");
                return file;
            } catch (IOException e) {
                LOGGER.atWarning().withCause(e).log("[VaryonRPG] Impossible de déplacer " + fileName + " vers config/");
                return legacy;
            }
        }
        try (InputStream in = ReferenceTomlInstaller.class.getClassLoader().getResourceAsStream(RESOURCE_PREFIX + fileName)) {
            if (in != null) {
                Files.createDirectories(configDir);
                Files.copy(in, file);
                LOGGER.atInfo().log("[VaryonRPG] " + fileName + " installé dans config/");
            }
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] Impossible d'installer " + fileName + " dans config/");
        }
        return file;
    }

    public static Path ensureInstalledAtRoot(Path dataDir, String fileName) {
        Path file = dataDir.resolve(fileName);
        if (Files.isRegularFile(file)) return file;
        try (InputStream in = ReferenceTomlInstaller.class.getClassLoader()
                .getResourceAsStream(fileName)) {
            if (in != null) {
                Files.createDirectories(dataDir);
                Files.copy(in, file);
                LOGGER.atInfo().log("[VaryonRPG] " + fileName + " installé à la racine");
            }
        } catch (IOException e) {
            LOGGER.atWarning().withCause(e).log("[VaryonRPG] Impossible d'installer " + fileName);
        }
        return file;
    }

    public static InputStream openClasspathResource(String fileName) {
        return ReferenceTomlInstaller.class.getClassLoader().getResourceAsStream(RESOURCE_PREFIX + fileName);
    }
}
