package fr.varyon.vrpg.classes.ombre;

public final class PasDesTenebresSkill {

    public static final String SKILL_ID       = "pas_des_tenebres";
    public static final String TALENT_NODE_ID = "ombre_0";

    private static final double[] DASH_DISTANCE  = {3, 4, 5, 6, 7, 8};
    private static final long[]   COOLDOWN_MS    = {30000, 28000, 26000, 24000, 22000, 20000};
    private static final float[]  STAMINA_COST   = {6f, 7f, 8f, 9f, 10f, 10f};
    private static final long[]   STEALTH_MS     = {1000, 1500, 2000, 2500, 3000, 3000};

    private PasDesTenebresSkill() {}

    public static int maxRank() { return COOLDOWN_MS.length; }

    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static double dashDistanceForRank(int rank)  { return DASH_DISTANCE[idx(rank)]; }
    public static long   cooldownMsForRank(int rank)    { return COOLDOWN_MS[idx(rank)]; }
    public static float  staminaCostForRank(int rank)   { return STAMINA_COST[idx(rank)]; }
    public static long   stealthDurationMs(int rank)    { return STEALTH_MS[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int dist = (int) dashDistanceForRank(rank);
        float sec = stealthDurationMs(rank) / 1000f;
        int cd = (int)(cooldownMsForRank(rank) / 1000);
        return dist + " blocs, invisibilité " + sec + "s, Délai " + cd + "s";
    }
}
