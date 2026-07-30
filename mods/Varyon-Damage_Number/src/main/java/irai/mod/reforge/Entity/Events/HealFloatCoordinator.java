package irai.mod.reforge.Entity.Events;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class HealFloatCoordinator {

    private static final long SUPPRESS_STAT_MS = 200L;
    private static final long SWEEP_INTERVAL_SECONDS = 60L;
    private static final Map<Ref<EntityStore>, Long> recentDamageEventHeal =
            new ConcurrentHashMap<>();
    private static final ScheduledExecutorService SWEEPER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "VaryonDamageNumber-HealFloatSweep");
        t.setDaemon(true);
        return t;
    });

    static {
        SWEEPER.scheduleAtFixedRate(HealFloatCoordinator::sweepStaleEntries,
                SWEEP_INTERVAL_SECONDS, SWEEP_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    private HealFloatCoordinator() {}

    public static void markFromDamageEvent(Ref<EntityStore> targetRef) {
        if (targetRef == null || !targetRef.isValid()) {
            return;
        }
        recentDamageEventHeal.put(targetRef, System.currentTimeMillis());
    }

    public static boolean shouldSuppressStatHeal(Ref<EntityStore> targetRef) {
        if (targetRef == null || !targetRef.isValid()) {
            return false;
        }
        Long t = recentDamageEventHeal.get(targetRef);
        if (t == null) {
            return false;
        }
        if (System.currentTimeMillis() - t > SUPPRESS_STAT_MS) {
            recentDamageEventHeal.remove(targetRef);
            return false;
        }
        return true;
    }

    public static void removeIfStale(Ref<EntityStore> targetRef) {
        if (targetRef == null || !targetRef.isValid()) {
            return;
        }
        Long t = recentDamageEventHeal.get(targetRef);
        if (t != null && System.currentTimeMillis() - t > SUPPRESS_STAT_MS) {
            recentDamageEventHeal.remove(targetRef);
        }
    }

    /** Reclaims entries left behind by entities that despawned before their suppression window expired. */
    private static void sweepStaleEntries() {
        try {
            long now = System.currentTimeMillis();
            Iterator<Map.Entry<Ref<EntityStore>, Long>> it = recentDamageEventHeal.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Ref<EntityStore>, Long> entry = it.next();
                Ref<EntityStore> ref = entry.getKey();
                if (ref == null || !ref.isValid() || now - entry.getValue() > SUPPRESS_STAT_MS) {
                    it.remove();
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
