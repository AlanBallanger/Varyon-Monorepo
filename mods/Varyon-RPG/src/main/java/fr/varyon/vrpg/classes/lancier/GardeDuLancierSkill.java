package fr.varyon.vrpg.classes.lancier;

public final class GardeDuLancierSkill {

    public static final String SKILL_ID       = "garde_du_lancier";
    public static final String TALENT_NODE_ID = "lancier_3";

    private static final long[]  WINDOW_MS         = {2000, 2500, 3000, 3500, 4000};
    private static final float[] NEXT_HIT_MULT     = {1.17f, 1.22f, 1.27f, 1.33f, 1.42f};
    private static final long[]  COOLDOWN_MS        = {20000, 18000, 16000, 14000, 12000};
    private static final float[] STAMINA_COST       = {7f, 7f, 8f, 8f, 9f};

    private GardeDuLancierSkill() {}

    public static int maxRank() { return COOLDOWN_MS.length; }

    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static long  windowMsForRank(int rank)      { return WINDOW_MS[idx(rank)]; }
    public static float nextHitMultForRank(int rank)   { return NEXT_HIT_MULT[idx(rank)]; }
    public static long  cooldownMsForRank(int rank)    { return COOLDOWN_MS[idx(rank)]; }
    public static float staminaCostForRank(int rank)   { return STAMINA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int mult = Math.round((nextHitMultForRank(rank) - 1f) * 100);
        int cd   = (int)(cooldownMsForRank(rank) / 1000);
        return "Pare la prochaine attaque et arme un coup dévastateur (+" + mult + "%), Délai " + cd + "s";
    }
}
