package fr.varyon.vrpg.classes.ombre;

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

public final class OmbrePoisonSystem extends EntityTickingSystem<EntityStore> {

    private static final long TICK_INTERVAL_MS = 1_000L;
    private static final int  TICKS_TOTAL = (int)(OmbrePassifs.POISON_DURATION_MS / TICK_INTERVAL_MS);

    private final ConcurrentHashMap<Integer, PoisonState> poisons = new ConcurrentHashMap<>();
    private Integer healthIdx = null;

    private static final class PoisonState {
        final float damagePerTick;
        int ticksLeft;
        long nextTickAt;

        PoisonState(float damagePerTick) {
            this.damagePerTick = damagePerTick;
            this.ticksLeft = TICKS_TOTAL;
            this.nextTickAt = System.currentTimeMillis() + TICK_INTERVAL_MS;
        }
    }

    public void applyPoison(@Nonnull Ref<EntityStore> victimRef, float weaponDamage,
                            @Nonnull Store<EntityStore> store) {
        float dpt = weaponDamage * OmbrePassifs.POISON_WEAPON_PCT;
        poisons.put(victimRef.getIndex(), new PoisonState(dpt));
        DamageFloatBridge.emit(store, victimRef, dpt, "POISON");
    }

    @Override
    public void tick(float deltaTime, int index,
                     ArchetypeChunk<EntityStore> chunk,
                     Store<EntityStore> store,
                     CommandBuffer<EntityStore> commandBuffer) {
        try {
            Ref<EntityStore> ref = chunk.getReferenceTo(index);
            PoisonState poison = poisons.get(ref.getIndex());
            if (poison == null) return;

            long now = System.currentTimeMillis();
            if (now < poison.nextTickAt) return;

            poison.nextTickAt = now + TICK_INTERVAL_MS;
            poison.ticksLeft--;

            int hIdx = healthIndex();
            if (hIdx >= 0) {
                EntityStatMap stats = store.getComponent(ref, EntityStatMap.getComponentType());
                if (stats != null) {
                    stats.addStatValue(hIdx, -poison.damagePerTick);
                    DamageFloatBridge.emit(store, commandBuffer, ref, poison.damagePerTick, "POISON");
                }
            }

            if (poison.ticksLeft <= 0) poisons.remove(ref.getIndex());
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
