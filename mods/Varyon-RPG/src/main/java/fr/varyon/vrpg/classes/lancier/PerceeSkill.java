package fr.varyon.vrpg.classes.lancier;

public final class PerceeSkill {

    public static final String SKILL_ID       = "percee";
    public static final String TALENT_NODE_ID = "lancier_0";

    private static final double[] DASH_DISTANCE = {3, 4, 5, 6, 7, 8};
    private static final long[]   COOLDOWN_MS   = {24000, 22000, 20000, 18000, 16000, 14000};
    private static final float[]  STAMINA_COST  = {5f, 5f, 6f, 6f, 7f, 7f};

    private PerceeSkill() {}

    public static int maxRank() { return COOLDOWN_MS.length; }

    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static double dashDistanceForRank(int rank)  { return DASH_DISTANCE[idx(rank)]; }
    public static long   cooldownMsForRank(int rank)    { return COOLDOWN_MS[idx(rank)]; }
    public static float  staminaCostForRank(int rank)   { return STAMINA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int dist = (int) dashDistanceForRank(rank);
        int cd   = (int)(cooldownMsForRank(rank) / 1000);
        return dist + " blocs vers l'arrière, Délai " + cd + "s";
    }
}
