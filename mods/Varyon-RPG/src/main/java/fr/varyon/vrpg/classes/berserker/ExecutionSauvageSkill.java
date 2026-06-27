package fr.varyon.vrpg.classes.berserker;

public final class ExecutionSauvageSkill {

    public static final String SKILL_ID       = "execution_sauvage";
    public static final String TALENT_NODE_ID = "berserker_11";

    private static final float[] BASE_DAMAGE_PCT   = {1.0f, 1.2f, 1.4f, 1.7f, 2.0f};
    private static final float[] LOW_HP_BONUS_PCT  = {0.20f, 0.30f, 0.40f, 0.55f, 0.70f};
    private static final float   LOW_HP_THRESHOLD  = 0.50f;
    private static final long[]  COOLDOWN_MS       = {16000, 15000, 13000, 11000, 9000};
    private static final float[] STAMINA_COST      = {7f, 8f, 8f, 9f, 10f};

    private ExecutionSauvageSkill() {}

    public static int maxRank() { return COOLDOWN_MS.length; }
    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static float  baseDamagePctForRank(int rank)  { return BASE_DAMAGE_PCT[idx(rank)]; }
    public static float  lowHpBonusPctForRank(int rank)  { return LOW_HP_BONUS_PCT[idx(rank)]; }
    public static float  lowHpThreshold()                { return LOW_HP_THRESHOLD; }
    public static long   cooldownMsForRank(int rank)     { return COOLDOWN_MS[idx(rank)]; }
    public static float  staminaCostForRank(int rank)    { return STAMINA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int pct    = Math.round(baseDamagePctForRank(rank) * 100);
        int bonus  = Math.round(lowHpBonusPctForRank(rank) * 100);
        int thres  = Math.round(LOW_HP_THRESHOLD * 100);
        int cd     = (int) (cooldownMsForRank(rank) / 1000);
        return pct + "% dégâts arme (+" + bonus + "% si cible < " + thres + "% HP), Délai " + cd + "s";
    }
}
