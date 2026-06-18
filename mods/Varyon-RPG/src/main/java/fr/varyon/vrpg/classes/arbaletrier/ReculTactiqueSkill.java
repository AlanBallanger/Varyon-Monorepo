package fr.varyon.vrpg.classes.arbaletrier;

public final class ReculTactiqueSkill {

    public static final String SKILL_ID       = "recul_tactique";
    public static final String TALENT_NODE_ID = "arbaletrier_0";

    private static final double[] DASH_DISTANCE  = {3, 4, 5, 6, 7, 8};
    private static final long[]   COOLDOWN_MS    = {24000, 22000, 20000, 18000, 16000, 14000};
    private static final float[]  STAMINA_COST   = {5f, 5f, 6f, 6f, 7f, 7f};
    private static final float[]  DAMAGE_BONUS   = {0.20f, 0.25f, 0.30f, 0.35f, 0.40f, 0.45f};
    private static final long     ARMED_WINDOW_MS = 8_000L;

    private ReculTactiqueSkill() {}

    public static int maxRank() { return COOLDOWN_MS.length; }

    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static double dashDistanceForRank(int rank)  { return DASH_DISTANCE[idx(rank)]; }
    public static long   cooldownMsForRank(int rank)    { return COOLDOWN_MS[idx(rank)]; }
    public static float  staminaCostForRank(int rank)   { return STAMINA_COST[idx(rank)]; }
    public static float  damageBonusForRank(int rank)   { return DAMAGE_BONUS[idx(rank)]; }
    public static long   armedWindowMs()                { return ARMED_WINDOW_MS; }

    public static String statLineForRank(int rank) {
        int dist = (int) dashDistanceForRank(rank);
        int bonus = Math.round(damageBonusForRank(rank) * 100);
        int cd = (int)(cooldownMsForRank(rank) / 1000);
        return dist + " blocs, prochain tir +" + bonus + "% dégâts, CD " + cd + "s";
    }
}
