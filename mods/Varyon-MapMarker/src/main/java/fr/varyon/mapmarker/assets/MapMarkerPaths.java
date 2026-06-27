package fr.varyon.mapmarker.assets;

import fr.varyon.mapmarker.VaryonMapMarkerPlugin;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

final class MapMarkerPaths {

    private MapMarkerPaths() {
    }

    static Path resolvePluginLocation() {
        try {
            return Paths.get(VaryonMapMarkerPlugin.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI());
        } catch (URISyntaxException exception) {
            throw new IllegalStateException("Failed to resolve Varyon-MapMarker plugin location", exception);
        }
    }

    static Path resolveModsDirectory() {
        Path pluginLocation = resolvePluginLocation();
        return Files.isDirectory(pluginLocation)
                ? pluginLocation
                : pluginLocation.getParent();
    }

    static Path resolveWorldRoot() {
        Path modsDirectory = resolveModsDirectory();
        return modsDirectory != null ? modsDirectory.getParent() : null;
    }

    static Path resolveSiblingPackRoot(String packDirectoryName) {
        Path modsDirectory = resolveModsDirectory();
        if (modsDirectory == null) {
            throw new IllegalStateException("Failed to resolve mods directory for Varyon-MapMarker");
        }
        return modsDirectory.resolve(packDirectoryName);
    }
}
