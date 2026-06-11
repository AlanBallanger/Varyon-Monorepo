package fr.varyon.vrpg.classes.berserker;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BerserkerState {

    // --- Assaut Bestial (armé sur prochain coup) ---
    private final ConcurrentHashMap<UUID, Long>    assautExpiry        = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> assautRank          = new ConcurrentHashMap<>();

    // --- Éviscération (armé sur prochain coup) ---
    private final ConcurrentHashMap<UUID, Long>    eviscExpiry         = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> eviscRank           = new ConcurrentHashMap<>();

    // --- Cri de Ralliement ---
    private final ConcurrentHashMap<UUID, Long>    criExpiry           = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Float>   criBonus            = new ConcurrentHashMap<>();

    // --- Cor de Guerre (stun + vitesse d'attaque, géré par état temporel) ---
    private final ConcurrentHashMap<UUID, Long>    corExpiry           = new ConcurrentHashMap<>();

    // --- Dernier Souffle (immortalité temporaire) ---
    private final ConcurrentHashMap<UUID, Long>    dernierSouffleExpiry = new ConcurrentHashMap<>();

    // --- Ferveur Guerrière (stacks dégâts, 1/s max 15s) ---
    private final ConcurrentHashMap<UUID, Integer> ferveurStacks       = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long>    ferveurLastTick     = new ConcurrentHashMap<>();

    // --- Fureur Sanguinaire (vol de vie après kill) ---
    private final ConcurrentHashMap<UUID, Long>    fureurExpiry        = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> fureurRank          = new ConcurrentHashMap<>();

    // --- Carnage (stacks XP sur série de kills) ---
    private final ConcurrentHashMap<UUID, Integer> carnageStacks       = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long>    carnageLastKill     = new ConcurrentHashMap<>();

    // --- Frénésie (stacks vitesse+dégâts après kill, max 3) ---
    private final ConcurrentHashMap<UUID, Integer> frenesieStacks      = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long>    frenesieExpiry      = new ConcurrentHashMap<>();

    public BerserkerState() {}

    // ---- Assaut Bestial ----

    public void armAssautBestial(@Nonnull UUID uuid, long windowMs, int rank) {
        assautExpiry.put(uuid, System.currentTimeMillis() + windowMs);
        assautRank.put(uuid, rank);
    }

    public int consumeAssautBestial(@Nonnull UUID uuid) {
        Long exp = assautExpiry.remove(uuid);
        Integer r = assautRank.remove(uuid);
        if (exp == null || System.currentTimeMillis() >= exp) return 0;
        return r != null ? r : 0;
    }

    // ---- Éviscération ----

    public void armEvisc(@Nonnull UUID uuid, long windowMs, int rank) {
        eviscExpiry.put(uuid, System.currentTimeMillis() + windowMs);
        eviscRank.put(uuid, rank);
    }

    public int consumeEvisc(@Nonnull UUID uuid) {
        Long exp = eviscExpiry.remove(uuid);
        Integer r = eviscRank.remove(uuid);
        if (exp == null || System.currentTimeMillis() >= exp) return 0;
        return r != null ? r : 0;
    }

    // ---- Cri de Ralliement ----

    public void startCriRalliement(@Nonnull UUID uuid, long durationMs, float bonus) {
        criExpiry.put(uuid, System.currentTimeMillis() + durationMs);
        criBonus.put(uuid, bonus);
    }

    public float getCriRalliementBonus(@Nonnull UUID uuid) {
        Long exp = criExpiry.get(uuid);
        if (exp == null || System.currentTimeMillis() >= exp) {
            criExpiry.remove(uuid);
            criBonus.remove(uuid);
            return 0f;
        }
        return criBonus.getOrDefault(uuid, 0f);
    }

    // ---- Cor de Guerre ----

    public void startCor(@Nonnull UUID uuid, long durationMs) {
        corExpiry.put(uuid, System.currentTimeMillis() + durationMs);
    }

    public boolean isCorActive(@Nonnull UUID uuid) {
        Long exp = corExpiry.get(uuid);
        if (exp == null) return false;
        if (System.currentTimeMillis() < exp) return true;
        corExpiry.remove(uuid);
        return false;
    }

    // ---- Dernier Souffle ----

    public void startDernierSouffle(@Nonnull UUID uuid, long durationMs) {
        dernierSouffleExpiry.put(uuid, System.currentTimeMillis() + durationMs);
    }

    public boolean isDernierSouffleActive(@Nonnull UUID uuid) {
        Long exp = dernierSouffleExpiry.get(uuid);
        if (exp == null) return false;
        if (System.currentTimeMillis() < exp) return true;
        dernierSouffleExpiry.remove(uuid);
        return false;
    }

    public void endDernierSouffle(@Nonnull UUID uuid) {
        dernierSouffleExpiry.remove(uuid);
    }

    // ---- Ferveur Guerrière ----

    public int getFerveurStacks(@Nonnull UUID uuid) {
        return ferveurStacks.getOrDefault(uuid, 0);
    }

    public int tickFerveur(@Nonnull UUID uuid, int maxStacks) {
        long now = System.currentTimeMillis();
        Long last = ferveurLastTick.get(uuid);
        if (last == null || now - last >= 1000L) {
            ferveurLastTick.put(uuid, now);
            int current = ferveurStacks.getOrDefault(uuid, 0);
            if (current < maxStacks) {
                current++;
                ferveurStacks.put(uuid, current);
            }
            return current;
        }
        return ferveurStacks.getOrDefault(uuid, 0);
    }

    public void resetFerveur(@Nonnull UUID uuid) {
        ferveurStacks.remove(uuid);
        ferveurLastTick.remove(uuid);
    }

    // ---- Fureur Sanguinaire ----

    public void triggerFureur(@Nonnull UUID uuid, long durationMs, int rank) {
        fureurExpiry.put(uuid, System.currentTimeMillis() + durationMs);
        fureurRank.put(uuid, rank);
    }

    public int getFureurRank(@Nonnull UUID uuid) {
        Long exp = fureurExpiry.get(uuid);
        if (exp == null || System.currentTimeMillis() >= exp) {
            fureurExpiry.remove(uuid);
            fureurRank.remove(uuid);
            return 0;
        }
        return fureurRank.getOrDefault(uuid, 0);
    }

    // ---- Carnage ----

    public int onKillCarnage(@Nonnull UUID uuid, int maxStacks, long combatWindowMs) {
        long now = System.currentTimeMillis();
        Long last = carnageLastKill.get(uuid);
        int stacks;
        if (last == null || now - last > combatWindowMs) {
            stacks = 1;
        } else {
            stacks = Math.min(carnageStacks.getOrDefault(uuid, 0) + 1, maxStacks);
        }
        carnageStacks.put(uuid, stacks);
        carnageLastKill.put(uuid, now);
        return stacks;
    }

    public int getCarnageStacks(@Nonnull UUID uuid) {
        return carnageStacks.getOrDefault(uuid, 0);
    }

    public void resetCarnage(@Nonnull UUID uuid) {
        carnageStacks.remove(uuid);
        carnageLastKill.remove(uuid);
    }

    // ---- Frénésie ----

    public int onKillFrenesie(@Nonnull UUID uuid, int maxStacks, long durationMs, long combatWindowMs) {
        long now = System.currentTimeMillis();
        Long exp = frenesieExpiry.get(uuid);
        int stacks;
        if (exp == null || now >= exp) {
            stacks = 1;
        } else {
            stacks = Math.min(frenesieStacks.getOrDefault(uuid, 0) + 1, maxStacks);
        }
        frenesieStacks.put(uuid, stacks);
        frenesieExpiry.put(uuid, now + durationMs);
        return stacks;
    }

    public int getFrenesieStacks(@Nonnull UUID uuid) {
        Long exp = frenesieExpiry.get(uuid);
        if (exp == null || System.currentTimeMillis() >= exp) {
            frenesieStacks.remove(uuid);
            frenesieExpiry.remove(uuid);
            return 0;
        }
        return frenesieStacks.getOrDefault(uuid, 0);
    }

    // ---- Cleanup ----

    public void cleanup(@Nonnull UUID uuid) {
        assautExpiry.remove(uuid);
        assautRank.remove(uuid);
        eviscExpiry.remove(uuid);
        eviscRank.remove(uuid);
        criExpiry.remove(uuid);
        criBonus.remove(uuid);
        corExpiry.remove(uuid);
        dernierSouffleExpiry.remove(uuid);
        ferveurStacks.remove(uuid);
        ferveurLastTick.remove(uuid);
        fureurExpiry.remove(uuid);
        fureurRank.remove(uuid);
        carnageStacks.remove(uuid);
        carnageLastKill.remove(uuid);
        frenesieStacks.remove(uuid);
        frenesieExpiry.remove(uuid);
    }
}
