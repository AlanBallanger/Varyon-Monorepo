package fr.varyon.vrpg.classes.ravageur;

public final class BondEcrasantSkill {

    public static final String SKILL_ID       = "bond_ecrasant";
    public static final String TALENT_NODE_ID = "ravageur_0";

    private static final double[] DASH_DISTANCE = {5.0, 6.0, 7.0, 8.0, 10.0};
    private static final float[]  DAMAGE_PCT    = {1.0f, 1.2f, 1.5f, 1.8f, 2.2f};
    private static final long[]   COOLDOWN_MS   = {20000, 18000, 16000, 14000, 12000};
    private static final float[]  STAMINA_COST  = {7f, 8f, 9f, 10f, 10f};

    private BondEcrasantSkill() {}

    public static int    maxRank()                   { return COOLDOWN_MS.length; }
    private static int   idx(int rank)               { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static double dashDistanceForRank(int rank) { return DASH_DISTANCE[idx(rank)]; }
    public static float  damagePctForRank(int rank)    { return DAMAGE_PCT[idx(rank)]; }
    public static long   cooldownMsForRank(int rank)   { return COOLDOWN_MS[idx(rank)]; }
    public static float  staminaCostForRank(int rank)  { return STAMINA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int dist = (int) dashDistanceForRank(rank);
        int pct  = Math.round(damagePctForRank(rank) * 100);
        int cd   = (int) (cooldownMsForRank(rank) / 1000);
        return "Bond " + dist + " blocs, " + pct + "% dégâts arme à l'atterrissage, Délai " + cd + "s";
    }
}
