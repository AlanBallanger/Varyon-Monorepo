package fr.varyon.vrpg.classes.rempart;

public final class ProvocationSkill {

    public static final String SKILL_ID       = "provocation";
    public static final String TALENT_NODE_ID = "rempart_11";

    private static final double  TAUNT_RADIUS = 8.0;
    private static final long[]  DURATION_MS  = {4000, 5000, 6000, 7000, 8000};
    private static final long[]  COOLDOWN_MS  = {30000, 28000, 25000, 22000, 20000};
    private static final float[] STAMINA_COST = {8f, 9f, 10f, 11f, 12f};

    private ProvocationSkill() {}

    public static int maxRank() { return COOLDOWN_MS.length; }
    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static double tauntRadius()              { return TAUNT_RADIUS; }
    public static long   durationMsForRank(int rank){ return DURATION_MS[idx(rank)]; }
    public static long   cooldownMsForRank(int rank){ return COOLDOWN_MS[idx(rank)]; }
    public static float  staminaCostForRank(int rank){ return STAMINA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        float dur = durationMsForRank(rank) / 1000f;
        int cd = (int)(cooldownMsForRank(rank) / 1000);
        return "Force les ennemis à " + (int)TAUNT_RADIUS + " blocs à attaquer, " + dur + "s, CD " + cd + "s";
    }
}
