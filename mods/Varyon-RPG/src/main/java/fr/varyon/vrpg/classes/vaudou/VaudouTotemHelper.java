package fr.varyon.vrpg.classes.vaudou;

import com.hypixel.hytale.builtin.deployables.DeployablesUtils;
import com.hypixel.hytale.builtin.deployables.config.DeployableConfig;
import com.hypixel.hytale.builtin.deployables.config.DeployableSpawner;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.MathUtil;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.server.core.modules.entity.DespawnComponent;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.vrpg.classes.EntityStoreCommandBuffers;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CopyOnWriteArrayList;

public final class VaudouTotemHelper {

    public record ActiveTotem(
        @Nonnull Vector3d position,
        long expiresAt,
        float radius,
        @Nonnull TotemType type,
        float magnitude,
        @Nullable Ref<EntityStore> deployableRef
    ) {}

    public enum TotemType { SLOWNESS, VULNERABILITY }

    private static final CopyOnWriteArrayList<ActiveTotem> GLOBAL_TOTEMS = new CopyOnWriteArrayList<>();

    public static final double THROW_RANGE = 14.0;

    private static final float TOTEM_RADIUS = 20.0f;

    private static final String SLOWNESS_DEPLOYABLE = "Vrpg_Slowness_Totem";
    private static final String VULNERABILITY_DEPLOYABLE = "Vrpg_Vulnerability_Totem";

    private VaudouTotemHelper() {}

    @Nonnull
    public static Vector3d throwLandingPosition(@Nullable World world,
                                                 @Nonnull Vector3d origin,
                                                 @Nonnull Vector3d direction) {
        if (world == null) return new Vector3d(origin);
        Vector3d dir = new Vector3d(direction);
        if (dir.lengthSquared() < 1e-8) return new Vector3d(origin);
        dir.normalize();

        Vector3d hit = fr.varyon.vrpg.classes.ability.BlockRaystep.hitPosition(
            world, origin, dir, THROW_RANGE, 0.25);
        double traveled = hit.distance(origin);
        if (traveled >= THROW_RANGE - 0.35) {
            double groundY = findGroundY(world, hit.x, hit.z, origin.y + 0.5);
            return new Vector3d(hit.x, groundY, hit.z);
        }
        return hit;
    }

    private static double findGroundY(@Nonnull World world, double x, double z, double searchTopY) {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        int top = (int) Math.floor(searchTopY);
        for (int by = top; by >= top - 64; by--) {
            if (!isSolidBlock(world, bx, by, bz)) continue;
            if (isSolidBlock(world, bx, by + 1, bz)) continue;
            return by + 1.0;
        }
        return searchTopY;
    }

