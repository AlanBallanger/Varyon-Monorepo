package com.varyon.tptoworld.portal;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Particle systems attached in an item's static BlockType.Particles[] are replayed
 * automatically by the client whenever the block enters render range. A dynamically
 * configured portal has no such static definition, so nothing replays it after a
 * chunk reload / reconnect. This system periodically re-triggers the configured
 * effect for portals near a player, standing in for that missing client-side replay.
 */
public final class PortalTemplateTickSystem extends EntityTickingSystem<EntityStore> {

    static final double PARTICLE_RENDER_DISTANCE = 100.0;
    private static final double PORTAL_RANGE_SQ = PARTICLE_RENDER_DISTANCE * PARTICLE_RENDER_DISTANCE;

    /**
     * The server has no API to cancel an in-flight particle effect. Instead, each replay is
     * capped to this duration, and a portal is re-triggered shortly before it expires (see
     * REPLAY_OVERLAP_SECONDS): as long as the portal stays in the registry it keeps getting
     * refreshed, but a portal removed from the registry (block broken/moved) goes dark ~12s
     * after instead of looping forever at its old position. A longer duration also means the
     * per-replay restart (which re-plays the vanilla system's spawn burst / flash and snaps a
     * background image back to frame 0) happens less often.
     */
    static final float PARTICLE_MAX_DURATION_SECONDS = 12f;

    /**
     * How often a portal is re-triggered. The ring effect (below) is spawned with a duration
     * well past this, so at the moment of re-trigger the previous ring instance is still alive
     * and fading: the new one ramps up underneath it, no visible seam. This is how it behaved
     * before the game update, when duration/interval happened to leave a multi-second overlap.
     */
    private static final float REPLAY_INTERVAL_SECONDS = 10.8f;

    /**
     * Ring duration: long enough past REPLAY_INTERVAL_SECONDS that the old instance is still
     * cross-fading out when the next replay starts. This overlap is what makes the reload look
     * smooth; it is fine for the ring (BlendLinear, not full-frame) even though it would
     * over-saturate the additive background image — which is why the background uses its own
     * shorter duration below instead of this one.
     */
    private static final float RING_DURATION_SECONDS = PARTICLE_MAX_DURATION_SECONDS;

    /**
     * The background image (BlendLinear, MaxConcurrentParticles=1, flat opacity) needs almost no
     * cross-fade — but with zero overlap it blinks for one frame at each re-trigger. A small
     * overlap (0.3s over a ~10.8s interval ≈ 3%) bridges that gap without visibly stacking.
     */
    private static final float BACKGROUND_OVERLAP_SECONDS = 0.3f;
    private static final float BACKGROUND_DURATION_SECONDS = REPLAY_INTERVAL_SECONDS + BACKGROUND_OVERLAP_SECONDS;

    private final ComponentType<EntityStore, PlayerRef> playerRefType = PlayerRef.getComponentType();
    private final Map<String, Float> secondsSinceReplayByPortal = new ConcurrentHashMap<>();

    @Override
    public void tick(float deltaTime, int index, ArchetypeChunk<EntityStore> chunk,
            Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer) {
        PlayerRef playerRef = chunk.getComponent(index, playerRefType);
        if (playerRef == null) {
            return;
        }

        try {
            Ref<EntityStore> ref = chunk.getReferenceTo(index);
            TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
            if (transform == null) {
                return;
            }

            World world = store.getExternalData().getWorld();
            if (world == null || world.getName() == null) {
                return;
            }

            List<PortalTemplateEntry> portals = PortalTemplateRegistry.getInstance().getInDimension(world.getName());
            if (portals.isEmpty()) {
                return;
            }

            Vector3d playerPos = transform.getPosition();
            for (PortalTemplateEntry portal : portals) {
                double gy = portal.y() + portal.centerOffsetY() + VaryonPortalConfig.baseHeightOffsetFor(portal.type());
                // Must NOT touch the world here: getBlockType/getChunk from inside a ticking
                // system can trip "Store is currently processing" (async chunk load tries to
                // write a locked Store). Use the plain arithmetic centre; the exact
                // rotation-aware centre is only needed for the X2 hitbox and is applied when the
                // portal is saved (VaryonPortalEditPage.replayPortalEffect), not per tick.
                double gx = portal.x() + (portal.wide() ? 1.0 : 0.5);
                double gz = portal.z() + 0.5;

                double dx = playerPos.x - gx;
                double dy = playerPos.y - gy;
                double dz = playerPos.z - gz;
                if (dx * dx + dy * dy + dz * dz > PORTAL_RANGE_SQ) {
                    continue;
                }

                // tick() runs once per player, but a portal must advance its timer once per
                // server frame regardless of how many players are nearby. computeIfAbsent seeds
                // it at the interval so a portal that just came into range replays immediately;
                // merge then advances by real elapsed time and we replay + reset when it crosses.
                secondsSinceReplayByPortal.computeIfAbsent(portal.key(), k -> REPLAY_INTERVAL_SECONDS);
                float elapsed = secondsSinceReplayByPortal.merge(portal.key(), deltaTime, Float::sum);
                if (elapsed < REPLAY_INTERVAL_SECONDS) {
                    continue;
                }
                secondsSinceReplayByPortal.put(portal.key(), 0f);

                replay(portal, gx, gy, gz, store, commandBuffer);
            }
        } catch (Exception ignored) {
        }
    }

    private void replay(PortalTemplateEntry portal, double gx, double gy, double gz,
            Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer) {
        try {
            ComponentAccessor<EntityStore> accessor = commandBuffer != null ? commandBuffer : store;
            SpatialResource<Ref<EntityStore>, EntityStore> playerSpatial =
                    accessor.getResource(EntityModule.get().getPlayerSpatialResourceType());
            List<Ref<EntityStore>> playerRefs = SpatialResource.getThreadLocalReferenceList();
            playerSpatial.getSpatialStructure().collect(new Vector3d(gx, gy, gz),
                    PARTICLE_RENDER_DISTANCE, playerRefs);

            float yawRadians = (float) Math.toRadians(portal.yaw());
            String systemId = VaryonPortalConfig.resolveParticleSystemId(portal.type());

            ParticleUtil.spawnParticleEffect(systemId, gx, gy, gz,
                    yawRadians, 0f, 0f, portal.scale(), null, null, playerRefs, accessor,
                    RING_DURATION_SECONDS);

            if (portal.hasBackground()) {
                String backgroundId = VaryonPortalConfig.resolveBackgroundParticleSystemId(portal.type(), portal.background());
                double bgY = gy + VaryonPortalConfig.backgroundHeightOffsetFor(portal.type());
                ParticleUtil.spawnParticleEffect(backgroundId, gx, bgY, gz,
                        yawRadians, 0f, 0f, portal.scale(), null, null, playerRefs, accessor,
                        BACKGROUND_DURATION_SECONDS);
            }
        } catch (Exception ignored) {
        }
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return playerRefType;
    }
}
