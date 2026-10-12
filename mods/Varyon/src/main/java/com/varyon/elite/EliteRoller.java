package com.varyon.elite;

import com.varyon.elite.EliteConfig.ZoneEliteDef;
import com.varyon.elite.ability.EliteAbility;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

/**
 * Deterministic elite rolls. A given (role, zoneId, spawn position) always resolves to the
 * same elite, which lets named mini-bosses emerge from the seed without hardcoding spawns.
 *
 * <p>Uses SplitMix64 mixing per stream instead of {@code new Random(seed).nextDouble()}:
 * the latter is strongly correlated for nearby seeds (adjacent spawn positions), which made
 * the elite chance effectively 0 or 1 across a whole area.
 */
public final class EliteRoller {

    private EliteRoller() {}

    public static long seedFor(@Nonnull String canonicalRole, int zoneId, double x, double y, double z) {
        long h = 1125899906842597L;
        h = 31 * h + canonicalRole.hashCode();
        h = 31 * h + zoneId;
        h = 31 * h + Double.hashCode(Math.floor(x));
        h = 31 * h + Double.hashCode(Math.floor(y));
        h = 31 * h + Double.hashCode(Math.floor(z));
        return h;
    }

    /** SplitMix64 finalizer - decorrelates nearby seeds. */
    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** Decorrelated 0..1 draw for a named sub-stream of {@code seed}. */
    static double roll01(long seed, long stream) {
        return new SplittableRandom(mix(seed ^ stream)).nextDouble();
    }

    public static boolean rollIsElite(long seed, @Nonnull ZoneEliteDef zoneDef) {
        return roll01(seed, 0x5DEECE66DL) < zoneDef.getEliteChance();
    }

    public static int rollTier(long seed, @Nonnull ZoneEliteDef zoneDef) {
        double[] weights = zoneDef.getTierWeights();
        double total = zoneDef.totalTierWeight();
        if (total <= 0) return 1;
        double pick = roll01(seed, 0xA5A5A5A5L) * total;
        double acc = 0;
        for (int i = 0; i < weights.length; i++) {
            acc += weights[i];
            if (pick < acc) return i + 1;
        }
        return weights.length;
    }

    @Nonnull
    public static MobClass rollClass(long seed, int tier, @Nonnull Map<MobClass, Double> classWeights) {
        List<MobClass> candidates = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        double total = 0;
        for (Map.Entry<MobClass, Double> e : classWeights.entrySet()) {
            MobClass mc = e.getKey();
            double w = e.getValue() != null ? e.getValue() : 0;
            if (w <= 0 || !mc.allowsTier(tier)) continue;
            candidates.add(mc);
            weights.add(w);
            total += w;
        }
        if (candidates.isEmpty()) return MobClass.BAGARREUR;
        double pick = roll01(seed, 0xC3C3C3C3L) * total;
        double acc = 0;
        for (int i = 0; i < candidates.size(); i++) {
            acc += weights.get(i);
            if (pick < acc) return candidates.get(i);
        }
        return candidates.get(candidates.size() - 1);
    }

    @Nonnull
    public static List<EliteAbility> rollAbilities(long seed, @Nonnull MobClass mobClass, int tier) {
        List<EliteAbility> pool = new ArrayList<>(mobClass.getPool());
        int count = mobClass.abilityCountForTier(tier);
        SplittableRandom rng = new SplittableRandom(mix(seed ^ 0x1B1B1B1BL));
        List<EliteAbility> chosen = new ArrayList<>(count);
        for (int i = 0; i < count && !pool.isEmpty(); i++) {
            chosen.add(pool.remove(rng.nextInt(pool.size())));
        }
        return chosen;
    }
}
