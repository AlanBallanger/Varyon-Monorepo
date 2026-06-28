package fr.varyon.vrpg.classes.gardiendesgaia;

public final class EvasionSylvestreSkill {

    public static final String SKILL_ID       = "evasion_sylvestre";
    public static final String TALENT_NODE_ID = "gardien_de_gaia_0";

    private static final double[] DASH_DISTANCE = {4, 5, 6, 7, 8};
    private static final long[]   COOLDOWN_MS   = {20000, 18000, 16000, 14000, 12000};
    private static final float[]  MANA_COST     = {6f, 7f, 8f, 9f, 10f};

    private EvasionSylvestreSkill() {}

    public static int    maxRank()                     { return COOLDOWN_MS.length; }
    private static int   idx(int rank)                 { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static double dashDistanceForRank(int rank) { return DASH_DISTANCE[idx(rank)]; }
    public static long   cooldownMsForRank(int rank)   { return COOLDOWN_MS[idx(rank)]; }
    public static float  manaCostForRank(int rank)     { return MANA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int dist = (int) dashDistanceForRank(rank);
        int cd   = (int) (cooldownMsForRank(rank) / 1000);
        int mana = Math.round(manaCostForRank(rank));
        return dist + " blocs en arrière, " + mana + " mana, Délai " + cd + "s";
    }
}
