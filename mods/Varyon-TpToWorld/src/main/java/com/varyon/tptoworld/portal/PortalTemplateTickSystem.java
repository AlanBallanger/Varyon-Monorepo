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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Particle systems attached in an item's static BlockType.Particles[] are replayed
 * automatically by the client whenever the block enters render range. A dynamically
 * configured portal has no such static definition, so nothing replays it after a
 * chunk reload / reconnect. This system periodically re-triggers the configured
 * effect for portals near a player, standing in for that missing client-side replay.
 */
public final class PortalTemplateTickSystem extends EntityTickingSystem<EntityStore> {

    private static final int CHECK_INTERVAL_TICKS = 100;
    static final double PARTICLE_RENDER_DISTANCE = 100.0;
    private static final double PORTAL_RANGE_SQ = PARTICLE_RENDER_DISTANCE * PARTICLE_RENDER_DISTANCE;

    /**
     * The server has no API to cancel an in-flight particle effect. Instead, each replay is
     * capped to a duration slightly longer than the tick interval: as long as the portal stays
     * in the registry it keeps getting refreshed before this expires, but a portal removed from
     * the registry (block broken/moved) goes dark shortly after instead of looping forever at
     * its old position.
     */
    static final float PARTICLE_MAX_DURATION_SECONDS = 8f;

    private final ComponentType<EntityStore, PlayerRef> playerRefType = PlayerRef.getComponentType();
    private final Map<UUID, Integer> tickCounters = new ConcurrentHashMap<>();

    @Override
    public void tick(float deltaTime, int index, ArchetypeChunk<EntityStore> chunk,
            Store<EntityStore> store, CommandBuffer<EntityStore> commandBuffer) {
        PlayerRef playerRef = chunk.getComponent(index, playerRefType);
        if (playerRef == null) {
            return;
        }

        UUID uuid = playerRef.getUuid();
        int tc = tickCounters.merge(uuid, 1, Integer::sum);
        if (tc % CHECK_INTERVAL_TICKS != 0) {
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
                double gx = portal.x() + (portal.wide() ? 1.0 : 0.5);
                double gy = portal.y() + portal.centerOffsetY() + VaryonPortalConfig.baseHeightOffsetFor(portal.type());
                double gz = portal.z() + 0.5;

                double dx = playerPos.x - gx;
                double dy = playerPos.y - gy;
                double dz = playerPos.z - gz;
                if (dx * dx + dy * dy + dz * dz > PORTAL_RANGE_SQ) {
                    continue;
                }

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
            ParticleUtil.spawnParticleEffect(portal.type(), gx, gy, gz,
                    yawRadians, 0f, 0f, portal.scale(), null, null, playerRefs, accessor,
                    PARTICLE_MAX_DURATION_SECONDS);

            if (portal.hasBackground()) {
                ParticleUtil.spawnParticleEffect(portal.background(), gx, gy, gz,
                        yawRadians, 0f, 0f, portal.scale(), null, null, playerRefs, accessor,
                        PARTICLE_MAX_DURATION_SECONDS);
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
