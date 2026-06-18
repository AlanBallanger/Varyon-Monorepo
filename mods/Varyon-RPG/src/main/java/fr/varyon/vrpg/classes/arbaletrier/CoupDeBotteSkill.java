package fr.varyon.vrpg.classes.arbaletrier;

public final class CoupDeBotteSkill {

    public static final String SKILL_ID       = "coup_de_botte";
    public static final String TALENT_NODE_ID = "arbaletrier_5";

    private static final float[]  DAMAGE_PCT    = {0.60f, 0.70f, 0.80f, 0.90f, 1.00f};
    private static final double[] KNOCKBACK     = {6.0, 7.0, 8.0, 9.0, 10.0};
    private static final long[]   COOLDOWN_MS   = {12000, 11000, 10000, 9000, 8000};
    private static final float[]  STAMINA_COST  = {4f, 4f, 5f, 5f, 5f};

    private CoupDeBotteSkill() {}

    public static int maxRank() { return COOLDOWN_MS.length; }

    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static float  damagePctForRank(int rank)   { return DAMAGE_PCT[idx(rank)]; }
    public static double knockbackForRank(int rank)   { return KNOCKBACK[idx(rank)]; }
    public static long   cooldownMsForRank(int rank)  { return COOLDOWN_MS[idx(rank)]; }
    public static float  staminaCostForRank(int rank) { return STAMINA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int dmg = Math.round(damagePctForRank(rank) * 100);
        int kb = (int) knockbackForRank(rank);
        int cd = (int)(cooldownMsForRank(rank) / 1000);
        return dmg + "% dégâts, recul " + kb + "u, CD " + cd + "s";
    }
}
