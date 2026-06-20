package fr.varyon.vrpg.classes.lancier;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import fr.varyon.vrpg.integration.DamageFloatBridge;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.ConcurrentHashMap;

public final class LancierBleedSystem extends EntityTickingSystem<EntityStore> {

    private static final long TICK_INTERVAL_MS = 1_000L;

    private final ConcurrentHashMap<Long, BleedState> bleeds = new ConcurrentHashMap<>();
    private Integer healthIdx = null;

    private static final class BleedState {
        final float damagePerTick;
        int ticksLeft;
        long nextTickAt;

        BleedState(float dpt, int ticks) {
            this.damagePerTick = dpt;
            this.ticksLeft = ticks;
            this.nextTickAt = System.currentTimeMillis() + TICK_INTERVAL_MS;
        }
    }

    public void applyBleed(@Nonnull Ref<EntityStore> victimRef, float damagePerTick, int ticks,
                           @Nonnull Store<EntityStore> store) {
        long key = victimRef.getIndex();
        BleedState existing = bleeds.get(key);
        if (existing != null && existing.damagePerTick >= damagePerTick) return;
        bleeds.put(key, new BleedState(damagePerTick, ticks));
        DamageFloatBridge.emit(store, victimRef, damagePerTick, "BLEED");
    }

    @Override
    public void tick(float deltaTime, int index,
                     ArchetypeChunk<EntityStore> chunk,
                     Store<EntityStore> store,
                     CommandBuffer<EntityStore> commandBuffer) {
        try {
            Ref<EntityStore> ref = chunk.getReferenceTo(index);
            long key = ref.getIndex();
            BleedState bleed = bleeds.get(key);
            if (bleed == null) return;

            long now = System.currentTimeMillis();
            if (now < bleed.nextTickAt) return;

            bleed.nextTickAt = now + TICK_INTERVAL_MS;
            bleed.ticksLeft--;

            int hIdx = healthIndex();
            if (hIdx >= 0) {
                EntityStatMap stats = store.getComponent(ref, EntityStatMap.getComponentType());
                if (stats != null) {
                    stats.addStatValue(hIdx, -bleed.damagePerTick);
                    DamageFloatBridge.emit(store, commandBuffer, ref, bleed.damagePerTick, "BLEED");
                }
            }

            if (bleed.ticksLeft <= 0) bleeds.remove(key);
        } catch (Exception ignored) {}
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return NPCEntity.getComponentType();
    }

    private int healthIndex() {
        if (healthIdx == null) {
            try { healthIdx = DefaultEntityStatTypes.getHealth(); } catch (Exception e) { healthIdx = -1; }
        }
        return healthIdx;
    }
}
