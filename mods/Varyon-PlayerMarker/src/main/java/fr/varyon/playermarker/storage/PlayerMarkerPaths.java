package fr.varyon.playermarker;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

final class PlayerMarkerPaths {

    private PlayerMarkerPaths() {
    }

    static Path resolvePluginLocation(Class<?> anchorType) {
        try {
            return Paths.get(anchorType.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI());
        } catch (URISyntaxException exception) {
            throw new IllegalStateException("Failed to resolve Varyon-PlayerMarker plugin location", exception);
        }
    }

    static Path resolveModsDirectory(Class<?> anchorType) {
        Path pluginLocation = resolvePluginLocation(anchorType);
        return Files.isDirectory(pluginLocation)
                ? pluginLocation
                : pluginLocation.getParent();
    }

    static Path resolveWorldRoot(Class<?> anchorType) {
        Path modsDirectory = resolveModsDirectory(anchorType);
        return modsDirectory != null ? modsDirectory.getParent() : null;
    }

    static Path resolvePluginDataRoot(Class<?> anchorType, String pluginDirectoryName) {
        Path worldRoot = resolveWorldRoot(anchorType);
        return worldRoot == null
                ? Paths.get(pluginDirectoryName)
                : worldRoot.resolve("plugins").resolve(pluginDirectoryName);
    }

    static Path resolveSiblingPackRoot(Class<?> anchorType, String packDirectoryName) {
        Path modsDirectory = resolveModsDirectory(anchorType);
        if (modsDirectory == null) {
            throw new IllegalStateException("Failed to resolve mods directory for Varyon-PlayerMarker");
        }
        return modsDirectory.resolve(packDirectoryName);
    }
}

