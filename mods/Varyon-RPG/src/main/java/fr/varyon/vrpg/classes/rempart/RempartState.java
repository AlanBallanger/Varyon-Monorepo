package fr.varyon.vrpg.classes.rempart;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RempartState {

    private final ConcurrentHashMap<UUID, Long>    forteresseExpiry      = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Float>   forteresseReduction   = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long>    gardeRapprocheExpiry  = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Float>   gardeRapprocheReduc   = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long>    provocationExpiry     = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long>    contreOffensifExpiry  = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> contreOffensifRank    = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long>    gardeImpExpiry        = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> gardeImpRank          = new ConcurrentHashMap<>();

    public RempartState() {}

    // --- Forteresse ---

    public void startForteresse(@Nonnull UUID uuid, long durationMs, float reduction) {
        forteresseExpiry.put(uuid, System.currentTimeMillis() + durationMs);
        forteresseReduction.put(uuid, reduction);
    }

    public float getForteresseReduction(@Nonnull UUID uuid) {
        Long exp = forteresseExpiry.get(uuid);
        if (exp == null || System.currentTimeMillis() >= exp) {
            forteresseExpiry.remove(uuid);
            forteresseReduction.remove(uuid);
            return 0f;
        }
        return forteresseReduction.getOrDefault(uuid, 0f);
    }

    // --- Garde Rapprochée ---

    public void startGardeRapprochee(@Nonnull UUID uuid, long durationMs, float reduction) {
        gardeRapprocheExpiry.put(uuid, System.currentTimeMillis() + durationMs);
        gardeRapprocheReduc.put(uuid, reduction);
    }

    public float getGardeRapprocheReduction(@Nonnull UUID uuid) {
        Long exp = gardeRapprocheExpiry.get(uuid);
        if (exp == null || System.currentTimeMillis() >= exp) {
            gardeRapprocheExpiry.remove(uuid);
            gardeRapprocheReduc.remove(uuid);
            return 0f;
        }
        return gardeRapprocheReduc.getOrDefault(uuid, 0f);
    }

    public boolean isGardeRapprocheActive(@Nonnull UUID uuid) {
        return getGardeRapprocheReduction(uuid) > 0f;
    }

    // --- Provocation ---

    public void startProvocation(@Nonnull UUID uuid, long durationMs) {
        provocationExpiry.put(uuid, System.currentTimeMillis() + durationMs);
    }

    public boolean isProvocationActive(@Nonnull UUID uuid) {
        Long exp = provocationExpiry.get(uuid);
        if (exp == null) return false;
        if (System.currentTimeMillis() < exp) return true;
        provocationExpiry.remove(uuid);
        return false;
    }

    // --- Contre Offensif ---

    public void armContreOffensif(@Nonnull UUID uuid, long windowMs, int rank) {
        contreOffensifExpiry.put(uuid, System.currentTimeMillis() + windowMs);
        contreOffensifRank.put(uuid, rank);
    }

    public int consumeContreOffensif(@Nonnull UUID uuid) {
        Long exp = contreOffensifExpiry.remove(uuid);
        Integer rank = contreOffensifRank.remove(uuid);
        if (exp == null || System.currentTimeMillis() >= exp) return 0;
        return rank != null ? rank : 0;
    }

    // --- Garde Impénétrable ---

    public void armGardeImpenetrable(@Nonnull UUID uuid, long durationMs, int rank) {
        gardeImpExpiry.put(uuid, System.currentTimeMillis() + durationMs);
        gardeImpRank.put(uuid, rank);
    }

    public int getGardeImpenetrableRank(@Nonnull UUID uuid) {
        Long exp = gardeImpExpiry.get(uuid);
        if (exp == null || System.currentTimeMillis() >= exp) {
            gardeImpExpiry.remove(uuid);
            gardeImpRank.remove(uuid);
            return 0;
        }
        return gardeImpRank.getOrDefault(uuid, 0);
    }

    public void cleanup(@Nonnull UUID uuid) {
        forteresseExpiry.remove(uuid);
        forteresseReduction.remove(uuid);
        gardeRapprocheExpiry.remove(uuid);
        gardeRapprocheReduc.remove(uuid);
        provocationExpiry.remove(uuid);
        contreOffensifExpiry.remove(uuid);
        contreOffensifRank.remove(uuid);
        gardeImpExpiry.remove(uuid);
        gardeImpRank.remove(uuid);
    }
}
