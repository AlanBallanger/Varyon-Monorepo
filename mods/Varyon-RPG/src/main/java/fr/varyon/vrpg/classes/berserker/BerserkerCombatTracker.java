package fr.varyon.vrpg.classes.berserker;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BerserkerCombatTracker {

    private static final long COMBAT_WINDOW_MS = 8_000L;

    private final ConcurrentHashMap<UUID, Long> lastCombatAt = new ConcurrentHashMap<>();

    public void markCombat(@Nonnull UUID uuid) {
        lastCombatAt.put(uuid, System.currentTimeMillis());
    }

    public boolean isInCombat(@Nonnull UUID uuid) {
        Long last = lastCombatAt.get(uuid);
        if (last == null) return false;
        if (System.currentTimeMillis() - last <= COMBAT_WINDOW_MS) return true;
        lastCombatAt.remove(uuid);
        return false;
    }

    public void remove(@Nonnull UUID uuid) {
        lastCombatAt.remove(uuid);
    }
}
