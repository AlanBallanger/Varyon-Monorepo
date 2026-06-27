package fr.varyon.vrpg.classes.rempart;

public final class GardeRapprocheSkill {

    public static final String SKILL_ID       = "garde_rapprochee";
    public static final String TALENT_NODE_ID = "rempart_11";

    private static final float[] DAMAGE_REDUCTION = {0.15f, 0.18f, 0.21f, 0.24f, 0.30f};
    private static final double  ALLY_RADIUS      = 6.0;
    private static final long[]  DURATION_MS      = {5000, 6000, 7000, 8000, 10000};
    private static final long[]  COOLDOWN_MS      = {35000, 32000, 30000, 27000, 25000};
    private static final float[] STAMINA_COST     = {8f, 9f, 10f, 11f, 12f};

    private GardeRapprocheSkill() {}

    public static int maxRank() { return COOLDOWN_MS.length; }
    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static float  damageReductionForRank(int rank) { return DAMAGE_REDUCTION[idx(rank)]; }
    public static double allyRadius()                     { return ALLY_RADIUS; }
    public static long   durationMsForRank(int rank)      { return DURATION_MS[idx(rank)]; }
    public static long   cooldownMsForRank(int rank)      { return COOLDOWN_MS[idx(rank)]; }
    public static float  staminaCostForRank(int rank)     { return STAMINA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int pct = Math.round(damageReductionForRank(rank) * 100);
        float dur = durationMsForRank(rank) / 1000f;
        int cd = (int)(cooldownMsForRank(rank) / 1000);
        return "-" + pct + "% dégâts (toi + alliés à " + (int)ALLY_RADIUS + " blocs), " + dur + "s, Délai " + cd + "s";
    }
}