    private static boolean isSolidBlock(@Nonnull World world, int x, int y, int z) {
        try {
            int blockId = world.getBlock(x, y, z);
            if (blockId == 0) return false;
            BlockType blockType = BlockType.getAssetMap().getAsset(blockId);
            return blockType != null && blockType.getMaterial() == BlockMaterial.Solid;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void activateTotem(@Nonnull World world,
                                     @Nonnull Store<EntityStore> store,
                                     @Nonnull Ref<EntityStore> casterRef,
                                     @Nonnull Vector3d pos,
                                     long durationMs,
                                     @Nonnull TotemType type,
                                     float magnitude) {
        Ref<EntityStore> deployableRef = spawnDeployableVisual(store, casterRef, pos, type, durationMs);
        if (type == TotemType.SLOWNESS) {
            deploySlownessTotem(pos, durationMs, deployableRef);
        } else {
            deployVulnerabilityTotem(pos, durationMs, magnitude, deployableRef);
        }
        scheduleDeployableCleanup(world, deployableRef, durationMs);
    }

    public static void cleanupExpired(@Nonnull World world) {
        long now = System.currentTimeMillis();
        Store<EntityStore> store = world.getEntityStore().getStore();
        for (ActiveTotem totem : GLOBAL_TOTEMS) {
            if (now < totem.expiresAt()) continue;
            Ref<EntityStore> deployableRef = totem.deployableRef();
            if (deployableRef != null && deployableRef.isValid()) {
                removeDeployable(store, deployableRef);
            }
        }
        GLOBAL_TOTEMS.removeIf(t -> now >= t.expiresAt());
    }

    public static float damageMultiplierAt(@Nonnull Vector3d pos, int rituelRank) {
        float mult = 1f;
        if (rituelRank > 0 && isInAnyTotem(pos)) {
            mult += VaudouPassifs.rituelBonusForRank(rituelRank);
        }
        float vuln = getVulnerabilityMagnitudeAt(pos);
        if (vuln > 0f) {
            mult += vuln;
        }
        return mult;
    }

    public static void deploySlownessTotem(@Nonnull Vector3d pos, long durationMs, @Nullable Ref<EntityStore> deployableRef) {
        long expiresAt = System.currentTimeMillis() + durationMs;
        GLOBAL_TOTEMS.removeIf(t -> System.currentTimeMillis() >= t.expiresAt());
        GLOBAL_TOTEMS.add(new ActiveTotem(new Vector3d(pos), expiresAt, TOTEM_RADIUS, TotemType.SLOWNESS, 0f, deployableRef));
    }

    public static void deployVulnerabilityTotem(@Nonnull Vector3d pos, long durationMs, float magnitude,
                                                @Nullable Ref<EntityStore> deployableRef) {
        long expiresAt = System.currentTimeMillis() + durationMs;
        GLOBAL_TOTEMS.removeIf(t -> System.currentTimeMillis() >= t.expiresAt());
        GLOBAL_TOTEMS.add(new ActiveTotem(new Vector3d(pos), expiresAt, TOTEM_RADIUS, TotemType.VULNERABILITY, magnitude, deployableRef));
    }

    @Nullable
    private static Ref<EntityStore> spawnDeployableVisual(@Nonnull Store<EntityStore> store,
                                                          @Nonnull Ref<EntityStore> casterRef,
                                                          @Nonnull Vector3d landing,
                                                          @Nonnull TotemType type,
                                                          long durationMs) {
        String spawnerId = type == TotemType.SLOWNESS ? SLOWNESS_DEPLOYABLE : VULNERABILITY_DEPLOYABLE;
        DeployableSpawner spawner = DeployableSpawner.getAssetMap().getAsset(spawnerId);
        if (spawner == null) return null;
        DeployableConfig config = spawner.getConfig();
        if (config == null) return null;

        Vector3d normal = new Vector3d(0, 1, 0);
        Vector3d position = new Vector3d(landing);
        Rotation3f rotation = MathUtil.getRotationForHitNormal(normal);
        String spawnFace = MathUtil.getNameForHitNormal(normal);

        return EntityStoreCommandBuffers.runWithResult(store, cb -> {
            Ref<EntityStore> deployRef = DeployablesUtils.spawnDeployable(
                cb, store, config, casterRef, position, rotation, spawnFace);
            if (deployRef != null && durationMs > 0L) {
                TimeResource timeResource = store.getResource(TimeResource.getResourceType());
                cb.putComponent(
                    deployRef,
                    DespawnComponent.getComponentType(),
                    DespawnComponent.despawnInMilliseconds(timeResource, durationMs));
            }
            return deployRef;
        });
    }

    private static void scheduleDeployableCleanup(@Nonnull World world,
                                                  @Nullable Ref<EntityStore> deployableRef,
                                                  long durationMs) {
        if (deployableRef == null || durationMs <= 0L) return;
        final Ref<EntityStore> fRef = deployableRef;
        java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "vaudou-totem-visual");
            t.setDaemon(true);
            return t;
        }).schedule(() -> world.execute(() -> {
            cleanupExpired(world);
            Store<EntityStore> liveStore = world.getEntityStore().getStore();
            if (fRef.isValid()) {
                removeDeployable(liveStore, fRef);
            }
        }), durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    private static void removeDeployable(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> deployableRef) {
        if (!deployableRef.isValid()) return;
        if (EntityStoreCommandBuffers.run(store, cb -> {
            if (deployableRef.isValid()) {
                cb.removeEntity(deployableRef, RemoveReason.REMOVE);
            }
            return Boolean.TRUE;
        })) {
            return;
        }
        try {
            store.removeEntity(deployableRef, RemoveReason.REMOVE);
        } catch (Exception ignored) {}
    }

    public static boolean hasActiveSlownessTotem() {
        long now = System.currentTimeMillis();
        for (ActiveTotem t : GLOBAL_TOTEMS) {
            if (now < t.expiresAt() && t.type() == TotemType.SLOWNESS) return true;
        }
        return false;
    }

    public static boolean isInSlownessTotem(@Nonnull Vector3d pos) {
        return isInTotem(pos, TotemType.SLOWNESS);
    }

    public static boolean isInAnyTotem(@Nonnull Vector3d pos) {
        long now = System.currentTimeMillis();
        for (ActiveTotem t : GLOBAL_TOTEMS) {
            if (now >= t.expiresAt()) continue;
            if (isInside(t, pos)) return true;
        }
        return false;
    }

    public static float getVulnerabilityMagnitudeAt(@Nonnull Vector3d pos) {
        long now = System.currentTimeMillis();
        float max = 0f;
        for (ActiveTotem t : GLOBAL_TOTEMS) {
            if (t.type() != TotemType.VULNERABILITY) continue;
            if (now >= t.expiresAt()) continue;
            if (isInside(t, pos) && t.magnitude() > max) max = t.magnitude();
        }
        return max;
    }

    private static boolean isInTotem(@Nonnull Vector3d pos, @Nonnull TotemType type) {
        long now = System.currentTimeMillis();
        for (ActiveTotem t : GLOBAL_TOTEMS) {
            if (t.type() != type) continue;
            if (now >= t.expiresAt()) continue;
            if (isInside(t, pos)) return true;
        }
        return false;
    }

    private static boolean isInside(@Nonnull ActiveTotem totem, @Nonnull Vector3d pos) {
        double dx = pos.x - totem.position().x;
        double dz = pos.z - totem.position().z;
        return dx * dx + dz * dz <= totem.radius() * totem.radius();
    }
}
