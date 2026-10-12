package fr.varyon.vrpg.classes;

public final class ClassXpCurve {

    public static final int MAX_LEVEL = 30;

    // xpForLevel(L) = OLD(L) * (1 + (L / rampMid)^rampExp)
    // OLD(L) = 50 * L * (100 + 1.2*L^2) / 100 — the pre-scaling curve, kept for the first few levels.
    // The ramp term stays near 1 until ~level rampMid, then dominates, so a mid/late level costs
    // orders of magnitude more than before. Tuned so L17->18 ≈ 37k and the L1..L30 total ≈ 4.06M.
    private static double rampMid = 9.0;
    private static double rampExp = 3.4;

    private ClassXpCurve() {}

    public static void configure(double rampMid, double rampExp) {
        ClassXpCurve.rampMid = rampMid;
        ClassXpCurve.rampExp = rampExp;
    }

    public static long xpForLevel(int level) {
        if (level >= MAX_LEVEL) return 0L;
        if (level < 1) level = 1;
        long l = level;
        long oldXp = 50L * l * (100L + (long) (l * l * 1.2)) / 100L;
        double mult = 1.0 + Math.pow(l / rampMid, rampExp);
        return Math.round(oldXp * mult);
    }

    private static final int[] TALENT_POINTS_PER_LEVEL = {
        1, 1, 1, 2, 1, 1, 1, 1, 2, 1, 1, 1, 1, 2, 1, 1, 1, 1, 2, 3, 2, 2, 2, 3, 2, 2, 2, 2, 3
    };

    public static int talentPointsAtLevel(int level) {
        if (level <= 1) return 0;
        int cap = Math.min(level, MAX_LEVEL);
        int points = 0;
        for (int L = 2; L <= cap; L++) {
            points += TALENT_POINTS_PER_LEVEL[L - 2];
        }
        return points;
    }
}
