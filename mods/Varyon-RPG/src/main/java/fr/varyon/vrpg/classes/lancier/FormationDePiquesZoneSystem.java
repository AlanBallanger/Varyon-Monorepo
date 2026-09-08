package fr.varyon.vrpg.classes.lancier;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.selector.Selector;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FormationDePiquesZoneSystem extends EntityTickingSystem<EntityStore> {

    private final LancierState lancierState;

    private static final class PiqueZone {
        final UUID casterUuid;
        final Ref<EntityStore> casterRef;
        final Vector3d center;
        final Vector3d direction;
        final double halfWidth;
        final double depth;
        final float damagePerTick;
        final float slowFactor;
        final int controleRank;
        long expiresAt;
        long nextTickAt;

        PiqueZone(UUID casterUuid, Ref<EntityStore> casterRef, Vector3d center, Vector3d direction,
                  double halfWidth, double depth, float damagePerTick, float slowFactor, long durationMs,
                  int controleRank) {
            this.casterUuid   = casterUuid;
            this.casterRef    = casterRef;
            this.center       = center;
            this.direction    = direction;
            this.halfWidth    = halfWidth;
            this.depth        = depth;
            this.damagePerTick = damagePerTick;
            this.slowFactor   = slowFactor;
            this.controleRank = controleRank;
            this.expiresAt    = System.currentTimeMillis() + durationMs;
            this.nextTickAt   = System.currentTimeMillis() + FormationDePiquesSkill.tickIntervalMs();
        }
    }

    private final ConcurrentHashMap<UUID, PiqueZone> zones = new ConcurrentHashMap<>();

    public FormationDePiquesZoneSystem(@Nonnull LancierState lancierState) {
        this.lancierState = lancierState;
    }

    public void createZone(@Nonnull UUID casterUuid,
                           @Nonnull Ref<EntityStore> casterRef,
                           @Nonnull Vector3d center,
                           @Nonnull Vector3d direction,
                           int rank,
                           float damagePerTick,
                           int controleRank) {
        double halfWidth = FormationDePiquesSkill.zoneWidthForRank(rank) / 2.0;
        double depth     = FormationDePiquesSkill.zoneDepth();
        float slow       = FormationDePiquesSkill.slowFactor(rank);
        long durationMs  = FormationDePiquesSkill.durationMsForRank(rank);
        Vector3d dir = new Vector3d(direction);
        double dirLen = dir.length();
        if (Double.isFinite(dirLen) && dirLen > 1e-6) {
            dir.mul(1.0 / dirLen);
        } else {
            dir.set(0.0, 0.0, 1.0);
        }
        zones.put(casterUuid, new PiqueZone(casterUuid, casterRef, new Vector3d(center),
            dir, halfWidth, depth, damagePerTick, slow, durationMs, controleRank));
        if (controleRank > 0) {
            lancierState.armCcDamage(casterUuid, controleRank,
                LancierPassifs.controleDurationMsForRank(controleRank));
        }
    }

    @Override
    @Nullable
    public Query<EntityStore> getQuery() {
        return NPCEntity.getComponentType();
    }

    @Override
    public void tick(float deltaTime, int index,
                     ArchetypeChunk<EntityStore> chunk,
                     Store<EntityStore> store,
                     CommandBuffer<EntityStore> commandBuffer) {
        if (zones.isEmpty()) return;
        try {
            Ref<EntityStore> ref = chunk.getReferenceTo(index);
            TransformComponent tc = store.getComponent(ref, TransformComponent.getComponentType());
            if (tc == null) return;

            long now = System.currentTimeMillis();
            for (PiqueZone zone : zones.values()) {
                if (now >= zone.expiresAt) {
                    zones.remove(zone.casterUuid);
                    continue;
                }
                if (now < zone.nextTickAt) continue;
                if (!isInZone(tc.getPosition(), zone)) continue;

                zone.nextTickAt = now + FormationDePiquesSkill.tickIntervalMs();

                if (store.getComponent(ref,
                        com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent.getComponentType()) != null) {
                    continue;
                }
                try {
                    DamageSystems.executeDamage(ref, store,
                        new Damage(new Damage.EntitySource(zone.casterRef), DamageCause.PHYSICAL, zone.damagePerTick));
                } catch (Exception ignored) {}

                applySlowToEntity(ref, store, commandBuffer, zone.slowFactor);
                if (zone.controleRank > 0) {
                    lancierState.armCcDamage(zone.casterUuid, zone.controleRank,
                        LancierPassifs.controleDurationMsForRank(zone.controleRank));
                }
            }

            for (UUID uid : new java.util.ArrayList<>(zones.keySet())) {
                PiqueZone z = zones.get(uid);
                if (z != null && now >= z.expiresAt) zones.remove(uid);
            }
        } catch (Exception ignored) {}
    }

    private boolean isInZone(Vector3d pos, PiqueZone zone) {
        Vector3d toPos = new Vector3d(pos).sub(zone.center);
        double along  = toPos.dot(zone.direction);
        if (along < 0 || along > zone.depth) return false;
        Vector3d perp = new Vector3d(toPos).sub(new Vector3d(zone.direction).mul(along));
        return perp.length() <= zone.halfWidth;
    }

    private void applySlowToEntity(Ref<EntityStore> ref, Store<EntityStore> store,
                                   CommandBuffer<EntityStore> commandBuffer, float slowFactor) {
        try {
            com.hypixel.hytale.server.core.modules.physics.component.Velocity vel =
                store.getComponent(ref, com.hypixel.hytale.server.core.modules.physics.component.Velocity.getComponentType());
            if (vel == null) return;
            org.joml.Vector3d cur = vel.getVelocity();
            if (cur == null) return;
            double speed = cur.length();
            if (speed < 0.01) return;
            vel.getInstructions().clear();
            vel.addInstruction(new org.joml.Vector3d(cur).mul(1.0 - slowFactor), null,
                com.hypixel.hytale.protocol.ChangeVelocityType.Set);
        } catch (Exception ignored) {}
    }
}
