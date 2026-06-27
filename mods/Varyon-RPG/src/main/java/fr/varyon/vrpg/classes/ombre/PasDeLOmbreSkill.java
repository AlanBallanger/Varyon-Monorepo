package fr.varyon.vrpg.classes.ombre;

public final class PasDeLOmbreSkill {

    public static final String SKILL_ID       = "pas_de_l_ombre";
    public static final String TALENT_NODE_ID = "ombre_9";

    private static final double[] RANGE_BLOCKS  = {5, 6, 7, 8, 10};
    private static final long[]   COOLDOWN_MS   = {20000, 19000, 18000, 17000, 15000};
    private static final float[]  STAMINA_COST  = {8f, 9f, 10f, 11f, 12f};
    private static final float[]  DAMAGE_PCT       = {4.5f, 5.4f, 6.3f, 7.2f, 9.0f};
    private static final float    LOW_HP_THRESHOLD = 0.30f;
    private static final float    LOW_HP_BONUS     = 0.50f;

    private PasDeLOmbreSkill() {}

    public static int maxRank() { return COOLDOWN_MS.length; }

    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static double rangeForRank(int rank)     { return RANGE_BLOCKS[idx(rank)]; }
    public static long   cooldownMsForRank(int rank){ return COOLDOWN_MS[idx(rank)]; }
    public static float  staminaCostForRank(int rank){ return STAMINA_COST[idx(rank)]; }
    public static float  damagePctForRank(int rank)  { return DAMAGE_PCT[idx(rank)]; }
    public static float  lowHpThreshold()           { return LOW_HP_THRESHOLD; }
    public static float  lowHpBonus()               { return LOW_HP_BONUS; }

    public static String statLineForRank(int rank) {
        int range = (int) rangeForRank(rank);
        int pct = Math.round(damagePctForRank(rank) * 100);
        int cd = (int)(cooldownMsForRank(rank) / 1000);
        return "Portée " + range + " blocs, " + pct + "% dégâts arme (+50% si cible <30% HP), Délai " + cd + "s";
    }
}
