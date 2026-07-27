package com.varyon.bossarena.system;

import com.varyon.bossarena.config.BossArenaConfig;
import com.varyon.bossarena.data.Arena;
import com.varyon.bossarena.data.ArenaRegistry;
import com.varyon.bossarena.util.EntityComponents;
import com.varyon.bossarena.BossArenaPlugin;
import com.hypixel.hytale.math.vector.Transform;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.util.EventTitleUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Logger;

public final class BossWaveNotificationService {
    private static final Logger LOGGER = Logger.getLogger("BossArena");
    private static final double DEFAULT_NOTIFY_RADIUS = 100.0d;
    private static final float TRANSIENT_DURATION_SECONDS = 2.0f;
    /** Slightly above the 1s HUD refresh so the banner fades soon after the event stops updating. */
    private static final float PERSISTENT_DURATION_SECONDS = 1.4f;
    private static final float FINAL_CLEAR_DURATION_SECONDS = 2.5f;
    private static final float WORLD_ALERT_DURATION_SECONDS = 4.0f;
    private static final long WORLD_ALERT_DURATION_MILLIS = (long) (WORLD_ALERT_DURATION_SECONDS * 1000f);
    /** Slightly above the 5s scheduler tick so grace / waiting titles stay visible between refreshes. */
    private static final float TIMED_ARENA_TITLE_DURATION_SECONDS = 6.0f;
    private static final Map<UUID, Long> TIMED_ALERT_SUPPRESS_UNTIL = new ConcurrentHashMap<>();
    private static final Pattern PLACEHOLDER_PATTERN =
            Pattern.compile("\\$([A-Za-z][A-Za-z0-9_]*)|\\{([A-Za-z][A-Za-z0-9_]*)\\}");
    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("([§&])([0-9a-fA-FrRlLoOkKmMnN])");

    private BossWaveNotificationService() {
    }

    private static double resolveNotificationRadius() {
        BossArenaPlugin plugin = BossArenaPlugin.getInstance();
        BossArenaConfig config = plugin != null ? plugin.getConfig() : null;
        return config != null ? config.getNotificationRadius() : DEFAULT_NOTIFY_RADIUS;
    }

    public static void notifyBossAliveStatus(World world,
                                             Vector3d eventCenter,
                                             String bossName,
                                             int aliveBossCount,
                                             int activeAdds,
                                             String context) {
        notifyBossAliveStatus(world, eventCenter, bossName, aliveBossCount, activeAdds, context, -1L, false, true);
    }

    public static void notifyBossAliveStatus(World world,
                                             Vector3d eventCenter,
                                             String bossName,
                                             int aliveBossCount,
                                             int activeAdds,
                                             String context,
                                             long remainingCountdownMillis) {
        notifyBossAliveStatus(
                world,
                eventCenter,
                bossName,
                aliveBossCount,
                activeAdds,
                context,
                remainingCountdownMillis,
                false,
                true
        );
    }

    public static void notifyBossAliveStatus(World world,
                                             Vector3d eventCenter,
                                             String bossName,
                                             int aliveBossCount,
                                             int activeAdds,
                                             String context,
                                             long remainingCountdownMillis,
                                             boolean forceActiveState) {
        notifyBossAliveStatus(
                world,
                eventCenter,
                bossName,
                aliveBossCount,
                activeAdds,
                context,
                remainingCountdownMillis,
                forceActiveState,
                true
        );
    }

    public static void notifyBossAliveStatus(World world,
                                             Vector3d eventCenter,
                                             String bossName,
                                             int aliveBossCount,
                                             int activeAdds,
                                             String context,
                                             long remainingCountdownMillis,
                                             boolean forceActiveState,
                                             boolean showVictoryOnFinish) {
        notifyBossAliveStatus(world, eventCenter, bossName, aliveBossCount, activeAdds, context,
                remainingCountdownMillis, forceActiveState, showVictoryOnFinish, -1.0d);
    }

    /** Same as above but with per-arena notification radius (blocks). Use -1 or invalid to fall back to config default. */
    public static void notifyBossAliveStatus(World world,
                                             Vector3d eventCenter,
                                             String bossName,
                                             int aliveBossCount,
                                             int activeAdds,
                                             String context,
                                             long remainingCountdownMillis,
                                             boolean forceActiveState,
                                             boolean showVictoryOnFinish,
                                             double notificationRadiusBlocks) {
        notifyBossAliveStatus(
                world,
                eventCenter,
                bossName,
                aliveBossCount,
                activeAdds,
                context,
                remainingCountdownMillis,
                forceActiveState,
                showVictoryOnFinish,
                notificationRadiusBlocks,
                null,
                0,
                0
        );
    }

