package fr.varyon.vrpg.classes.rodeur;

public final class FlecheDeReculsSkill {

    public static final String SKILL_ID       = "fleche_de_recul";
    public static final String TALENT_NODE_ID = "rodeur_4";

    private static final float[] DAMAGE_PCT    = {0.80f, 0.90f, 1.00f, 1.10f, 1.20f};
    private static final double[] KNOCKBACK    = {5.0, 6.0, 7.0, 8.0, 10.0};
    private static final long[]  COOLDOWN_MS   = {15000, 14000, 13000, 12000, 10000};
    private static final float[] STAMINA_COST  = {4f, 4f, 5f, 5f, 6f};

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
