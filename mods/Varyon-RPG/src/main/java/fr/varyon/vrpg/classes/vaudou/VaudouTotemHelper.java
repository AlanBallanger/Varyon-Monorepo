package fr.varyon.vrpg.classes.vaudou;

import com.hypixel.hytale.builtin.deployables.config.DeployableConfig;
import com.hypixel.hytale.builtin.deployables.interaction.SpawnDeployableAtLocationInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class VaudouTotemHelper {

    public record ActiveTotem(
        @Nonnull Vector3d position,
        long expiresAt,
        float radius,
        @Nonnull TotemType type,
        float magnitude
    ) {}

    public enum TotemType { SLOWNESS, VULNERABILITY }

    private static final CopyOnWriteArrayList<ActiveTotem> GLOBAL_TOTEMS = new CopyOnWriteArrayList<>();

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "VaudouTotemHelper-restore");
        t.setDaemon(true);
        return t;
    });

    private static final long RESTORE_DELAY_MS = 5000;

    private static final float TOTEM_RADIUS = 20.0f;

    private static Field LIVE_DURATION_FIELD = null;
    private static Field SPAWN_DEPLOYABLE_CONFIG_FIELD = null;

    static {
        try {
            LIVE_DURATION_FIELD = DeployableConfig.class.getDeclaredField("liveDuration");
            LIVE_DURATION_FIELD.setAccessible(true);
            System.out.println("[VaudouTotem] liveDuration field resolved OK");
        } catch (Exception e) {
            System.out.println("[VaudouTotem] liveDuration field FAILED: " + e);
        }
        try {
            SPAWN_DEPLOYABLE_CONFIG_FIELD = SpawnDeployableAtLocationInteraction.class.getDeclaredField("config");
            SPAWN_DEPLOYABLE_CONFIG_FIELD.setAccessible(true);
            System.out.println("[VaudouTotem] SpawnDeployableAtLocationInteraction.config field resolved OK");
        } catch (Exception e) {
            System.out.println("[VaudouTotem] SpawnDeployableAtLocationInteraction.config field FAILED: " + e);
        }
    }

    private VaudouTotemHelper() {}

    private static List<DeployableConfig> findDeployableConfigs(String deployableId) {
        List<DeployableConfig> result = new ArrayList<>();
        if (SPAWN_DEPLOYABLE_CONFIG_FIELD == null) return result;
        try {
            Map<?, ?> allInteractions = Interaction.getAssetMap().getAssetMap();
            for (Object interaction : allInteractions.values()) {
                if (!(interaction instanceof SpawnDeployableAtLocationInteraction spawn)) continue;
                DeployableConfig cfg = (DeployableConfig) SPAWN_DEPLOYABLE_CONFIG_FIELD.get(spawn);
                if (cfg != null && deployableId.equals(cfg.getId())) {
                    result.add(cfg);
                    System.out.println("[VaudouTotem] Found DeployableConfig id=" + deployableId + " in interaction " + spawn);
                }
            }
        } catch (Exception e) {
            System.out.println("[VaudouTotem] findDeployableConfigs failed: " + e);
        }
        if (result.isEmpty()) {
            System.out.println("[VaudouTotem] findDeployableConfigs: no configs found for id=" + deployableId);
        }
        return result;
    }

    public static void patchAndRun(@Nonnull String deployableId, long durationMs, @Nonnull Runnable spawnProjectile) {
        if (LIVE_DURATION_FIELD == null) {
            System.out.println("[VaudouTotem] patchAndRun: LIVE_DURATION_FIELD null, running without patch");
            spawnProjectile.run();
            return;
        }
        List<DeployableConfig> configs = findDeployableConfigs(deployableId);
        if (configs.isEmpty()) {
            System.out.println("[VaudouTotem] patchAndRun: no configs to patch, running without patch");
            spawnProjectile.run();
            return;
        }
        float newDuration = durationMs / 1000f;
        float[] originals = new float[configs.size()];
        try {
            for (int i = 0; i < configs.size(); i++) {
                originals[i] = configs.get(i).getLiveDuration();
                LIVE_DURATION_FIELD.setFloat(configs.get(i), newDuration);
                System.out.println("[VaudouTotem] patched liveDuration " + originals[i] + " -> " + newDuration + " on " + configs.get(i).getId());
            }
            spawnProjectile.run();
        } catch (Exception e) {
            System.out.println("[VaudouTotem] patchAndRun patch failed: " + e);
            spawnProjectile.run();
        }
        final List<DeployableConfig> capturedConfigs = List.copyOf(configs);
        final float[] capturedOriginals = originals.clone();
        SCHEDULER.schedule(() -> {
            for (int i = 0; i < capturedConfigs.size(); i++) {
                try {
                    LIVE_DURATION_FIELD.setFloat(capturedConfigs.get(i), capturedOriginals[i]);
                    System.out.println("[VaudouTotem] restored liveDuration -> " + capturedOriginals[i] + " on " + capturedConfigs.get(i).getId());
                } catch (Exception ignored) {}
            }
        }, RESTORE_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    public static void deploySlownessTotem(@Nonnull Vector3d pos, long durationMs) {
        long expiresAt = System.currentTimeMillis() + durationMs;
        GLOBAL_TOTEMS.removeIf(t -> System.currentTimeMillis() >= t.expiresAt());
        GLOBAL_TOTEMS.add(new ActiveTotem(new Vector3d(pos), expiresAt, TOTEM_RADIUS, TotemType.SLOWNESS, 0f));
    }

    public static void deployVulnerabilityTotem(@Nonnull Vector3d pos, long durationMs, float magnitude) {
        long expiresAt = System.currentTimeMillis() + durationMs;
        GLOBAL_TOTEMS.removeIf(t -> System.currentTimeMillis() >= t.expiresAt());
        GLOBAL_TOTEMS.add(new ActiveTotem(new Vector3d(pos), expiresAt, TOTEM_RADIUS, TotemType.VULNERABILITY, magnitude));
    }

    public static boolean isInAnyTotem(@Nonnull Vector3d pos) {
        long now = System.currentTimeMillis();
        for (ActiveTotem t : GLOBAL_TOTEMS) {
            if (now >= t.expiresAt()) continue;
            double dx = pos.x - t.position().x;
            double dz = pos.z - t.position().z;
            if (dx * dx + dz * dz <= t.radius() * t.radius()) return true;
        }
        return false;
    }

    public static float getVulnerabilityMagnitudeAt(@Nonnull Vector3d pos) {
        long now = System.currentTimeMillis();
        float max = 0f;
        for (ActiveTotem t : GLOBAL_TOTEMS) {
            if (t.type() != TotemType.VULNERABILITY) continue;
            if (now >= t.expiresAt()) continue;
            double dx = pos.x - t.position().x;
            double dz = pos.z - t.position().z;
            if (dx * dx + dz * dz <= t.radius() * t.radius()) {
                if (t.magnitude() > max) max = t.magnitude();
            }
        }
        return max;
    }
}
