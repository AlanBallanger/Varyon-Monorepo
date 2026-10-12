package com.varyon.elite.loot;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.varyon.elite.EliteConfig;
import com.varyon.elite.EliteEntities;
import com.varyon.elite.combat.EliteThreatRegistry;
import com.varyon.elite.component.EliteComponent;
import com.varyon.elite.component.ElitePendingLootComponent;

import javax.annotation.Nonnull;
import java.util.Random;

/**
 * On elite death: independently rolls a per-tier chance for Cristal du Vide (tiers 1-4) and
 * Coeur de Corruption (tiers 3-5); each roll that hits drops exactly 1, never more. Queues
 * the result on an {@link ElitePendingLootComponent}. See {@code docs/ELITE_MOBS_DESIGN.md} §6.
 */
public class EliteLootSystem extends DeathSystems.OnDeathSystem {

    private final EliteConfig eliteConfig;
    private final Random random = new Random();

    public EliteLootSystem(@Nonnull EliteConfig eliteConfig) {
        this.eliteConfig = eliteConfig;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return NPCEntity.getComponentType();
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onComponentAdded(@Nonnull Ref ref, @Nonnull DeathComponent death,
                                 @Nonnull Store store, @Nonnull CommandBuffer commandBuffer) {
        Ref<EntityStore> victim = (Ref<EntityStore>) ref;
        EliteComponent elite = (EliteComponent) store.getComponent(victim, EliteComponent.getComponentType());
        if (elite == null) {
            return;
        }

        java.util.UUID id = EliteEntities.uuid((Store<EntityStore>) store, victim);
        if (id != null) {
            EliteThreatRegistry.clear(id);
        }

        int crystals = random.nextDouble() < eliteConfig.getVoidCrystalChance(elite.getTier()) ? 1 : 0;
        int cores = random.nextDouble() < eliteConfig.getCorruptionCoreChance(elite.getTier()) ? 1 : 0;

        if (crystals > 0 || cores > 0) {
            commandBuffer.putComponent(victim, ElitePendingLootComponent.getComponentType(),
                    new ElitePendingLootComponent(crystals, cores));
        }
    }
}
