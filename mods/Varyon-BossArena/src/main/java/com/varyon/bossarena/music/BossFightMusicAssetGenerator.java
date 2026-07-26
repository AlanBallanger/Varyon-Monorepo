package com.varyon.bossarena.music;

import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.common.plugin.PluginManifest;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.varyon.bossarena.BossArenaPlugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Dedicated music-only asset pack.
 * Music containers must live under {@code Server/Audio/MusicContainers} (plural) — matching vanilla assets.
 */
final class BossFightMusicAssetGenerator {

    static final String PACK_ID = "com.varyon:Varyon-BossArena-music";

    private static final Logger LOGGER = Logger.getLogger("Varyon-BossArena");
    private static volatile Path packRoot;

    private BossFightMusicAssetGenerator() {}

    static synchronized void rebuildPack(Path musicSrc) throws IOException {
        if (packRoot == null || !Files.isDirectory(packRoot)) {
            packRoot = Files.createTempDirectory("VaryonBossArenaMusic");
            packRoot.toFile().deleteOnExit();
            LOGGER.info("[BossArena] music pack dir=" + packRoot);
        }

        Path oggDestDir = packRoot.resolve("Common").resolve("Music").resolve("VaryonBA");
        // Vanilla path is MusicContainers (plural), not MusicContainer.
        Path ambDestDir = packRoot.resolve("Server").resolve("Audio").resolve("AmbienceFX").resolve("Music").resolve("Global");
        Path mcDestDir = packRoot.resolve("Server").resolve("Audio").resolve("MusicContainers").resolve("VaryonBA");
        Files.createDirectories(oggDestDir);
        Files.createDirectories(ambDestDir);
        Files.createDirectories(mcDestDir);
        Files.createDirectories(packRoot.resolve("Server").resolve("NPC").resolve("Roles"));

        // Remove legacy misspelled folder from older builds so it cannot confuse hot-reload.
        deleteTreeQuietly(packRoot.resolve("Server").resolve("Audio").resolve("MusicContainer"));

        Set<String> expectedOgg = new HashSet<>();
        Set<String> expectedAmb = new HashSet<>();
        Set<String> expectedMc = new HashSet<>();

        if (Files.isDirectory(musicSrc, LinkOption.NOFOLLOW_LINKS)) {
            try (Stream<Path> stream = Files.list(musicSrc)) {
                stream.filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".ogg"))
                        .forEach(oggSource -> {
                            try {
                                String fileName = oggSource.getFileName().toString();
                                // One physical copy per restart slot so client resume memory
                                // keyed by track path cannot reuse progress across restarts.
                                for (int slot = 0; slot < BossFightMusicIds.RESTART_SLOTS; slot++) {
                                    String oggFileName = BossFightMusicIds.musicOggFileName(fileName, slot);
                                    Files.copy(
                                            oggSource,
                                            oggDestDir.resolve(oggFileName),
                                            StandardCopyOption.REPLACE_EXISTING);
                                    expectedOgg.add(oggFileName);

                                    String trackPath = BossFightMusicIds.musicCommonTrackPath(fileName, slot);
                                    String mcId = BossFightMusicIds.musicContainerId(fileName, slot);
                                    String mcFileName = mcId + ".json";
                                    Files.writeString(
                                            mcDestDir.resolve(mcFileName),
                                            buildMusicContainerJson(trackPath),
                                            StandardCharsets.UTF_8);
                                    expectedMc.add(mcFileName);

                                    String ambId = BossFightMusicIds.ambienceAssetId(fileName, slot);
                                    String ambFileName = ambId + ".json";
                                    Files.writeString(
                                            ambDestDir.resolve(ambFileName),
                                            buildAmbienceFxJson(mcId),
                                            StandardCharsets.UTF_8);
                                    expectedAmb.add(ambFileName);
                                }
                            } catch (IOException e) {
                                LOGGER.warning("[BossArena] music pack fail: " + oggSource + " — " + e.getMessage());
                            }
                        });
            }
        }

        deleteStaleFiles(oggDestDir, expectedOgg);
        deleteStaleFiles(ambDestDir, expectedAmb);
        deleteStaleFiles(mcDestDir, expectedMc);

        AssetModule am = AssetModule.get();
        if (am == null) {
            LOGGER.warning("[BossArena] AssetModule indisponible pour recharger la musique");
            return;
        }

        boolean alreadyRegistered = am.getAssetPack(PACK_ID) != null;
        if (expectedOgg.isEmpty() && !alreadyRegistered) {
            LOGGER.info("[BossArena] Aucun .ogg — music pack non enregistré");
            return;
        }

        if (!alreadyRegistered) {
            PluginManifest m = new PluginManifest();
            m.setGroup("com.varyon");
            m.setName("Varyon-BossArena-music");
            m.setDescription("Pack genere - musiques de combat BossArena");
            BossArenaPlugin plugin = BossArenaPlugin.getInstance();
            if (plugin != null && plugin.getManifest() != null && plugin.getManifest().getVersion() != null) {
                m.setVersion(plugin.getManifest().getVersion());
            }
            am.registerPack(PACK_ID, packRoot, m, AssetPack.PackSource.MODS);
            am.initPendingStores();
            LOGGER.info("[BossArena] Music pack enregistré, tracks=" + expectedOgg.size() + " root=" + packRoot);
        } else {
            am.initPendingStores();
            LOGGER.info("[BossArena] Music pack mis à jour, tracks=" + expectedOgg.size() + " root=" + packRoot);
        }
    }

    private static void deleteTreeQuietly(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }

    private static void deleteStaleFiles(Path dir, Set<String> expected) throws IOException {
        if (!Files.isDirectory(dir, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> !expected.contains(p.getFileName().toString()))
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {
                        }
                    });
        }
    }

    private static String buildMusicContainerJson(String commonTrackPath) {
        return "{\n"
                + "  \"Type\": \"SingleTrack\",\n"
                + "  \"Track\": \""
                + commonTrackPath
                + "\",\n"
                + "  \"LoopCount\": 0,\n"
                + "  \"ResumeMemoryDuration\": 0,\n"
                + "  \"AudioCategory\": \"AudioCat_Music\"\n"
                + "}\n";
    }

    private static String buildAmbienceFxJson(String musicContainerId) {
        // Conditions never match in-world — only ForcedMusic may play this track.
        // Without this, Priority 100 AmbienceFX can keep playing as global ambient
        // after leaving the fight radius / ending the event.
        return "{\n"
                + "  \"MusicContainer\": \""
                + musicContainerId
                + "\",\n"
                + "  \"Priority\": 100,\n"
                + "  \"AudioCategory\": \"AudioCat_Music\",\n"
                + "  \"Conditions\": {\n"
                + "    \"EnvironmentIds\": [\"Unknown\"]\n"
                + "  }\n"
                + "}\n";
    }
}
