package fr.varyon.musiczones;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import fr.varyon.musiczones.commands.MusicZoneRootCommand;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public final class VaryonMusicZonesPlugin extends JavaPlugin {

    public static final String CONFIG_FILE = "config.yml";
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static volatile VaryonMusicZonesPlugin instance;

    private MusicZoneRepository repository;
    private final Map<UUID, CornerSession> cornerSessions = new ConcurrentHashMap<>();
    private MusicZoneApplySystem applySystem;
    private MusicZoneVisualizer visualizer;
    private Path packRoot;

    public VaryonMusicZonesPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    public static VaryonMusicZonesPlugin getInstance() {
        return instance;
    }

    public MusicZoneVisualizer getVisualizer() {
        if (visualizer == null) {
            throw new IllegalStateException("Plugin not started");
        }
        return visualizer;
    }

    public MusicZoneApplySystem getApplySystem() {
        if (applySystem == null) {
            throw new IllegalStateException("Plugin not started");
        }
        return applySystem;
    }

    public MusicZoneRepository getRepository() {
        if (repository == null) {
            throw new IllegalStateException("Plugin not started");
        }
        return repository;
    }

    @Override
    protected void setup() {
        try {
            getCommandRegistry().registerCommand(new MusicZoneRootCommand("musiczone"));
            getCommandRegistry().registerCommand(new MusicZoneRootCommand("mz"));
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[MusicZones] commandes");
        }
        applySystem = new MusicZoneApplySystem(this);
        getEntityStoreRegistry().registerSystem(applySystem);
        visualizer = new MusicZoneVisualizer(this);
        getEntityStoreRegistry().registerSystem(visualizer);
    }

    @Override
    protected void start() {
        instance = this;
        this.repository = new MusicZoneRepository(getDataDirectory());
        try {
            Files.createDirectories(repository.getMusicDirectory());
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[MusicZones] dossier music");
        }
        repository.load();
        try {
            packRoot = Path.of(System.getProperty("java.io.tmpdir"), "VaryonMusicZones-pack");
            deleteTreeQuietly(packRoot);
            Files.createDirectories(packRoot);
            packRoot.toFile().deleteOnExit();
            LOGGER.atInfo().log("[MusicZones] pack dir=" + packRoot);
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[MusicZones] création temp dir");
        }
        try {
            // Au boot uniquement : packRoot est un temp dir neuf, aucun client connecté -> on
            // peut élaguer les assets orphelins sans risque. À chaud, rebuildAssetPack() reste
            // add-only (voir ZoneMusicAssetGenerator.rebuildPack).
            ZoneMusicAssetGenerator.rebuildPack(this, packRoot, repository.getZonesReadOnly(), true);
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[MusicZones] pack initial");
        }
    }

    @Override
    protected void shutdown() {
        instance = null;
    }

    private void deleteTreeQuietly(Path root) {
        if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (Exception ignored) {
                }
            });
        } catch (Exception ignored) {
        }
    }

    // Mise à jour légère pour un simple changement de champ du MusicContainer (ex. Volume) :
    // n'écrit que les JSON MC/AmbienceFX et recharge ces stores, sans recopier les .ogg ni
    // rappeler loadCommonAssets (qui peut figer le tick en cas de contention avec le watcher).
    public void rebuildContainersOnly() throws Exception {
        ZoneMusicAssetGenerator.rebuildContainersOnly(packRoot, repository.getZonesReadOnly());
    }

    public void rebuildAssetPack() throws Exception {
        LOGGER.atInfo().log("[MusicZones] rebuildAssetPack() start, zones=" + repository.getZonesReadOnly().size());
        try {
            ZoneMusicAssetGenerator.rebuildPack(this, packRoot, repository.getZonesReadOnly());
            LOGGER.atInfo().log("[MusicZones] rebuildAssetPack() done");
        } catch (Throwable t) {
            LOGGER.atSevere().withCause(t).log("[MusicZones] rebuildAssetPack() FAILED");
            throw t;
        }
    }

    public void setPendingCorner1(String worldName, UUID playerUuid, double x, double y, double z) {
        if (playerUuid == null) {
            return;
        }
        cornerSessions.compute(playerUuid, (k, v) -> {
            CornerSession s = v != null && worldName.equals(v.worldName) ? v : new CornerSession();
            s.worldName = worldName;
            s.c1 = new Corner(x, y, z);
            return s;
        });
    }

    public void setPendingCorner2(String worldName, UUID playerUuid, double x, double y, double z) {
        if (playerUuid == null) {
            return;
        }
        cornerSessions.compute(playerUuid, (k, v) -> {
            CornerSession s = v != null && worldName.equals(v.worldName) ? v : new CornerSession();
            s.worldName = worldName;
            s.c2 = new Corner(x, y, z);
            return s;
        });
    }

    @Nullable
    public PendingBox takePendingBox(UUID playerUuid, String worldName) {
        if (playerUuid == null) {
            return null;
        }
        CornerSession s = cornerSessions.remove(playerUuid);
        if (s == null || s.c1 == null || s.c2 == null) {
            return null;
        }
        if (!worldName.equals(s.worldName)) {
            return null;
        }
        return new PendingBox(s.c1.x, s.c1.y, s.c1.z, s.c2.x, s.c2.y, s.c2.z);
    }

    // Noms de fichiers .ogg présents dans Varyon-MusicZones/music/, triés (casse ignorée),
    // sans doublon. Alimente la liste déroulante « musique » de l'éditeur.
    public java.util.List<String> listMusicFileNames() {
        Path dir = repository.getMusicDirectory();
        if (!Files.isDirectory(dir, LinkOption.NOFOLLOW_LINKS)) {
            return java.util.List.of();
        }
        try (Stream<Path> stream = Files.list(dir)) {
            java.util.TreeSet<String> names = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            stream.filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .filter(fn -> fn.toLowerCase(Locale.ROOT).endsWith(".ogg"))
                    .forEach(names::add);
            return new java.util.ArrayList<>(names);
        } catch (Exception e) {
            return java.util.List.of();
        }
    }

    @Nullable
    public Path resolveMusicFile(String name) {
        Path dir = repository.getMusicDirectory();
        if (!Files.isDirectory(dir, LinkOption.NOFOLLOW_LINKS)) {
            return null;
        }
        if (name == null || name.isBlank()) {
            return null;
        }
        String base = name.trim().replace('\\', '/');
        if (base.contains("/")) {
            base = base.substring(base.lastIndexOf('/') + 1);
        }
        final String stem = base.toLowerCase(Locale.ROOT).endsWith(".ogg") ? base.substring(0, base.length() - 4) : base;
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.filter(Files::isRegularFile)
                    .filter(p -> {
                        String fn = p.getFileName().toString();
                        if (!fn.toLowerCase(Locale.ROOT).endsWith(".ogg")) {
                            return false;
                        }
                        if (fn.equalsIgnoreCase(stem + ".ogg")) {
                            return true;
                        }
                        String fnStem = fn.substring(0, fn.length() - 4);
                        return fnStem.equalsIgnoreCase(stem);
                    })
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    public record PendingBox(double x1, double y1, double z1, double x2, double y2, double z2) {}

    private static final class CornerSession {
        String worldName;
        Corner c1;
        Corner c2;
    }

    private static final class Corner {
        final double x;
        final double y;
        final double z;

        Corner(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
