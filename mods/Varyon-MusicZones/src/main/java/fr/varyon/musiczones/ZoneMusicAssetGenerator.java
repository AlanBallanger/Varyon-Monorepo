package fr.varyon.musiczones;

import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.common.plugin.PluginManifest;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.asset.common.CommonAssetModule;
import com.hypixel.hytale.server.core.asset.type.ambiencefx.config.AmbienceFX;
import com.hypixel.hytale.server.core.asset.type.musiccontainer.config.MusicContainer;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

final class ZoneMusicAssetGenerator {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    static final String PACK_ID = "fr.varyon:Varyon-MusicZones-generated";

    private ZoneMusicAssetGenerator() {}

    static void rebuildPack(JavaPlugin plugin, Path packRoot, List<MusicZone> zones) throws IOException {

        Path oggDestDir = packRoot.resolve("Common").resolve("Music").resolve("VaryonMZ");
        Path ambDestDir = packRoot.resolve("Server").resolve("Audio").resolve("AmbienceFX").resolve("Music").resolve("Global");
        Path mcDestDir = packRoot.resolve("Server").resolve("Audio").resolve("MusicContainer").resolve("VaryonMZ");
        Files.createDirectories(oggDestDir);
        Files.createDirectories(ambDestDir);
        Files.createDirectories(mcDestDir);
        ensurePackStubDirectories(packRoot);

        Path musicSrc = plugin.getDataDirectory().resolve("music");

        Set<String> expectedOgg = new HashSet<>();
        Set<String> expectedAmb = new HashSet<>();
        Set<String> expectedMc = new HashSet<>();

        for (MusicZone zone : zones) {
            Path oggSource = resolveMusicFile(musicSrc, zone.getMusicFileName());
            if (!Files.isRegularFile(oggSource, LinkOption.NOFOLLOW_LINKS)) {
                LOGGER.atWarning().log("[MusicZones] OGG manquant pour la zone " + zone.getId() + " : " + zone.getMusicFileName());
                continue;
            }
            String oggFileName = zone.musicOggFileName();
            Files.copy(oggSource, oggDestDir.resolve(oggFileName), StandardCopyOption.REPLACE_EXISTING);
            expectedOgg.add(oggFileName);

            String mcId = zone.musicContainerId();
            String mcFileName = mcId + ".json";
            Files.writeString(mcDestDir.resolve(mcFileName), buildMusicContainerJson(zone.musicCommonTrackPath()), StandardCharsets.UTF_8);
            expectedMc.add(mcFileName);

            String ambId = zone.ambienceAssetId();
            String ambFileName = ambId + ".json";
            Files.writeString(ambDestDir.resolve(ambFileName), buildAmbienceFxJson(mcId), StandardCharsets.UTF_8);
            expectedAmb.add(ambFileName);
        }

        deleteStaleFiles(oggDestDir, expectedOgg);
        deleteStaleFiles(ambDestDir, expectedAmb);
        deleteStaleFiles(mcDestDir, expectedMc);

        AssetModule am = AssetModule.get();
        if (am == null) {
            LOGGER.atWarning().log("[MusicZones] AssetModule indisponible");
            return;
        }

        AssetPack pack;
        if (am.getAssetPack(PACK_ID) == null) {
            LOGGER.atInfo().log("[MusicZones] registerPack...");
            PluginManifest m = new PluginManifest();
            m.setGroup("fr.varyon");
            m.setName("Varyon-MusicZones-generated");
            m.setDescription("Pack généré — musiques de zone");
            PluginManifest pm = plugin.getManifest();
            if (pm != null && pm.getVersion() != null) {
                m.setVersion(pm.getVersion());
            }
            am.registerPack(PACK_ID, packRoot, m, AssetPack.PackSource.MODS);
            am.initPendingStores();
            pack = am.getAssetPack(PACK_ID);
            LOGGER.atInfo().log("[MusicZones] Pack enregistré, zones=" + zones.size());
        } else {
            pack = am.getAssetPack(PACK_ID);
            LOGGER.atInfo().log("[MusicZones] Pack déjà enregistré, fichiers mis à jour en place, zones=" + zones.size());
        }

        // registerPack() ne déclenche loadCommonAssets() (indexation synchrone des .ogg dans
        // CommonAssetRegistry) qu'au tout premier enregistrement du pack via AssetPackRegisterEvent.
        // Sur la branche "pack déjà enregistré" (création/suppression de zone après le boot), cet
        // event ne refire jamais : sans ce réappel explicite, seul le file watcher asynchrone finit
        // par indexer les .ogg (~3s plus tard), trop tard pour la validation immédiate du
        // MusicContainer qui suit, qui échoue alors avec "Common Asset ... doesn't exist".
        if (pack != null) {
            try {
                CommonAssetModule.get().loadCommonAssets(pack, System.currentTimeMillis());
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("[MusicZones] échec rechargement des Common Assets (ogg)");
            }
        }

        // initPendingStores() ne recharge que les AssetStore créés APRES le boot.
        // Les stores AmbienceFX/MusicContainer existent déjà au démarrage du serveur,
        // il faut donc forcer leur (re)chargement explicitement, MusicContainer avant AmbienceFX
        // (AmbienceFX résout et met en cache l'index de son MusicContainer à son propre chargement).
        try {
            var mcResult = MusicContainer.getAssetStore().loadAssetsFromDirectory(PACK_ID, mcDestDir);
            LOGGER.atInfo().log("[MusicZones] MusicContainer rechargés depuis " + mcDestDir + " -> " + mcResult);
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[MusicZones] échec rechargement MusicContainer");
        }
        try {
            var ambResult = AmbienceFX.getAssetStore().loadAssetsFromDirectory(PACK_ID, ambDestDir);
            LOGGER.atInfo().log("[MusicZones] AmbienceFX rechargés depuis " + ambDestDir + " -> " + ambResult);
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[MusicZones] échec rechargement AmbienceFX");
        }
    }

    private static void ensurePackStubDirectories(Path root) throws IOException {
        Files.createDirectories(root.resolve("Server").resolve("NPC").resolve("Roles"));
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

    private static String buildMusicContainerJson(String commonTrackPath) {
        return "{\n"
                + "  \"Type\": \"SingleTrack\",\n"
                + "  \"Track\": \""
                + commonTrackPath
                + "\",\n"
                + "  \"LoopCount\": 0,\n"
                + "  \"ResumeMemoryDuration\": 0\n"
                + "}\n";
    }

    private static String buildAmbienceFxJson(String musicContainerId) {
        return "{\n"
                + "  \"MusicContainer\": \""
                + musicContainerId
                + "\",\n"
                + "  \"Priority\": 0,\n"
                + "  \"AudioCategory\": \"AudioCat_Music\",\n"
                + "  \"Conditions\": { \"Never\": true }\n"
                + "}\n";
    }
}
