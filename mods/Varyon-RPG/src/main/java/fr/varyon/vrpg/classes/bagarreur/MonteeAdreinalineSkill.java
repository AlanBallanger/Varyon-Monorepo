package fr.varyon.vrpg.classes.bagarreur;

public final class MonteeAdreinalineSkill {

    public static final String SKILL_ID       = "montee_adrenaline";
    public static final String TALENT_NODE_ID = "bagarreur_3";

    private static final float[] DAMAGE_BONUS = {0.40f, 0.55f, 0.70f, 0.85f, 1.00f};
    private static final long[]  DURATION_MS  = {4000, 4500, 5000, 5500, 6000};
    private static final long[]  COOLDOWN_MS  = {22000, 21000, 20000, 19000, 16000};
    private static final float[] STAMINA_COST = {5f, 6f, 7f, 8f, 8f};

    private MonteeAdreinalineSkill() {}

    public static int    maxRank()                   { return COOLDOWN_MS.length; }
    private static int   idx(int rank)               { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static float  damageBonusForRank(int rank) { return DAMAGE_BONUS[idx(rank)]; }
    public static long   durationMsForRank(int rank)  { return DURATION_MS[idx(rank)]; }
    public static long   cooldownMsForRank(int rank)  { return COOLDOWN_MS[idx(rank)]; }
    public static float  staminaCostForRank(int rank) { return STAMINA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int pct = Math.round(damageBonusForRank(rank) * 100);
        long durMs = durationMsForRank(rank);
        String dur = durMs % 1000L == 0L
            ? (durMs / 1000L) + "s"
            : String.format(java.util.Locale.ROOT, "%.1fs", durMs / 1000.0);
        int cd = (int) (cooldownMsForRank(rank) / 1000);
        return "+" + pct + "% dégâts pendant " + dur + ", Délai " + cd + "s";
    }
}
