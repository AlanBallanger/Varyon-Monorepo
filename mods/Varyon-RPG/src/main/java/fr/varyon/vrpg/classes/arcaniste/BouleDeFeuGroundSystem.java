package fr.varyon.vrpg.classes.arcaniste;

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
import com.hypixel.hytale.server.core.modules.projectile.component.Projectile;
import com.hypixel.hytale.server.core.modules.projectile.config.StandardPhysicsProvider;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Applique les dégâts de zone de la Boule de Feu quand le projectile s'immobilise.
 * Le projectile lui-même est purement visuel (RemoveEntity seul cote JSON) : passer par
 * les interactions d'impact du staff vanilla relance toute leur chaine sur le lanceur
 * et bloque le personnage.
 */
public final class BouleDeFeuGroundSystem extends EntityTickingSystem<EntityStore> {

    private static final Query<EntityStore> QUERY = Query.and(
        Projectile.getComponentType(),
        StandardPhysicsProvider.getComponentType(),
        TransformComponent.getComponentType()
    );

    private static final String IMPACT_PARTICLE = "Explosion_Small";

    private static final com.hypixel.hytale.logger.HytaleLogger LOG =
        com.hypixel.hytale.logger.HytaleLogger.forEnclosingClass();

    private final ArcanistState arcanistState;
    private final ConcurrentHashMap<Ref<EntityStore>, UUID> trackedProjectiles = new ConcurrentHashMap<>();

    public BouleDeFeuGroundSystem(@Nonnull ArcanistState arcanistState) {
        this.arcanistState = arcanistState;
    }

    public void trackProjectile(@Nonnull Ref<EntityStore> projectileRef, @Nonnull UUID creatorUuid) {
        trackedProjectiles.put(projectileRef, creatorUuid);
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
            UUID creatorUuid = trackedProjectiles.get(projRef);
            if (creatorUuid == null) return;

            StandardPhysicsProvider physics = chunk.getComponent(index, StandardPhysicsProvider.getComponentType());
            if (physics == null || physics.getState() == StandardPhysicsProvider.STATE.ACTIVE) return;

            trackedProjectiles.remove(projRef);

            float dmg = arcanistState.consumePendingProjectileDmg(creatorUuid);
            // Sans ça, ArcanistOutgoingDamageSystem reconsommerait le pending sur nos propres
            // dégâts PHYSICAL et écraserait le montant calculé ici.
            arcanistState.setLastCastFire(creatorUuid, false);

            TransformComponent tc = chunk.getComponent(index, TransformComponent.getComponentType());
            Ref<EntityStore> casterRef = commandBuffer.getExternalData().getRefFromUUID(creatorUuid);

            if (dmg > 0f && tc != null && casterRef != null && casterRef.isValid()) {
                final org.joml.Vector3d center = new org.joml.Vector3d(tc.getPosition());
                final Ref<EntityStore> fCasterRef = casterRef;
                final float fDmg = dmg;

                try {
                    ParticleUtil.spawnParticleEffect(IMPACT_PARTICLE, center, commandBuffer);
                } catch (Exception ignored) {}

                try {
                    com.hypixel.hytale.server.core.modules.interaction.interaction.config.selector.Selector
                        .selectNearbyEntities(store, center, BouleDeFeuSkill.damageRadius(), t -> {
                            try {
                                if (t.getIndex() == fCasterRef.getIndex()) return;
                                if (store.getComponent(t, NPCEntity.getComponentType()) == null) return;
                                DamageSystems.executeDamage(t, store,
                                    new Damage(new Damage.EntitySource(fCasterRef), DamageCause.PHYSICAL, fDmg));
                            } catch (Exception ignored) {}
                        }, t -> t.getIndex() != fCasterRef.getIndex());
                } catch (Exception e) {
                    LOG.atWarning().withCause(e).log("[BouleDeFeuGround] AoE ERREUR");
                }
            }

            // Retrait cote Java, comme RodeurArrowGroundSystem : laisser le RemoveEntity du
            // JSON detruire seul le projectile casse le personnage du lanceur a l'impact.
            try {
                commandBuffer.removeEntity(projRef, com.hypixel.hytale.component.RemoveReason.REMOVE);
            } catch (Exception e) {
                LOG.atWarning().withCause(e).log("[BouleDeFeuGround] removeEntity ERREUR");
            }

        } catch (Exception e) {
            LOG.atWarning().withCause(e).log("[BouleDeFeuGround] tick ERREUR");
        }
    }
}
