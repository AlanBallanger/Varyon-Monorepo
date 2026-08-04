package fr.varyon.mapmarker.assets;

import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.common.plugin.AuthorInfo;
import com.hypixel.hytale.common.plugin.PluginManifest;
import com.hypixel.hytale.common.semver.Semver;
import com.hypixel.hytale.common.semver.SemverRange;
import com.hypixel.hytale.protocol.packets.setup.RequestCommonAssetsRebuild;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.asset.common.CommonAsset;
import com.hypixel.hytale.server.core.asset.common.CommonAssetModule;
import com.hypixel.hytale.server.core.universe.Universe;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public final class MapMarkerAssetPack {

    public static final String PACK_ID = "Varyon:Varyon-MapMarkerAssets";
    public static final String FALLBACK_MARKER_IMAGE = "vmm-placeholder.png";
    private static final String PACK_GROUP = "Varyon";
    private static final String PACK_NAME = "Varyon-MapMarkerAssets";
    private static final String PACK_VERSION = "1.0.0";
    private static final String TARGET_SERVER_VERSION = "0.5.0";
    private static final String WORLD_MAP_MARKER_PREFIX = "UI/WorldMap/MapMarkers/";
    private static final String ICON_FILE_PREFIX = "vmm-";
    private static final int MARKER_ICON_SIZE = 64;

    private static final String MANIFEST_JSON = """
            {
              "Group": "Varyon",
              "Name": "Varyon-MapMarkerAssets",
              "Version": "1.0.0",
              "Description": "Pack assets marqueurs carte Varyon-MapMarker",
              "Authors": [
                {
                  "Name": "Varyon"
                }
              ],
              "ServerVersion": "0.5.0",
              "Dependencies": {},
              "OptionalDependencies": {},
              "DisabledByDefault": false,
              "IncludesAssetPack": true
            }
            """;

    private static final ConcurrentHashMap<String, Path> publishedPackFiles = new ConcurrentHashMap<>();
    private static volatile boolean initialized;
    private static volatile boolean registered;
    private static Path packRoot;

    private MapMarkerAssetPack() {
    }

    public static void init() {
        ensureInitialized();
        registerPackIfNeeded();
    }

    public static String toUiAssetPath(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return null;
        }
        return WORLD_MAP_MARKER_PREFIX + toPackFileName(fileName);
    }

    public static Path publishMarkerImage(String fileName, byte[] pngBytes) {
        ensureInitialized();
        registerPackIfNeeded();
        if (fileName == null || fileName.isBlank() || pngBytes == null || pngBytes.length == 0) {
            return null;
        }

        String packFileName = toPackFileName(fileName);
        byte[] normalizedPng = MapMarkerImageHelper.normalizeForMapMarker(pngBytes, MARKER_ICON_SIZE);
        try {
            Path worldMapOutput = packRoot.resolve("Common/UI/WorldMap/MapMarkers").resolve(packFileName);
            boolean worldChanged = writeBytesIfChanged(worldMapOutput, normalizedPng);
            if (!worldChanged) {
                Path existing = publishedPackFiles.get(WORLD_MAP_MARKER_PREFIX + packFileName);
                if (existing != null && Files.exists(existing)) {
                    return existing;
                }
            }
            pushAssetToClients(WORLD_MAP_MARKER_PREFIX + packFileName, normalizedPng, worldMapOutput);
            publishedPackFiles.put(WORLD_MAP_MARKER_PREFIX + packFileName, worldMapOutput);
            return worldMapOutput;
        } catch (IOException ignored) {
            return null;
        }
    }

    private static void pushAssetToClients(String assetName, byte[] pngBytes, Path filePath) {
        try {
            CommonAssetModule cam = CommonAssetModule.get();
            if (cam == null) {
                return;
            }

            CommonAsset asset = new MapMarkerRuntimePngAsset(assetName, pngBytes);
            cam.addCommonAsset(assetName, asset, true);
            publishedPackFiles.put(assetName, filePath);

            Universe universe = Universe.get();
            if (universe != null && universe.getPlayerCount() > 0) {
                universe.broadcastPacketNoCache(new RequestCommonAssetsRebuild());
            }
        } catch (Exception ignored) {
        }
    }

    private static void ensureInitialized() {
        if (initialized) {
            return;
        }

        synchronized (MapMarkerAssetPack.class) {
            if (initialized) {
                return;
            }

            try {
                packRoot = MapMarkerPaths.resolveSiblingPackRoot(PACK_NAME);
                Files.createDirectories(packRoot);
                Files.writeString(packRoot.resolve("manifest.json"), MANIFEST_JSON, StandardCharsets.UTF_8);
                ensureStaticWorldMapAssets();
                cleanupLegacyHudAssets();
                cleanupLegacyPackMarkerAssets();
                Path worldRoot = MapMarkerPaths.resolveWorldRoot();
                if (worldRoot != null) {
                    ensurePackEnabled(worldRoot.resolve("config.json"));
                }
                registerPackIfNeeded();
                initialized = true;
            } catch (IOException e) {
                throw new IllegalStateException("Failed to initialize Varyon-MapMarker asset pack", e);
            }
        }
    }

    private static void registerPackIfNeeded() {
        if (registered) {
            return;
        }

        AssetModule assetModule = AssetModule.get();
        if (assetModule == null) {
            return;
        }

        if (assetModule.getAssetPack(PACK_ID) != null) {
            registered = true;
            return;
        }

        assetModule.registerPack(PACK_ID, packRoot, buildRuntimeManifest(), AssetPack.PackSource.MODS);
        assetModule.initPendingStores();
        registered = true;
    }

    private static void ensureStaticWorldMapAssets() {
        byte[] fallback = MapMarkerImageHelper.createFallbackMarkerPng(MARKER_ICON_SIZE);
        writeStaticCommonAsset(
                WORLD_MAP_MARKER_PREFIX + FALLBACK_MARKER_IMAGE,
                packRoot.resolve("Common/UI/WorldMap/MapMarkers").resolve(FALLBACK_MARKER_IMAGE),
                fallback);
    }

    private static void cleanupLegacyHudAssets() {
        Path legacyHudDir = packRoot.resolve("Common/UI/MapMarkers");
        if (!Files.isDirectory(legacyHudDir)) {
            return;
        }
        try (var stream = Files.list(legacyHudDir)) {
            stream.filter(Files::isRegularFile).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
        try {
            Files.deleteIfExists(legacyHudDir);
        } catch (IOException ignored) {
        }
    }

    private static void cleanupLegacyPackMarkerAssets() {
        Path markersDir = packRoot.resolve("Common/UI/WorldMap/MapMarkers");
        if (!Files.isDirectory(markersDir)) {
            return;
        }
        try (var stream = Files.list(markersDir)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
                        return name.endsWith(".png") && !name.startsWith(ICON_FILE_PREFIX);
                    })
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
    }

    private static void writeStaticCommonAsset(String assetName, Path output, byte[] pngBytes) {
        if (pngBytes == null || pngBytes.length == 0) {
            return;
        }

        try {
            if (!writeBytesIfChanged(output, pngBytes)) {
                return;
            }
            pushAssetToClients(assetName, pngBytes, output);
        } catch (IOException ignored) {
        }
    }

    private static PluginManifest buildRuntimeManifest() {
        PluginManifest manifest = new PluginManifest();
        manifest.setGroup(PACK_GROUP);
        manifest.setName(PACK_NAME);
        manifest.setVersion(Semver.fromString(PACK_VERSION));
        manifest.setDescription("Pack assets marqueurs carte Varyon-MapMarker");
        manifest.setWebsite("");
        manifest.setServerVersion(SemverRange.fromString(TARGET_SERVER_VERSION));

        AuthorInfo author = new AuthorInfo();
        author.setName("Varyon");
        manifest.setAuthors(List.of(author));
        return manifest;
    }

    private static void ensurePackEnabled(Path configPath) {
        if (!Files.exists(configPath)) {
            return;
        }

        try {
            String json = Files.readString(configPath, StandardCharsets.UTF_8);
            String updated = json;

            if (updated.contains('"' + PACK_ID + '"')) {
                updated = updated.replaceAll(
                        "(\\\"" + java.util.regex.Pattern.quote(PACK_ID) + "\\\"\\s*:\\s*\\{\\s*\\\"Enabled\\\"\\s*:\\s*)false",
                        "$1true");
            } else if (updated.contains("\"Mods\": {")) {
                updated = updated.replace(
                        "\"Mods\": {",
                        "\"Mods\": {\n    \"" + PACK_ID + "\": {\n      \"Enabled\": true\n    },");
            }

            if (!updated.equals(json)) {
                Files.writeString(configPath, updated, StandardCharsets.UTF_8);
            }
        } catch (IOException ignored) {
        }
    }

    private static boolean writeBytesIfChanged(Path output, byte[] pngBytes) throws IOException {
        Files.createDirectories(output.getParent());
        if (Files.exists(output) && java.util.Arrays.equals(Files.readAllBytes(output), pngBytes)) {
            return false;
        }
        Files.write(output, pngBytes);
        return true;
    }

    public static String normalizeIconFileName(String fileName) {
        return toPackFileName(fileName);
    }

    public static byte[] normalizeMarkerPng(byte[] pngBytes) {
        return MapMarkerImageHelper.normalizeForMapMarker(pngBytes, MARKER_ICON_SIZE);
    }

    private static String toPackFileName(String fileName) {
        String normalized = normalizeFileName(fileName);
        if (normalized.isEmpty()) {
            return normalized;
        }
        String lower = normalized.toLowerCase(java.util.Locale.ROOT);
        if (lower.startsWith(ICON_FILE_PREFIX)) {
            return normalized;
        }
        return ICON_FILE_PREFIX + normalized;
    }

    private static String normalizeFileName(String fileName) {
        String normalized = fileName.trim().replace('\\', '/');
        if (normalized.contains("/")) {
            normalized = normalized.substring(normalized.lastIndexOf('/') + 1);
        }
        return normalized;
    }
}