    public static void notifyBossAliveStatus(World world,
                                             Vector3d eventCenter,
                                             String bossName,
                                             int aliveBossCount,
                                             int activeAdds,
                                             String context,
                                             long remainingCountdownMillis,
                                             boolean forceActiveState,
                                             boolean showVictoryOnFinish,
                                             double notificationRadiusBlocks,
                                             UUID eventId,
                                             int currentWaveNumber) {
        notifyBossAliveStatus(
                world,
                eventCenter,
                bossName,
                aliveBossCount,
                activeAdds,
                context,
                remainingCountdownMillis,
                forceActiveState,
                showVictoryOnFinish,
                notificationRadiusBlocks,
                eventId,
                currentWaveNumber,
                0
        );
    }

    public static void notifyBossAliveStatus(World world,
                                             Vector3d eventCenter,
                                             String bossName,
                                             int aliveBossCount,
                                             int activeAdds,
                                             String context,
                                             long remainingCountdownMillis,
                                             boolean forceActiveState,
                                             boolean showVictoryOnFinish,
                                             double notificationRadiusBlocks,
                                             UUID eventId,
                                             int currentWaveNumber,
                                             int totalWaveCount) {
        if (world == null || eventCenter == null) {
            return;
        }
        int bossesAlive = Math.max(0, aliveBossCount);
        int addsAlive = Math.max(0, activeAdds);
        int monstersAlive = bossesAlive + addsAlive;
        boolean eventFinished = !forceActiveState && monstersAlive <= 0;
        String bossDisplay = safeBossDisplayName(bossName);
        float duration = (forceActiveState || monstersAlive > 0)
                ? PERSISTENT_DURATION_SECONDS
                : FINAL_CLEAR_DURATION_SECONDS;

        if (eventFinished) {
            if (!showVictoryOnFinish) {
                return;
            }
            String titleText = BossArenaConfig.DEFAULT_EVENT_VICTORY_TITLE_TEMPLATE;
            Message title = toPlainMessage(stripColorCodes(titleText));
            showToNearbyPlayers(
                    world,
                    eventCenter,
                    title,
                    duration,
                    notificationRadiusBlocks,
                    playerRef -> {
                        long damage = resolvePlayerDamage(eventId, playerRef);
                        return toPlainMessage(stripColorCodes("Dégâts infligés : " + formatDamageAmount(damage)));
                    }
            );
            return;
        }

        // In-combat "Boss : X" / "Monstres restants : X" title removed — this progress is now
        // shown in the BossDpsHud panel instead (wave progress / kill counts), not as a screen title.
    }

    /** Returns e.g. {@code Vague 4/5}, or {@code Vague 4} when total is unknown, or null if no wave yet. */
    static String formatWaveProgress(int currentWaveNumber, int totalWaveCount) {
        if (currentWaveNumber <= 0) {
            return null;
        }
        if (totalWaveCount > 0) {
            return "Vague " + currentWaveNumber + "/" + totalWaveCount;
        }
        return "Vague " + currentWaveNumber;
    }

    public static void notifyWaveSpawn(World world,
                                       Vector3d eventCenter,
                                       String bossName,
                                       int waveNumber,
                                       int spawnedNow,
                                       int activeAdds,
                                       long remainingCountdownMillis) {
        notifyWaveSpawn(world, eventCenter, bossName, waveNumber, 0, spawnedNow, activeAdds, remainingCountdownMillis, 1);
    }

    public static void notifyWaveSpawn(World world,
                                       Vector3d eventCenter,
                                       String bossName,
                                       int waveNumber,
                                       int totalWaveCount,
                                       int spawnedNow,
                                       int activeAdds,
                                       long remainingCountdownMillis,
                                       int aliveBossCount) {
        if (world == null || eventCenter == null || spawnedNow <= 0) {
            return;
        }
        notifyBossAliveStatus(
                world,
                eventCenter,
                bossName,
                Math.max(0, aliveBossCount),
                activeAdds,
                "Wave " + waveNumber + " spawned: " + spawnedNow,
                remainingCountdownMillis,
                true,
                true,
                -1.0d,
                null,
                waveNumber,
                totalWaveCount
        );
    }

