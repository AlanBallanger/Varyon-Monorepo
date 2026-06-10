package fr.varyon.vrpg.classes.ombre;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class OmbreState {

    private final ConcurrentHashMap<UUID, Long> stealthExpiry         = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> ecranFumeeExpiry      = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> frappeFataleExpiry    = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> chasseOuverteExpiry   = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> chasseOuverteRank  = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> embuscadeExpiry       = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> embuscadeRank      = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> danseLamesExpiry      = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Float> danseLamesBonus      = new ConcurrentHashMap<>();

    public OmbreState() {}

    // --- Pas des Ténèbres / Écran de Fumée — invisibilité ---

    public void startStealth(@Nonnull UUID uuid, long durationMs) {
        stealthExpiry.put(uuid, System.currentTimeMillis() + durationMs);
    }

    public boolean isInStealth(@Nonnull UUID uuid) {
        Long exp = stealthExpiry.get(uuid);
        if (exp == null) return false;
        if (System.currentTimeMillis() < exp) return true;
        stealthExpiry.remove(uuid);
        return false;
    }

    public void breakStealth(@Nonnull UUID uuid) {
        stealthExpiry.remove(uuid);
    }

    // --- Écran de Fumée ---

    public void startEcranFumee(@Nonnull UUID uuid, long durationMs) {
        ecranFumeeExpiry.put(uuid, System.currentTimeMillis() + durationMs);
        startStealth(uuid, durationMs);
    }

    public boolean isEcranFumeeActive(@Nonnull UUID uuid) {
        Long exp = ecranFumeeExpiry.get(uuid);
        if (exp == null) return false;
        if (System.currentTimeMillis() < exp) return true;
        ecranFumeeExpiry.remove(uuid);
        return false;
    }

    // --- Frappe Fatale ---

    public void armFrappeFatale(@Nonnull UUID uuid, long windowMs) {
        frappeFataleExpiry.put(uuid, System.currentTimeMillis() + windowMs);
    }

    public boolean consumeFrappeFatale(@Nonnull UUID uuid) {
        Long exp = frappeFataleExpiry.remove(uuid);
        return exp != null && System.currentTimeMillis() < exp;
    }

    public boolean isFrappeFataleArmed(@Nonnull UUID uuid) {
        Long exp = frappeFataleExpiry.get(uuid);
        return exp != null && System.currentTimeMillis() < exp;
    }

    // --- Chasse Ouverte ---

    public void startChaseOuverte(@Nonnull UUID uuid, long durationMs, int rank) {
        chasseOuverteExpiry.put(uuid, System.currentTimeMillis() + durationMs);
        chasseOuverteRank.put(uuid, rank);
    }

    public int getChaseOuverteRank(@Nonnull UUID uuid) {
        Long exp = chasseOuverteExpiry.get(uuid);
        if (exp == null || System.currentTimeMillis() >= exp) {
            chasseOuverteExpiry.remove(uuid);
            chasseOuverteRank.remove(uuid);
            return 0;
        }
        return chasseOuverteRank.getOrDefault(uuid, 0);
    }

    // --- Embuscade (passif) — immobilise après stealth ---

    public void armEmbuscade(@Nonnull UUID uuid, long windowMs, int rank) {
        embuscadeExpiry.put(uuid, System.currentTimeMillis() + windowMs);
        embuscadeRank.put(uuid, rank);
    }

    public int consumeEmbuscade(@Nonnull UUID uuid) {
        Long exp = embuscadeExpiry.remove(uuid);
        Integer rank = embuscadeRank.remove(uuid);
        if (exp == null || System.currentTimeMillis() >= exp) return 0;
        return rank != null ? rank : 0;
    }

    // --- Danse des Lames (passif — speed après crit) ---

    public void startDanseLames(@Nonnull UUID uuid, float bonus, long durationMs) {
        danseLamesExpiry.put(uuid, System.currentTimeMillis() + durationMs);
        danseLamesBonus.put(uuid, bonus);
    }

    public float getDanseLamesBonus(@Nonnull UUID uuid) {
        Long exp = danseLamesExpiry.get(uuid);
        if (exp == null || System.currentTimeMillis() >= exp) {
            danseLamesExpiry.remove(uuid);
            danseLamesBonus.remove(uuid);
            return 0f;
        }
        return danseLamesBonus.getOrDefault(uuid, 0f);
    }

    public boolean isDanseLamesActive(@Nonnull UUID uuid) {
        return getDanseLamesBonus(uuid) > 0f;
    }

    public void cleanup(@Nonnull UUID uuid) {
        stealthExpiry.remove(uuid);
        ecranFumeeExpiry.remove(uuid);
        frappeFataleExpiry.remove(uuid);
        chasseOuverteExpiry.remove(uuid);
        chasseOuverteRank.remove(uuid);
        embuscadeExpiry.remove(uuid);
        embuscadeRank.remove(uuid);
        danseLamesExpiry.remove(uuid);
        danseLamesBonus.remove(uuid);
    }
}
