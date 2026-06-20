package fr.varyon.vrpg.classes.lancier;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LancierState {

    private final ConcurrentHashMap<UUID, Long>    gardeExpiry         = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> gardeRank           = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long>    gardeDamageExpiry   = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> gardeDamageRank     = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<UUID, Long>    harponnagePullExpiry = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> harponnagePullTarget = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<UUID, Long>    empalementBleedExpiry = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Float>   empalementBleedDmg    = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long>    empalementRootExpiry  = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<UUID, Long>    postureExpiry       = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> postureRank         = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<UUID, Long>    ccDamageExpiry      = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> ccDamageRank        = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Long, Long>    rootedEntities      = new ConcurrentHashMap<>();

    public LancierState() {}

    // --- Garde du Lancier ---

    public void armGarde(@Nonnull UUID uuid, int rank, long windowMs) {
        gardeRank.put(uuid, rank);
        gardeExpiry.put(uuid, System.currentTimeMillis() + windowMs);
    }

    public int consumeGarde(@Nonnull UUID uuid) {
        Long exp = gardeExpiry.remove(uuid);
        Integer rank = gardeRank.remove(uuid);
        if (exp == null || System.currentTimeMillis() >= exp || rank == null) return 0;
        return rank;
    }

    public boolean isGardeArmed(@Nonnull UUID uuid) {
        Long exp = gardeExpiry.get(uuid);
        return exp != null && System.currentTimeMillis() < exp;
    }

    public void clearGarde(@Nonnull UUID uuid) {
        gardeExpiry.remove(uuid);
        gardeRank.remove(uuid);
    }

    public void armGardeDamage(@Nonnull UUID uuid, int rank, long windowMs) {
        gardeDamageRank.put(uuid, rank);
        gardeDamageExpiry.put(uuid, System.currentTimeMillis() + windowMs);
    }

    public int consumeGardeDamage(@Nonnull UUID uuid) {
        Long exp = gardeDamageExpiry.remove(uuid);
        Integer rank = gardeDamageRank.remove(uuid);
        if (exp == null || System.currentTimeMillis() >= exp || rank == null) return 0;
        return rank;
    }

    public boolean isGardeDamageArmed(@Nonnull UUID uuid) {
        Long exp = gardeDamageExpiry.get(uuid);
        return exp != null && System.currentTimeMillis() < exp;
    }

    // --- Harponnage — tirer la cible ---

    public void setPendingHarponnagePull(@Nonnull UUID uuid, int targetEntityIdx) {
        harponnagePullTarget.put(uuid, targetEntityIdx);
        harponnagePullExpiry.put(uuid, System.currentTimeMillis() + 2000L);
    }

    public int consumeHarponnagePull(@Nonnull UUID uuid) {
        Long exp = harponnagePullExpiry.remove(uuid);
        Integer idx = harponnagePullTarget.remove(uuid);
        if (exp == null || System.currentTimeMillis() >= exp || idx == null) return -1;
        return idx;
    }

    // --- Empalement — immobilisation ---

    public void rootEntity(long entityIdx, long durationMs) {
        rootedEntities.put(entityIdx, System.currentTimeMillis() + durationMs);
    }

    public boolean isRooted(long entityIdx) {
        Long exp = rootedEntities.get(entityIdx);
        if (exp == null) return false;
        if (System.currentTimeMillis() >= exp) {
            rootedEntities.remove(entityIdx);
            return false;
        }
        return true;
    }

    public void clearRoot(long entityIdx) {
        rootedEntities.remove(entityIdx);
    }

    public void armEmpalementBleed(@Nonnull UUID caster, long entityIdx, float dmgPerTick, long durationMs) {
        empalementBleedDmg.put(caster, dmgPerTick);
        empalementBleedExpiry.put(caster, System.currentTimeMillis() + durationMs);
        rootedEntities.put(entityIdx, System.currentTimeMillis() + durationMs);
        empalementRootExpiry.put(caster, System.currentTimeMillis() + durationMs);
    }

    public float getEmpalementBleedDmg(@Nonnull UUID caster) {
        return empalementBleedDmg.getOrDefault(caster, 0f);
    }

    public boolean isEmpalementBleedActive(@Nonnull UUID caster) {
        Long exp = empalementBleedExpiry.get(caster);
        return exp != null && System.currentTimeMillis() < exp;
    }

    // --- Posture dominante (passif) ---

    public void armPosture(@Nonnull UUID uuid, int rank, long durationMs) {
        postureRank.put(uuid, rank);
        postureExpiry.put(uuid, System.currentTimeMillis() + durationMs);
    }

    public boolean isPostureActive(@Nonnull UUID uuid) {
        Long exp = postureExpiry.get(uuid);
        return exp != null && System.currentTimeMillis() < exp;
    }

    public int getPostureRank(@Nonnull UUID uuid) {
        return postureRank.getOrDefault(uuid, 0);
    }

    // --- Contrôle de l'espace (passif) — bonus dégâts après CC ---

    public void armCcDamage(@Nonnull UUID uuid, int rank, long durationMs) {
        ccDamageRank.put(uuid, rank);
        ccDamageExpiry.put(uuid, System.currentTimeMillis() + durationMs);
    }

    public boolean isCcDamageActive(@Nonnull UUID uuid) {
        Long exp = ccDamageExpiry.get(uuid);
        return exp != null && System.currentTimeMillis() < exp;
    }

    public int getCcDamageRank(@Nonnull UUID uuid) {
        return ccDamageRank.getOrDefault(uuid, 0);
    }

    public void cleanup(@Nonnull UUID uuid) {
        gardeExpiry.remove(uuid);
        gardeRank.remove(uuid);
        gardeDamageExpiry.remove(uuid);
        gardeDamageRank.remove(uuid);
        harponnagePullExpiry.remove(uuid);
        harponnagePullTarget.remove(uuid);
        empalementBleedExpiry.remove(uuid);
        empalementBleedDmg.remove(uuid);
        empalementRootExpiry.remove(uuid);
        postureExpiry.remove(uuid);
        postureRank.remove(uuid);
        ccDamageExpiry.remove(uuid);
        ccDamageRank.remove(uuid);
    }
}
