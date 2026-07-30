package fr.varyon.holograms.hologram;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.Direction;
import com.hypixel.hytale.protocol.ModelTransform;
import com.hypixel.hytale.protocol.Position;
import com.hypixel.hytale.protocol.TransformUpdate;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.EntityTrackerSystems;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class BillboardManager {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final long TICK_MS = 50;
    private static final double MAX_TRACKING_DISTANCE = 64.0;
    private static final double MIN_TRACKING_DISTANCE = 32.0;

    private record BillboardEntry(UUID entityId, UUID worldId, float customMinDistance, float defaultYaw) {}

    /** Minimum yaw change (radians) before re-allocating/queuing a transform update for a viewer. */
    private static final float YAW_EPSILON = 0.02f;

    private final Map<UUID, BillboardEntry> billboards = new ConcurrentHashMap<>();
    /** Last yaw sent per (billboard entity, viewer ref), to skip redundant allocation/network updates. */
    private final Map<UUID, Map<Ref<EntityStore>, Float>> lastYawByViewer = new ConcurrentHashMap<>();
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
        lastYawByViewer.clear();
    }

    public void register(@Nonnull UUID entityId, @Nonnull UUID worldId, float trackingDistance) {
        register(entityId, worldId, trackingDistance, 0f);
    }

    public void register(@Nonnull UUID entityId, @Nonnull UUID worldId, float trackingDistance, float defaultYaw) {
        float customMin = trackingDistance > 0 ? trackingDistance : -1f;
        billboards.put(entityId, new BillboardEntry(entityId, worldId, customMin, defaultYaw));
    }

    public void unregister(@Nonnull UUID entityId) {
        billboards.remove(entityId);
        lastYawByViewer.remove(entityId);
    }

    public void unregisterAll(@Nonnull Collection<UUID> entityIds) {
        entityIds.forEach(id -> {
            billboards.remove(id);
            lastYawByViewer.remove(id);
        });
    }

    private void tick() {
        if (billboards.isEmpty()) return;
        Universe universe = Universe.get();
        if (universe == null) return;

        Map<UUID, List<BillboardEntry>> byWorld = new java.util.HashMap<>();
        for (BillboardEntry entry : billboards.values()) {
            byWorld.computeIfAbsent(entry.worldId(), k -> new ArrayList<>()).add(entry);
        }

        for (Map.Entry<UUID, List<BillboardEntry>> worldEntry : byWorld.entrySet()) {
            World world = findWorld(universe, worldEntry.getKey());
            if (world == null) continue;
            List<BillboardEntry> entries = worldEntry.getValue();
            world.execute(() -> {
                try {
                    updateBillboards(world, entries);
                } catch (Exception e) {
                    LOGGER.at(Level.FINE).log("[Varyon-Holograms] Billboard tick error: %s", e.getMessage());
                }
            });
        }
    }

    private void updateBillboards(@Nonnull World world, @Nonnull List<BillboardEntry> entries) {
        Store<EntityStore> store = world.getEntityStore().getStore();

        for (BillboardEntry entry : entries) {
            try {
                Ref<EntityStore> billboardRef = ((EntityStore) store.getExternalData()).getRefFromUUID(entry.entityId());
                if (billboardRef == null || !billboardRef.isValid()) continue;

                TransformComponent billboardTransform = store.getComponent(billboardRef, TransformComponent.getComponentType());
                if (billboardTransform == null) continue;

                Vector3d billboardPos = billboardTransform.getPosition();
                EntityTrackerSystems.Visible visible = store.getComponent(billboardRef, EntityModule.get().getVisibleComponentType());
                if (visible == null || visible.visibleTo.isEmpty()) continue;

                double minDist = entry.customMinDistance() > 0
                    ? entry.customMinDistance()
                    : MIN_TRACKING_DISTANCE;
                double maxDist = MAX_TRACKING_DISTANCE;

                Map<Ref<EntityStore>, Float> lastYaws = lastYawByViewer.computeIfAbsent(
                    entry.entityId(), k -> new ConcurrentHashMap<>());
                // Drop stale entries for viewers no longer watching this billboard, so this map
                // doesn't accumulate refs for players who moved away/disconnected.
                lastYaws.keySet().retainAll(visible.visibleTo.keySet());

                for (Map.Entry<Ref<EntityStore>, EntityTrackerSystems.EntityViewer> viewerEntry : visible.visibleTo.entrySet()) {
                    Ref<EntityStore> playerRef = viewerEntry.getKey();
                    EntityTrackerSystems.EntityViewer viewer = viewerEntry.getValue();
                    if (!playerRef.isValid()) continue;

                    TransformComponent playerTransform = store.getComponent(playerRef, TransformComponent.getComponentType());
                    if (playerTransform == null) continue;

                    Vector3d playerPos = playerTransform.getPosition();
                    double dx = playerPos.x - billboardPos.x;
                    double dz = playerPos.z - billboardPos.z;
                    double distSq = dx * dx + dz * dz;
                    if (distSq > maxDist * maxDist) continue;

                    float yaw;
                    if (distSq <= minDist * minDist) {
                        yaw = (float) Math.atan2(-dx, -dz);
                    } else {
                        yaw = entry.defaultYaw();
                    }

                    boolean nowVisible = viewer.visible.contains(billboardRef);
                    Float lastYaw = lastYaws.get(playerRef);
                    if (nowVisible && lastYaw != null && Math.abs(yaw - lastYaw) < YAW_EPSILON) {
                        continue; // Angle hasn't changed meaningfully — skip allocation and network update.
                    }

                    ModelTransform transform = new ModelTransform();
                    transform.position = new Position(billboardPos.x, billboardPos.y, billboardPos.z);
                    transform.bodyOrientation = new Direction(yaw, 0f, 0f);
                    transform.lookOrientation = new Direction(yaw, 0f, 0f);
                    TransformUpdate update = new TransformUpdate(transform);
                    if (!nowVisible) {
                        viewer.visible.add(billboardRef);
                    }
                    viewer.queueUpdate(billboardRef, update);
                    lastYaws.put(playerRef, yaw);
                }
            } catch (Exception e) {
                LOGGER.at(Level.FINE).log("[Varyon-Holograms] Billboard update error for %s: %s", entry.entityId(), e.getMessage());
            }
        }
    }

    @Nullable
    private World findWorld(@Nonnull Universe universe, @Nonnull UUID worldId) {
        return universe.getWorld(worldId);
    }
}
