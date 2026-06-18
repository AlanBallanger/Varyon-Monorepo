package fr.varyon.vrpg.classes.arbaletrier;

public final class CarreauLourdSkill {

    public static final String SKILL_ID       = "carreau_lourd";
    public static final String TALENT_NODE_ID = "arbaletrier_2";

    private static final float[] DAMAGE_BONUS   = {0.30f, 0.40f, 0.50f, 0.60f, 0.75f};
    private static final long[]  COOLDOWN_MS    = {12000, 11000, 10000, 9000, 8000};
    private static final float[] STAMINA_COST   = {4f, 4f, 5f, 5f, 6f};
    private static final long    ARMED_WINDOW_MS = 8_000L;

    private CarreauLourdSkill() {}

    public static int maxRank() { return COOLDOWN_MS.length; }

    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static float damageBonusForRank(int rank)  { return DAMAGE_BONUS[idx(rank)]; }
    public static long  cooldownMsForRank(int rank)   { return COOLDOWN_MS[idx(rank)]; }
    public static float staminaCostForRank(int rank)  { return STAMINA_COST[idx(rank)]; }
    public static long  armedWindowMs()               { return ARMED_WINDOW_MS; }

    public static String statLineForRank(int rank) {
        int bonus = Math.round(damageBonusForRank(rank) * 100);
        int cd = (int)(cooldownMsForRank(rank) / 1000);
        return "Prochain carreau +" + bonus + "% dégâts, CD " + cd + "s";
    }
}
