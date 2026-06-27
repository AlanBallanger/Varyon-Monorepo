package fr.varyon.vrpg.classes.lancier;

public final class GardeDuLancierSkill {

    public static final String SKILL_ID       = "garde_du_lancier";
    public static final String TALENT_NODE_ID = "lancier_3";

    private static final long[]  WINDOW_MS         = {3000, 3000, 3000, 3000, 3000};
    private static final float[] NEXT_HIT_MULT     = {1.50f, 1.65f, 1.80f, 2.00f, 2.25f};
    private static final long[]  COOLDOWN_MS        = {20000, 18000, 16000, 14000, 12000};
    private static final float[] STAMINA_COST       = {5f, 5f, 6f, 6f, 7f};

    private GardeDuLancierSkill() {}

    public static int maxRank() { return COOLDOWN_MS.length; }

    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static long  windowMsForRank(int rank)      { return WINDOW_MS[idx(rank)]; }
    public static float nextHitMultForRank(int rank)   { return NEXT_HIT_MULT[idx(rank)]; }
    public static long  cooldownMsForRank(int rank)    { return COOLDOWN_MS[idx(rank)]; }
    public static float staminaCostForRank(int rank)   { return STAMINA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int mult = Math.round(nextHitMultForRank(rank) * 100);
        int cd   = (int)(cooldownMsForRank(rank) / 1000);
        return "Pare la prochaine attaque (3s), prochain coup +" + mult + "% dégâts, Délai " + cd + "s";
    }
}
