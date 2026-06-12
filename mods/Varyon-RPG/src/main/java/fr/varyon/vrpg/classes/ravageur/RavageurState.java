package fr.varyon.vrpg.classes.ravageur;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RavageurState {

    // --- Peau de Fer (réduction dégâts temporaire) ---
    private final ConcurrentHashMap<UUID, Long>    peauDeFerExpiry     = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Float>   peauDeFerReduction  = new ConcurrentHashMap<>();

    // --- Déchaînement (bonus dégâts temporaire) ---
    private final ConcurrentHashMap<UUID, Long>    dechainementExpiry  = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Float>   dechainementBonus   = new ConcurrentHashMap<>();

    // --- Élan destructeur (prochain coup armé après kill) ---
    private final ConcurrentHashMap<UUID, Long>    elanExpiry          = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> elanRank            = new ConcurrentHashMap<>();

    // --- Arme lourde (ralentissement sur critique, géré côté outgoing) ---
    private final ConcurrentHashMap<UUID, Long>    armeLourdeSlowExpiry = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Float>   armeLourdeSlowValue  = new ConcurrentHashMap<>();

    public RavageurState() {}

    // ---- Peau de Fer ----

    public void startPeauDeFer(@Nonnull UUID uuid, long durationMs, float reduction) {
        peauDeFerExpiry.put(uuid, System.currentTimeMillis() + durationMs);
        peauDeFerReduction.put(uuid, reduction);
    }

    public float getPeauDeFerReduction(@Nonnull UUID uuid) {
        Long exp = peauDeFerExpiry.get(uuid);
        if (exp == null || System.currentTimeMillis() >= exp) {
            peauDeFerExpiry.remove(uuid);
            peauDeFerReduction.remove(uuid);
            return 0f;
        }
        return peauDeFerReduction.getOrDefault(uuid, 0f);
    }

    // ---- Déchaînement ----

    public void startDechainement(@Nonnull UUID uuid, long durationMs, float bonus) {
        dechainementExpiry.put(uuid, System.currentTimeMillis() + durationMs);
        dechainementBonus.put(uuid, bonus);
    }

    public float getDechainementBonus(@Nonnull UUID uuid) {
        Long exp = dechainementExpiry.get(uuid);
        if (exp == null || System.currentTimeMillis() >= exp) {
            dechainementExpiry.remove(uuid);
            dechainementBonus.remove(uuid);
            return 0f;
        }
        return dechainementBonus.getOrDefault(uuid, 0f);
    }

    // ---- Élan destructeur ----

    public void armElan(@Nonnull UUID uuid, int rank) {
        elanExpiry.put(uuid, System.currentTimeMillis() + RavageurPassifs.ELAN_WINDOW_MS);
        elanRank.put(uuid, rank);
    }

    public int consumeElan(@Nonnull UUID uuid) {
        Long exp = elanExpiry.remove(uuid);
        Integer r = elanRank.remove(uuid);
        if (exp == null || System.currentTimeMillis() >= exp) return 0;
        return r != null ? r : 0;
    }

    // ---- Moissonneur (stacks XP en série de kills) ----

    private final ConcurrentHashMap<UUID, Integer> moissonneurStacks   = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long>    moissonneurLastKill = new ConcurrentHashMap<>();

    public int onKillMoissonneur(@Nonnull UUID uuid, int maxStacks, long combatWindowMs) {
        long now = System.currentTimeMillis();
        Long last = moissonneurLastKill.get(uuid);
        int stacks;
        if (last == null || now - last > combatWindowMs) {
            stacks = 1;
        } else {
            stacks = Math.min(moissonneurStacks.getOrDefault(uuid, 0) + 1, maxStacks);
        }
        moissonneurStacks.put(uuid, stacks);
        moissonneurLastKill.put(uuid, now);
        return stacks;
    }

    public int getMoissonneurStacks(@Nonnull UUID uuid) {
        return moissonneurStacks.getOrDefault(uuid, 0);
    }

    // ---- Arme lourde (slow sur cible après critique) ----

    public void applyArmeLourde(@Nonnull UUID targetUuid, long durationMs, float slow) {
        armeLourdeSlowExpiry.put(targetUuid, System.currentTimeMillis() + durationMs);
        armeLourdeSlowValue.put(targetUuid, slow);
    }

    // ---- Cleanup ----

    public void cleanup(@Nonnull UUID uuid) {
        peauDeFerExpiry.remove(uuid);
        peauDeFerReduction.remove(uuid);
        dechainementExpiry.remove(uuid);
        dechainementBonus.remove(uuid);
        elanExpiry.remove(uuid);
        elanRank.remove(uuid);
        armeLourdeSlowExpiry.remove(uuid);
        armeLourdeSlowValue.remove(uuid);
        moissonneurStacks.remove(uuid);
        moissonneurLastKill.remove(uuid);
    }
}
