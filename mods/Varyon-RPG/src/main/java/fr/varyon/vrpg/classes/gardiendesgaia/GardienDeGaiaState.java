package fr.varyon.vrpg.classes.gardiendesgaia;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GardienDeGaiaState {

    // --- Marque de Renaissance (protection contre la mort) ---
    private final ConcurrentHashMap<UUID, Long> marqueExpiry = new ConcurrentHashMap<>();

    // --- Écorce Protectrice (réduction de dégâts + soin tick) ---
    private final ConcurrentHashMap<UUID, Long>  ecorceExpiry     = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Float> ecorceReduction  = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Float> ecorceHealPerSec = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, UUID>  ecorceTarget     = new ConcurrentHashMap<>(); // lanceur → UUID cible
    private final ConcurrentHashMap<UUID, com.hypixel.hytale.component.Ref<com.hypixel.hytale.server.core.universe.world.storage.EntityStore>> ecorceTargetRef = new ConcurrentHashMap<>();

    // --- Bénédiction de Gaïa (heal AoE en attente d'application) ---
    private final ConcurrentHashMap<UUID, Float> pendingHeal = new ConcurrentHashMap<>();

    // --- Étreinte de Gaïa (rank en attente d'impact projectile) ---
    private final ConcurrentHashMap<UUID, Integer> pendingEtreinteRank = new ConcurrentHashMap<>();

    // --- Appel du Tréant (UUID de l'invocation active + facteur de dégâts) ---
    private final ConcurrentHashMap<UUID, UUID>  treantSummon    = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Float> treantDmgFactor = new ConcurrentHashMap<>(); // playerUuid → multiplicateur
    private final ConcurrentHashMap<UUID, com.hypixel.hytale.component.Ref<com.hypixel.hytale.server.core.universe.world.storage.EntityStore>> treantRef = new ConcurrentHashMap<>();

    // --- Cycle de Vie (flag : dernier soin a restauré du mana) ---
    private final ConcurrentHashMap<UUID, Long> lastHealForCycle = new ConcurrentHashMap<>();

    public GardienDeGaiaState() {}

    // ---- Marque de Renaissance ----

    public void armMarque(@Nonnull UUID uuid, long durationMs) {
        marqueExpiry.put(uuid, System.currentTimeMillis() + durationMs);
    }

    public boolean consumeMarque(@Nonnull UUID uuid) {
        Long exp = marqueExpiry.get(uuid);
        if (exp == null) return false;
        marqueExpiry.remove(uuid);
        return System.currentTimeMillis() < exp;
    }

    public boolean hasMarque(@Nonnull UUID uuid) {
        Long exp = marqueExpiry.get(uuid);
        if (exp == null) return false;
        if (System.currentTimeMillis() >= exp) { marqueExpiry.remove(uuid); return false; }
        return true;
    }

    // ---- Écorce Protectrice ----

    public void startEcorce(@Nonnull UUID casterUuid, @Nonnull UUID targetUuid,
                             @javax.annotation.Nullable com.hypixel.hytale.component.Ref<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> targetEntityRef,
                             long durationMs, float reduction, float healPerSec) {
        long expiry = System.currentTimeMillis() + durationMs;
        ecorceExpiry.put(casterUuid, expiry);
        ecorceReduction.put(casterUuid, reduction);
        ecorceHealPerSec.put(casterUuid, healPerSec);
        ecorceTarget.put(casterUuid, targetUuid);
        if (targetEntityRef != null) ecorceTargetRef.put(casterUuid, targetEntityRef);
        else ecorceTargetRef.remove(casterUuid);
        // Si la cible est un joueur différent du lanceur, stocker aussi sous le UUID cible pour la réduction de dégâts
        if (!casterUuid.equals(targetUuid)) {
            ecorceExpiry.put(targetUuid, expiry);
            ecorceReduction.put(targetUuid, reduction);
            ecorceHealPerSec.put(targetUuid, 0f);
        }
    }

    @Nullable
    public com.hypixel.hytale.component.Ref<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> getEcorceTargetRef(@Nonnull UUID casterUuid) {
        return ecorceTargetRef.get(casterUuid);
    }

    public boolean isEcorceActive(@Nonnull UUID uuid) {
        Long exp = ecorceExpiry.get(uuid);
        if (exp == null) return false;
        if (System.currentTimeMillis() >= exp) {
            ecorceExpiry.remove(uuid);
            ecorceReduction.remove(uuid);
            ecorceHealPerSec.remove(uuid);
            return false;
        }
        return true;
    }

    public float getEcorceReduction(@Nonnull UUID uuid) {
        return isEcorceActive(uuid) ? ecorceReduction.getOrDefault(uuid, 0f) : 0f;
    }

    public float getEcorceHealPerSec(@Nonnull UUID uuid) {
        return isEcorceActive(uuid) ? ecorceHealPerSec.getOrDefault(uuid, 0f) : 0f;
    }

    // ---- Heal en attente (Bénédiction) ----

    public void setPendingHeal(@Nonnull UUID uuid, float amount) {
        pendingHeal.put(uuid, amount);
    }

    public float consumePendingHeal(@Nonnull UUID uuid) {
        Float h = pendingHeal.remove(uuid);
        return h != null ? h : 0f;
    }

    // ---- Tréant ----

    public void setTreant(@Nonnull UUID playerUuid,
                          @Nonnull com.hypixel.hytale.component.Ref<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> entityRef,
                          float dmgFactor) {
        UUID placeholder = new UUID(entityRef.getIndex(), 0L);
        treantSummon.put(playerUuid, placeholder);
        treantDmgFactor.put(playerUuid, dmgFactor);
        treantRef.put(playerUuid, entityRef);
    }

    public float getTreantDmgFactorForRef(
            @Nonnull com.hypixel.hytale.component.Ref<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> attackerRef) {
        for (var entry : treantRef.entrySet()) {
            com.hypixel.hytale.component.Ref<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> stored = entry.getValue();
            com.hypixel.hytale.logger.HytaleLogger.getLogger().getSubLogger("TreantState")
                .atInfo().log(String.format("[TreantRefCheck] stored idx=%d attacker idx=%d match=%b",
                    stored.getIndex(), attackerRef.getIndex(), stored.getIndex() == attackerRef.getIndex()));
            if (stored == attackerRef || stored.getIndex() == attackerRef.getIndex()) {
                return treantDmgFactor.getOrDefault(entry.getKey(), 1f);
            }
        }
        return 1f;
    }

    public UUID getTreant(@Nonnull UUID playerUuid) {
        return treantSummon.get(playerUuid);
    }

    public void removeTreant(@Nonnull UUID playerUuid) {
        treantSummon.remove(playerUuid);
    }

    public boolean hasTreant(@Nonnull UUID playerUuid) {
        return treantSummon.containsKey(playerUuid);
    }

    // ---- Cycle de Vie ----

    public void notifyHeal(@Nonnull UUID uuid) {
        lastHealForCycle.put(uuid, System.currentTimeMillis());
    }

    public boolean consumeCycleHeal(@Nonnull UUID uuid) {
        return lastHealForCycle.remove(uuid) != null;
    }

    // ---- Cleanup ----

    // ---- Étreinte de Gaïa ----

    public void armEtreinte(@Nonnull UUID uuid, int rank) {
        pendingEtreinteRank.put(uuid, rank);
    }

    public int consumeEtreinte(@Nonnull UUID uuid) {
        Integer rank = pendingEtreinteRank.remove(uuid);
        return rank != null ? rank : -1;
    }

    @Nullable
    public UUID getEcorceTarget(@Nonnull UUID casterUuid) {
        return ecorceTarget.get(casterUuid);
    }

    public void cleanup(@Nonnull UUID uuid) {
        marqueExpiry.remove(uuid);
        ecorceExpiry.remove(uuid);
        ecorceReduction.remove(uuid);
        ecorceHealPerSec.remove(uuid);
        ecorceTarget.remove(uuid);
        pendingEtreinteRank.remove(uuid);
        ecorceTargetRef.remove(uuid);
        treantSummon.remove(uuid);
        treantDmgFactor.remove(uuid);
        treantRef.remove(uuid);
        pendingHeal.remove(uuid);
        treantSummon.remove(uuid);
        lastHealForCycle.remove(uuid);
    }
}
