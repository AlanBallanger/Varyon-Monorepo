package fr.varyon.vrpg.classes.bagarreur;

public final class MonteeAdreinalineSkill {

    public static final String SKILL_ID       = "montee_adrenaline";
    public static final String TALENT_NODE_ID = "bagarreur_3";

    private static final float[] DAMAGE_BONUS = {0.20f, 0.25f, 0.30f, 0.38f, 0.50f};
    private static final long[]  DURATION_MS  = {4000, 5000, 6000, 7000, 8000};
    private static final long[]  COOLDOWN_MS  = {28000, 25000, 22000, 20000, 16000};
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
        int dur = (int) (durationMsForRank(rank) / 1000);
        int cd  = (int) (cooldownMsForRank(rank) / 1000);
        return "+" + pct + "% dégâts pendant " + dur + "s, Délai " + cd + "s";
    }
}
