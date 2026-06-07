package fr.varyon.vrpg.classes.duelliste;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DuellisteState {

    private static final long CONTRE_ATTAQUE_WINDOW_MS = 8_000L;

    private final ConcurrentHashMap<UUID, Long> assautBretteurExpiry  = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Long> desarmementExpiry  = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> desarmementTarget  = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> contreAttaqueExpiry   = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> momentumStacks     = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> lastHitTakenAt        = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> lastDamageDealtAt     = new ConcurrentHashMap<>();

    public DuellisteState() {}

    // --- Assaut du Bretteur ---

    public void startAssautBretteur(@Nonnull UUID uuid, long durationMs) {
        assautBretteurExpiry.put(uuid, System.currentTimeMillis() + durationMs);
    }

    public boolean isAssautBretteurActive(@Nonnull UUID uuid) {
        Long exp = assautBretteurExpiry.get(uuid);
        return exp != null && System.currentTimeMillis() < exp;
    }

    public boolean consumeAssautBretteurIfExpired(@Nonnull UUID uuid) {
        Long exp = assautBretteurExpiry.get(uuid);
        if (exp == null) return false;
        if (System.currentTimeMillis() >= exp) {
            assautBretteurExpiry.remove(uuid);
            return true;
        }
        return false;
    }

    // --- Désarmement (applied to enemy NPC, tracked by attacker uuid → victim entity key) ---

    public void startDesarmement(@Nonnull UUID attackerUuid, int victimRefIndex, long durationMs) {
        desarmementExpiry.put(victimRefIndex, System.currentTimeMillis() + durationMs);
        desarmementTarget.put(attackerUuid, victimRefIndex);
    }

    public boolean isDesarmed(int victimRefIndex) {
        Long exp = desarmementExpiry.get(victimRefIndex);
        if (exp == null) return false;
        if (System.currentTimeMillis() < exp) return true;
        desarmementExpiry.remove(victimRefIndex);
        return false;
    }

    // --- Contre-Attaque (buff after a parry/block) ---

    public void recordParry(@Nonnull UUID uuid) {
        contreAttaqueExpiry.put(uuid, System.currentTimeMillis() + CONTRE_ATTAQUE_WINDOW_MS);
    }

    public boolean consumeContreAttaque(@Nonnull UUID uuid) {
        Long exp = contreAttaqueExpiry.remove(uuid);
        return exp != null && System.currentTimeMillis() < exp;
    }

    public boolean hasContreAttaqueBuff(@Nonnull UUID uuid) {
        Long exp = contreAttaqueExpiry.get(uuid);
        return exp != null && System.currentTimeMillis() < exp;
    }

    // --- Momentum ---

    public void recordDamageDealt(@Nonnull UUID uuid) {
        lastDamageDealtAt.put(uuid, System.currentTimeMillis());
    }

    public void recordHitTaken(@Nonnull UUID uuid) {
        lastHitTakenAt.put(uuid, System.currentTimeMillis());
        momentumStacks.remove(uuid);
    }

    private static final long MOMENTUM_WINDOW_MS = 5_000L;

    public int incrementMomentum(@Nonnull UUID uuid, int maxStacks) {
        long now = System.currentTimeMillis();
        Long prevDealt = lastDamageDealtAt.get(uuid);
        Long lastHit   = lastHitTakenAt.get(uuid);

        lastDamageDealtAt.put(uuid, now);

        if (lastHit != null && prevDealt != null && lastHit > prevDealt) {
            momentumStacks.remove(uuid);
            return 1;
        }
        if (prevDealt == null || now - prevDealt > MOMENTUM_WINDOW_MS) {
            momentumStacks.put(uuid, 1);
            return 1;
        }
        int stacks = momentumStacks.merge(uuid, 1, Integer::sum);
        return Math.min(stacks, maxStacks);
    }

    public int getMomentumStacks(@Nonnull UUID uuid) {
        return momentumStacks.getOrDefault(uuid, 0);
    }

    public void cleanup(@Nonnull UUID uuid) {
        assautBretteurExpiry.remove(uuid);
        Integer targetIdx = desarmementTarget.remove(uuid);
        if (targetIdx != null) desarmementExpiry.remove(targetIdx);
        contreAttaqueExpiry.remove(uuid);
        momentumStacks.remove(uuid);
        lastHitTakenAt.remove(uuid);
        lastDamageDealtAt.remove(uuid);
    }
}
