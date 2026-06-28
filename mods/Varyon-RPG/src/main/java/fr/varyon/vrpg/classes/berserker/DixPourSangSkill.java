package fr.varyon.vrpg.classes.berserker;

public final class DixPourSangSkill {

    public static final String SKILL_ID       = "dix_pour_sang";
    public static final String TALENT_NODE_ID = "berserker_4";

    private static final float  SELF_HP_COST_PCT  = 0.10f;
    private static final float[] DAMAGE_PCT       = {1.5f, 1.8f, 2.1f, 2.5f, 3.0f};
    private static final float[] STAMINA_COST     = {6f, 7f, 8f, 9f, 10f};
    private static final double  HIT_RADIUS       = 2.5;
    private static final long[]  COOLDOWN_MS      = {18000, 16000, 14000, 12000, 10000};

    private DixPourSangSkill() {}

    public static int maxRank() { return COOLDOWN_MS.length; }
    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static float  selfHpCostPct()            { return SELF_HP_COST_PCT; }
    public static float  damagePctForRank(int rank) { return DAMAGE_PCT[idx(rank)]; }
    public static float  staminaCostForRank(int rank) { return STAMINA_COST[idx(rank)]; }
    public static double hitRadius()                { return HIT_RADIUS; }
    public static long   cooldownMsForRank(int rank){ return COOLDOWN_MS[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int pct = Math.round(damagePctForRank(rank) * 100);
        int cost = Math.round(staminaCostForRank(rank));
        int cd  = (int) (cooldownMsForRank(rank) / 1000);
        return "Frappe puissante qui consomme 10% de tes PV, " + pct + "% dégâts arme (zone), "
            + cost + " endurance, Délai " + cd + "s";
    }
}
