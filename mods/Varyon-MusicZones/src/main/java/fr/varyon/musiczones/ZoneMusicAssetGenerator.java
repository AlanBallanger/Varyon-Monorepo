package fr.varyon.musiczones;

import com.hypixel.hytale.common.plugin.PluginManifest;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

final class ZoneMusicAssetGenerator {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    static final String PACK_ID = "fr.varyon:Varyon-MusicZones-generated";

    private ZoneMusicAssetGenerator() {}

    static void rebuildPack(JavaPlugin plugin, List<MusicZone> zones) throws IOException {
        Path packRoot = plugin.getDataDirectory().resolve("generated_pack");
        if (Files.exists(packRoot, LinkOption.NOFOLLOW_LINKS)) {
            deleteRecursive(packRoot);
        }
        Files.createDirectories(packRoot);
        ensurePackStubDirectories(packRoot);

        Path musicSrc = plugin.getDataDirectory().resolve("music");

        for (MusicZone zone : zones) {
            Path oggSource = resolveMusicFile(musicSrc, zone.getMusicFileName());
            if (!Files.isRegularFile(oggSource, LinkOption.NOFOLLOW_LINKS)) {
                LOGGER.atWarning().log("[MusicZones] OGG manquant pour la zone " + zone.getId() + " : " + zone.getMusicFileName());
                continue;
            }
            String ambId = zone.ambienceAssetId();
            Path oggDestDir = packRoot.resolve("Common").resolve("Music").resolve("VaryonMZ");
            Files.createDirectories(oggDestDir);
            Path oggDest = oggDestDir.resolve(zone.musicOggFileName());
            Files.copy(oggSource, oggDest, StandardCopyOption.REPLACE_EXISTING);

            Path ambPath = packRoot.resolve("Server")
                    .resolve("Audio")
                    .resolve("AmbienceFX")
                    .resolve("Music")
                    .resolve("Global");
            Files.createDirectories(ambPath);
            String ambJson = buildAmbienceFxJson(ambId, zone.musicCommonTrackPath());
            Files.writeString(ambPath.resolve(ambId + ".json"), ambJson, StandardCharsets.UTF_8);
        }

        AssetModule am = AssetModule.get();
        if (am == null) {
            LOGGER.atWarning().log("[MusicZones] AssetModule indisponible");
            return;
        }
        try {
            if (am.getAssetPack(PACK_ID) != null) {
                am.unregisterPack(PACK_ID);
            }
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[MusicZones] unregisterPack");
        }
        if (!zones.isEmpty()) {
            PluginManifest m = new PluginManifest();
            m.setGroup("fr.varyon");
            m.setName("Varyon-MusicZones-generated");
            m.setDescription("Pack généré — musiques de zone");
            PluginManifest pm = plugin.getManifest();
            if (pm != null && pm.getVersion() != null) {
                m.setVersion(pm.getVersion());
            }
            am.registerPack(PACK_ID, packRoot, m, true);
            am.initPendingStores();
            LOGGER.atInfo().log("[MusicZones] Pack enregistré, zones=" + zones.size());
        }
    }

    private static void ensurePackStubDirectories(Path packRoot) throws IOException {
        Files.createDirectories(packRoot.resolve("Server").resolve("NPC").resolve("Roles"));
    }

    private static Path resolveMusicFile(Path musicDir, String name) {
        if (name == null || name.isBlank()) {
            return musicDir.resolve("__invalid__");
        }
        String base = name.trim().replace('\\', '/');
        if (base.contains("/")) {
            base = base.substring(base.lastIndexOf('/') + 1);
        }
        final String stem = base.toLowerCase(Locale.ROOT).endsWith(".ogg") ? base.substring(0, base.length() - 4) : base;
        try (Stream<Path> stream = Files.list(musicDir)) {
            return stream.filter(Files::isRegularFile)
                    .filter(p -> matchOgg(stem, p.getFileName().toString()))
                    .findFirst()
                    .orElse(musicDir.resolve(name));
        } catch (Exception e) {
            return musicDir.resolve(name);
        }
    }

    private static boolean matchOgg(String stem, String fileName) {
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".ogg")) {
            return false;
        }
        if (fileName.equalsIgnoreCase(stem + ".ogg")) {
            return true;
        }
        String fnStem = fileName.substring(0, fileName.length() - 4);
        return fnStem.equalsIgnoreCase(stem);
    }

    private static String buildAmbienceFxJson(String ambienceId, String commonMusicOggPathUnderCommon) {
        return "{\n"
                + "  \"Id\": \""
                + ambienceId
                + "\",\n"
                + "  \"Music\": {\n"
                + "    \"Tracks\": [\n"
                + "      \""
                + commonMusicOggPathUnderCommon
                + "\"\n"
                + "    ],\n"
                + "    \"Volume\": 1.0\n"
                + "  },\n"
                + "  \"Priority\": 100,\n"
                + "  \"AudioCategory\": \"AudioCat_Music\"\n"
                + "}\n";
    }

    private static void deleteRecursive(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {
                        }
                    });
        }
    }
}