    public static void notifyAddsRemaining(World world,
                                           Vector3d eventCenter,
                                           String bossName,
                                           int activeAdds) {
        notifyBossAliveStatus(world, eventCenter, bossName, 0, activeAdds, null);
    }

    public static void notifyTimedSpawn(String bossName,
                                        String arenaId,
                                        World world,
                                        String customMessage,
                                        boolean announceServerWide,
                                        boolean announceWorldWide) {
        LOGGER.info(() -> "notifyTimedSpawn called: boss=" + bossName + " arena=" + arenaId
                + " world=" + (world != null ? world.getName() : "null")
                + " serverWide=" + announceServerWide + " worldWide=" + announceWorldWide);

        // No announcement when both server-wide and world-wide are disabled; spawn still occurs, only the global alert is skipped.
        if ((!announceServerWide && !announceWorldWide) || world == null) {
            LOGGER.info(() -> "notifyTimedSpawn skipped: no announcement scope enabled or world is null (world="
                    + world + ")");
            return;
        }

        String bossDisplay = safeBossDisplayName(bossName);
        String arenaDisplay = safeText(arenaId, "unknown arena");
        String worldDisplay = safeText(world.getName(), "unknown world");

        String messageTemplate = customMessage == null || customMessage.isBlank()
                ? BossArenaConfig.DEFAULT_TIMED_ANNOUNCEMENT_TEXT
                : customMessage.trim();
        String chatMessage = applyTimedAnnouncementPlaceholders(messageTemplate, bossDisplay, arenaDisplay, worldDisplay);

        Message title = Message.raw("WORLD BOSS ALERT");
        Message subtitle = Message.raw("Boss : " + bossDisplay + " | Arène : " + arenaDisplay);

        // Players already at the arena get the boss spawn context from the in-arena HUD/notifications —
        // showing them the world-wide "WORLD BOSS ALERT" title on top is redundant. Chat message still
        // reaches everyone, only the on-screen title is suppressed for players already inside the arena.
        Vector3d arenaCenter = null;
        double arenaRadiusSq = -1.0d;
        Arena arena = ArenaRegistry.get(arenaId);
        if (arena != null) {
            arenaCenter = arena.getPosition();
            double arenaRadius = arena.getNotificationRadius();
            arenaRadiusSq = arenaRadius * arenaRadius;
        }
        final Vector3d finalArenaCenter = arenaCenter;
        final double finalArenaRadiusSq = arenaRadiusSq;

        Iterable<PlayerRef> targets;
        if (announceServerWide) {
            Universe universe = Universe.get();
            if (universe == null) {
                LOGGER.warning("notifyTimedSpawn aborted: server-wide announcement requested but Universe.get() returned null");
                return;
            }
            targets = universe.getPlayers();
        } else {
            targets = world.getPlayerRefs();
        }

        int notified = 0;
        int skippedInvalid = 0;
        int chatFailures = 0;
        int titleFailures = 0;
        for (PlayerRef playerRef : targets) {
            if (playerRef == null || !playerRef.isValid()) {
                skippedInvalid++;
                continue;
            }
            suppressLocalStatusTitles(playerRef);
            try {
                playerRef.sendMessage(toColoredMessage(chatMessage));
            } catch (Exception e) {
                chatFailures++;
                LOGGER.warning("Failed to send timed global alert chat message to "
                        + playerRef.getUuid() + ": " + e.getMessage());
            }
            if (isPlayerInsideArena(playerRef, finalArenaCenter, finalArenaRadiusSq)) {
                continue;
            }
            try {
                EventTitleUtil.hideEventTitleFromPlayer(playerRef, 0f);
                EventTitleUtil.showEventTitleToPlayer(
                        playerRef,
                        title,
                        subtitle,
                        true,
                        null,
                        WORLD_ALERT_DURATION_SECONDS,
                        0f,
                        0f
                );
                notified++;
            } catch (Exception e) {
                titleFailures++;
                LOGGER.warning("Failed to show timed global alert title to "
                        + playerRef.getUuid() + ": " + e.getMessage());
            }
        }

        final int notifiedFinal = notified;
        final int skippedInvalidFinal = skippedInvalid;
        final int chatFailuresFinal = chatFailures;
        final int titleFailuresFinal = titleFailures;
        LOGGER.info(() -> "notifyTimedSpawn done: boss=" + bossDisplay + " arena=" + arenaDisplay
                + " scope=" + (announceServerWide ? "server" : "world") + " notified=" + notifiedFinal
                + " skippedInvalid=" + skippedInvalidFinal + " chatFailures=" + chatFailuresFinal
                + " titleFailures=" + titleFailuresFinal);
    }

