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

    // Mise à jour légère : ne réécrit que les JSON MusicContainer + AmbienceFX et recharge ces
    // deux stores. Ne touche pas aux .ogg, ne rappelle ni registerPack() ni loadCommonAssets()
    // (c'est ce dernier qui prend le lock lourd du CommonAssetRegistry et peut figer le tick si
    // le file-watcher recharge en parallèle). À utiliser quand seuls des champs du MusicContainer
    // changent — p.ex. le Volume via /mz intensity — pas la liste des zones ni leurs fichiers.
    static void rebuildContainersOnly(Path packRoot, List<MusicZone> zones) throws IOException {
        Path ambDestDir = packRoot.resolve("Server").resolve("Audio").resolve("AmbienceFX").resolve("Music").resolve("Global");
        Path mcDestDir = packRoot.resolve("Server").resolve("Audio").resolve("MusicContainer").resolve("VaryonMZ");
        Files.createDirectories(ambDestDir);
        Files.createDirectories(mcDestDir);

        for (MusicZone zone : zones) {
            String mcId = zone.musicContainerId();
            writeAtomic(mcDestDir.resolve(mcId + ".json"), buildMusicContainerJson(zone.musicCommonTrackPath(), zone.getVolumeDb()));
            writeAtomic(
                    ambDestDir.resolve(zone.ambienceAssetId() + ".json"),
                    buildAmbienceFxJson(mcId));
        }

        try {
            var mcResult = MusicContainer.getAssetStore().loadAssetsFromDirectory(PACK_ID, mcDestDir);
            LOGGER.atInfo().log("[MusicZones] (light) MusicContainer rechargés -> " + mcResult);
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[MusicZones] (light) échec rechargement MusicContainer");
        }
        try {
            var ambResult = AmbienceFX.getAssetStore().loadAssetsFromDirectory(PACK_ID, ambDestDir);
            LOGGER.atInfo().log("[MusicZones] (light) AmbienceFX rechargés -> " + ambResult);
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[MusicZones] (light) échec rechargement AmbienceFX");
        }
    }

    static void rebuildPack(JavaPlugin plugin, Path packRoot, List<MusicZone> zones) throws IOException {
        rebuildPack(plugin, packRoot, zones, false);
    }

    // pruneOrphans : true UNIQUEMENT au démarrage serveur (packRoot est un temp dir neuf, aucun
    // client connecté). À chaud c'est TOUJOURS false : supprimer un .ogg / MC / AmbienceFX du
    // pack fait envoyer un paquet AssetUpdate de suppression aux clients connectés ; si l'un
    // d'eux a encore un MusicContainer qui référence la piste retirée, il tente de la relire,
    // ne la trouve pas -> KeyNotFoundException NON catché -> crash client, MÊME pour un joueur
    // hors de la zone modifiée. En add-only, les assets orphelins restent chargés jusqu'au
    // prochain reboot (quelques Mo, sans effet de bord).
    static void rebuildPack(JavaPlugin plugin, Path packRoot, List<MusicZone> zones, boolean pruneOrphans)
            throws IOException {

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

        // Copier TOUS les .ogg de music/ dans le pack, pas seulement ceux référencés par une
        // zone. Ainsi une clé .ogg n'a jamais besoin d'être ajoutée à chaud quand une zone
        // change de musique : le fichier est déjà là. (Le contenu d'une clé donnée ne change
        // jamais non plus, puisque la clé dérive du nom de fichier source.)
        if (Files.isDirectory(musicSrc, LinkOption.NOFOLLOW_LINKS)) {
            try (Stream<Path> stream = Files.list(musicSrc)) {
                for (Path src : (Iterable<Path>) stream.filter(Files::isRegularFile)
                        .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".ogg"))::iterator) {
                    String stem = src.getFileName().toString();
                    stem = stem.substring(0, stem.length() - 4);
                    String oggFileName = "VaryonMZ_" + MusicZone.sanitizeToken(stem) + ".ogg";
                    Files.copy(src, oggDestDir.resolve(oggFileName), StandardCopyOption.REPLACE_EXISTING);
                    expectedOgg.add(oggFileName);
                }
            }
        }

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
            writeAtomic(mcDestDir.resolve(mcFileName), buildMusicContainerJson(zone.musicCommonTrackPath(), zone.getVolumeDb()));
            expectedMc.add(mcFileName);

            String ambId = zone.ambienceAssetId();
            String ambFileName = ambId + ".json";
            writeAtomic(ambDestDir.resolve(ambFileName), buildAmbienceFxJson(mcId));
            expectedAmb.add(ambFileName);
        }

        if (pruneOrphans) {
            deleteStaleFiles(oggDestDir, expectedOgg);
            deleteStaleFiles(ambDestDir, expectedAmb);
            deleteStaleFiles(mcDestDir, expectedMc);
        }

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

        // Laisser le paquet AssetUpdate du .ogg (Common Asset) partir et s'installer chez les
        // clients connectés AVANT de recharger MusicContainer/AmbienceFX. Le client applique les
        // stores dans l'ordre de réception et ne réordonne pas les dépendances : s'il reçoit le
        // MusicContainer (qui référence Music/VaryonMZ/VaryonMZ_*.ogg) avant le .ogg, il résout la
        // track immédiatement, ne la trouve pas et lève un KeyNotFoundException NON catché ->
        // crash client ("The given key '...' was not present in the dictionary"). Ce sleep tourne
        // dans la partie async du rebuild (jamais le thread-monde), le bloquer est sans risque.
        if (pack != null) {
            try {
                Thread.sleep(750L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
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

    // Écriture atomique : un lecteur concurrent (loadAssetsFromDirectory du file-watcher, ou
    // un autre rebuild) voit soit l'ancien fichier complet, soit le nouveau complet, jamais un
    // fichier tronqué en cours d'écriture (source de "Unexpected character ... expected ','").
    private static void writeAtomic(Path target, String content) throws IOException {
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp-" + Long.toHexString(System.nanoTime()));
        Files.writeString(tmp, content, StandardCharsets.UTF_8);
        try {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(tmp);
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

    private static String buildMusicContainerJson(String commonTrackPath, double volumeDb) {
        return "{\n"
                + "  \"Type\": \"SingleTrack\",\n"
                + "  \"Track\": \""
                + commonTrackPath
                + "\",\n"
                + "  \"Volume\": "
                + volumeDb
                + ",\n"
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
