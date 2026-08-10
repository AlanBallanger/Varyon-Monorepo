package com.varyon.points;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

/**
 * Applique le bonus de dégâts de faction dominante (voir {@link FactionBonusManager})
 * aux dégâts infligés par un joueur. L'attaquant est résolu via {@link Damage.EntitySource},
 * pas via la query (qui porte sur la victime).
 */
public class FactionDamageBonusSystem extends DamageEventSystem {

    private final FactionBonusManager bonusManager;

    public FactionDamageBonusSystem(@Nonnull FactionBonusManager bonusManager) {
        this.bonusManager = bonusManager;
    }

    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                       @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull Damage damage) {
        if (damage.isCancelled() || damage.getAmount() <= 0.0f) {
            return;
        }

        Damage.Source source = damage.getSource();
        if (!(source instanceof Damage.EntitySource entitySource)) {
            return;
        }
        Ref<EntityStore> attackerRef = entitySource.getRef();
        if (attackerRef == null || !attackerRef.isValid()) return;

        PlayerRef playerRef = store.getComponent(attackerRef, PlayerRef.getComponentType());
        if (playerRef == null) {
            playerRef = commandBuffer.getComponent(attackerRef, PlayerRef.getComponentType());
        }
        if (playerRef == null) return;

        float mult = bonusManager.getDamageMultiplier(playerRef);
        if (mult != 1.0f) {
            damage.setAmount(damage.getAmount() * mult);
        }
    }
}
