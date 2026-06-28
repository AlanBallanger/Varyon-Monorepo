package fr.varyon.vrpg.classes.berserker;

public final class CorDeGuerreSkill {

    public static final String SKILL_ID       = "cor_de_guerre";
    public static final String TALENT_NODE_ID = "berserker_5";

    private static final float[] SPEED_BONUS  = {0.10f, 0.12f, 0.14f, 0.16f, 0.18f};
    private static final long[]  DURATION_MS  = {2500, 3000, 3500, 4000, 4500};
    private static final long[]  COOLDOWN_MS  = {38000, 35000, 32000, 29000, 26000};
    private static final float[] STAMINA_COST = {10f, 12f, 14f, 16f, 18f};
    private static final double  ALLY_RADIUS  = 8.0;

    private CorDeGuerreSkill() {}

    public static int maxRank() { return COOLDOWN_MS.length; }
    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static float speedBonusForRank(int rank) { return SPEED_BONUS[idx(rank)]; }
    public static long  durationMsForRank(int rank)  { return DURATION_MS[idx(rank)]; }
    public static long  cooldownMsForRank(int rank)  { return COOLDOWN_MS[idx(rank)]; }
    public static float staminaCostForRank(int rank) { return STAMINA_COST[idx(rank)]; }
    public static double allyRadius()               { return ALLY_RADIUS; }

    public static String statLineForRank(int rank) {
        int spdPct = Math.round(speedBonusForRank(rank) * 100);
        int dur    = (int) (durationMsForRank(rank) / 1000);
        int cd     = (int) (cooldownMsForRank(rank) / 1000);
        return "+" + spdPct + "% vitesse (toi + alliés à " + (int) ALLY_RADIUS + " blocs) pendant " + dur + "s, Délai " + cd + "s";
    }
}
