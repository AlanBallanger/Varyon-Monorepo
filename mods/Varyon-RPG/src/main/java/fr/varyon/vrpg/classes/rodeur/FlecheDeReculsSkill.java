package fr.varyon.vrpg.classes.rodeur;

public final class FlecheDeReculsSkill {

    public static final String SKILL_ID       = "fleche_de_recul";
    public static final String TALENT_NODE_ID = "rodeur_7";

    private static final float[] DAMAGE_PCT    = {2.00f, 2.25f, 2.50f, 2.75f, 3.00f};
    private static final double[] KNOCKBACK    = {5.0, 6.0, 7.0, 8.0, 10.0};
    private static final long[]  COOLDOWN_MS   = {22000, 20000, 18000, 17000, 15000};
    private static final float[] STAMINA_COST  = {7f, 7f, 8f, 8f, 9f};

    private FlecheDeReculsSkill() {}

    public static int maxRank() { return COOLDOWN_MS.length; }

    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static float  damagePctForRank(int rank)    { return DAMAGE_PCT[idx(rank)]; }
    public static double knockbackForRank(int rank)    { return KNOCKBACK[idx(rank)]; }
    public static long   cooldownMsForRank(int rank)   { return COOLDOWN_MS[idx(rank)]; }
    public static float  staminaCostForRank(int rank)  { return STAMINA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int dmg = Math.round(damagePctForRank(rank) * 100);
        int kb = (int) knockbackForRank(rank);
        int cd = (int)(cooldownMsForRank(rank) / 1000);
        return dmg + "% dégâts, recul " + kb + "u, Délai " + cd + "s";
    }
}
