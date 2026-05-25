package fr.varyon.playermarker;

import com.hypixel.hytale.common.plugin.AuthorInfo;
import com.hypixel.hytale.common.plugin.PluginManifest;
import com.hypixel.hytale.common.semver.Semver;
import com.hypixel.hytale.protocol.packets.setup.RequestCommonAssetsRebuild;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.asset.common.CommonAssetModule;
import com.hypixel.hytale.server.core.asset.common.asset.FileCommonAsset;
import com.hypixel.hytale.server.core.universe.Universe;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

final class PlayerMarkerAssetPack {

    private static final String PACK_ID = "Varyon:Varyon-PlayerMarkerAssets";
    private static final String PACK_GROUP = "Varyon";
    private static final String PACK_NAME = "Varyon-PlayerMarkerAssets";
    private static final String PACK_VERSION = "1.0.0";
    private static final String TARGET_SERVER_VERSION = "2026.03.26-89796e57b";
    private static final String FALLBACK_MARKER_IMAGE = "vpm-placeholder.png";
    private static final String MARKER_ASSET_PREFIX = "UI/WorldMap/MapMarkers/";
    private static final String WORLDMAP_ASSET_PREFIX = "UI/WorldMap/";
    private static final String PACK_ICON_RESOURCE = "/asset-pack-icon-256.png";
    private static final String PACK_ICON_FILE = "icon-256.png";

    private static final String MANIFEST_JSON = """
            {
              "Group": "Varyon",
              "Name": "Varyon-PlayerMarkerAssets",
              "Version": "1.0.0",
              "Description": "Pack assets marqueurs joueurs (carte, minicarte, boussole)",
              "Authors": [
                {
                  "Name": "Varyon"
                }
              ],
              "ServerVersion": "2026.03.26-89796e57b",
              "Dependencies": {},
              "OptionalDependencies": {},
              "DisabledByDefault": false,
              "IncludesAssetPack": true
            }
            """;

    private static final ConcurrentHashMap<String, FileCommonAsset> pushedAssets = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Boolean> writtenAvatarAssets = new ConcurrentHashMap<>();
    private static volatile boolean initialized;
    private static volatile boolean registered;
    private static Path packRoot;

    private PlayerMarkerAssetPack() {}

    static void init() {
        ensureInitialized();
        registerPackIfNeeded();
    }

    static void writeAvatar(String slotImage, byte[] pngBytes) {
        ensureInitialized();
        registerPackIfNeeded();
        if (slotImage == null || pngBytes == null || pngBytes.length == 0) {
            return;
        }

        if (writtenAvatarAssets.containsKey(slotImage)) {
            return;
        }

        try {
            Path output = packRoot.resolve("Common/UI/WorldMap/MapMarkers").resolve(slotImage);
            boolean changed = writeBytesIfChanged(output, pngBytes);
            if (changed) {
                pushAssetToClients(MARKER_ASSET_PREFIX + slotImage, pngBytes, output);
            }
            writtenAvatarAssets.put(slotImage, Boolean.TRUE);
        } catch (IOException e) {
        }
    }

    private static void pushAssetToClients(String assetName, byte[] pngBytes, Path filePath) {
        try {
            CommonAssetModule cam = CommonAssetModule.get();
            if (cam == null) {
                return;
            }

            FileCommonAsset asset = new FileCommonAsset(filePath, assetName, pngBytes);
            cam.addCommonAsset(assetName, asset, true);
            pushedAssets.put(assetName, asset);
            Universe universe = Universe.get();
            if (universe != null && universe.getPlayerCount() > 0) {
                universe.broadcastPacketNoCache(new RequestCommonAssetsRebuild());
            }
        } catch (Exception e) {
        }
    }

    private static void ensureInitialized() {
        if (initialized) {
            return;
        }

        synchronized (PlayerMarkerAssetPack.class) {
            if (initialized) {
                return;
            }

            try {
                packRoot = PlayerMarkerPaths.resolveSiblingPackRoot(VaryonPlayerMarkerPlugin.class, PACK_NAME);

                Files.createDirectories(packRoot);
                Path manifestPath = packRoot.resolve("manifest.json");
                Files.writeString(manifestPath, MANIFEST_JSON, StandardCharsets.UTF_8);
                ensurePackIcon();
                ensureStaticWorldMapAssets();
                Path worldRoot = PlayerMarkerPaths.resolveWorldRoot(VaryonPlayerMarkerPlugin.class);
                if (worldRoot != null) {
                    ensurePackEnabled(worldRoot.resolve("config.json"));
                }

                registerPackIfNeeded();
                initialized = true;
            } catch (IOException e) {
                throw new IllegalStateException("Failed to initialize Varyon-PlayerMarker asset pack", e);
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

        assetModule.registerPack(PACK_ID, packRoot, buildRuntimeManifest(), true);
        registered = true;
    }

    private static void ensurePackIcon() {
        if (packRoot == null) {
            return;
        }

        try (var stream = PlayerMarkerAssetPack.class.getResourceAsStream(PACK_ICON_RESOURCE)) {
            if (stream == null) {
                return;
            }

            byte[] iconBytes = stream.readAllBytes();
            if (iconBytes.length == 0) {
                return;
            }

            writeBytesIfChanged(packRoot.resolve(PACK_ICON_FILE), iconBytes);
        } catch (IOException e) {
        }
    }

    private static void ensureStaticWorldMapAssets() {
        writeStaticCommonAsset(
                WORLDMAP_ASSET_PREFIX + "Player.png",
                packRoot.resolve("Common/UI/WorldMap/Player.png"),
                PlayerMarkerImageProcessor.createTransparentPng());
        writeStaticCommonAsset(
                MARKER_ASSET_PREFIX + "Player.png",
                packRoot.resolve("Common/UI/WorldMap/MapMarkers/Player.png"),
                PlayerMarkerImageProcessor.createTransparentPng());
        writeStaticCommonAsset(
                MARKER_ASSET_PREFIX + FALLBACK_MARKER_IMAGE,
                packRoot.resolve("Common/UI/WorldMap/MapMarkers").resolve(FALLBACK_MARKER_IMAGE),
                PlayerMarkerImageProcessor.createFallbackMarkerPng(64));
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
        } catch (IOException e) {
        }
    }

    private static PluginManifest buildRuntimeManifest() {
        PluginManifest manifest = new PluginManifest();
        manifest.setGroup(PACK_GROUP);
        manifest.setName(PACK_NAME);
        manifest.setVersion(Semver.fromString(PACK_VERSION));
        manifest.setDescription("Pack assets marqueurs joueurs (carte, minicarte, boussole)");
        manifest.setWebsite("");
        manifest.setServerVersion(TARGET_SERVER_VERSION);

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
        } catch (IOException e) {
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
}
