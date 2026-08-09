package fr.varyon.readablebooks.util;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.logger.HytaleLogger.Api;
import com.hypixel.hytale.server.core.Constants;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

public final class FileUtils {
    private static final HytaleLogger logger = HytaleLogger.getLogger().getSubLogger("ReadableBooks-Files");
    public static final Path MAIN_PATH = Constants.UNIVERSE_PATH.resolve("VaryonReadableBooks");
    public static final String BOOKS_PATH = MAIN_PATH.resolve("Books.json").toString();

    private FileUtils() {
    }

    public static void ensureDirectory(Path path) {
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            path.toFile().mkdirs();
        }
    }

    public static void ensureMainDirectory() {
        ensureDirectory(MAIN_PATH);
    }

    public static File ensureFile(String path, String defaultContent) {
        File file = new File(path);
        if (!file.exists()) {
            try {
                File parentDir = file.getParentFile();
                if (parentDir != null) {
                    parentDir.mkdirs();
                }

                Files.writeString(file.toPath(), defaultContent);
            } catch (IOException e) {
                ((Api) logger.at(Level.WARNING).withCause(e)).log("Failed to create file: " + path);
            }
        }

        return file;
    }
}
