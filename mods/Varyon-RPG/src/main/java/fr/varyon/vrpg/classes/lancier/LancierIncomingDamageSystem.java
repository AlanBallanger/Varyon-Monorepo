package fr.varyon.vrpg.classes.lancier;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.vrpg.classes.ClassAccount;
import fr.varyon.vrpg.classes.ClassManager;
import fr.varyon.vrpg.classes.PlayerClass;
import fr.varyon.vrpg.classes.PlayerSpecialization;

import javax.annotation.Nonnull;
import java.util.UUID;

public final class LancierIncomingDamageSystem extends DamageEventSystem {

    private final ClassManager classManager;
    private final LancierState lancierState;

    public LancierIncomingDamageSystem(@Nonnull ClassManager classManager,
                                       @Nonnull LancierState lancierState) {
        this.classManager = classManager;
        this.lancierState = lancierState;
    }

    @Override
    public SystemGroup<EntityStore> getGroup() { return DamageModule.get().getFilterDamageGroup(); }

    @Override
    public Query<EntityStore> getQuery() { return Player.getComponentType(); }

    @Override
    public void handle(int index,
                       @Nonnull ArchetypeChunk<EntityStore> chunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull Damage damage) {
        try {
            if (damage.isCancelled()) return;

            PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
            if (playerRef == null) return;

            UUID uuid = playerRef.getUuid();
            ClassAccount acc = classManager.getOrLoad(uuid);
            if (acc.getActiveClass() != PlayerClass.TIREUR) return;
            if (acc.getActiveSpec(PlayerClass.TIREUR) != PlayerSpecialization.LANCIER) return;

            float incoming = damage.getAmount();
            if (incoming <= 0f) return;

            // Garde du Lancier — annuler l'attaque et armer le bonus prochain coup
            int gardeRank = lancierState.consumeGarde(uuid);
            if (gardeRank > 0) {
                damage.setAmount(0f);
                lancierState.armGardeDamage(uuid, gardeRank, GardeDuLancierSkill.windowMsForRank(gardeRank));
                return;
            }

            // Posture dominante — réduction dégâts reçus après touche mêlée
            if (lancierState.isPostureActive(uuid)) {
                int postureRank = lancierState.getPostureRank(uuid);
                float reduction = LancierPassifs.postureReductionForRank(postureRank);
                damage.setAmount(incoming * (1f - reduction));
            }

        } catch (Exception ignored) {}
    }
}
