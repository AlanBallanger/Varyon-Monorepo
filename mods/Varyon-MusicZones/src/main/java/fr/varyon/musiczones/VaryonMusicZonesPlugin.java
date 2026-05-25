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

    public VaryonMusicZonesPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    public static VaryonMusicZonesPlugin getInstance() {
        return instance;
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
            ZoneMusicAssetGenerator.rebuildPack(this, repository.getZonesReadOnly());
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("[MusicZones] pack initial");
        }
    }

    @Override
    protected void shutdown() {
        instance = null;
    }

    public void rebuildAssetPack() throws Exception {
        ZoneMusicAssetGenerator.rebuildPack(this, repository.getZonesReadOnly());
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
