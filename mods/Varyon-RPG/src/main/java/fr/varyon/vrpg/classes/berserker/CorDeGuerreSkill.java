package fr.varyon.vrpg.classes.berserker;

public final class CorDeGuerreSkill {

    public static final String SKILL_ID       = "cor_de_guerre";
    public static final String TALENT_NODE_ID = "berserker_5";

    private static final float[] SPEED_BONUS  = {0.20f, 0.25f, 0.30f, 0.35f, 0.40f};
    private static final long[]  DURATION_MS  = {4000, 5000, 6000, 7000, 8000};
    private static final long[]  COOLDOWN_MS  = {28000, 26000, 24000, 21000, 18000};
    private static final float[] STAMINA_COST = {6f, 7f, 7f, 8f, 9f};

    private CorDeGuerreSkill() {}

    public static int maxRank() { return COOLDOWN_MS.length; }
    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static float speedBonusForRank(int rank) { return SPEED_BONUS[idx(rank)]; }
    public static long  durationMsForRank(int rank)  { return DURATION_MS[idx(rank)]; }
    public static long  cooldownMsForRank(int rank)  { return COOLDOWN_MS[idx(rank)]; }
    public static float staminaCostForRank(int rank) { return STAMINA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int spdPct = Math.round(speedBonusForRank(rank) * 100);
        int dur    = (int) (durationMsForRank(rank) / 1000);
        int cd     = (int) (cooldownMsForRank(rank) / 1000);
        return "+" + spdPct + "% vitesse de déplacement pendant " + dur + "s, Délai " + cd + "s";
    }
}
