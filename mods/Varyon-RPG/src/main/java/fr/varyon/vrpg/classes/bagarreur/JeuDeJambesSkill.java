package fr.varyon.vrpg.classes.bagarreur;

public final class JeuDeJambesSkill {

    public static final String SKILL_ID       = "jeu_de_jambes";
    public static final String TALENT_NODE_ID = "bagarreur_0";

    private static final double[] DASH_DISTANCE = {3.0, 3.5, 4.0, 4.5, 5.0};
    private static final long[]   COOLDOWN_MS   = {12000, 10000, 9000, 8000, 6000};
    private static final float[]  STAMINA_COST  = {4f, 5f, 5f, 6f, 6f};

    private JeuDeJambesSkill() {}

    public static int    maxRank()                    { return COOLDOWN_MS.length; }
    private static int   idx(int rank)                { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static double dashDistanceForRank(int rank) { return DASH_DISTANCE[idx(rank)]; }
    public static long   cooldownMsForRank(int rank)   { return COOLDOWN_MS[idx(rank)]; }
    public static float  staminaCostForRank(int rank)  { return STAMINA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int dist = (int) dashDistanceForRank(rank);
        int cd   = (int) (cooldownMsForRank(rank) / 1000);
        return "Dash latéral droit " + dist + " blocs, CD " + cd + "s";
    }
}
