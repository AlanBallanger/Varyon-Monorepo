package fr.varyon.vrpg.classes.ravageur;

public final class PeauDeFerSkill {

    public static final String SKILL_ID       = "peau_de_fer";
    public static final String TALENT_NODE_ID = "ravageur_3";

    private static final float[] DAMAGE_REDUCTION = {0.15f, 0.18f, 0.22f, 0.27f, 0.35f};
    private static final long[]  DURATION_MS      = {3000, 4000, 5000, 6000, 7000};
    private static final long[]  COOLDOWN_MS       = {30000, 28000, 26000, 24000, 21000};
    private static final float[] STAMINA_COST      = {5f, 6f, 7f, 8f, 8f};

    private PeauDeFerSkill() {}

    public static int    maxRank()                      { return COOLDOWN_MS.length; }
    private static int   idx(int rank)                  { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static float  damageReductionForRank(int rank) { return DAMAGE_REDUCTION[idx(rank)]; }
    public static long   durationMsForRank(int rank)      { return DURATION_MS[idx(rank)]; }
    public static long   cooldownMsForRank(int rank)      { return COOLDOWN_MS[idx(rank)]; }
    public static float  staminaCostForRank(int rank)     { return STAMINA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int red = Math.round(damageReductionForRank(rank) * 100);
        int dur = (int) (durationMsForRank(rank) / 1000);
        int cd  = (int) (cooldownMsForRank(rank) / 1000);
        return "-" + red + "% dégâts subis pendant " + dur + "s, Délai " + cd + "s";
    }
}
