package fr.varyon.vrpg.combat;

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
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import fr.varyon.vrpg.classes.ClassAccount;
import fr.varyon.vrpg.classes.ClassManager;
import fr.varyon.vrpg.classes.ClassPlayerStats;
import fr.varyon.vrpg.classes.ClassStatDefinition;
import fr.varyon.vrpg.classes.PlayerClass;
import fr.varyon.vrpg.classes.PlayerSpecialization;
import fr.varyon.vrpg.integration.DamageFloatBridge;

import javax.annotation.Nonnull;
import java.util.UUID;

public final class VrpgCritDamageSystem extends DamageEventSystem {

    private static final com.hypixel.hytale.logger.HytaleLogger LOG =
        com.hypixel.hytale.logger.HytaleLogger.forEnclosingClass();

    private final ClassManager classManager;

    public VrpgCritDamageSystem(@Nonnull ClassManager classManager) {
        this.classManager = classManager;
    }

    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
    }

    @Override
    public Query<EntityStore> getQuery() {
        return NPCEntity.getComponentType();
    }

    @Override
    public void handle(int index,
                       @Nonnull ArchetypeChunk<EntityStore> chunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull Damage damage) {
        try {
            if (damage.isCancelled() || damage.getAmount() <= 0f) return;

            Damage.Source source = damage.getSource();
            if (!(source instanceof Damage.EntitySource entitySource)) return;
            Ref<EntityStore> attackerRef = entitySource.getRef();
            if (attackerRef == null || !attackerRef.isValid()) return;

            Player player = store.getComponent(attackerRef, Player.getComponentType());
            if (player == null) player = commandBuffer.getComponent(attackerRef, Player.getComponentType());
            if (player == null) return;

            PlayerRef playerRef = store.getComponent(attackerRef, PlayerRef.getComponentType());
            if (playerRef == null) playerRef = commandBuffer.getComponent(attackerRef, PlayerRef.getComponentType());
            if (playerRef == null) return;

            UUID uuid = playerRef.getUuid();
            ClassAccount acc = classManager.getOrLoad(uuid);
            PlayerClass activeClass = acc.getActiveClass();
            PlayerSpecialization spec = acc.getActiveSpec(activeClass);
            if (spec == null || spec == PlayerSpecialization.ARCANISTE || spec == PlayerSpecialization.VAUDOU) {
                return;
            }

            ClassPlayerStats stats = classManager.getStatEngine().getStats(uuid);
            if (stats == null && activeClass != null) {
                stats = ClassStatDefinition.compute(acc.getProgress(activeClass).getLevel(), spec);
            }
            if (stats == null || stats.critChancePct() <= 0) return;
            if (Math.random() >= stats.critChancePct() / 100.0) return;

            float base = damage.getAmount();
            float critMult = 1.0f + stats.critDamagePct() / 100.0f;
            damage.setAmount(base * critMult);
            DamageFloatBridge.markCritical(damage);
            if (fr.varyon.vrpg.config.VrpgConfig.isDebugCombat()) {
                LOG.atInfo().log(String.format("[Crit] spec=%s chance=%d%% dmg=+%d%% %.1f->%.1f",
                    spec.getId(), stats.critChancePct(), stats.critDamagePct(), base, base * critMult));
            }
        } catch (Exception ignored) {}
    }
}