    private static void showToNearbyPlayers(World world,
                                            Vector3d center,
                                            Message title,
                                            float durationSeconds,
                                            double notificationRadiusBlocks,
                                            java.util.function.Function<PlayerRef, Message> subtitleForPlayer) {
        double radius = (Double.isFinite(notificationRadiusBlocks) && notificationRadiusBlocks > 0)
                ? notificationRadiusBlocks
                : resolveNotificationRadius();
        long now = System.currentTimeMillis();
        final String titleTextForLog = title != null ? title.toString() : "null";
        LOGGER.info(() -> "showToNearbyPlayers: world=" + world.getName() + " center=" + center
                + " radius=" + radius + " duration=" + durationSeconds + " title=" + titleTextForLog);
        int shown = 0;
        int outOfRange = 0;
        int failures = 0;
        for (PlayerRef playerRef : world.getPlayerRefs()) {
            if (playerRef == null) {
                continue;
            }
            if (isLocalStatusSuppressed(playerRef, now)) {
                continue;
            }

            Transform transform = playerRef.getTransform();
            org.joml.Vector3d rawPlayerPos = transform != null ? transform.getPosition() : null;
            if (rawPlayerPos == null) {
                continue;
            }
            Vector3d playerPosition = new Vector3d(rawPlayerPos.x, rawPlayerPos.y, rawPlayerPos.z);

            if (playerPosition.distance(center) > radius) {
                outOfRange++;
                try {
                    EventTitleUtil.hideEventTitleFromPlayer(playerRef, 0f);
                } catch (Exception e) {
                    LOGGER.fine(() -> "Failed to hide out-of-range wave notification: " + e.getMessage());
                }
                continue;
            }

            try {
                EventTitleUtil.hideEventTitleFromPlayer(playerRef, 0f);
                Message subtitle = subtitleForPlayer != null ? subtitleForPlayer.apply(playerRef) : null;
                if (title != null || subtitle != null) {
                    EventTitleUtil.showEventTitleToPlayer(
                            playerRef,
                            title,
                            subtitle,
                            true,
                            null,
                            durationSeconds,
                            0f,
                            0f
                    );
                    shown++;
                }
            } catch (Exception e) {
                failures++;
                LOGGER.warning("Failed to update wave notification visibility for "
                        + playerRef.getUuid() + ": " + e.getMessage());
            }
        }
        final int shownFinal = shown;
        final int outOfRangeFinal = outOfRange;
        final int failuresFinal = failures;
        LOGGER.info(() -> "showToNearbyPlayers done: shown=" + shownFinal
                + " outOfRange=" + outOfRangeFinal + " failures=" + failuresFinal);
    }

    private static long resolvePlayerDamage(UUID eventId, PlayerRef playerRef) {
        if (eventId == null || playerRef == null) {
            return 0L;
        }
        BossArenaPlugin plugin = BossArenaPlugin.getInstance();
        if (plugin == null || plugin.getDamageChartTracker() == null) {
            return 0L;
        }
        UUID playerUuid = playerRef.getUuid();
        if (playerUuid == null) {
            return 0L;
        }
        return plugin.getDamageChartTracker().getDamage(eventId, playerUuid);
    }

    private static String formatDamageAmount(long damage) {
        long safe = Math.max(0L, damage);
        if (safe >= 1_000_000L) {
            return String.format(Locale.ROOT, "%.1fM", safe / 1_000_000.0d);
        }
        if (safe >= 1_000L) {
            return String.format(Locale.ROOT, "%.1fK", safe / 1_000.0d);
        }
        return Long.toString(safe);
    }

    private static String safeBossName(String bossName) {
        if (bossName == null || bossName.isBlank()) {
            return "Boss";
        }
        return bossName.trim().toUpperCase(Locale.ROOT);
    }

    private static String safeBossDisplayName(String bossName) {
        if (bossName == null || bossName.isBlank()) {
            return "Boss";
        }
        return bossName.trim();
    }

