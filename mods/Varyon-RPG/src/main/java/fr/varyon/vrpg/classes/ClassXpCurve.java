package fr.varyon.vrpg.classes;

public final class ClassXpCurve {

    public static final int MAX_LEVEL = 30;

    private ClassXpCurve() {}

    public static long xpForLevel(int level) {
        if (level >= MAX_LEVEL) return 0L;
        if (level < 1) level = 1;
        long l = level;
        return 35L * l * (100L + l * l) / 100L;
    }

    private static final int[] TALENT_POINTS_PER_LEVEL = {
        1, 1, 1, 2, 1, 1, 1, 1, 2, 1, 1, 1, 1, 2, 1, 1, 1, 1, 2, 2, 2, 2, 2, 3, 2, 2, 2, 2, 3
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
