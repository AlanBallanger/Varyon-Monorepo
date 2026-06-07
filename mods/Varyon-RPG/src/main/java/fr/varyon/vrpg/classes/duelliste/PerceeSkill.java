package fr.varyon.vrpg.classes.duelliste;

public final class PerceeSkill {

    public static final String SKILL_ID       = "percee";
    public static final String TALENT_NODE_ID = "6";

    private static final float[] BASE_MULT    = {0.90f, 1.00f, 1.10f, 1.20f, 1.30f};
    private static final long[]  COOLDOWN_MS  = {20000, 19000, 18000, 17000, 15000};
    private static final float   FULL_HP_BONUS = 0.30f;
    private static final float   FULL_HP_THRESHOLD = 0.80f;

    private PerceeSkill() {}

    public static int maxRank() { return BASE_MULT.length; }

    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, BASE_MULT.length - 1)); }

    public static float  baseMultForRank(int rank)  { return BASE_MULT[idx(rank)]; }
    public static long   cooldownMsForRank(int rank){ return COOLDOWN_MS[idx(rank)]; }
    public static float  fullHpBonus()              { return FULL_HP_BONUS; }
    public static float  fullHpThreshold()          { return FULL_HP_THRESHOLD; }

    public static String statLineForRank(int rank) {
        int base = Math.round(baseMultForRank(rank) * 100);
        int bonus = Math.round(FULL_HP_BONUS * 100);
        int cd   = (int) (cooldownMsForRank(rank) / 1000);
        return base + "% dégâts, +" + bonus + "% si cible > 80% PV, CD " + cd + "s";
    }
}
