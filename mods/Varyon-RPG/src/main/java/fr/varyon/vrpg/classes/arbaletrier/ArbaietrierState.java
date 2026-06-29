package fr.varyon.vrpg.classes.arbaletrier;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ArbaietrierState {

    private final ConcurrentHashMap<UUID, Long>    reculBonusExpiry   = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Float>   reculBonusValue    = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long>    carreauLourdExpiry = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> carreauLourdRank   = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long>    miseEnJouExpiry    = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> miseEnJouRank      = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long>    miseEnJouViseeUntil = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long>    immobileStart      = new ConcurrentHashMap<>();

    // Types de carreaux en vol
    public static final int CARREAU_TYPE_LOURD        = 1;
    public static final int CARREAU_TYPE_EXPLOSIF     = 2;
    public static final int CARREAU_TYPE_TRANSPERCANT = 3;

    private final ConcurrentHashMap<UUID, Integer> pendingCarreauType     = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Float>   pendingCarreauDmg      = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> pendingCarreauRank     = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Boolean> pendingMiseEnJouActive = new ConcurrentHashMap<>();

    public ArbaietrierState() {}

    // --- Recul tactique — bonus prochain tir ---

    public void armReculBonus(@Nonnull UUID uuid, float bonus, long windowMs) {
        reculBonusValue.put(uuid, bonus);
        reculBonusExpiry.put(uuid, System.currentTimeMillis() + windowMs);
    }

    public float consumeReculBonus(@Nonnull UUID uuid) {
        Long exp = reculBonusExpiry.remove(uuid);
        Float val = reculBonusValue.remove(uuid);
        if (exp == null || System.currentTimeMillis() >= exp || val == null) return 0f;
        return val;
    }

    public boolean isReculBonusArmed(@Nonnull UUID uuid) {
        Long exp = reculBonusExpiry.get(uuid);
        return exp != null && System.currentTimeMillis() < exp;
    }

    // --- Carreau lourd — bonus prochain tir ---

    public void armCarreauLourd(@Nonnull UUID uuid, int rank, long windowMs) {
        carreauLourdRank.put(uuid, rank);
        carreauLourdExpiry.put(uuid, System.currentTimeMillis() + windowMs);
    }

    public int consumeCarreauLourd(@Nonnull UUID uuid) {
        Long exp = carreauLourdExpiry.remove(uuid);
        Integer rank = carreauLourdRank.remove(uuid);
        if (exp == null || System.currentTimeMillis() >= exp || rank == null) return 0;
        return rank;
    }

    public boolean isCarreauLourdArmed(@Nonnull UUID uuid) {
        Long exp = carreauLourdExpiry.get(uuid);
        return exp != null && System.currentTimeMillis() < exp;
    }

    // --- Mise en joue — crit garanti + bonus dégâts prochain tir ---

    public void armMiseEnJou(@Nonnull UUID uuid, int rank, long windowMs) {
        miseEnJouRank.put(uuid, rank);
        miseEnJouExpiry.put(uuid, System.currentTimeMillis() + windowMs);
    }

    public int consumeMiseEnJou(@Nonnull UUID uuid) {
        Long exp = miseEnJouExpiry.remove(uuid);
        Integer rank = miseEnJouRank.remove(uuid);
        if (exp == null || System.currentTimeMillis() >= exp || rank == null) return 0;
        return rank;
    }

    public boolean isMiseEnJouArmed(@Nonnull UUID uuid) {
        Long exp = miseEnJouExpiry.get(uuid);
        return exp != null && System.currentTimeMillis() < exp;
    }

    public void startMiseEnJouVisee(@Nonnull UUID uuid, long durationMs) {
        miseEnJouViseeUntil.put(uuid, System.currentTimeMillis() + durationMs);
    }

    public boolean isMiseEnJouVisant(@Nonnull UUID uuid) {
        Long until = miseEnJouViseeUntil.get(uuid);
        return until != null && System.currentTimeMillis() < until;
    }

    public void clearMiseEnJouVisee(@Nonnull UUID uuid) {
        miseEnJouViseeUntil.remove(uuid);
    }

    // --- Carreaux en vol (Lourd, Explosif, Transpercant) ---

    public void setPendingCarreau(@Nonnull UUID uuid, int type, float dmg, int rank, boolean miseEnJouActive) {
        pendingCarreauType.put(uuid, type);
        pendingCarreauDmg.put(uuid, dmg);
        pendingCarreauRank.put(uuid, rank);
        pendingMiseEnJouActive.put(uuid, miseEnJouActive);
    }


    public int getPendingCarreauType(@Nonnull UUID uuid) {
        return pendingCarreauType.getOrDefault(uuid, 0);
    }

    public float consumePendingCarreauDmg(@Nonnull UUID uuid) {
        pendingCarreauType.remove(uuid);
        Float dmg = pendingCarreauDmg.remove(uuid);
        return dmg != null ? dmg : 0f;
    }

    public int getPendingCarreauRank(@Nonnull UUID uuid) {
        return pendingCarreauRank.getOrDefault(uuid, 1);
    }

    public boolean isPendingMiseEnJouActive(@Nonnull UUID uuid) {
        return Boolean.TRUE.equals(pendingMiseEnJouActive.getOrDefault(uuid, false));
    }

    public void clearPendingCarreau(@Nonnull UUID uuid) {
        pendingCarreauType.remove(uuid);
        pendingCarreauDmg.remove(uuid);
        pendingCarreauRank.remove(uuid);
        pendingMiseEnJouActive.remove(uuid);
    }

    // --- Détection d'immobilité (pour Ancrage + Tireur Embusqué) ---

    public void markMoving(@Nonnull UUID uuid) {
        immobileStart.remove(uuid);
    }

    public void ensureImmobileTimer(@Nonnull UUID uuid) {
        immobileStart.putIfAbsent(uuid, System.currentTimeMillis());
    }

    public long immobileDurationMs(@Nonnull UUID uuid) {
        Long start = immobileStart.get(uuid);
        if (start == null) return 0L;
        return System.currentTimeMillis() - start;
    }

    public boolean isImmobileFor(@Nonnull UUID uuid, long requiredMs) {
        return immobileDurationMs(uuid) >= requiredMs;
    }

    public void cleanup(@Nonnull UUID uuid) {
        reculBonusExpiry.remove(uuid);
        reculBonusValue.remove(uuid);
        carreauLourdExpiry.remove(uuid);
        carreauLourdRank.remove(uuid);
        miseEnJouExpiry.remove(uuid);
        miseEnJouRank.remove(uuid);
        immobileStart.remove(uuid);
        clearPendingCarreau(uuid);
        miseEnJouViseeUntil.remove(uuid);
    }
}
