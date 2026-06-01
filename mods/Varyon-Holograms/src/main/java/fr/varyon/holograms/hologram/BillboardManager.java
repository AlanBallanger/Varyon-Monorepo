package fr.varyon.holograms.hologram;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;
import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class BillboardManager {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final float DEFAULT_TRACKING_DISTANCE = 64f;
    private static final long TICK_MS = 100;

    private record BillboardEntry(UUID entityId, UUID worldId, Vector3d position, float trackingDistance) {}

    private final Map<UUID, BillboardEntry> billboards = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> task;

    public void start() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "VaryonHolograms-Billboard");
            t.setDaemon(true);
            return t;
        });
        task = scheduler.scheduleAtFixedRate(this::tick, TICK_MS, TICK_MS, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        if (task != null) task.cancel(false);
        if (scheduler != null) scheduler.shutdownNow();
        billboards.clear();
    }

    public void register(@Nonnull UUID entityId, @Nonnull UUID worldId, @Nonnull Vector3d position, float trackingDistance) {
        float dist = trackingDistance > 0 ? trackingDistance : DEFAULT_TRACKING_DISTANCE;
        billboards.put(entityId, new BillboardEntry(entityId, worldId, new Vector3d(position), dist));
    }

    public void unregister(@Nonnull UUID entityId) {
        billboards.remove(entityId);
    }

    public void unregisterAll(@Nonnull Collection<UUID> entityIds) {
        entityIds.forEach(billboards::remove);
    }

    private void tick() {
        if (billboards.isEmpty()) return;
        Universe universe = Universe.get();
        if (universe == null) return;

        for (World world : universe.getWorlds().values()) {
            Collection<PlayerRef> playerRefs = world.getPlayerRefs();
            if (playerRefs.isEmpty()) continue;

            for (BillboardEntry entry : billboards.values()) {
                if (!entry.worldId().equals(world.getWorldConfig().getUuid())) continue;

                world.execute(() -> {
                    try {
                        Store<EntityStore> store = world.getEntityStore().getStore();
                        Ref<EntityStore> ref = ((EntityStore) store.getExternalData()).getRefFromUUID(entry.entityId());
                        if (ref == null || !ref.isValid()) return;

                        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
                        if (transform == null) return;

                        PlayerRef nearest = findNearestPlayer(playerRefs, entry.position(), entry.trackingDistance(), world);
                        if (nearest == null) return;

                        Vector3d playerPos = nearest.getTransform().getPosition();
                        double dx = playerPos.x - entry.position().x;
                        double dz = playerPos.z - entry.position().z;
                        float yaw = (float) Math.atan2(-dx, dz);
                        transform.getRotation().set(0f, yaw, 0f);
                        transform.markChunkDirty(store);
                    } catch (Exception e) {
                        LOGGER.at(Level.FINE).log("[Varyon-Holograms] Billboard tick error: %s", e.getMessage());
                    }
                });
            }
        }
    }

    @javax.annotation.Nullable
    private PlayerRef findNearestPlayer(@Nonnull Collection<PlayerRef> playerRefs, @Nonnull Vector3d position,
                                         float maxDistance, @Nonnull World world) {
        PlayerRef nearest = null;
        double nearestDist = maxDistance * maxDistance;
        for (PlayerRef ref : playerRefs) {
            try {
                Vector3d pos = ref.getTransform().getPosition();
                double dx = pos.x - position.x;
                double dz = pos.z - position.z;
                double distSq = dx * dx + dz * dz;
                if (distSq < nearestDist) {
                    nearestDist = distSq;
                    nearest = ref;
                }
            } catch (Exception ignored) {
            }
        }
        return nearest;
    }
}
