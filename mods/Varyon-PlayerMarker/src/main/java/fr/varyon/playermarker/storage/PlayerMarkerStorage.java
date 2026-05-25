package fr.varyon.playermarker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class PlayerMarkerStorage {

    private static final String PLUGIN_DATA_DIR = "Varyon-PlayerMarker";

    private static volatile boolean initialized;
    private static Path dataRoot;

    private PlayerMarkerStorage() {
    }

    static void init() {
        ensureInitialized();
    }

    static Path getDataRoot() {
        ensureInitialized();
        return dataRoot;
    }

    private static void ensureInitialized() {
        if (initialized) {
            return;
        }

        synchronized (PlayerMarkerStorage.class) {
            if (initialized) {
                return;
            }

            try {
                dataRoot = PlayerMarkerPaths.resolvePluginDataRoot(VaryonPlayerMarkerPlugin.class, PLUGIN_DATA_DIR);
                Files.createDirectories(dataRoot);
                initialized = true;
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to initialize Varyon-PlayerMarker storage", exception);
            }
        }
    }
}
