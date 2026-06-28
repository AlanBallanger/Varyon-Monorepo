package fr.varyon.vrpg.classes.bagarreur;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.vrpg.classes.ClassAccount;
import fr.varyon.vrpg.classes.ClassManager;
import fr.varyon.vrpg.classes.PlayerClass;
import fr.varyon.vrpg.classes.PlayerSpecialization;

import javax.annotation.Nonnull;
import java.util.UUID;

public final class BagarreurIncomingDamageSystem extends DamageEventSystem {

    private final ClassManager classManager;
    private final BagarreurState bagarreurState;

    public BagarreurIncomingDamageSystem(@Nonnull ClassManager classManager,
                                          @Nonnull BagarreurState bagarreurState) {
        this.classManager   = classManager;
        this.bagarreurState = bagarreurState;
    }

    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Player.getComponentType();
    }

    @Override
    public void handle(int index,
                       @Nonnull ArchetypeChunk<EntityStore> chunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull Damage damage) {
        try {
            if (damage.isCancelled() || damage.getAmount() <= 0f) return;

            PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
            if (playerRef == null) return;

            UUID uuid = playerRef.getUuid();
            ClassAccount acc = classManager.getOrLoad(uuid);
            if (acc.getActiveClass() != PlayerClass.BARBARE) return;
            if (acc.getActiveSpec(PlayerClass.BARBARE) != PlayerSpecialization.BAGARREUR) return;

            // Adrénaline — déclenche speed boost
            int adrenalineRank = acc.getTalentRank(PlayerClass.BARBARE, BagarreurPassifs.ADRENALINE_NODE);
            if (adrenalineRank > 0) {
                bagarreurState.onDamageAdrenaline(uuid,
                    BagarreurPassifs.adrenalineDurationMsForRank(adrenalineRank),
                    BagarreurPassifs.adrenalineSpeedPerStackForRank(adrenalineRank),
                    BagarreurPassifs.ADRENALINE_MAX_STACKS);
            }

            // Garde du boxeur — réduction si attaque de face
            int gardeRank = acc.getTalentRank(PlayerClass.BARBARE, BagarreurPassifs.GARDE_BOXEUR_NODE);
            if (gardeRank > 0) {
                Damage.Source src = damage.getSource();
                if (src instanceof Damage.EntitySource entitySrc) {
                    Ref<EntityStore> attackerRef = entitySrc.getRef();
                    if (attackerRef != null && attackerRef.isValid()) {
                        Ref<EntityStore> victimRef = chunk.getReferenceTo(index);
                        if (isAttackFromFront(victimRef, attackerRef, store)) {
                            float reduc = BagarreurPassifs.gardeBoxeurReducForRank(gardeRank);
                            damage.setAmount(damage.getAmount() * (1f - reduc));
                        }
                    }
                }
            }

        } catch (Exception ignored) {}
    }

    private boolean isAttackFromFront(@Nonnull Ref<EntityStore> victim,
                                       @Nonnull Ref<EntityStore> attacker,
                                       @Nonnull Store<EntityStore> store) {
        try {
            TransformComponent victimTc   = store.getComponent(victim,   TransformComponent.getComponentType());
            TransformComponent attackerTc = store.getComponent(attacker, TransformComponent.getComponentType());
            HeadRotation       victimHr   = store.getComponent(victim,   HeadRotation.getComponentType());
            if (victimTc == null || attackerTc == null || victimHr == null) return false;

            org.joml.Vector3d facing = victimHr.getDirection();
            org.joml.Vector3d toAttacker = new org.joml.Vector3d(
                attackerTc.getPosition().x - victimTc.getPosition().x,
                0,
                attackerTc.getPosition().z - victimTc.getPosition().z).normalize();

            double dot = facing.x * toAttacker.x + facing.z * toAttacker.z;
            return dot > Math.cos(BagarreurPassifs.GARDE_BOXEUR_ANGLE / 2.0);
        } catch (Exception e) { return false; }
    }
}
