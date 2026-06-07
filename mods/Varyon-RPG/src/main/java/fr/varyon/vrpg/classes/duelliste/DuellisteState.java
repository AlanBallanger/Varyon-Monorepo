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
    private final ConcurrentHashMap<UUID, Long>  coupEstocExpiry      = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Float> coupEstocMult        = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> feintExpiry           = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> riposteWindowExpiry   = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> riposteRank        = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> perceeExpiry          = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> perceeRank         = new ConcurrentHashMap<>();

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

    // --- Coup d'Estoc (prochain coup armé) ---

    public void armCoupEstoc(@Nonnull UUID uuid, float mult, long windowMs) {
        coupEstocExpiry.put(uuid, System.currentTimeMillis() + windowMs);
        coupEstocMult.put(uuid, mult);
    }

    public float consumeCoupEstoc(@Nonnull UUID uuid) {
        Long exp = coupEstocExpiry.remove(uuid);
        Float mult = coupEstocMult.remove(uuid);
        if (exp == null || System.currentTimeMillis() >= exp) return 0f;
        return mult != null ? mult : 0f;
    }

    // --- Feinte ---

    public void startFeinte(@Nonnull UUID uuid, long windowMs) {
        feintExpiry.put(uuid, System.currentTimeMillis() + windowMs);
    }

    public boolean consumeFeinte(@Nonnull UUID uuid) {
        Long exp = feintExpiry.remove(uuid);
        return exp != null && System.currentTimeMillis() < exp;
    }

    // --- Riposte Parfaite ---

    public void startRiposteWindow(@Nonnull UUID uuid, long windowMs, int rank) {
        riposteWindowExpiry.put(uuid, System.currentTimeMillis() + windowMs);
        riposteRank.put(uuid, rank);
    }

    public boolean isRiposteWindowActive(@Nonnull UUID uuid) {
        Long exp = riposteWindowExpiry.get(uuid);
        if (exp == null) return false;
        if (System.currentTimeMillis() < exp) return true;
        riposteWindowExpiry.remove(uuid);
        riposteRank.remove(uuid);
        return false;
    }

    public int consumeRiposte(@Nonnull UUID uuid) {
        Long exp = riposteWindowExpiry.remove(uuid);
        Integer rank = riposteRank.remove(uuid);
        if (exp == null || System.currentTimeMillis() >= exp) return 0;
        return rank != null ? rank : 0;
    }

    // --- Percée (prochaine attaque boostée) ---

    public void startPercee(@Nonnull UUID uuid, long windowMs, int rank) {
        perceeExpiry.put(uuid, System.currentTimeMillis() + windowMs);
        perceeRank.put(uuid, rank);
    }

    public int consumePercee(@Nonnull UUID uuid) {
        Long exp = perceeExpiry.remove(uuid);
        Integer rank = perceeRank.remove(uuid);
        if (exp == null || System.currentTimeMillis() >= exp) return 0;
        return rank != null ? rank : 0;
    }

    public void cleanup(@Nonnull UUID uuid) {
        assautBretteurExpiry.remove(uuid);
        Integer targetIdx = desarmementTarget.remove(uuid);
        if (targetIdx != null) desarmementExpiry.remove(targetIdx);
        contreAttaqueExpiry.remove(uuid);
        momentumStacks.remove(uuid);
        lastHitTakenAt.remove(uuid);
        lastDamageDealtAt.remove(uuid);
        coupEstocExpiry.remove(uuid);
        coupEstocMult.remove(uuid);
        feintExpiry.remove(uuid);
        riposteWindowExpiry.remove(uuid);
        riposteRank.remove(uuid);
        perceeExpiry.remove(uuid);
        perceeRank.remove(uuid);
    }
}
