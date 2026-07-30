package irai.mod.reforge.Entity.Events;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import irai.mod.DynamicFloatingDamageFormatter.DamageNumbers;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

public final class HealingFloatTickSystem extends EntityTickingSystem<EntityStore> {

    private static final float HEAL_EPSILON = 0.5f;
    private static final long SWEEP_INTERVAL_SECONDS = 60L;
    private final Map<Ref<EntityStore>, Float> previousHealth = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService SWEEPER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "VaryonDamageNumber-HealthTrackSweep");
        t.setDaemon(true);
        return t;
    });

    {
        SWEEPER.scheduleAtFixedRate(this::sweepInvalidEntries,
                SWEEP_INTERVAL_SECONDS, SWEEP_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    /** Reclaims entries left behind by entities that despawned without being revisited by tick(). */
    private void sweepInvalidEntries() {
        try {
            Iterator<Ref<EntityStore>> it = previousHealth.keySet().iterator();
            while (it.hasNext()) {
                Ref<EntityStore> ref = it.next();
                if (ref == null || !ref.isValid()) {
                    it.remove();
                }
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public @Nullable Query<EntityStore> getQuery() {
        return Query.and(EntityStatMap.getComponentType(), TransformComponent.getComponentType());
    }

    @Override
    public void tick(float dt,
                     int index,
                     ArchetypeChunk<EntityStore> chunk,
                     Store<EntityStore> store,
                     CommandBuffer<EntityStore> commandBuffer) {
        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        if (!ref.isValid()) {
            previousHealth.remove(ref);
            return;
        }
        HealFloatCoordinator.removeIfStale(ref);

        EntityStatMap statMap = chunk.getComponent(index, EntityStatMap.getComponentType());
        if (statMap == null) {
            previousHealth.remove(ref);
            return;
        }
        int healthIdx = DefaultEntityStatTypes.getHealth();
        EntityStatValue health = statMap.get(healthIdx);
        if (health == null) {
            previousHealth.remove(ref);
            return;
        }
        float current = health.get();
        Float prev = previousHealth.put(ref, current);
        if (prev == null) {
            return;
        }
        if (prev <= 0f) {
            return;
        }
        float delta = current - prev;
        if (delta <= HEAL_EPSILON) {
            return;
        }
        if (HealFloatCoordinator.shouldSuppressStatHeal(ref)) {
            return;
        }
        DamageNumbers.emit(store, commandBuffer, ref, delta, "HEAL");
    }
}
