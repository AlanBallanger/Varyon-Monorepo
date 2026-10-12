package com.varyon.elite.combat;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.varyon.elite.EliteStats;
import com.varyon.elite.component.EliteComponent;

import javax.annotation.Nonnull;

/**
 * Feeds the attacker's contribution into the elite's threat table. Tankiness comes only from the
 * class passive's {@code DamageResistance} (see the {@code Varyon_Elite_Passive_*} effect assets) -
 * there used to also be a per-tier multiplier here, but it stacked with the class resistance and
 * made high-tier elites of a resistant class (Archer, Boss, SeigneurDeGuerre) take under 55% of
 * incoming damage. Removed; the tier now only scales HP, damage dealt and size.
 */
public class EliteDamageTakenSystem extends DamageEventSystem {

    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
    }

    @Override
    @Nonnull
    public Query<EntityStore> getQuery() {
        return NPCEntity.getComponentType();
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
                       @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull Damage damage) {
        Ref<EntityStore> victimRef = chunk.getReferenceTo(index);
        EliteComponent eliteDbg = store.getComponent(victimRef, EliteComponent.getComponentType());
        if (eliteDbg != null) {
            var ec = store.getComponent(victimRef, com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent.getComponentType());
            com.hypixel.hytale.logger.HytaleLogger.forEnclosingClass().at(java.util.logging.Level.INFO).log(
                    "[EliteDmgDebug] class=" + eliteDbg.getMobClass() + " tier=" + eliteDbg.getTier()
                    + " amount=" + damage.getAmount() + " cancelled=" + damage.isCancelled()
                    + " cause=" + damage.getCause() + " activeEffects="
                    + (ec != null ? ec.getActiveEffects().keySet() : "null")
                    + " effectIndices=" + eliteDbg.getEffectIndices());
        }
        if (damage.isCancelled() || damage.getAmount() <= 0f) {
            return;
        }
        EliteComponent elite = store.getComponent(victimRef, EliteComponent.getComponentType());
        if (elite == null) {
            return;
        }

        Damage.Source source = damage.getSource();
        if (source instanceof Damage.EntitySource entitySource) {
            Ref<EntityStore> attackerRef = entitySource.getRef();
            if (attackerRef != null && attackerRef.isValid()) {
                java.util.UUID attackerId =
                        com.varyon.elite.EliteEntities.uuid(store, attackerRef);
                java.util.UUID eliteId =
                        com.varyon.elite.EliteEntities.uuid(store, victimRef);
                if (attackerId != null && eliteId != null) {
                    EliteThreatRegistry.get(eliteId).addThreat(attackerId, damage.getAmount());
                }
            }
        }
    }
}
