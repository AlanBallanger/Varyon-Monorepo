package fr.varyon.vrpg.classes.rempart;

public final class ChargeLourdeSkill {

    public static final String SKILL_ID       = "charge_lourde";
    public static final String TALENT_NODE_ID = "rempart_0";

    private static final double[] DASH_DISTANCE = {4, 5, 6, 7, 8, 9};
    private static final float[]  DAMAGE_PCT    = {1.2f, 1.5f, 1.8f, 2.1f, 2.4f, 3.0f};
    private static final long[]   COOLDOWN_MS   = {20000, 18000, 16000, 14000, 12000};
    private static final float[]  STAMINA_COST  = {6f, 7f, 8f, 9f, 10f, 10f};

    private ChargeLourdeSkill() {}

    public static int maxRank() { return COOLDOWN_MS.length; }
    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static double dashDistanceForRank(int rank) { return DASH_DISTANCE[idx(rank)]; }
    public static float  damagePctForRank(int rank)    { return DAMAGE_PCT[idx(rank)]; }
    public static long   cooldownMsForRank(int rank)   { return COOLDOWN_MS[idx(rank)]; }
    public static float  staminaCostForRank(int rank)  { return STAMINA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int dist = (int) dashDistanceForRank(rank);
        int pct = Math.round(damagePctForRank(rank) * 100);
        int cd = (int)(cooldownMsForRank(rank) / 1000);
        return "Charge " + dist + " blocs, " + pct + "% dégâts arme, Délai " + cd + "s";
    }
}
