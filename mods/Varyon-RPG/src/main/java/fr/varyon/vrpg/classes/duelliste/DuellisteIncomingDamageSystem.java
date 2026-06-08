package fr.varyon.vrpg.classes.duelliste;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.vrpg.classes.ClassAccount;
import fr.varyon.vrpg.classes.ClassManager;
import fr.varyon.vrpg.classes.PlayerClass;
import fr.varyon.vrpg.classes.PlayerSpecialization;

import javax.annotation.Nonnull;
import java.util.UUID;

import static com.hypixel.hytale.logger.HytaleLogger.forEnclosingClass;

public final class DuellisteIncomingDamageSystem extends DamageEventSystem {

    private static final com.hypixel.hytale.logger.HytaleLogger LOG = forEnclosingClass();

    private final ClassManager classManager;
    private final DuellisteState state;

    public DuellisteIncomingDamageSystem(@Nonnull ClassManager classManager,
                                         @Nonnull DuellisteState state) {
        this.classManager = classManager;
        this.state = state;
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
            if (acc.getActiveClass() != PlayerClass.GUERRIER) return;
            if (acc.getActiveSpec(PlayerClass.GUERRIER) != PlayerSpecialization.DUELLISTE) return;

            float incoming = damage.getAmount();
            boolean debug = fr.varyon.vrpg.config.VrpgConfig.isDebugCombat();

            // Contre-Attaque — détection parade via Damage.BLOCKED (avant le guard amount <= 0)
            int contreRank = acc.getTalentRank(PlayerClass.GUERRIER, DuellistePassifs.CONTRE_NODE);
            if (contreRank > 0) {
                Boolean blocked = damage.getIfPresentMetaObject(Damage.BLOCKED);
                if (Boolean.TRUE.equals(blocked)) {
                    state.recordParry(uuid);
                    if (debug) LOG.atInfo().log(String.format("[Recu] %.1f PARE -> ContreAttaque prete", incoming));
                    return;
                }
            }

            if (incoming <= 0f) return;

            // Esquive du Bretteur — chance to negate damage entirely
            int dodgeRank = acc.getTalentRank(PlayerClass.GUERRIER, DuellistePassifs.ESQUIVE_NODE);
            if (dodgeRank > 0 && Math.random() < DuellistePassifs.dodgeChanceForRank(dodgeRank)) {
                damage.setAmount(0f);
                state.recordParry(uuid);
                if (debug) LOG.atInfo().log(String.format("[Recu] %.1f ESQUIVE (EsquiveBretteur rank=%d)", incoming, dodgeRank));
                return;
            }

            // Riposte Parfaite — contre-attaque si fenêtre active
            int riposteRank = state.consumeRiposte(uuid);
            if (riposteRank > 0) {
                Damage.Source source = damage.getSource();
                if (source instanceof Damage.EntitySource entitySource) {
                    Ref<EntityStore> attackerRef = entitySource.getRef();
                    if (attackerRef != null && attackerRef.isValid()) {
                        try {
                            Ref<EntityStore> playerEntityRef = chunk.getReferenceTo(index);
                            float riposteDmg = incoming * (1.0f + RiposteParfaiteSkill.dmgBonusForRank(riposteRank));
                            DamageCause cause = DamageCause.PHYSICAL;
                            DamageSystems.executeDamage(attackerRef, store,
                                new Damage(new Damage.EntitySource(playerEntityRef), cause, riposteDmg));
                            if (debug) LOG.atInfo().log(String.format("[Recu] RIPOSTE -> %.1f degats", riposteDmg));
                        } catch (Exception ignored) {}
                    }
                }
            }

            // Track hit taken for Momentum reset
            int momentumRank = acc.getTalentRank(PlayerClass.GUERRIER, DuellistePassifs.MOMENTUM_NODE);
            if (momentumRank > 0 && !damage.isCancelled()) {
                state.recordHitTaken(uuid);
            }

            if (debug) LOG.atInfo().log(String.format("[Recu] %.1f degats recus", incoming));

        } catch (Exception ignored) {}
    }
}
