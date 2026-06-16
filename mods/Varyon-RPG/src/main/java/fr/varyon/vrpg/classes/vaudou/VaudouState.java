package fr.varyon.vrpg.classes.vaudou;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VaudouState {

    private final ConcurrentHashMap<UUID, Long>    curseExpiry    = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> curseRank      = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Long> curseOnNpc     = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Long> totemExpiry    = new ConcurrentHashMap<>();

    public record PendingFleau(float dpt, long durationMs, int rank) {}
    public enum TotemKind { SLOWNESS, VULNERABILITY }
    public record PendingTotem(TotemKind kind, long durationMs, float magnitude) {}

    private final ConcurrentHashMap<UUID, PendingFleau>  pendingFleau    = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Float>         pendingSkillDmg = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, PendingTotem>  pendingTotem    = new ConcurrentHashMap<>();

    public VaudouState() {}

    // --- Malédiction sur une cible NPC ---

    public void curseNpc(int npcIdx, long durationMs) {
        curseOnNpc.put(npcIdx, System.currentTimeMillis() + durationMs);
    }

    public boolean isNpcCursed(int npcIdx) {
        Long exp = curseOnNpc.get(npcIdx);
        if (exp == null) return false;
        if (System.currentTimeMillis() < exp) return true;
        curseOnNpc.remove(npcIdx);
        return false;
    }

    // --- Activation d'une malédiction par le joueur (pour buffs passifs) ---

    public void armCurse(@Nonnull UUID uuid, long durationMs, int rank) {
        curseExpiry.put(uuid, System.currentTimeMillis() + durationMs);
        curseRank.put(uuid, rank);
    }

    public int getCurseRank(@Nonnull UUID uuid) {
        Long exp = curseExpiry.get(uuid);
        if (exp == null || System.currentTimeMillis() >= exp) {
            curseExpiry.remove(uuid);
            curseRank.remove(uuid);
            return 0;
        }
        return curseRank.getOrDefault(uuid, 0);
    }

    // --- Totems actifs (indexés par entityIdx du totem) ---

    public void registerTotem(int totemIdx, long durationMs) {
        totemExpiry.put(totemIdx, System.currentTimeMillis() + durationMs);
    }

    public boolean isTotemActive(int totemIdx) {
        Long exp = totemExpiry.get(totemIdx);
        if (exp == null) return false;
        if (System.currentTimeMillis() < exp) return true;
        totemExpiry.remove(totemIdx);
        return false;
    }

    public void setPendingSkillDmg(@Nonnull UUID uuid, float dmg) {
        pendingSkillDmg.put(uuid, dmg);
    }

    public float consumePendingSkillDmg(@Nonnull UUID uuid) {
        Float v = pendingSkillDmg.remove(uuid);
        return v != null ? v : 0f;
    }

    public void setPendingFleau(@Nonnull UUID uuid, float dpt, long durationMs, int rank) {
        pendingFleau.put(uuid, new PendingFleau(dpt, durationMs, rank));
    }

    public boolean hasPendingFleau(@Nonnull UUID uuid) {
        return pendingFleau.containsKey(uuid);
    }

    public PendingFleau consumePendingFleau(@Nonnull UUID uuid) {
        return pendingFleau.remove(uuid);
    }

    public void setPendingTotem(@Nonnull UUID uuid, @Nonnull TotemKind kind, long durationMs, float magnitude) {
        pendingTotem.put(uuid, new PendingTotem(kind, durationMs, magnitude));
    }

    public PendingTotem consumePendingTotem(@Nonnull UUID uuid) {
        return pendingTotem.remove(uuid);
    }

    public void cleanup(@Nonnull UUID uuid) {
        curseExpiry.remove(uuid);
        curseRank.remove(uuid);
        pendingFleau.remove(uuid);
        pendingSkillDmg.remove(uuid);
    }
}
