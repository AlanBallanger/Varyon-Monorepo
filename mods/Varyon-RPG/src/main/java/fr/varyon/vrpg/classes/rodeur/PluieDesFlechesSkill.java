package fr.varyon.vrpg.classes.rodeur;

public final class PluieDesFlechesSkill {

    public static final String SKILL_ID       = "pluie_des_fleches";
    public static final String TALENT_NODE_ID = "rodeur_2";

    private static final float[] DAMAGE_PCT   = {0.60f, 0.70f, 0.80f, 0.90f, 1.00f};
    private static final int[]   ARROW_COUNT  = {6, 7, 8, 9, 10};
    private static final long    DURATION_MS  = 1000L;
    private static final double  RADIUS       = 6.0;
    private static final long[]  COOLDOWN_MS  = {30000, 28000, 26000, 24000, 22000};
    private static final float[] STAMINA_COST = {8f, 8f, 9f, 9f, 10f};

    private PluieDesFlechesSkill() {}

    public static int maxRank() { return COOLDOWN_MS.length; }

    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static float  damagePctForRank(int rank)  { return DAMAGE_PCT[idx(rank)]; }
    public static int    arrowCountForRank(int rank) { return ARROW_COUNT[idx(rank)]; }
    public static long   durationMs()               { return DURATION_MS; }
    public static double radius()                    { return RADIUS; }
    public static long   cooldownMsForRank(int rank) { return COOLDOWN_MS[idx(rank)]; }
    public static float  staminaCostForRank(int rank){ return STAMINA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int dmg = Math.round(damagePctForRank(rank) * 100);
        int arrows = arrowCountForRank(rank);
        int cd = (int)(cooldownMsForRank(rank) / 1000);
        return arrows + " flèches, " + dmg + "% dégâts/flèche, " + (DURATION_MS / 1000f) + "s, Délai " + cd + "s";
    }
}
