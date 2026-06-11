package fr.varyon.vrpg.classes.rempart;

public final class ForteresseSkill {

    public static final String SKILL_ID       = "forteresse";
    public static final String TALENT_NODE_ID = "rempart_4";

    private static final float[] DAMAGE_REDUCTION = {0.40f, 0.45f, 0.50f, 0.55f, 0.65f};
    private static final long[]  DURATION_MS      = {3000, 3500, 4000, 4500, 5000};
    private static final long[]  COOLDOWN_MS      = {29000, 27000, 25000, 23000, 19000};
    private static final float[] STAMINA_COST     = {8f, 9f, 10f, 11f, 12f};

    private ForteresseSkill() {}

    public static int maxRank() { return COOLDOWN_MS.length; }
    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static float damageReductionForRank(int rank) { return DAMAGE_REDUCTION[idx(rank)]; }
    public static long  durationMsForRank(int rank)      { return DURATION_MS[idx(rank)]; }
    public static long  cooldownMsForRank(int rank)      { return COOLDOWN_MS[idx(rank)]; }
    public static float staminaCostForRank(int rank)     { return STAMINA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int pct = Math.round(damageReductionForRank(rank) * 100);
        float dur = durationMsForRank(rank) / 1000f;
        int cd = (int)(cooldownMsForRank(rank) / 1000);
        return "-" + pct + "% dégâts reçus pendant " + dur + "s, CD " + cd + "s";
    }
}
