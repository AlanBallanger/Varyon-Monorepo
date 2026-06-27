package fr.varyon.vrpg.classes.duelliste;

public final class FeintSkill {

    public static final String SKILL_ID       = "feinte";
    public static final String TALENT_NODE_ID = "duelliste_6";

    private static final long[] COOLDOWN_MS = {20000, 19000, 18000, 16000, 14000};
    private static final float[] STAMINA_COST = {7f, 8f, 9f, 10f, 11f};
    private static final long   WINDOW_MS   = 3_000L;

    private FeintSkill() {}

    public static int maxRank() { return COOLDOWN_MS.length; }

    public static long cooldownMsForRank(int rank) {
        return COOLDOWN_MS[Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1))];
    }

    public static long windowMs() { return WINDOW_MS; }

    public static float staminaCostForRank(int rank) {
        return STAMINA_COST[Math.max(0, Math.min(rank - 1, STAMINA_COST.length - 1))];
    }

    public static String statLineForRank(int rank) {
        int stamina = Math.round(staminaCostForRank(rank));
        int cd = (int) (cooldownMsForRank(rank) / 1000);
        return "Prochain coup imparable (3s), " + stamina + " endurance, Délai " + cd + "s";
    }
}
