package fr.varyon.vrpg.classes.rodeur;

public final class RafaleSkill {

    public static final String SKILL_ID       = "rafale";
    public static final String TALENT_NODE_ID = "rodeur_11";

    private static final int[]   ARROW_COUNT   = {2, 2, 3, 3, 4};
    private static final float[] DAMAGE_PCT    = {0.50f, 0.55f, 0.60f, 0.65f, 0.70f};
    private static final long[]  DELAY_MS      = {300, 280, 260, 240, 220};
    private static final long[]  COOLDOWN_MS   = {20000, 18000, 16000, 14000, 12000};
    private static final float[] STAMINA_COST  = {7f, 7f, 8f, 8f, 9f};

    private RafaleSkill() {}

    public static int maxRank() { return COOLDOWN_MS.length; }

    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static int   arrowCountForRank(int rank)   { return ARROW_COUNT[idx(rank)]; }
    public static float damagePctForRank(int rank)    { return DAMAGE_PCT[idx(rank)]; }
    public static long  delayMsBetween(int rank)      { return DELAY_MS[idx(rank)]; }
    public static long  cooldownMsForRank(int rank)   { return COOLDOWN_MS[idx(rank)]; }
    public static float staminaCostForRank(int rank)  { return STAMINA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int arrows = arrowCountForRank(rank);
        int dmg = Math.round(damagePctForRank(rank) * 100);
        int cd = (int)(cooldownMsForRank(rank) / 1000);
        return arrows + " flèches à " + dmg + "% dégâts, CD " + cd + "s";
    }
}
