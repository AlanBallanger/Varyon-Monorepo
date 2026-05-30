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

    public static int talentPointsAtLevel(int level) {
        if (level <= 1) return 0;
        int points = 0;
        int cap = Math.min(level, MAX_LEVEL);
        for (int L = 2; L <= cap; L++) {
            switch (L) {
                case 10 -> points += 2;
                case 20 -> points += 3;
                case MAX_LEVEL -> points += 4;
                default -> points += 1;
            }
        }
        return points;
    }
}
