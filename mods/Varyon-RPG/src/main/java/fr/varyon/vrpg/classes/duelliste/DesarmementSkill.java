package fr.varyon.vrpg.classes.duelliste;

public final class DesarmementSkill {

    public static final String SKILL_ID       = "desarmement";
    public static final String TALENT_NODE_ID = "duelliste_9";

    private static final float[]  REDUCTION   = {0.30f, 0.35f, 0.40f, 0.45f, 0.50f};
    private static final long[]   DURATION_MS = {4000, 4000, 5000, 5000, 6000};
    private static final long[]   COOLDOWN_MS = {38000, 36000, 33000, 31000, 28000};
    private static final float[]  STAMINA_COST = {7f, 8f, 9f, 10f, 11f};

    private DesarmementSkill() {}

    public static int maxRank() { return REDUCTION.length; }

    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, REDUCTION.length - 1)); }

    public static float reductionForRank(int rank)  { return REDUCTION[idx(rank)]; }
    public static long  durationMsForRank(int rank) { return DURATION_MS[idx(rank)]; }
    public static long  cooldownMsForRank(int rank) { return COOLDOWN_MS[idx(rank)]; }
    public static float staminaCostForRank(int rank) { return STAMINA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int red = Math.round(reductionForRank(rank) * 100);
        int dur = (int) (durationMsForRank(rank) / 1000);
        int stamina = Math.round(staminaCostForRank(rank));
        int cd  = (int) (cooldownMsForRank(rank) / 1000);
        return "-" + red + "% dégâts, -50% vitesse, " + dur + "s, "
            + stamina + " endurance, Délai " + cd + "s";
    }
}
