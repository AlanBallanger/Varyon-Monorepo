package fr.varyon.mapmarker;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import org.joml.Vector3d;
import com.hypixel.hytale.protocol.packets.setup.RequestCommonAssetsRebuild;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.worldmap.WorldMapManager;
import com.hypixel.hytale.server.core.universe.world.worldmap.markers.user.UserMapMarker;
import com.hypixel.hytale.server.core.universe.world.worldmap.markers.worldstore.WorldMarkersResource;
import fr.varyon.mapmarker.commands.MapMarkerRootCommand;
import fr.varyon.mapmarker.assets.MapMarkerAssetPublisher;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

public final class VaryonMapMarkerPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String SHARED_MARKER_ID_PREFIX = "mk-shared-";
    private static final String MARKERS_STATE_FILE = "markers.db";
    private static final String CONFIG_FILE = "config.yml";
    private static final String MAP_MARKER_CLIENT_TEXTURE_PREFIX = "UI/WorldMap/MapMarkers/";
    private static final Pattern DEBUG_PATTERN = Pattern.compile("(?m)^\\s*debug\\s*:\\s*(true|false)\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern MARKER_OBJECT_PATTERN = Pattern.compile("\\{(.*?)\\}", Pattern.DOTALL);
    private static VaryonMapMarkerPlugin instance;
    private final List<SavedMarker> savedMarkers = Collections.synchronizedList(new ArrayList<>());
    private volatile boolean debugLogging = true;

    public VaryonMapMarkerPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static VaryonMapMarkerPlugin getInstance() {
        return instance;
    }

    @Override
    protected void setup() {
        debug("Configuration de l’initialisation");
        registerCommands();
    }

    @Override
    protected void start() {
        ensureDirectories();
        ensureConfigFile();
        loadConfig();
        extractDefaultBundledImages();
        loadSavedMarkers();
        importSavedMarkers();
        registerMarkerTextureWarmupEvents();
        printStartupBanner();
        debug("Démarrage dataDir=%s", getDataDirectory());
        debug(
                "Démarrage terminé images=%s mapmarkers=%s savedMarkers=%s",
                getImagesDir(),
                getMapMarkersAssetDir(),
                savedMarkers.size());
    }

    @Override
    protected void shutdown() {
        debug("Arrêt terminé");
        instance = null;
    }

    public Path getImagesDir() {
        return getDataDirectory().resolve("images");
    }

    public Path getMapMarkersAssetDir() {
        return getDataDirectory().resolve("Common/UI/WorldMap/MapMarkers");
    }

    public Path resolvePngInImages(String imageName) {
        if (imageName == null || imageName.isBlank()) {
            return null;
        }
        ensureDirectories();
        Path dir = getImagesDir();
        if (!Files.isDirectory(dir)) {
            return null;
        }
        String base = imageName.trim().replace('\\', '/');
        if (base.contains("/")) {
            base = base.substring(base.lastIndexOf('/') + 1);
        }
        if (base.isEmpty()) {
            return null;
        }
        final String request = base;
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                    .filter(p -> matchesPngRequest(request, p.getFileName().toString()))
                    .min(Comparator.comparing(
                            p -> p.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                    .orElse(null);
        } catch (Exception e) {
            ((HytaleLogger.Api) LOGGER.at(Level.WARNING).withCause(e))
                    .log("[Varyon-MapMarker] Échec résolution PNG %s", imageName);
            return null;
        }
    }

    private static boolean matchesPngRequest(String requestedBase, String fileName) {
        if (fileName.equalsIgnoreCase(requestedBase)) {
            return true;
        }
        String fileStem = fileName.substring(0, fileName.length() - 4);
        if (hasPngExtension(requestedBase)) {
            String reqStem = requestedBase.substring(0, requestedBase.length() - 4);
            return fileStem.equalsIgnoreCase(reqStem);
        }
        return fileStem.equalsIgnoreCase(requestedBase);
    }

    private static boolean hasPngExtension(String name) {
        int len = name.length();
        return len >= 4 && name.substring(len - 4).toLowerCase(Locale.ROOT).equals(".png");
    }

    private static String mapMarkerClientTexturePath(String imageBaseName) {
        if (imageBaseName == null || imageBaseName.isBlank()) {
            return imageBaseName;
        }
        String normalized = imageBaseName.trim().replace('\\', '/');
        if (normalized.contains("/")) {
            return normalized;
        }
        return MAP_MARKER_CLIENT_TEXTURE_PREFIX + normalized;
    }

    private void pushMapMarkerPngToAllOnlinePlayers(String pngBaseFileName) {
        Path file = getMapMarkersAssetDir().resolve(pngBaseFileName);
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            debug("pushMapMarkerPng fichier absent %s", file);
            return;
        }
        try {
            byte[] bytes = Files.readAllBytes(file);
            String clientPath = mapMarkerClientTexturePath(pngBaseFileName);
            Universe universe = Universe.get();
            if (universe == null) {
                return;
            }
            int ok = 0;
            for (World w : universe.getWorlds().values()) {
                for (PlayerRef ref : w.getPlayerRefs()) {
                    if (MapMarkerAssetPublisher.deliver(ref, clientPath, bytes, false)) {
                        ok++;
                    }
                }
            }
            debug("PNG marqueur poussé clients chemin=%s livraisons=%s octets=%s", clientPath, ok, bytes.length);
        } catch (Exception e) {
            ((HytaleLogger.Api) LOGGER.at(Level.WARNING).withCause(e))
                    .log("[Varyon-MapMarker] Échec push texture marqueur %s", pngBaseFileName);
        }
    }

    private void registerMarkerTextureWarmupEvents() {
        getEventRegistry().registerGlobal(PlayerReadyEvent.class, this::onPlayerReadyPushMapMarkerTextures);
        debug("Écoute PlayerReadyEvent pour livrer les textures marqueurs aux reconnexions");
    }

    private void onPlayerReadyPushMapMarkerTextures(PlayerReadyEvent event) {
        Ref<EntityStore> entityRef = event.getPlayerRef();
        if (entityRef == null || !entityRef.isValid()) {
            return;
        }
        Store<EntityStore> store = entityRef.getStore();
        if (store == null) {
            return;
        }
        PlayerRef playerRef = store.getComponent(entityRef, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }
        pushAllMapMarkerTexturesToPlayer(playerRef);
        Player player = event.getPlayer();
        if (player != null) {
            try {
                player.getWorldMapTracker().clear();
            } catch (Exception e) {
                ((HytaleLogger.Api) LOGGER.at(Level.WARNING).withCause(e))
                        .log("[Varyon-MapMarker] Échec clear tracker carte après warm-up textures joueur");
            }
        }
    }

    private void pushAllMapMarkerTexturesToPlayer(PlayerRef playerRef) {
        if (playerRef == null) {
            return;
        }
        Path dir = getMapMarkersAssetDir();
        if (!Files.isDirectory(dir, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        int delivered = 0;
        try (Stream<Path> stream = Files.list(dir)) {
            ArrayList<Path> pngs = new ArrayList<>();
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                    .forEach(pngs::add);
            for (Path file : pngs) {
                String base = file.getFileName().toString();
                try {
                    byte[] bytes = Files.readAllBytes(file);
                    if (MapMarkerAssetPublisher.deliver(
                            playerRef, mapMarkerClientTexturePath(base), bytes, false)) {
                        delivered++;
                    }
                } catch (Exception e) {
                    ((HytaleLogger.Api) LOGGER.at(Level.WARNING).withCause(e))
                            .log("[Varyon-MapMarker] Échec push PNG joueur %s fichier=%s", playerRef.getUuid(), base);
                }
            }
        } catch (Exception e) {
            ((HytaleLogger.Api) LOGGER.at(Level.WARNING).withCause(e))
                    .log("[Varyon-MapMarker] Échec warm-up textures MapMarkers pour joueur");
            return;
        }
        try {
            if (delivered > 0 && playerRef.getPacketHandler() != null) {
                playerRef.getPacketHandler().writeNoCache(new RequestCommonAssetsRebuild());
            }
        } catch (Exception e) {
            ((HytaleLogger.Api) LOGGER.at(Level.WARNING).withCause(e))
                    .log("[Varyon-MapMarker] Échec RequestCommonAssetsRebuild warm-up joueur");
        }
        debug("Warm-up textures marqueur joueur uuid=%s fichiers_livrés=%s", playerRef.getUuid(), delivered);
    }

    public void createSharedMarkerFromPlayer(@Nonnull PlayerRef playerRef, @Nonnull World world, @Nonnull String imageName, @Nonnull String markerName) {
        Path sourceImage = resolvePngInImages(imageName);
        if (sourceImage == null) {
            debug("createSharedMarkerFromPlayer annulé : PNG introuvable image=%s", imageName);
            playerRef.sendMessage(Message.raw("Erreur : image introuvable dans images/ : " + imageName));
            return;
        }
        String fileName = sourceImage.getFileName().toString();
        if (!copyImageToMapMarkers(sourceImage, fileName)) {
            debug("createSharedMarkerFromPlayer annulé : échec copie image=%s", fileName);
            playerRef.sendMessage(Message.raw("Erreur : impossible de copier l'image vers la carte."));
            return;
        }
        pushMapMarkerPngToAllOnlinePlayers(fileName);
        requestClientsRebuildCommonAssets();
        debug("Création marqueur monde=%s joueur=%s image=%s nom=%s", world.getName(), playerRef.getUsername(), fileName, markerName);
        try {
            org.joml.Vector3d position = playerRef.getTransform().getPosition();
            UUID playerUuid = playerRef.getUuid();
            String displayName = playerRef.getUsername();
            WorldMarkersResource resource = getWorldMarkersResource(world);
            if (resource == null) {
                playerRef.sendMessage(Message.raw("Erreur : échec de la création du marqueur sur la carte."));
                return;
            }
            UserMapMarker marker = new UserMapMarker();
            String markerId = SHARED_MARKER_ID_PREFIX + UUID.randomUUID();
            marker.setId(markerId);
            marker.setPosition((float) position.x, (float) position.z);
            marker.setName(markerName);
            marker.setIcon(fileName);
            marker.withCreatedByName(displayName);
            marker.withCreatedByUuid(playerUuid);
            upsertSavedMarker(new SavedMarker(
                    markerId,
                    world.getName(),
                    fileName,
                    markerName,
                    (float) position.x,
                    (float) position.z,
                    playerUuid,
                    displayName));
            rebuildManagedMarkersForWorld(world);
            refreshWorldMapTrackers(world);
            debug(
                    "Marqueur créé monde=%s id=%s image=%s nom=%s x=%.2f z=%.2f",
                    world.getName(),
                    markerId,
                    fileName,
                    markerName,
                    position.x,
                    position.z);
            playerRef.sendMessage(Message.raw("Marqueur créé sur la carte avec l'image : " + fileName + ", nom : " + markerName));
        } catch (Exception e) {
            ((HytaleLogger.Api) LOGGER.at(Level.WARNING).withCause(e))
                    .log("[Varyon-MapMarker] Échec création marqueur monde %s", world.getName());
            playerRef.sendMessage(Message.raw("Erreur : échec de la création du marqueur sur la carte."));
        }
    }

    public List<String> listAvailablePngs() {
        ensureDirectories();
        ArrayList<String> result = new ArrayList<>();
        try (Stream<Path> stream = Files.list(getImagesDir())) {
            stream.filter(path -> path.getFileName().toString().toLowerCase().endsWith(".png"))
                    .map(path -> path.getFileName().toString())
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .forEach(result::add);
        } catch (Exception e) {
            ((HytaleLogger.Api) LOGGER.at(Level.WARNING).withCause(e)).log("[Varyon-MapMarker] Échec lors de la liste des PNG");
        }
        debug("listAvailablePngs total=%s", result.size());
        return result;
    }

    public List<MarkerEntry> findMarkersInWorldByName(@Nonnull World world, String markerName) {
        if (markerName == null || markerName.isBlank()) {
            return List.of();
        }
        String wname = world.getName();
        ArrayList<MarkerEntry> out = new ArrayList<>();
        synchronized (savedMarkers) {
            for (SavedMarker sm : savedMarkers) {
                if (wname.equals(sm.worldName()) && markerName.equalsIgnoreCase(sm.markerName())) {
                    String author = sm.createdByName() != null ? sm.createdByName() : "";
                    out.add(new MarkerEntry(
                            sm.id(),
                            sm.worldName(),
                            sm.imageName(),
                            sm.markerName(),
                            sm.x(),
                            sm.z(),
                            author));
                }
            }
        }
        out.sort(Comparator.comparing(MarkerEntry::id, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(out);
    }

    public List<MarkerEntry> listMarkersInWorld(@Nonnull World world) {
        String wname = world.getName();
        ArrayList<MarkerEntry> out = new ArrayList<>();
        synchronized (savedMarkers) {
            for (SavedMarker sm : savedMarkers) {
                if (wname.equals(sm.worldName())) {
                    String author = sm.createdByName() != null ? sm.createdByName() : "";
                    out.add(new MarkerEntry(
                            sm.id(),
                            sm.worldName(),
                            sm.imageName(),
                            sm.markerName(),
                            sm.x(),
                            sm.z(),
                            author));
                }
            }
        }
        out.sort(Comparator
                .comparing(MarkerEntry::markerName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(MarkerEntry::id, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(out);
    }

    public int clearMarkersByName(@Nonnull World world, @Nonnull String markerName) {
        if (markerName == null || markerName.isBlank()) {
            debug("clearMarkersByName annulé : nom vide");
            return 0;
        }
        debug("clearMarkersByName monde=%s nom=%s", world.getName(), markerName);
        try {
            WorldMarkersResource resource = getWorldMarkersResource(world);
            if (resource == null) {
                return 0;
            }
            ArrayList<UserMapMarker> updated = new ArrayList<>();
            int removedCount = 0;
            for (UserMapMarker marker : resource.getUserMapMarkers()) {
                if (marker == null) {
                    continue;
                }
                if (isManagedMarker(marker) && markerName.equalsIgnoreCase(marker.getName())) {
                    removedCount++;
                    continue;
                }
                updated.add(marker);
            }
            if (removedCount > 0) {
                resource.setUserMapMarkers(updated);
                removeSavedMarkersByWorldAndName(world.getName(), markerName);
                refreshWorldMapTrackers(world);
                debug(
                        "Marqueurs retirés monde=%s nom=%s count=%s restants=%s",
                        world.getName(),
                        markerName,
                        removedCount,
                        updated.size());
            }
            return removedCount;
        } catch (Exception e) {
            ((HytaleLogger.Api) LOGGER.at(Level.WARNING).withCause(e))
                    .log(
                            "[Varyon-MapMarker] Échec suppression marqueurs nom %s monde %s",
                            markerName,
                            world.getName());
            return 0;
        }
    }

    public boolean reloadMarkerAssets() {
        try {
            debug("reloadMarkerAssets début");
            ensureDirectories();
            ArrayList<Path> copied = new ArrayList<>();
            try (Stream<Path> stream = Files.list(getImagesDir())) {
                stream.filter(path -> path.getFileName().toString().toLowerCase().endsWith(".png"))
                        .forEach(path -> {
                            String base = path.getFileName().toString();
                            if (copyImageToMapMarkers(path, base)) {
                                copied.add(path);
                                pushMapMarkerPngToAllOnlinePlayers(base);
                            }
                        });
            }
            boolean imported = importSavedMarkers();
            debug("reloadMarkerAssets fin copiés=%s importOk=%s", copied.size(), imported);
            return imported && (!copied.isEmpty() || Files.exists(getImagesDir(), LinkOption.NOFOLLOW_LINKS));
        } catch (Exception e) {
            ((HytaleLogger.Api) LOGGER.at(Level.WARNING).withCause(e))
                    .log("[Varyon-MapMarker] Échec rechargement des assets marqueurs");
            return false;
        }
    }

    public boolean importSavedMarkers() {
        try {
            debug("importSavedMarkers début total=%s", savedMarkers.size());
            Universe universe = Universe.get();
            if (universe == null) {
                debug("importSavedMarkers annulé : univers nul");
                return false;
            }
            ArrayList<CompletableFuture<Void>> rebuilds = new ArrayList<>();
            for (World world : universe.getWorlds().values()) {
                CompletableFuture<Void> rebuild = new CompletableFuture<>();
                rebuilds.add(rebuild);
                world.execute(() -> {
                    try {
                        rebuildManagedMarkersForWorld(world);
                        rebuild.complete(null);
                    } catch (Exception e) {
                        rebuild.completeExceptionally(e);
                    }
                });
            }
            synchronized (savedMarkers) {
                for (SavedMarker savedMarker : savedMarkers) {
                    World world = universe.getWorlds().get(savedMarker.worldName());
                    if (world == null) {
                        debug(
                                "importSavedMarkers ignoré : monde absent monde=%s id=%s",
                                savedMarker.worldName(),
                                savedMarker.id());
                        continue;
                    }
                    Path source = resolvePngInImages(savedMarker.imageName());
                    if (source != null) {
                        String base = source.getFileName().toString();
                        if (copyImageToMapMarkers(source, base)) {
                            pushMapMarkerPngToAllOnlinePlayers(base);
                        }
                    }
                }
            }
            for (CompletableFuture<Void> completableFuture : rebuilds) {
                completableFuture.join();
            }
            refreshAllWorldMapTrackers();
            requestClientsRebuildCommonAssets();
            debug("importSavedMarkers terminé");
            return true;
        } catch (Exception e) {
            ((HytaleLogger.Api) LOGGER.at(Level.WARNING).withCause(e))
                    .log("[Varyon-MapMarker] Échec import des marqueurs sauvegardés");
            return false;
        }
    }

    private void requestClientsRebuildCommonAssets() {
        try {
            Universe universe = Universe.get();
            if (universe != null) {
                universe.broadcastPacketNoCache(new RequestCommonAssetsRebuild());
                debug("RequestCommonAssetsRebuild diffusé");
            }
        } catch (Exception e) {
            ((HytaleLogger.Api) LOGGER.at(Level.WARNING).withCause(e))
                    .log("[Varyon-MapMarker] Échec diffusion RequestCommonAssetsRebuild");
        }
    }

    private void registerCommands() {
        try {
            getCommandRegistry().registerCommand(new MapMarkerRootCommand("mapmarker"));
            getCommandRegistry().registerCommand(new MapMarkerRootCommand("mm"));
            debug("Commandes enregistrées (mapmarker, mm)");
        } catch (Exception e) {
            ((HytaleLogger.Api) LOGGER.at(Level.WARNING).withCause(e))
                    .log("[Varyon-MapMarker] Échec enregistrement des commandes");
        }
    }

    private void ensureDirectories() {
        ensureDirectory(getImagesDir());
        ensureDirectory(getMapMarkersAssetDir());
        debug("Dossiers prêts images=%s mapmarkers=%s", getImagesDir(), getMapMarkersAssetDir());
    }

    private void ensureDirectory(Path dir) {
        try {
            Files.createDirectories(dir, new FileAttribute<?>[0]);
        } catch (Exception e) {
            ((HytaleLogger.Api) LOGGER.at(Level.WARNING).withCause(e))
                    .log("[Varyon-MapMarker] Échec création dossier %s", dir);
        }
    }

    private Path getMarkersStateFile() {
        return getDataDirectory().resolve(MARKERS_STATE_FILE);
    }

    private Path getConfigFilePath() {
        return getDataDirectory().resolve(CONFIG_FILE);
    }

    private void ensureConfigFile() {
        Path configFile = getConfigFilePath();
        if (Files.exists(configFile, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        try {
            ensureDirectory(configFile.getParent());
            Files.writeString(
                    configFile,
                    buildDefaultConfigYml(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
        } catch (Exception e) {
            ((HytaleLogger.Api) LOGGER.at(Level.WARNING).withCause(e))
                    .log("[Varyon-MapMarker] Échec création config.yml");
        }
    }

    private void loadConfig() {
        Path configFile = getConfigFilePath();
        if (!Files.exists(configFile, LinkOption.NOFOLLOW_LINKS)) {
            debugLogging = true;
            return;
        }
        try {
            String yml = Files.readString(configFile, StandardCharsets.UTF_8);
            Matcher debugMatcher = DEBUG_PATTERN.matcher(yml);
            debugLogging = debugMatcher.find() ? Boolean.parseBoolean(debugMatcher.group(1)) : true;
            debug(
                    "Config chargée debug=%s path=%s",
                    debugLogging,
                    configFile);
        } catch (Exception e) {
            debugLogging = true;
            ((HytaleLogger.Api) LOGGER.at(Level.WARNING).withCause(e))
                    .log("[Varyon-MapMarker] Échec chargement config.yml");
        }
    }

    private String buildDefaultConfigYml() {
        return """
####################################################################################################
# Varyon-MapMarker — marqueurs personnalisés sur la carte Hytale
#
# Dossier du mod : Varyon-MapMarker/
#   images/              → tes PNG (c’est là que tu ajoutes des fichiers)
#   markers.db           → positions, mondes, noms + nom de fichier d’icône uniquement (pas les octets du PNG)
#   Common/UI/WorldMap/MapMarkers/ → Copie des PNG ; le mod les envoie aussi au client (pipeline CommonAsset),
#     comme les avatars PlayerMarker, afin que l’UI carte résolve la texture sans croix rouge.
#
# Commandes (anglais) : /mapmarker … ou /mm …
#   Noms d’images : extension .png facultative, casse ignorée. Évite le fichier « Home.png » : le client le traite
#     souvent comme l’icône « maison » système ; utilise plutôt Homestead.png (inclus dans le mod) ou un autre nom.
#   Noms de marqueurs (clear) : casse ignorée ; /clear <nom> uniquement.
#   Sous-commandes : list (marqueurs du monde courant), info, tp, set, clear, import, reload
#
# debug : journaux détaillés dans la console serveur.
# Après chaque mutation de marqueur, le mod appelle toujours sendSettings() puis WorldMapTracker.clear()
# (ClearWorldMap côté client) pour que les icônes et la liste des pins se resynchronisent.
####################################################################################################

debug: true

# Référence rapide (exemples)
commands_help:
  list: "/mapmarker list  ou  /mm list"
  info: "/mapmarker info <nom affiché>"
  tp: "/mapmarker tp <nom affiché>"
  set: "/mapmarker set <fichier> <nom affiché>"
  clear: "/mapmarker clear <nom affiché>   # casse ignorée"
  import: "/mapmarker import"
  reload: "/mapmarker reload"
""";
    }

    private void printStartupBanner() {
        int pngCount = listAvailablePngs().size();
        logInfo("################################################################################");
        logInfo("# Varyon-MapMarker démarré");
        logInfo("# Dossier données    : Varyon-MapMarker/");
        logInfo("# PNG dans images/ : " + pngCount);
        logInfo("# Fichier marqueurs : Varyon-MapMarker/markers.db");
        logInfo("# Commandes         : /mapmarker … | /mm …");
        logInfo("################################################################################");
    }

    private void extractDefaultBundledImages() {
        int extracted = 0;
        for (String fileName : getDefaultBundledImageNames()) {
            Path target = getImagesDir().resolve(fileName);
            if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                continue;
            }
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("default-images/" + fileName)) {
                if (in == null) {
                    continue;
                }
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                extracted++;
            } catch (Exception e) {
                ((HytaleLogger.Api) LOGGER.at(Level.WARNING).withCause(e))
                        .log("[Varyon-MapMarker] Échec extraction image par défaut %s", fileName);
            }
        }
        debug("extractDefaultBundledImages extraits=%s", extracted);
    }

    private boolean copyImageToMapMarkers(Path sourceImage, String fileName) {
        try {
            ensureDirectories();
            if (!Files.exists(sourceImage, LinkOption.NOFOLLOW_LINKS)) {
                debug("copyImageToMapMarkers annulé : source absente %s", sourceImage);
                return false;
            }
            Path target = getMapMarkersAssetDir().resolve(fileName);
            Files.copy(sourceImage, target, StandardCopyOption.REPLACE_EXISTING);
            debug("PNG copié source=%s cible=%s", sourceImage, target);
            return true;
        } catch (Exception e) {
            ((HytaleLogger.Api) LOGGER.at(Level.WARNING).withCause(e))
                    .log("[Varyon-MapMarker] Échec copie image %s vers MapMarkers", sourceImage);
            return false;
        }
    }

    private void refreshWorldMapTrackers(World world) {
        try {
            WorldMapManager mapManager = world.getWorldMapManager();
            if (mapManager != null) {
                mapManager.sendSettings();
                debug("WorldMap sendSettings monde=%s", world.getName());
            }
        } catch (Exception e) {
            ((HytaleLogger.Api) LOGGER.at(Level.WARNING).withCause(e))
                    .log("[Varyon-MapMarker] Échec WorldMap sendSettings monde=%s", world.getName());
        }
        int cleared = 0;
        for (Player player : getWorldPlayers(world)) {
            player.getWorldMapTracker().clear();
            cleared++;
        }
        debug("Trackers carte effacés (ClearWorldMap) monde=%s joueurs=%s", world.getName(), cleared);
    }

    private void refreshAllWorldMapTrackers() {
        try {
            Universe universe = Universe.get();
            if (universe == null) {
                debug("refreshAllWorldMapTrackers annulé : univers nul");
                return;
            }
            for (World world : universe.getWorlds().values()) {
                world.execute(() -> refreshWorldMapTrackers(world));
            }
            debug("refreshAllWorldMapTrackers planifié mondes=%s", universe.getWorlds().size());
        } catch (Exception e) {
            ((HytaleLogger.Api) LOGGER.at(Level.WARNING).withCause(e))
                    .log("[Varyon-MapMarker] Échec rafraîchissement trackers pour tous les mondes");
        }
    }

    private PlayerRef findPlayerRef(World world, String displayName, Player sender) {
        Ref playerEntityRef = sender.getReference();
        for (PlayerRef playerRef : world.getPlayerRefs()) {
            Ref entityRef = world.getEntityRef(playerRef.getUuid());
            if (entityRef != null && entityRef.equals(playerEntityRef)) {
                return playerRef;
            }
        }
        for (PlayerRef playerRef : world.getPlayerRefs()) {
            if (displayName.equalsIgnoreCase(playerRef.getUsername())) {
                return playerRef;
            }
        }
        debug("findPlayerRef échec monde=%s joueur=%s", world.getName(), displayName);
        return null;
    }

    private WorldMarkersResource getWorldMarkersResource(World world) {
        return (WorldMarkersResource)
                world.getChunkStore().getStore().getResource(WorldMarkersResource.getResourceType());
    }

    private boolean isManagedMarker(UserMapMarker marker) {
        return marker.getId() != null && marker.getId().startsWith(SHARED_MARKER_ID_PREFIX);
    }

    private void rebuildManagedMarkersForWorld(World world) {
        WorldMarkersResource resource = getWorldMarkersResource(world);
        if (resource == null) {
            debug("rebuildManagedMarkersForWorld annulé : resource nulle monde=%s", world.getName());
            return;
        }
        ArrayList<UserMapMarker> updated = new ArrayList<>();
        int removed = 0;
        for (UserMapMarker marker : resource.getUserMapMarkers()) {
            if (marker == null) {
                continue;
            }
            if (isManagedMarker(marker)) {
                removed++;
                continue;
            }
            updated.add(marker);
        }
        int restored = 0;
        synchronized (savedMarkers) {
            for (SavedMarker savedMarker : savedMarkers) {
                if (!world.getName().equals(savedMarker.worldName())) {
                    continue;
                }
                UserMapMarker marker = new UserMapMarker();
                marker.setId(savedMarker.id());
                marker.setPosition(savedMarker.x(), savedMarker.z());
                marker.setName(savedMarker.markerName());
                marker.setIcon(savedMarker.imageName());
                marker.withCreatedByUuid(savedMarker.createdByUuid());
                marker.withCreatedByName(savedMarker.createdByName());
                updated.add(marker);
                restored++;
            }
        }
        resource.setUserMapMarkers(updated);
        debug(
                "rebuildManagedMarkersForWorld monde=%s retirés=%s restaurés=%s total=%s",
                world.getName(),
                removed,
                restored,
                updated.size());
    }

    private void upsertSavedMarker(SavedMarker marker) {
        synchronized (savedMarkers) {
            savedMarkers.removeIf(existing -> existing.id().equals(marker.id()));
            savedMarkers.add(marker);
        }
        debug(
                "Marqueur sauvé id=%s monde=%s image=%s nom=%s total=%s",
                marker.id(),
                marker.worldName(),
                marker.imageName(),
                marker.markerName(),
                savedMarkers.size());
        saveSavedMarkers();
    }

    private void removeSavedMarkersByWorldAndName(String worldName, String markerName) {
        synchronized (savedMarkers) {
            savedMarkers.removeIf(marker -> worldName.equals(marker.worldName())
                    && markerName.equalsIgnoreCase(marker.markerName()));
        }
        debug(
                "Marqueurs retirés du stockage monde=%s nom=%s total=%s",
                worldName,
                markerName,
                savedMarkers.size());
        saveSavedMarkers();
    }

    private void loadSavedMarkers() {
        savedMarkers.clear();
        Path stateFile = getMarkersStateFile();
        if (!Files.exists(stateFile, LinkOption.NOFOLLOW_LINKS)) {
            debug("loadSavedMarkers : fichier absent path=%s", stateFile);
            return;
        }
        try {
            String json = Files.readString(stateFile, StandardCharsets.UTF_8);
            Matcher matcher = MARKER_OBJECT_PATTERN.matcher(json);
            while (matcher.find()) {
                SavedMarker marker = SavedMarker.fromJsonObject(matcher.group(1));
                if (marker != null) {
                    savedMarkers.add(marker);
                }
            }
            debug("loadSavedMarkers terminé total=%s path=%s", savedMarkers.size(), stateFile);
        } catch (Exception e) {
            ((HytaleLogger.Api) LOGGER.at(Level.WARNING).withCause(e))
                    .log("[Varyon-MapMarker] Échec chargement des marqueurs sauvegardés");
        }
    }

    private void saveSavedMarkers() {
        Path stateFile = getMarkersStateFile();
        try {
            ensureDirectory(stateFile.getParent());
            Files.writeString(
                    stateFile,
                    toJson(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
            debug("saveSavedMarkers terminé total=%s path=%s", savedMarkers.size(), stateFile);
        } catch (Exception e) {
            ((HytaleLogger.Api) LOGGER.at(Level.WARNING).withCause(e))
                    .log("[Varyon-MapMarker] Échec sauvegarde des marqueurs");
        }
    }

    private Iterable<Player> getWorldPlayers(World world) {
        ArrayList<Player> players = new ArrayList<>();
        for (PlayerRef playerRef : world.getPlayerRefs()) {
            Ref entityRef = world.getEntityRef(playerRef.getUuid());
            if (entityRef == null || !entityRef.isValid()) {
                continue;
            }
            Player player = (Player) world.getEntityStore()
                    .getStore()
                    .getComponent(entityRef, Player.getComponentType());
            if (player != null) {
                players.add(player);
            }
        }
        return players;
    }

    private List<String> getDefaultBundledImageNames() {
        return Arrays.asList(
                "Campfire.png",
                "Coordinate.png",
                "Death.png",
                "Homestead.png",
                "Player.png",
                "PlayerAbove.png",
                "PlayerBelow.png",
                "Portal.png",
                "PortalInvasion.png",
                "Prefab.png",
                "Spawn.png",
                "Temple_Gateway.png",
                "UserA.png",
                "UserB.png",
                "UserC.png",
                "UserD.png",
                "UserE.png",
                "UserF.png",
                "Warp.png");
    }

    public void debug(String message, Object... args) {
        if (!debugLogging) {
            return;
        }
        String formatted = args == null || args.length == 0 ? message : String.format(message, args);
        LOGGER.at(Level.INFO).log("[Varyon-MapMarker][DEBUG] %s", formatted);
    }

    private String toJson() {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        builder.append("  \"markers\": [\n");
        synchronized (savedMarkers) {
            for (int i = 0; i < savedMarkers.size(); i++) {
                builder.append(savedMarkers.get(i).toJson());
                if (i + 1 < savedMarkers.size()) {
                    builder.append(',');
                }
                builder.append('\n');
            }
        }
        builder.append("  ]\n");
        builder.append("}\n");
        return builder.toString();
    }

    private void logInfo(String message, Object... args) {
        if (args == null || args.length == 0) {
            LOGGER.at(Level.INFO).log("%s", message);
            return;
        }
        LOGGER.at(Level.INFO).log(message, args);
    }

    private record SavedMarker(
            String id,
            String worldName,
            String imageName,
            String markerName,
            float x,
            float z,
            UUID createdByUuid,
            String createdByName) {

        private String toJson() {
            return "    {\n      \"id\": \""
                    + SavedMarker.escapeJson(id)
                    + "\",\n      \"worldName\": \""
                    + SavedMarker.escapeJson(worldName)
                    + "\",\n      \"imageName\": \""
                    + SavedMarker.escapeJson(imageName)
                    + "\",\n      \"markerName\": \""
                    + SavedMarker.escapeJson(markerName)
                    + "\",\n      \"x\": "
                    + x
                    + ",\n      \"z\": "
                    + z
                    + ",\n      \"createdByUuid\": "
                    + (createdByUuid != null ? "\"" + createdByUuid + "\"" : "null")
                    + ",\n      \"createdByName\": \""
                    + SavedMarker.escapeJson(createdByName)
                    + "\"\n    }";
        }

        private static SavedMarker fromJsonObject(String jsonObject) {
            String id = SavedMarker.extractString(jsonObject, "id");
            String worldName = SavedMarker.extractString(jsonObject, "worldName");
            String imageName = SavedMarker.extractString(jsonObject, "imageName");
            String markerName = SavedMarker.extractString(jsonObject, "markerName");
            Float x = SavedMarker.extractFloat(jsonObject, "x");
            Float z = SavedMarker.extractFloat(jsonObject, "z");
            String createdByUuidRaw = SavedMarker.extractNullableString(jsonObject, "createdByUuid");
            String createdByName = SavedMarker.extractString(jsonObject, "createdByName");
            if (id == null
                    || worldName == null
                    || imageName == null
                    || markerName == null
                    || x == null
                    || z == null
                    || createdByName == null) {
                return null;
            }
            try {
                UUID uuid = createdByUuidRaw == null || createdByUuidRaw.isBlank()
                        ? null
                        : UUID.fromString(createdByUuidRaw);
                return new SavedMarker(id, worldName, imageName, markerName, x, z, uuid, createdByName);
            } catch (Exception ignored) {
                return null;
            }
        }

        private static String escapeJson(String value) {
            if (value == null) {
                return "";
            }
            return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\r", "\\r")
                    .replace("\n", "\\n")
                    .replace("\t", "\\t");
        }

        private static String extractString(String jsonObject, String field) {
            Matcher matcher = Pattern.compile("\"" + field + "\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"")
                    .matcher(jsonObject);
            if (!matcher.find()) {
                return null;
            }
            return SavedMarker.unescapeJson(matcher.group(1));
        }

        private static String extractNullableString(String jsonObject, String field) {
            Matcher stringMatcher = Pattern.compile("\"" + field + "\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"")
                    .matcher(jsonObject);
            if (stringMatcher.find()) {
                return SavedMarker.unescapeJson(stringMatcher.group(1));
            }
            Matcher nullMatcher = Pattern.compile("\"" + field + "\"\\s*:\\s*null").matcher(jsonObject);
            return nullMatcher.find() ? null : null;
        }

        private static Float extractFloat(String jsonObject, String field) {
            Matcher matcher = Pattern.compile("\"" + field + "\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?)")
                    .matcher(jsonObject);
            if (!matcher.find()) {
                return null;
            }
            try {
                return Float.parseFloat(matcher.group(1));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        private static String unescapeJson(String value) {
            return value.replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                    .replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\t", "\t");
        }
    }
}
