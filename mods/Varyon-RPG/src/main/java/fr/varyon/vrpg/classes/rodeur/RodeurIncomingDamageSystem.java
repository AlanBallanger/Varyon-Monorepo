package fr.varyon.vrpg.classes.rodeur;

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
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.vrpg.classes.ClassAccount;
import fr.varyon.vrpg.classes.ClassManager;
import fr.varyon.vrpg.classes.PlayerClass;
import fr.varyon.vrpg.classes.PlayerSpecialization;
import fr.varyon.vrpg.config.VrpgConfig;

import javax.annotation.Nonnull;
import java.util.UUID;

public final class RodeurIncomingDamageSystem extends DamageEventSystem {

    private static final com.hypixel.hytale.logger.HytaleLogger LOG =
        com.hypixel.hytale.logger.HytaleLogger.forEnclosingClass();

    private final ClassManager classManager;

    public RodeurIncomingDamageSystem(@Nonnull ClassManager classManager) {
        this.classManager = classManager;
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
            if (damage.isCancelled()) return;

            PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
            if (playerRef == null) return;

            UUID uuid = playerRef.getUuid();
            ClassAccount acc = classManager.getOrLoad(uuid);
            if (acc.getActiveClass() != PlayerClass.TIREUR) return;
            if (acc.getActiveSpec(PlayerClass.TIREUR) != PlayerSpecialization.RODEUR) return;

            float incoming = damage.getAmount();
            if (incoming <= 0f) return;

            // Instinct de survie — esquive passive
            int survieRank = acc.getTalentRank(PlayerClass.TIREUR, RodeurPassifs.INSTINCT_SURVIE_NODE);
            if (survieRank > 0) {
                float dodgeChance = RodeurPassifs.dodgeChanceForRank(survieRank);
                if (Math.random() < dodgeChance) {
                    damage.setAmount(0f);
                    try {
                        fr.varyon.vrpg.audio.ClassSkillSounds.playSkillSound(
                            fr.varyon.vrpg.audio.ClassSkillSounds.ESQUIVE_BRETTEUR_SOUND,
                            playerRef, new org.joml.Vector3d(), commandBuffer);
                    } catch (Exception ignored2) {}
                    if (VrpgConfig.isDebugCombat())
                        LOG.atInfo().log(String.format("[RodeurRecu] %.1f ESQUIVÉ (%.0f%% chance)", incoming, dodgeChance * 100));
                }
            }

        } catch (Exception ignored) {}
    }
}
