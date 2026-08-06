package fr.varyon.vrpg.classes.gardiendesgaia;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.projectile.component.Projectile;
import com.hypixel.hytale.server.core.modules.projectile.config.StandardPhysicsProvider;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Applique l'Étreinte de Gaïa à l'endroit où le projectile s'immobilise réellement.
 *
 * Deux raisons d'être ici plutôt que dans le cast :
 *  - le retrait du projectile doit se faire cote Java ; laisser le RemoveEntity du JSON
 *    le detruire seul casse le personnage du lanceur a l'impact ;
 *  - l'effet etait auparavant declenche par un timer cale sur un raycast fait au lancement,
 *    donc il partait meme quand le projectile n'avait rien touche (tir en l'air).
 */
public final class EtreinteGroundSystem extends EntityTickingSystem<EntityStore> {

    private static final Query<EntityStore> QUERY = Query.and(
        Projectile.getComponentType(),
        StandardPhysicsProvider.getComponentType(),
        TransformComponent.getComponentType()
    );

    private static final String ROOT_EFFECT_ID = "Vrpg_Etreinte_Root";
    /** Éclat de glace vanilla joue a l'impact (l'AoE circulaire type totem est volontairement absente). */
    private static final String IMPACT_PARTICLE = "IceBall_Explosion";
    private static final String IMPACT_SOUND    = "SFX_Ice_Ball_Death";
    private static int impactSoundIndex = 0;

    private static final com.hypixel.hytale.logger.HytaleLogger LOG =
        com.hypixel.hytale.logger.HytaleLogger.forEnclosingClass();

    private record Pending(UUID casterUuid, int rank) {}

    private final ConcurrentHashMap<Ref<EntityStore>, Pending> trackedProjectiles = new ConcurrentHashMap<>();

    public void trackProjectile(@Nonnull Ref<EntityStore> projectileRef, @Nonnull UUID creatorUuid, int rank) {
        trackedProjectiles.put(projectileRef, new Pending(creatorUuid, rank));
    }

    @Override
    public Query<EntityStore> getQuery() {
        return QUERY;
    }

    @Override
    public boolean isExplicitQuery() {
        return true;
    }

    @Override
    public void tick(float dt, int index,
                     @Nonnull ArchetypeChunk<EntityStore> chunk,
                     @Nonnull Store<EntityStore> store,
                     @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        if (trackedProjectiles.isEmpty()) return;

        try {
            Ref<EntityStore> projRef = chunk.getReferenceTo(index);
            Pending pending = trackedProjectiles.get(projRef);
            if (pending == null) return;

            StandardPhysicsProvider physics = chunk.getComponent(index, StandardPhysicsProvider.getComponentType());
            if (physics == null || physics.getState() == StandardPhysicsProvider.STATE.ACTIVE) return;

            trackedProjectiles.remove(projRef);

            TransformComponent tc = chunk.getComponent(index, TransformComponent.getComponentType());
            Ref<EntityStore> casterRef = commandBuffer.getExternalData().getRefFromUUID(pending.casterUuid());

            if (tc != null && casterRef != null && casterRef.isValid()) {
                final org.joml.Vector3d impact = new org.joml.Vector3d(tc.getPosition());
                applyRoots(store, commandBuffer, impact, casterRef, pending.rank());
            }

            try {
                commandBuffer.removeEntity(projRef, RemoveReason.REMOVE);
            } catch (Exception e) {
                LOG.atWarning().withCause(e).log("[EtreinteGround] removeEntity ERREUR");
            }
        } catch (Exception e) {
            LOG.atWarning().withCause(e).log("[EtreinteGround] tick ERREUR");
        }
    }

    private void applyRoots(@Nonnull Store<EntityStore> store,
                            @Nonnull CommandBuffer<EntityStore> commandBuffer,
                            @Nonnull org.joml.Vector3d impact,
                            @Nonnull Ref<EntityStore> casterRef,
                            int rank) {
        try {
            ParticleUtil.spawnParticleEffect(IMPACT_PARTICLE, impact, commandBuffer);
        } catch (Exception ignored) {}

        try {
            if (impactSoundIndex == 0) {
                impactSoundIndex = com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent
                    .getAssetMap().getIndex(IMPACT_SOUND);
            }
            if (impactSoundIndex != 0) {
                com.hypixel.hytale.server.core.universe.world.SoundUtil.playSoundEvent3d(
                    impactSoundIndex, com.hypixel.hytale.protocol.SoundCategory.SFX,
                    impact.x, impact.y, impact.z, commandBuffer);
            }
        } catch (Exception ignored) {}

        try {
            int effIdx = EntityEffect.getAssetMap().getIndex(ROOT_EFFECT_ID);
            if (effIdx < 0) return;
            EntityEffect rootEff = (EntityEffect) EntityEffect.getAssetMap().getAsset(effIdx);
            if (rootEff == null) return;

            final float rootSec = EtreinteDeGaiaSkill.rootDurationMs(rank) / 1000f;
            final float radius  = (float) EtreinteDeGaiaSkill.rootRadius();
            final long casterIdx = casterRef.getIndex();
            final EntityEffect fRootEff = rootEff;

            com.hypixel.hytale.server.core.modules.interaction.interaction.config.selector.Selector
                .selectNearbyEntities(store, impact, radius, targetRef -> {
                    try {
                        if (targetRef.getIndex() == casterIdx) return;
                        EffectControllerComponent ec =
                            store.getComponent(targetRef, EffectControllerComponent.getComponentType());
                        if (ec != null) {
                            ec.addEffect(targetRef, fRootEff, rootSec, OverlapBehavior.OVERWRITE, store);
                        }
                    } catch (Exception ignored) {}
                }, t -> t.getIndex() != casterIdx);
        } catch (Exception e) {
            LOG.atWarning().withCause(e).log("[EtreinteGround] applyRoots ERREUR");
        }
    }
}
