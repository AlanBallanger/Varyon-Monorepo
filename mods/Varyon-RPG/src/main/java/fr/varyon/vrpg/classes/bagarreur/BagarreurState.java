package fr.varyon.vrpg.classes.bagarreur;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BagarreurState {

    // --- Montée d'adrénaline (dmg boost temporaire) ---
    private final ConcurrentHashMap<UUID, Long>  monteeExpiry = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Float> monteeBonus  = new ConcurrentHashMap<>();

    // --- Adrénaline (speed boost après dégâts reçus) ---
    private final ConcurrentHashMap<UUID, Long>  adrenalineExpiry = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Float> adrenalineSpeed  = new ConcurrentHashMap<>();

    // --- Acharnement (stacks dmg par cible) ---
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<Long, Integer>> acharnementStacks   = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<Long, Long>>    acharnementLastHit  = new ConcurrentHashMap<>();

    public BagarreurState() {}

    // ---- Montée d'adrénaline ----

    public void startMonteeAdrenaline(@Nonnull UUID uuid, long durationMs, float bonus) {
        monteeExpiry.put(uuid, System.currentTimeMillis() + durationMs);
        monteeBonus.put(uuid, bonus);
    }

    public float getMonteeBonus(@Nonnull UUID uuid) {
        Long exp = monteeExpiry.get(uuid);
        if (exp == null || System.currentTimeMillis() >= exp) {
            monteeExpiry.remove(uuid);
            monteeBonus.remove(uuid);
            return 0f;
        }
        return monteeBonus.getOrDefault(uuid, 0f);
    }

    // ---- Adrénaline ----

    public void triggerAdrenaline(@Nonnull UUID uuid, long durationMs, float speed) {
        adrenalineExpiry.put(uuid, System.currentTimeMillis() + durationMs);
        adrenalineSpeed.put(uuid, speed);
    }

    public float getAdrenalineSpeed(@Nonnull UUID uuid) {
        Long exp = adrenalineExpiry.get(uuid);
        if (exp == null || System.currentTimeMillis() >= exp) {
            adrenalineExpiry.remove(uuid);
            adrenalineSpeed.remove(uuid);
            return 0f;
        }
        return adrenalineSpeed.getOrDefault(uuid, 0f);
    }

    // ---- Acharnement (stacks par cible) ----

    public int onHitAcharnement(@Nonnull UUID attacker, long targetIdx, int maxStacks, long windowMs) {
        long now = System.currentTimeMillis();
        ConcurrentHashMap<Long, Integer> stacks = acharnementStacks.computeIfAbsent(attacker, k -> new ConcurrentHashMap<>());
        ConcurrentHashMap<Long, Long>    times  = acharnementLastHit.computeIfAbsent(attacker, k -> new ConcurrentHashMap<>());

        Long last = times.get(targetIdx);
        int current;
        if (last == null || now - last > windowMs) {
            current = 1;
        } else {
            current = Math.min(stacks.getOrDefault(targetIdx, 0) + 1, maxStacks);
        }
        stacks.put(targetIdx, current);
        times.put(targetIdx, now);
        return current;
    }

    public int getAcharnementStacks(@Nonnull UUID attacker, long targetIdx, long windowMs) {
        ConcurrentHashMap<Long, Long> times = acharnementLastHit.get(attacker);
        if (times == null) return 0;
        Long last = times.get(targetIdx);
        if (last == null || System.currentTimeMillis() - last > windowMs) return 0;
        ConcurrentHashMap<Long, Integer> stacks = acharnementStacks.get(attacker);
        return stacks != null ? stacks.getOrDefault(targetIdx, 0) : 0;
    }

    // ---- Uppercut knockback pending ----

    private final ConcurrentHashMap<UUID, double[]> uppercutKb = new ConcurrentHashMap<>();

    public void setPendingUppercutKb(@Nonnull UUID uuid, double vx, double vy, double vz) {
        uppercutKb.put(uuid, new double[]{vx, vy, vz});
    }

    public double[] consumePendingUppercutKb(@Nonnull UUID uuid) {
        return uppercutKb.remove(uuid);
    }

    // ---- Cleanup ----

    public void cleanup(@Nonnull UUID uuid) {
        monteeExpiry.remove(uuid);
        monteeBonus.remove(uuid);
        adrenalineExpiry.remove(uuid);
        adrenalineSpeed.remove(uuid);
        acharnementStacks.remove(uuid);
        acharnementLastHit.remove(uuid);
    }
}
