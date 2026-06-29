package fr.varyon.vrpg.classes.rodeur;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RodeurState {

    private final ConcurrentHashMap<UUID, Long>    speedExpiry      = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Float>   speedBonus       = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long>    marqueExpiry     = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> marqueRank       = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long>    marqueTargetIdx  = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> marqueConsumes   = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long>    miseEnJouExpiry  = new ConcurrentHashMap<>();

    // --- Flèches en vol (Recul, Entravante, Rafale) ---
    // Type: 0=none, 1=recul, 2=entravante, 3=rafale, 4=marquage
    private final ConcurrentHashMap<UUID, Float>   pendingArrowDmg       = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> pendingArrowType      = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Double>  pendingArrowKb        = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long>    pendingArrowRoot      = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> pendingMarqueRank     = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long>    pendingMarqueDuration = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> pendingMarqueConsumes = new ConcurrentHashMap<>();

    public RodeurState() {}

    // --- Recul Stratégique — speed boost ---

    public void startSpeedBoost(@Nonnull UUID uuid, float bonus, long durationMs) {
        speedBonus.put(uuid, bonus);
        speedExpiry.put(uuid, System.currentTimeMillis() + durationMs);
    }

    public float getSpeedBonus(@Nonnull UUID uuid) {
        Long exp = speedExpiry.get(uuid);
        if (exp == null || System.currentTimeMillis() >= exp) {
            speedExpiry.remove(uuid);
            speedBonus.remove(uuid);
            return 0f;
        }
        return speedBonus.getOrDefault(uuid, 0f);
    }

    // --- Marque du Chasseur ---

    public void startMarque(@Nonnull UUID uuid, int rank, long durationMs, long targetEntityIdx, int maxConsumes) {
        marqueExpiry.put(uuid, System.currentTimeMillis() + durationMs);
        marqueRank.put(uuid, rank);
        marqueTargetIdx.put(uuid, targetEntityIdx);
        marqueConsumes.put(uuid, maxConsumes);
    }

    public int getMarqueRank(@Nonnull UUID uuid) {
        Long exp = marqueExpiry.get(uuid);
        if (exp == null || System.currentTimeMillis() >= exp) {
            clearMarque(uuid);
            return 0;
        }
        return marqueRank.getOrDefault(uuid, 0);
    }

    public long getMarqueTargetIdx(@Nonnull UUID uuid) {
        return marqueTargetIdx.getOrDefault(uuid, -1L);
    }

    public boolean isMarqueActive(@Nonnull UUID uuid) {
        return getMarqueRank(uuid) > 0;
    }

    public boolean consumeMarque(@Nonnull UUID uuid) {
        if (getMarqueRank(uuid) <= 0) return false;
        int remaining = marqueConsumes.merge(uuid, -1, Integer::sum);
        if (remaining <= 0) clearMarque(uuid);
        return true;
    }

    public void clearMarque(@Nonnull UUID uuid) {
        marqueExpiry.remove(uuid);
        marqueRank.remove(uuid);
        marqueTargetIdx.remove(uuid);
        marqueConsumes.remove(uuid);
    }

    public static final int ARROW_TYPE_RECUL      = 1;
    public static final int ARROW_TYPE_ENTRAVANTE = 2;
    public static final int ARROW_TYPE_RAFALE     = 3;
    public static final int ARROW_TYPE_MARQUAGE   = 4;

    public void setPendingArrow(@Nonnull UUID uuid, int type, float dmg, double kb, long rootMs) {
        pendingArrowDmg.put(uuid, dmg);
        pendingArrowType.put(uuid, type);
        pendingArrowKb.put(uuid, kb);
        pendingArrowRoot.put(uuid, rootMs);
    }

    public void setPendingMarquage(@Nonnull UUID uuid, int rank, long durationMs, int maxConsumes) {
        pendingArrowType.put(uuid, ARROW_TYPE_MARQUAGE);
        pendingMarqueRank.put(uuid, rank);
        pendingMarqueDuration.put(uuid, durationMs);
        pendingMarqueConsumes.put(uuid, maxConsumes);
    }

    public int consumePendingMarquageRank(@Nonnull UUID uuid) {
        pendingArrowType.remove(uuid);
        Integer rank = pendingMarqueRank.remove(uuid);
        return rank != null ? rank : 0;
    }

    public long getPendingMarqueDuration(@Nonnull UUID uuid) {
        return pendingMarqueDuration.getOrDefault(uuid, 0L);
    }

    public int getPendingMarqueConsumes(@Nonnull UUID uuid) {
        return pendingMarqueConsumes.getOrDefault(uuid, 0);
    }

    public int getPendingArrowType(@Nonnull UUID uuid) {
        return pendingArrowType.getOrDefault(uuid, 0);
    }

    public float consumePendingArrowDmg(@Nonnull UUID uuid) {
        Float dmg = pendingArrowDmg.remove(uuid);
        pendingArrowType.remove(uuid);
        return dmg != null ? dmg : 0f;
    }

    public void clearPendingArrow(@Nonnull UUID uuid) {
        pendingArrowDmg.remove(uuid);
        pendingArrowType.remove(uuid);
        pendingArrowKb.remove(uuid);
        pendingArrowRoot.remove(uuid);
    }

    public double getPendingArrowKb(@Nonnull UUID uuid) {
        return pendingArrowKb.getOrDefault(uuid, 0.0);
    }

    public long getPendingArrowRoot(@Nonnull UUID uuid) {
        return pendingArrowRoot.getOrDefault(uuid, 0L);
    }

    // --- Mise en jou (Arbalétrier) — prochain tir garanti crit + dégâts augmentés ---

    public void armMiseEnJou(@Nonnull UUID uuid, long windowMs) {
        miseEnJouExpiry.put(uuid, System.currentTimeMillis() + windowMs);
    }

    public boolean consumeMiseEnJou(@Nonnull UUID uuid) {
        Long exp = miseEnJouExpiry.remove(uuid);
        return exp != null && System.currentTimeMillis() < exp;
    }

    public boolean isMiseEnJouArmed(@Nonnull UUID uuid) {
        Long exp = miseEnJouExpiry.get(uuid);
        return exp != null && System.currentTimeMillis() < exp;
    }

    public void cleanup(@Nonnull UUID uuid) {
        speedExpiry.remove(uuid);
        speedBonus.remove(uuid);
        clearMarque(uuid);
        miseEnJouExpiry.remove(uuid);
        pendingArrowDmg.remove(uuid);
        pendingArrowType.remove(uuid);
        pendingArrowKb.remove(uuid);
        pendingArrowRoot.remove(uuid);
        pendingMarqueRank.remove(uuid);
        pendingMarqueDuration.remove(uuid);
        pendingMarqueConsumes.remove(uuid);
    }
}
