package fr.varyon.vrpg.classes.arcaniste;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ArcanistState {

    // --- Surcharge (bonus dégâts temporaire) ---
    private final ConcurrentHashMap<UUID, Long>  surchargeExpiry = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Float> surchargeBonus  = new ConcurrentHashMap<>();

    // --- Pouvoir Grandissant (bonus si pas touché) ---
    private final ConcurrentHashMap<UUID, Boolean> pouvoirActive = new ConcurrentHashMap<>();

    // --- Écho Arcanique (dernier skill utilisé pour bypass délai) ---
    private final ConcurrentHashMap<UUID, String> echoPending = new ConcurrentHashMap<>();

    // --- Dernier sort lancé (Fire vs Ice pour le tag de dégâts projectile) ---
    private final ConcurrentHashMap<UUID, Boolean> lastCastFire = new ConcurrentHashMap<>();

    // --- Dégâts attendus des projectiles arcaniste (dmg par hit + nombre de hits restants) ---
    private final ConcurrentHashMap<UUID, Float>    pendingProjectileDmg  = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer>  pendingProjectileHits = new ConcurrentHashMap<>();

    public ArcanistState() {}

    // ---- Surcharge ----

    public void startSurcharge(@Nonnull UUID uuid, long durationMs, float bonus) {
        surchargeExpiry.put(uuid, System.currentTimeMillis() + durationMs);
        surchargeBonus.put(uuid, bonus);
    }

    public float getSurchargeBonus(@Nonnull UUID uuid) {
        Long exp = surchargeExpiry.get(uuid);
        if (exp == null || System.currentTimeMillis() >= exp) {
            surchargeExpiry.remove(uuid);
            surchargeBonus.remove(uuid);
            return 0f;
        }
        return surchargeBonus.getOrDefault(uuid, 0f);
    }

    // ---- Pouvoir Grandissant ----

    public void setPouvoirGrandissant(@Nonnull UUID uuid, boolean active) {
        if (active) pouvoirActive.put(uuid, true);
        else pouvoirActive.remove(uuid);
    }

    public boolean isPouvoirGrandissantActive(@Nonnull UUID uuid) {
        return Boolean.TRUE.equals(pouvoirActive.get(uuid));
    }

    // ---- Écho Arcanique ----

    public void armEchoArcanique(@Nonnull UUID uuid, @Nonnull String skillId) {
        echoPending.put(uuid, skillId);
    }

    public boolean consumeEchoArcanique(@Nonnull UUID uuid, @Nonnull String skillId) {
        String pending = echoPending.get(uuid);
        if (skillId.equals(pending)) {
            echoPending.remove(uuid);
            return true;
        }
        return false;
    }

    // ---- Dernier sort lancé ----

    public void setLastCastFire(@Nonnull UUID uuid, boolean fire) {
        lastCastFire.put(uuid, fire);
    }

    public boolean isLastCastFire(@Nonnull UUID uuid) {
        return Boolean.TRUE.equals(lastCastFire.get(uuid));
    }

    // ---- Dégâts projectile ----

    public void setPendingProjectileDmg(@Nonnull UUID uuid, float dmg, int hits) {
        pendingProjectileDmg.put(uuid, dmg);
        pendingProjectileHits.put(uuid, hits);
    }

    public boolean hasPendingProjectileDmg(@Nonnull UUID uuid) {
        return pendingProjectileDmg.containsKey(uuid);
    }

    public float consumePendingProjectileDmg(@Nonnull UUID uuid) {
        Float dmg = pendingProjectileDmg.get(uuid);
        if (dmg == null) return -1f;
        int remaining = pendingProjectileHits.merge(uuid, -1, Integer::sum);
        if (remaining <= 0) {
            pendingProjectileDmg.remove(uuid);
            pendingProjectileHits.remove(uuid);
        }
        return dmg;
    }

    // ---- Cleanup ----

    public void cleanup(@Nonnull UUID uuid) {
        surchargeExpiry.remove(uuid);
        surchargeBonus.remove(uuid);
        pouvoirActive.remove(uuid);
        echoPending.remove(uuid);
        lastCastFire.remove(uuid);
        pendingProjectileDmg.remove(uuid);
        pendingProjectileHits.remove(uuid);
    }
}
