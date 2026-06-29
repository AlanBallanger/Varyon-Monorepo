package fr.varyon.vrpg.classes.arbaletrier;

public final class MiseEnJouSkill {

    public static final String SKILL_ID       = "mise_en_jou";
    public static final String TALENT_NODE_ID = "arbaletrier_11";

    private static final float   DAMAGE_MULTIPLIER = 3.0f;
    private static final long[]  COOLDOWN_MS    = {35000, 33000, 30000, 27000, 25000};
    private static final float[] STAMINA_COST   = {11f, 11f, 12f, 12f, 13f};
    private static final long    ARMED_WINDOW_MS  = 10_000L;
    private static final long    VISEE_DURATION_MS = 1_500L;

    private MiseEnJouSkill() {}

    public static int maxRank() { return COOLDOWN_MS.length; }

    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static float damageMultiplierForRank(int rank) { return DAMAGE_MULTIPLIER; }
    public static long  cooldownMsForRank(int rank)   { return COOLDOWN_MS[idx(rank)]; }
    public static float staminaCostForRank(int rank)  { return STAMINA_COST[idx(rank)]; }
    public static long  armedWindowMs()               { return ARMED_WINDOW_MS; }
    public static long  viseeDurationMs()             { return VISEE_DURATION_MS; }

    public static String statLineForRank(int rank) {
        int cd = (int)(cooldownMsForRank(rank) / 1000);
        return "Prochain carreau critique ×" + (int) DAMAGE_MULTIPLIER + ", Délai " + cd + "s";
    }
}
