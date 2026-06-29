package fr.varyon.vrpg.classes.rodeur;

public final class ReculStrategiqueSkill {

    public static final String SKILL_ID       = "recul_strategique";
    public static final String TALENT_NODE_ID = "rodeur_0";

    private static final double[] DASH_DISTANCE = {3, 4, 5, 6, 7, 8};
    private static final long[]   COOLDOWN_MS   = {20000, 18000, 16000, 14000, 12000};
    private static final float[]  STAMINA_COST  = {8f, 8f, 9f, 9f, 10f, 10f};
    private static final long[]   SPEED_MS      = {2000, 2500, 3000, 3500, 4000, 4000};
    private static final float[]  SPEED_BONUS   = {0.15f, 0.18f, 0.20f, 0.23f, 0.25f, 0.30f};

    private ReculStrategiqueSkill() {}

    public static int maxRank() { return COOLDOWN_MS.length; }

    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static double dashDistanceForRank(int rank)  { return DASH_DISTANCE[idx(rank)]; }
    public static long   cooldownMsForRank(int rank)    { return COOLDOWN_MS[idx(rank)]; }
    public static float  staminaCostForRank(int rank)   { return STAMINA_COST[idx(rank)]; }
    public static long   speedDurationMsForRank(int rank) { return SPEED_MS[idx(rank)]; }
    public static float  speedBonusForRank(int rank)    { return SPEED_BONUS[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int dist = (int) dashDistanceForRank(rank);
        int spd = Math.round(speedBonusForRank(rank) * 100);
        float sec = speedDurationMsForRank(rank) / 1000f;
        int cd = (int)(cooldownMsForRank(rank) / 1000);
        return dist + " blocs, +" + spd + "% vitesse " + sec + "s, Délai " + cd + "s";
    }
}
