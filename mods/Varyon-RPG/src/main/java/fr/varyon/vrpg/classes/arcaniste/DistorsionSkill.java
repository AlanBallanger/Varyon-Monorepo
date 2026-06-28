package fr.varyon.vrpg.classes.arcaniste;

public final class DistorsionSkill {

    public static final String SKILL_ID       = "distorsion";
    public static final String TALENT_NODE_ID = "arcaniste_0";

    private static final double[] DASH_DISTANCE = {4, 5, 6, 7, 8};
    private static final long[]   COOLDOWN_MS   = {20000, 18000, 16000, 14000, 12000};
    private static final float[]  STAMINA_COST  = {8f, 9f, 10f, 11f, 12f};

    private DistorsionSkill() {}

    public static int    maxRank()                     { return COOLDOWN_MS.length; }
    private static int   idx(int rank)                 { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static double dashDistanceForRank(int rank) { return DASH_DISTANCE[idx(rank)]; }
    public static long   cooldownMsForRank(int rank)   { return COOLDOWN_MS[idx(rank)]; }
    public static float  staminaCostForRank(int rank)  { return STAMINA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int dist = (int) dashDistanceForRank(rank);
        int cd   = (int) (cooldownMsForRank(rank) / 1000);
        int stamina = Math.round(staminaCostForRank(rank));
        return dist + " blocs en arrière, " + stamina + " endurance, Délai " + cd + "s";
    }
}
