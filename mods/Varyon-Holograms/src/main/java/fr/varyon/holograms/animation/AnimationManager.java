package fr.varyon.holograms.animation;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;
import org.joml.Vector3f;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class AnimationManager {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final long TICK_MS = 50;

    private final Map<UUID, HologramAnimGroup> hologramGroups = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> task;

    public void start() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "VaryonHolograms-AnimTick");
            t.setDaemon(true);
            return t;
        });
        task = scheduler.scheduleAtFixedRate(this::tick, TICK_MS, TICK_MS, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        if (task != null) task.cancel(false);
        if (scheduler != null) scheduler.shutdownNow();
        hologramGroups.clear();
    }

    public void registerHologramAnimation(@Nonnull UUID hologramId, @Nonnull UUID worldId,
                                           @Nonnull AnimationData animation, @Nonnull Vector3d anchor,
                                           @Nonnull List<HologramAnimGroup.Member> members) {
        HologramAnimGroup group = new HologramAnimGroup(worldId, animation, anchor);
        for (HologramAnimGroup.Member member : members) {
            group.addMember(member);
        }
        hologramGroups.put(hologramId, group);
    }

    public void unregisterHologramAnimation(@Nonnull UUID hologramId) {
        hologramGroups.remove(hologramId);
    }

    public void unregisterAnimation(@Nonnull UUID entityId) {
        for (Map.Entry<UUID, HologramAnimGroup> entry : hologramGroups.entrySet()) {
            HologramAnimGroup group = entry.getValue();
            boolean found = group.getMembers().stream().anyMatch(m -> m.entityId.equals(entityId));
            if (found) {
                hologramGroups.remove(entry.getKey());
                return;
            }
        }
    }

    public boolean hasAnimation(@Nonnull UUID hologramId) {
        return hologramGroups.containsKey(hologramId);
    }

    private void tick() {
        if (hologramGroups.isEmpty()) return;
        Universe universe = Universe.get();
        if (universe == null) return;

        Map<UUID, List<Map.Entry<UUID, HologramAnimGroup>>> byWorld = new java.util.HashMap<>();
        for (Map.Entry<UUID, HologramAnimGroup> entry : hologramGroups.entrySet()) {
            UUID worldId = entry.getValue().getWorldId();
            byWorld.computeIfAbsent(worldId, k -> new ArrayList<>()).add(entry);
        }

        float delta = TICK_MS / 1000f;
        for (Map.Entry<UUID, List<Map.Entry<UUID, HologramAnimGroup>>> worldEntry : byWorld.entrySet()) {
            World world = findWorld(universe, worldEntry.getKey());
            if (world == null) continue;
            List<Map.Entry<UUID, HologramAnimGroup>> entries = worldEntry.getValue();
            world.execute(() -> {
                for (Map.Entry<UUID, HologramAnimGroup> entry : entries) {
                    applyHologramAnimation(world, entry.getValue(), delta);
                }
            });
        }
    }

    private void applyHologramAnimation(@Nonnull World world, @Nonnull HologramAnimGroup group, float delta) {
        try {
            Store<EntityStore> store = world.getEntityStore().getStore();
            List<HologramAnimGroup.Member> members = group.getMembers();
            if (members.isEmpty()) return;

            HologramAnimGroup.Member syncMember = members.get(0);
            Ref<EntityStore> syncRef = ((EntityStore) store.getExternalData()).getRefFromUUID(syncMember.entityId);
            if (syncRef != null && syncRef.isValid()) {
                TransformComponent syncTransform = store.getComponent(syncRef, TransformComponent.getComponentType());
                if (syncTransform != null) {
                    syncAnchorFromExternalMove(group, syncMember, syncTransform.getPosition());
                }
            }

            for (HologramAnimGroup.Member member : members) {
                Ref<EntityStore> ref = ((EntityStore) store.getExternalData()).getRefFromUUID(member.entityId);
                if (ref == null || !ref.isValid()) continue;

                TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
                if (transform == null) continue;

                Vector3d finalPos = group.getMemberPosition(member.lineOffset);
                transform.teleportPosition(new Vector3d(finalPos.x, finalPos.y, finalPos.z));
                member.setLastSetPosition(new Vector3d(finalPos));

                Vector3f finalRot = group.getMemberRotation(member.baseRotation);
                transform.teleportRotation(new Rotation3f(finalRot.x, finalRot.y, finalRot.z));

                EntityScaleComponent scale = store.getComponent(ref, EntityScaleComponent.getComponentType());
                if (scale != null) {
                    float newScale = group.getMemberScale(member.baseScale);
                    if (Math.abs(newScale - scale.getScale()) > scale.getScale() * 0.01f) {
                        scale.setScale(newScale);
                    }
                }
            }

            group.setLastSetAnchor(group.getAnimatedAnchor());
            group.tick(delta);
        } catch (Exception e) {
            LOGGER.at(Level.FINE).log("[Varyon-Holograms] Anim tick error: %s", e.getMessage());
        }
    }

    private static void syncAnchorFromExternalMove(@Nonnull HologramAnimGroup group,
                                                    @Nonnull HologramAnimGroup.Member syncMember,
                                                    @Nonnull Vector3d currentPos) {
        Vector3d lastSetPos = syncMember.getLastSetPosition();
        if (lastSetPos == null) return;
        double dx = currentPos.x - lastSetPos.x;
        double dy = currentPos.y - lastSetPos.y;
        double dz = currentPos.z - lastSetPos.z;
        if (Math.abs(dx) > 0.001 || Math.abs(dy) > 0.001 || Math.abs(dz) > 0.001) {
            Vector3d oldAnchor = group.getAnchorPosition();
            group.setAnchorPosition(new Vector3d(oldAnchor.x + dx, oldAnchor.y + dy, oldAnchor.z + dz));
        }
    }

    @javax.annotation.Nullable
    private World findWorld(@Nonnull Universe universe, @Nonnull UUID worldId) {
        return universe.getWorld(worldId);
    }
}
