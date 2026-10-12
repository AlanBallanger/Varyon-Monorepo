package com.varyon.elite;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.elite.combat.EliteThreatRegistry;
import com.varyon.elite.component.EliteAbilityStateComponent;
import com.varyon.elite.component.EliteComponent;
import com.varyon.elite.component.ElitePendingAbilityComponent;

import javax.annotation.Nonnull;
import java.util.UUID;

/**
 * When a de-elite pass is requested (config reload that turns the subsystem off, or
 * {@code /elite deelite}), strips the elite components and stat modifiers from every elite
 * so no orphaned elites remain. See {@code docs/ELITE_MOBS_DESIGN.md} §9.
 */
public class EliteDeeliteSystem extends EntityTickingSystem<EntityStore> {

    private static volatile long purgeUntilMs = 0L;

    /** Requests a de-elite sweep for the next few seconds. */
    public static void requestPurge() {
        purgeUntilMs = System.currentTimeMillis() + 5000L;
    }

    public static boolean isPurging() {
        return System.currentTimeMillis() < purgeUntilMs;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(EliteComponent.getComponentType());
    }

    @Override
    public void tick(float dt, int index,
                     @Nonnull ArchetypeChunk<EntityStore> chunk,
                     @Nonnull Store<EntityStore> store,
                     @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        if (!isPurging()) {
            return;
        }
        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        deelite(ref, store, commandBuffer);
    }

    public static void deelite(@Nonnull Ref<EntityStore> ref,
                               @Nonnull Store<EntityStore> store,
                               @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        EliteComponent elite = store.getComponent(ref, EliteComponent.getComponentType());
        if (elite == null) {
            return;
        }
        EntityStatMap statMap = store.getComponent(ref, EntityStatMap.getComponentType());
        if (statMap != null) {
            EliteStats.stripHealth(statMap);
        }
        EliteEquipment.strip(ref, store, elite.getEquipment());
        EliteEffects.strip(ref, store, elite.getEffectIndices());
        EliteSpawnRefSystem.applyModelScale(ref, store, 1.0f);
        commandBuffer.removeComponent(ref, EliteComponent.getComponentType());
        if (store.getComponent(ref, EliteAbilityStateComponent.getComponentType()) != null) {
            commandBuffer.removeComponent(ref, EliteAbilityStateComponent.getComponentType());
        }
        if (store.getComponent(ref, ElitePendingAbilityComponent.getComponentType()) != null) {
            commandBuffer.removeComponent(ref, ElitePendingAbilityComponent.getComponentType());
        }
        UUID id = EliteEntities.uuid(store, ref);
        if (id != null) {
            EliteRegistry.get().unregister(id);
            EliteThreatRegistry.clear(id);
        }
    }
}