    private static String safeText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private static void suppressLocalStatusTitles(PlayerRef playerRef) {
        if (playerRef == null) {
            return;
        }
        UUID playerUuid = EntityComponents.uuid(playerRef);
        if (playerUuid == null) {
            return;
        }
        TIMED_ALERT_SUPPRESS_UNTIL.put(playerUuid, System.currentTimeMillis() + WORLD_ALERT_DURATION_MILLIS);
    }

    private static boolean isPlayerInsideArena(PlayerRef playerRef, Vector3d arenaCenter, double arenaRadiusSq) {
        if (playerRef == null || arenaCenter == null || arenaRadiusSq <= 0.0d) {
            return false;
        }
        Transform transform = playerRef.getTransform();
        org.joml.Vector3d rawPos = transform != null ? transform.getPosition() : null;
        if (rawPos == null) {
            return false;
        }
        double dx = rawPos.x - arenaCenter.x;
        double dy = rawPos.y - arenaCenter.y;
        double dz = rawPos.z - arenaCenter.z;
        return (dx * dx) + (dy * dy) + (dz * dz) <= arenaRadiusSq;
    }

    private static boolean isLocalStatusSuppressed(PlayerRef playerRef, long nowEpochMs) {
        if (playerRef == null) {
            return false;
        }
        UUID playerUuid = EntityComponents.uuid(playerRef);
        if (playerUuid == null) {
            return false;
        }
        Long untilEpochMs = TIMED_ALERT_SUPPRESS_UNTIL.get(playerUuid);
        if (untilEpochMs == null) {
            return false;
        }
        if (untilEpochMs > nowEpochMs) {
            return true;
        }
        TIMED_ALERT_SUPPRESS_UNTIL.remove(playerUuid, untilEpochMs);
        return false;
    }

    /**
     * Title for players inside the arena Rayon Décl during the pre-spawn grace countdown.
     * {@code $time} is formatted as {@code mm:ss}.
     */
    public static void notifyTimedGraceTitle(World world,
                                             Vector3d arenaCenter,
                                             double radiusBlocks,
                                             String bossName,
                                             String arenaId,
                                             String customTemplate,
                                             long remainingMillis) {
        if (world == null || arenaCenter == null || radiusBlocks <= 0.0d) {
            return;
        }
        String bossDisplay = safeBossDisplayName(bossName);
        String arenaDisplay = safeText(arenaId, "Arena");
        String worldDisplay = safeText(world.getName(), "World");
        String template = customTemplate == null || customTemplate.isBlank()
                ? BossArenaConfig.DEFAULT_TIMED_GRACE_TITLE_TEXT
                : customTemplate.trim();
        String titleText = applyTimedAnnouncementPlaceholders(
                template,
                bossDisplay,
                arenaDisplay,
                worldDisplay,
                formatCountdownValue(remainingMillis)
        );
        if (titleText.isBlank()) {
            return;
        }
        Message title = toPlainMessage(stripColorCodes(titleText));
        Message subtitle = toPlainMessage(bossDisplay);
        showTitleInRadius(world, arenaCenter, radiusBlocks, title, subtitle, TIMED_ARENA_TITLE_DURATION_SECONDS);
    }

    /** Fixed waiting title when Planifié needs more players in the Rayon Décl. */
    public static void notifyTimedWaitingPlayersTitle(World world,
                                                      Vector3d arenaCenter,
                                                      double radiusBlocks,
                                                      int requiredPlayers) {
        if (world == null || arenaCenter == null || radiusBlocks <= 0.0d || requiredPlayers <= 0) {
            return;
        }
        String titleText = "Vous devez être " + requiredPlayers
                + " joueurs minimum pour que le combat se lance";
        Message title = toPlainMessage(titleText);
        showTitleInRadius(world, arenaCenter, radiusBlocks, title, null, TIMED_ARENA_TITLE_DURATION_SECONDS);
    }

