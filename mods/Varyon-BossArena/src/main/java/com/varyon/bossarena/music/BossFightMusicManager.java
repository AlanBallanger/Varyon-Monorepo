package com.varyon.bossarena.music;

import com.hypixel.hytale.server.core.asset.type.ambiencefx.config.AmbienceFX;
import com.hypixel.hytale.server.core.universe.world.World;
import com.varyon.bossarena.data.BossDefinition;
import org.joml.Vector3d;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class BossFightMusicManager {

    private static final Logger LOGGER = Logger.getLogger("Varyon-BossArena");
    private static final String SESSION_PREFIX = "bossarena-";

    private final Path musicDirectory;
    private final Map<String, BossFightMusicSession> sessions = new ConcurrentHashMap<>();
    private final AtomicLong generationCounter = new AtomicLong(1);

    public BossFightMusicManager(Path musicDirectory) {
        this.musicDirectory = musicDirectory;
    }

    public Path getMusicDirectory() {
        return musicDirectory;
    }

    public void start() {
        try {
            Files.createDirectories(musicDirectory);
        } catch (Exception e) {
            LOGGER.warning("[BossArena] Impossible de créer music/: " + e.getMessage());
        }
        rebuildPack();
    }

    public void shutdown() {
        sessions.clear();
    }

    public List<String> listMusicFileNames() {
        if (!Files.isDirectory(musicDirectory, LinkOption.NOFOLLOW_LINKS)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(musicDirectory)) {
            return stream.filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .filter(n -> n.toLowerCase(Locale.ROOT).endsWith(".ogg"))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(Collectors.toUnmodifiableList());
        } catch (Exception e) {
            LOGGER.warning("[BossArena] listMusicFileNames: " + e.getMessage());
            return List.of();
        }
    }

    public boolean hasActiveSessions() {
        return !sessions.isEmpty();
    }

    public List<BossFightMusicSession> sessionsForWorld(String worldName) {
        if (worldName == null || worldName.isBlank() || sessions.isEmpty()) {
            return List.of();
        }
        List<BossFightMusicSession> out = new ArrayList<>();
        for (BossFightMusicSession s : sessions.values()) {
            if (worldName.equals(s.getWorldName())) {
                out.add(s);
            }
        }
        return out;
    }

    public void startForEvent(UUID eventId, World world, Vector3d center, BossDefinition def) {
        if (eventId == null || world == null || center == null || def == null || !def.hasFightMusic()) {
            return;
        }
        String worldName = world.getName();
        if (worldName == null || worldName.isBlank()) {
            return;
        }
        Path resolved = resolveMusicFile(def.musicFileName.trim());
        if (resolved == null) {
            LOGGER.warning("[BossArena] Musique manquante dans " + musicDirectory.toAbsolutePath()
                    + " : " + def.musicFileName);
            return;
        }
        String fileName = resolved.getFileName().toString();
        long generation = generationCounter.getAndIncrement();
        int slot = BossFightMusicIds.normalizeSlot((int) generation);
        AmbienceFX packed = AmbienceFX.getAssetMap().getAsset(BossFightMusicIds.ambienceAssetId(fileName, slot));
        if (packed == null || packed.getMusicContainerIndex() < 0) {
            rebuildPack();
            packed = AmbienceFX.getAssetMap().getAsset(BossFightMusicIds.ambienceAssetId(fileName, slot));
        }
        if (packed == null || packed.getMusicContainerIndex() < 0) {
            LOGGER.warning("[BossArena] Track packé mais AmbienceFX introuvable pour '" + fileName
                    + "' (id=" + BossFightMusicIds.ambienceAssetId(fileName, slot) + ")");
        }
        sessions.put(
                sessionId(eventId),
                new BossFightMusicSession(
                        sessionId(eventId),
                        worldName,
                        center.x,
                        center.y,
                        center.z,
                        def.getMusicRadius(),
                        fileName,
                        generation));
        LOGGER.info("[BossArena] Musique combat démarrée: " + fileName
                + " slot=" + slot
                + " rayon=" + def.getMusicRadius()
                + " centre=(" + center.x + "," + center.y + "," + center.z + ")"
                + " monde=" + worldName
                + " event=" + eventId);
    }

    public void stopForEvent(UUID eventId) {
        if (eventId == null) {
            return;
        }
        sessions.remove(sessionId(eventId));
    }

    public void rebuildPack() {
        try {
            BossFightMusicAssetGenerator.rebuildPack(musicDirectory);
        } catch (Exception e) {
            LOGGER.warning("[BossArena] rebuildPack musique: " + e.getMessage());
        }
    }

    private Path resolveMusicFile(String name) {
        if (name == null || name.isBlank() || !Files.isDirectory(musicDirectory, LinkOption.NOFOLLOW_LINKS)) {
            return null;
        }
        String base = name.trim().replace('\\', '/');
        if (base.contains("/")) {
            base = base.substring(base.lastIndexOf('/') + 1);
        }
        final String stem = base.toLowerCase(Locale.ROOT).endsWith(".ogg")
                ? base.substring(0, base.length() - 4)
                : base;
        try (Stream<Path> stream = Files.list(musicDirectory)) {
            return stream.filter(Files::isRegularFile)
                    .filter(p -> {
                        String fn = p.getFileName().toString();
                        if (!fn.toLowerCase(Locale.ROOT).endsWith(".ogg")) {
                            return false;
                        }
                        if (fn.equalsIgnoreCase(stem + ".ogg")) {
                            return true;
                        }
                        return fn.substring(0, fn.length() - 4).equalsIgnoreCase(stem);
                    })
                    .findFirst()
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private static String sessionId(UUID eventId) {
        return SESSION_PREFIX + eventId;
    }
}
