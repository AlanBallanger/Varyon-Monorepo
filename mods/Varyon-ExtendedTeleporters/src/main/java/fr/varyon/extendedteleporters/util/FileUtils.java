package fr.varyon.extendedteleporters.util;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.logger.HytaleLogger.Api;
import com.hypixel.hytale.server.core.Constants;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

public final class FileUtils {
   private static final HytaleLogger logger = HytaleLogger.getLogger().getSubLogger("ExtendedTeleport-Files");
   public static final Path MAIN_PATH = Constants.UNIVERSE_PATH.resolve("VaryonExtendedTeleporters");
   public static final String TELEPORTERS_PATH = MAIN_PATH.resolve("Teleporters.json").toString();
   public static final String CONFIG_PATH = MAIN_PATH.resolve("config.json").toString();
   public static final String CUSTOM_DESTINATIONS_PATH = MAIN_PATH.resolve("CustomDestinations.json").toString();

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
            ((Api)logger.at(Level.WARNING).withCause(e)).log("Failed to create file: " + path);
         }
      }

      return file;
   }
}
