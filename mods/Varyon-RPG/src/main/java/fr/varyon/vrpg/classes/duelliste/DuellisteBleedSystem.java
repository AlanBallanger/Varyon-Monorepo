package fr.varyon.vrpg.classes.duelliste;

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

import static com.hypixel.hytale.logger.HytaleLogger.forEnclosingClass;

public final class DuellisteBleedSystem extends EntityTickingSystem<EntityStore> {

    private static final com.hypixel.hytale.logger.HytaleLogger LOG = forEnclosingClass();
    private static final long   TICK_INTERVAL_MS = 1_000L;
    private static final int    TICKS_TOTAL      = (int) (DuellistePassifs.BLEED_DURATION_MS / TICK_INTERVAL_MS);

    private final ConcurrentHashMap<Integer, BleedState> bleeds = new ConcurrentHashMap<>();
    private int healthIdx = Integer.MIN_VALUE;

    private static final class BleedState {
        final float damagePerTick;
        int ticksLeft;
        long nextTickAt;

        BleedState(float damagePerTick) {
            this.damagePerTick = damagePerTick;
            this.ticksLeft     = TICKS_TOTAL;
            this.nextTickAt    = System.currentTimeMillis() + TICK_INTERVAL_MS;
        }
    }

    public void applyBleed(@Nonnull Ref<EntityStore> victimRef, float victimMaxHp,
                           @Nonnull Store<EntityStore> store) {
        float dpt = Math.min(
            victimMaxHp * DuellistePassifs.BLEED_DPS_PCT,
            DuellistePassifs.BLEED_MAX_DAMAGE / TICKS_TOTAL
        );
        bleeds.put(victimRef.getIndex(), new BleedState(dpt));
        DamageFloatBridge.emit(store, victimRef, dpt, "BLEED");
        if (fr.varyon.vrpg.config.VrpgConfig.isDebugCombat())
            LOG.atInfo().log(String.format("[Bleed] applique dpt=%.1f/s maxHp=%.0f", dpt, victimMaxHp));
    }

    @Override
    public void tick(float deltaTime, int index,
                     ArchetypeChunk<EntityStore> chunk,
                     Store<EntityStore> store,
                     CommandBuffer<EntityStore> commandBuffer) {
        try {
            Ref<EntityStore> ref = chunk.getReferenceTo(index);
            BleedState bleed = bleeds.get(ref.getIndex());
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
                    if (fr.varyon.vrpg.config.VrpgConfig.isDebugCombat())
                        LOG.atInfo().log(String.format("[Bleed] tick=%.1f left=%d", bleed.damagePerTick, bleed.ticksLeft));
                }
            }

            if (bleed.ticksLeft <= 0) bleeds.remove(ref.getIndex());
        } catch (Exception ignored) {}
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return NPCEntity.getComponentType();
    }

    private int healthIndex() {
        if (healthIdx == Integer.MIN_VALUE) {
            try { healthIdx = DefaultEntityStatTypes.getHealth(); } catch (Exception e) { healthIdx = -1; }
        }
        return healthIdx;
    }
}
