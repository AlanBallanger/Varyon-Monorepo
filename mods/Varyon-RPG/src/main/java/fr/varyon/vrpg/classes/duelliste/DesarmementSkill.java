package fr.varyon.vrpg.classes.duelliste;

public final class DesarmementSkill {

    public static final String SKILL_ID       = "desarmement";
    public static final String TALENT_NODE_ID = "9";

    private static final float[]  REDUCTION   = {0.10f, 0.12f, 0.15f, 0.18f, 0.20f};
    private static final long[]   DURATION_MS = {4000, 4000, 5000, 5000, 6000};
    private static final long[]   COOLDOWN_MS = {45000, 43000, 40000, 38000, 35000};

    private DesarmementSkill() {}

    public static int maxRank() { return REDUCTION.length; }

    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, REDUCTION.length - 1)); }

    public static float reductionForRank(int rank)  { return REDUCTION[idx(rank)]; }
    public static long  durationMsForRank(int rank) { return DURATION_MS[idx(rank)]; }
    public static long  cooldownMsForRank(int rank) { return COOLDOWN_MS[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int red = Math.round(reductionForRank(rank) * 100);
        int dur = (int) (durationMsForRank(rank) / 1000);
        int cd  = (int) (cooldownMsForRank(rank) / 1000);
        return "-" + red + "% dégâts cible, " + dur + "s, CD " + cd + "s";
    }
}
