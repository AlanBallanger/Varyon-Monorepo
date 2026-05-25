package irai.mod.reforge.Entity.Events;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class HealFloatCoordinator {

    private static final long SUPPRESS_STAT_MS = 200L;
    private static final Map<Ref<EntityStore>, Long> recentDamageEventHeal =
            new ConcurrentHashMap<>();

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
}
