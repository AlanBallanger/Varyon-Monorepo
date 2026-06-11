package fr.varyon.vrpg.classes.rempart;

public final class SecondSouffleSkill {

    public static final String SKILL_ID       = "second_souffle_rempart";
    public static final String TALENT_NODE_ID = "rempart_6";

    private static final float[] HEAL_PCT    = {0.20f, 0.25f, 0.30f, 0.35f, 0.45f};
    private static final long[]  COOLDOWN_MS = {45000, 42000, 38000, 34000, 30000};
    private static final float[] STAMINA_COST = {10f, 11f, 12f, 13f, 14f};

    private SecondSouffleSkill() {}

    public static int maxRank() { return COOLDOWN_MS.length; }
    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static float healPctForRank(int rank)    { return HEAL_PCT[idx(rank)]; }
    public static long  cooldownMsForRank(int rank) { return COOLDOWN_MS[idx(rank)]; }
    public static float staminaCostForRank(int rank){ return STAMINA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int pct = Math.round(healPctForRank(rank) * 100);
        int cd = (int)(cooldownMsForRank(rank) / 1000);
        return "Restaure " + pct + "% HP max, CD " + cd + "s";
    }
}
