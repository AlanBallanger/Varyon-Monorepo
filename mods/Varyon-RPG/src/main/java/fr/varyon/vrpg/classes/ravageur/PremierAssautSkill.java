package fr.varyon.vrpg.classes.ravageur;

public final class PremierAssautSkill {

    public static final String SKILL_ID       = "premier_assaut";
    public static final String TALENT_NODE_ID = "ravageur_8";

    public static final float  HEALTHY_THRESHOLD   = 0.70f;
    private static final float[] DAMAGE_PCT        = {1.5f, 1.8f, 2.2f, 2.6f, 3.2f};
    private static final long[]  COOLDOWN_MS       = {21000, 19000, 17000, 15000, 13000};
    private static final float[] STAMINA_COST      = {8f, 9f, 10f, 11f, 12f};

    private PremierAssautSkill() {}

    public static int    maxRank()                   { return COOLDOWN_MS.length; }
    private static int   idx(int rank)               { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static float  damagePctForRank(int rank)  { return DAMAGE_PCT[idx(rank)]; }
    public static long   cooldownMsForRank(int rank) { return COOLDOWN_MS[idx(rank)]; }
    public static float  staminaCostForRank(int rank){ return STAMINA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int pct = Math.round(damagePctForRank(rank) * 100);
        int cd  = (int) (cooldownMsForRank(rank) / 1000);
        return pct + "% dégâts arme si la cible a >70% PV, Délai " + cd + "s";
    }
}
