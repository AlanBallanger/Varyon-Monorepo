package fr.varyon.vrpg.classes.arbaletrier;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.projectile.component.Projectile;
import com.hypixel.hytale.server.core.modules.projectile.config.StandardPhysicsProvider;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CarreauExplosifGroundSystem extends EntityTickingSystem<EntityStore> {

    private static final Query<EntityStore> QUERY = Query.and(
        Projectile.getComponentType(),
        StandardPhysicsProvider.getComponentType(),
        TransformComponent.getComponentType()
    );

    private static final String EXPLOSION_SOUND_ID = "SFX_Goblin_Lobber_Bomb_Death";
    private static int explosionSoundIndex = 0;

    private final ArbaietrierState arbaState;
    private final ConcurrentHashMap<Ref<EntityStore>, UUID> trackedProjectiles = new ConcurrentHashMap<>();

    public CarreauExplosifGroundSystem(@Nonnull ArbaietrierState arbaState) {
        this.arbaState = arbaState;
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

            if (arbaState.getPendingCarreauType(creatorUuid) != ArbaietrierState.CARREAU_TYPE_EXPLOSIF) return;

            float dmg = arbaState.consumePendingCarreauDmg(creatorUuid);
            int rank = arbaState.getPendingCarreauRank(creatorUuid);
            arbaState.clearPendingCarreau(creatorUuid);
            if (dmg <= 0f) dmg = 1f;

            TransformComponent tc = chunk.getComponent(index, TransformComponent.getComponentType());
            if (tc == null) return;

            org.joml.Vector3d center = new org.joml.Vector3d(tc.getPosition());
            Ref<EntityStore> attackerRef = commandBuffer.getExternalData().getRefFromUUID(creatorUuid);
            if (attackerRef == null || !attackerRef.isValid()) return;

            final float aoeAmount = dmg;
            final Ref<EntityStore> fAttackerRef = attackerRef;

            try {
                ParticleUtil.spawnParticleEffect("Explosion_Medium", center, commandBuffer);
            } catch (Exception ignored) {}

            try {
                if (explosionSoundIndex == 0) {
                    explosionSoundIndex = SoundEvent.getAssetMap().getIndex(EXPLOSION_SOUND_ID);
                }
                if (explosionSoundIndex != 0) {
                    SoundUtil.playSoundEvent3d(explosionSoundIndex, SoundCategory.SFX, center.x, center.y, center.z, commandBuffer);
                }
            } catch (Exception ignored) {}

            try {
                com.hypixel.hytale.server.core.modules.interaction.interaction.config.selector.Selector
                    .selectNearbyEntities(store, center, (float) CarreauExplosifSkill.radiusForRank(rank), t -> {
                        try {
                            if (t.getIndex() == fAttackerRef.getIndex()) return;
                            if (store.getComponent(t, NPCEntity.getComponentType()) == null) return;
                            DamageSystems.executeDamage(t, store,
                                new Damage(
                                    new Damage.EntitySource(fAttackerRef),
                                    DamageCause.PHYSICAL, aoeAmount));
                        } catch (Exception ignored) {}
                    }, t -> t.getIndex() != fAttackerRef.getIndex());
            } catch (Exception ignored) {}

        } catch (Exception ignored) {}
    }
}