    private static void showTitleInRadius(World world,
                                          Vector3d center,
                                          double radiusBlocks,
                                          Message title,
                                          Message subtitle,
                                          float durationSeconds) {
        double radiusSq = radiusBlocks * radiusBlocks;
        final String titleTextForLog = title != null ? title.toString() : "null";
        LOGGER.info(() -> "showTitleInRadius: world=" + world.getName() + " center=" + center
                + " radiusBlocks=" + radiusBlocks + " duration=" + durationSeconds + " title=" + titleTextForLog);
        int shown = 0;
        int outOfRange = 0;
        int failures = 0;
        for (PlayerRef playerRef : world.getPlayerRefs()) {
            if (playerRef == null || !playerRef.isValid()) {
                continue;
            }
            Transform transform = playerRef.getTransform();
            org.joml.Vector3d rawPlayerPos = transform != null ? transform.getPosition() : null;
            if (rawPlayerPos == null) {
                continue;
            }
            double dx = rawPlayerPos.x - center.x;
            double dy = rawPlayerPos.y - center.y;
            double dz = rawPlayerPos.z - center.z;
            if ((dx * dx) + (dy * dy) + (dz * dz) > radiusSq) {
                outOfRange++;
                continue;
            }
            try {
                EventTitleUtil.hideEventTitleFromPlayer(playerRef, 0f);
                if (title != null || subtitle != null) {
                    EventTitleUtil.showEventTitleToPlayer(
                            playerRef,
                            title,
                            subtitle,
                            true,
                            null,
                            durationSeconds,
                            0f,
                            0f
                    );
                    shown++;
                }
            } catch (Exception e) {
                failures++;
                LOGGER.warning("Failed to show timed arena title to "
                        + playerRef.getUuid() + ": " + e.getMessage());
            }
        }
        final int shownFinal = shown;
        final int outOfRangeFinal = outOfRange;
        final int failuresFinal = failures;
        LOGGER.info(() -> "showTitleInRadius done: shown=" + shownFinal
                + " outOfRange=" + outOfRangeFinal + " failures=" + failuresFinal);
    }

    private static String applyTimedAnnouncementPlaceholders(String template,
                                                             String bossDisplay,
                                                             String arenaDisplay,
                                                             String worldDisplay) {
        return applyTimedAnnouncementPlaceholders(template, bossDisplay, arenaDisplay, worldDisplay, null);
    }

    private static String applyTimedAnnouncementPlaceholders(String template,
                                                             String bossDisplay,
                                                             String arenaDisplay,
                                                             String worldDisplay,
                                                             String timeDisplay) {
        if (template == null || template.isBlank()) {
            return "";
        }
        Map<String, String> values = Map.of(
                "boss", defaultIfBlank(bossDisplay, "Boss"),
                "arena", defaultIfBlank(arenaDisplay, "Arena"),
                "world", defaultIfBlank(worldDisplay, "World"),
                "time", defaultIfBlank(timeDisplay, "")
        );
        return renderTemplate(template, values);
    }

