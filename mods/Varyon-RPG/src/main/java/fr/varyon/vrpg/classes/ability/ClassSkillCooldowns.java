package fr.varyon.vrpg.classes.ability;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ClassSkillCooldowns {

    private final Map<UUID, Map<String, Long>> lastUse = new ConcurrentHashMap<>();

    public boolean isOnCooldown(@Nonnull UUID playerId, @Nonnull String skillId, long cooldownMs) {
        Map<String, Long> cds = lastUse.get(playerId);
        if (cds == null) return false;
        Long ts = cds.get(skillId);
        if (ts == null) return false;
        return System.currentTimeMillis() - ts < cooldownMs;
    }

    public long remainingMs(@Nonnull UUID playerId, @Nonnull String skillId, long cooldownMs) {
        Map<String, Long> cds = lastUse.get(playerId);
        if (cds == null) return 0L;
        Long ts = cds.get(skillId);
        if (ts == null) return 0L;
        return Math.max(0L, cooldownMs - (System.currentTimeMillis() - ts));
    }

    public void markUsed(@Nonnull UUID playerId, @Nonnull String skillId) {
        lastUse.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
            .put(skillId, System.currentTimeMillis());
    }

    public void clearCooldown(@Nonnull UUID playerId, @Nonnull String skillId) {
        Map<String, Long> cds = lastUse.get(playerId);
        if (cds != null) cds.remove(skillId);
    }

    public void cleanup(@Nonnull UUID playerId) {
        lastUse.remove(playerId);
    }
}