    private static String formatCountdownValue(long remainingCountdownMillis) {
        if (remainingCountdownMillis < 0L) {
            return "";
        }

        long totalSeconds = Math.max(0L, remainingCountdownMillis / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
    }

    private static BossArenaConfig.EventBannerTemplates resolveEventBannerTemplates() {
        BossArenaConfig.EventBannerTemplates out = new BossArenaConfig.EventBannerTemplates();
        BossArenaPlugin plugin = BossArenaPlugin.getInstance();
        BossArenaConfig config = plugin != null ? plugin.getConfig() : null;
        if (config == null || config.eventBanner == null) {
            return out;
        }
        BossArenaConfig.EventBannerTemplates loaded = config.eventBanner;
        out.activeTitle = defaultIfBlank(loaded.activeTitle, BossArenaConfig.DEFAULT_EVENT_ACTIVE_TITLE_TEMPLATE);
        out.activeSubtitle = defaultIfBlank(loaded.activeSubtitle, BossArenaConfig.DEFAULT_EVENT_ACTIVE_SUBTITLE_TEMPLATE);
        out.victoryTitle = defaultIfBlank(loaded.victoryTitle, BossArenaConfig.DEFAULT_EVENT_VICTORY_TITLE_TEMPLATE);
        out.victorySubtitle = defaultIfBlank(loaded.victorySubtitle, BossArenaConfig.DEFAULT_EVENT_VICTORY_SUBTITLE_TEMPLATE);
        return out;
    }

    private static String applyEventBannerPlaceholders(String template,
                                                       String bossDisplay,
                                                       int bossesAlive,
                                                       int addsAlive,
                                                       String contextRaw,
                                                       String contextPrefix,
                                                       String countdown,
                                                       String countdownLabel,
                                                       String countdownPrefix,
                                                       boolean eventFinished) {
        String resolved = defaultIfBlank(template, "");
        String context = contextRaw == null || contextRaw.isBlank() ? "" : contextRaw.trim();
        String state = eventFinished ? "victory" : "active";
        Map<String, String> values = Map.ofEntries(
                Map.entry("boss", defaultIfBlank(bossDisplay, "Boss")),
                Map.entry("bossupper", safeBossName(bossDisplay)),
                Map.entry("bossalive", Integer.toString(Math.max(0, bossesAlive))),
                Map.entry("addsalive", Integer.toString(Math.max(0, addsAlive))),
                Map.entry("context", context),
                Map.entry("contextraw", context),
                // Clearer aliases for user-facing templates.
                Map.entry("contextline", defaultIfBlank(contextPrefix, "")),
                // Backwards-compatible legacy alias.
                Map.entry("contextprefix", defaultIfBlank(contextPrefix, "")),
                Map.entry("countdown", defaultIfBlank(countdown, "")),
                Map.entry("countdownlabel", defaultIfBlank(countdownLabel, "")),
                // Clearer aliases for user-facing templates.
                Map.entry("countdownline", defaultIfBlank(countdownPrefix, "")),
                // Backwards-compatible legacy alias.
                Map.entry("countdownprefix", defaultIfBlank(countdownPrefix, "")),
                Map.entry("state", state)
        );
        return renderTemplate(resolved, values);
    }

    private static String renderTemplate(String template, Map<String, String> values) {
        if (template == null || template.isEmpty()) {
            return "";
        }
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            String normalized = key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
            String replacement = values.getOrDefault(normalized, "");
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static String defaultIfBlank(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    /** Removes & and § color codes (e.g. &7, §e) so text displays cleanly when the banner doesn't support formatting. */
    private static String stripColorCodes(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return COLOR_CODE_PATTERN.matcher(text).replaceAll("");
    }

    private static Message toPlainMessage(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        return Message.raw(text);
    }

    /**
     * Builds a chat {@link Message} that applies Minecraft-style {@code &} / {@code §} codes:
     * colors {@code 0-9a-f}, bold {@code &l}, italic {@code &o}, reset {@code &r}.
     */
    static Message toColoredMessage(String text) {
        if (text == null || text.isEmpty()) {
            return Message.raw("");
        }
        String normalized = text.replace('§', '&');
        if (!normalized.contains("&")) {
            return Message.raw(normalized);
        }
        List<Message> parts = new ArrayList<>();
        String currentHex = null;
        boolean bold = false;
        boolean italic = false;
        String[] chunks = normalized.split("(?=&[0-9a-fk-orA-FK-OR])");
        for (String chunk : chunks) {
            if (chunk.isEmpty()) {
                continue;
            }
            if (chunk.length() < 2 || chunk.charAt(0) != '&') {
                parts.add(applyStyle(Message.raw(chunk), currentHex, bold, italic));
                continue;
            }
            char code = Character.toLowerCase(chunk.charAt(1));
            String content = chunk.substring(2);
            if (code == 'l') {
                bold = true;
            } else if (code == 'o') {
                italic = true;
            } else if (code == 'r') {
                currentHex = null;
                bold = false;
                italic = false;
            } else {
                String hex = hexFromLegacyColorCode(code);
                if (hex != null) {
                    currentHex = hex;
                }
            }
            if (!content.isEmpty()) {
                parts.add(applyStyle(Message.raw(content), currentHex, bold, italic));
            }
        }
        if (parts.isEmpty()) {
            return Message.raw("");
        }
        if (parts.size() == 1) {
            return parts.get(0);
        }
        return Message.join(parts.toArray(new Message[0]));
    }

    private static Message applyStyle(Message message, String hex, boolean bold, boolean italic) {
        Message out = message;
        if (hex != null && !hex.isEmpty()) {
            out = out.color(hex);
        }
        if (bold) {
            out = out.bold(true);
        }
        if (italic) {
            out = out.italic(true);
        }
        return out;
    }

    private static String hexFromLegacyColorCode(char code) {
        return switch (code) {
            case '0' -> "#000000";
            case '1' -> "#0000AA";
            case '2' -> "#00AA00";
            case '3' -> "#00AAAA";
            case '4' -> "#AA0000";
            case '5' -> "#AA00AA";
            case '6' -> "#FFAA00";
            case '7' -> "#AAAAAA";
            case '8' -> "#555555";
            case '9' -> "#5555FF";
            case 'a' -> "#55FF55";
            case 'b' -> "#55FFFF";
            case 'c' -> "#FF5555";
            case 'd' -> "#FF55FF";
            case 'e' -> "#FFFF55";
            case 'f' -> "#FFFFFF";
            default -> null;
        };
    }
}
