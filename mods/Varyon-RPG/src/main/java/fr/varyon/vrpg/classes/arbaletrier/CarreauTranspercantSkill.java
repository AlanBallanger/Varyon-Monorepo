package fr.varyon.vrpg.classes.arbaletrier;

public final class CarreauTranspercantSkill {

    public static final String SKILL_ID       = "carreau_transpercant";
    public static final String TALENT_NODE_ID = "arbaletrier_6";

    private static final float[] DAMAGE_PCT    = {2.00f, 2.25f, 2.50f, 2.75f, 3.00f};
    private static final float[] SLOW_FACTOR   = {0.40f, 0.45f, 0.50f, 0.55f, 0.60f};
    private static final long[]  SLOW_MS       = {3000, 3500, 4000, 4500, 5000};
    private static final long[]  COOLDOWN_MS   = {23000, 21000, 20000, 19000, 17000};
    private static final float[] STAMINA_COST  = {11f, 11f, 12f, 12f, 13f};
    private static final int[]   PIERCE_COUNT  = {2, 2, 3, 3, 4};

    private CarreauTranspercantSkill() {}

    public static int maxRank() { return COOLDOWN_MS.length; }

    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static float damagePctForRank(int rank)   { return DAMAGE_PCT[idx(rank)]; }
    public static float slowFactorForRank(int rank)  { return SLOW_FACTOR[idx(rank)]; }
    public static long  slowMsForRank(int rank)      { return SLOW_MS[idx(rank)]; }
    public static long  cooldownMsForRank(int rank)  { return COOLDOWN_MS[idx(rank)]; }
    public static float staminaCostForRank(int rank) { return STAMINA_COST[idx(rank)]; }
    public static int   pierceCountForRank(int rank) { return PIERCE_COUNT[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int dmg = Math.round(damagePctForRank(rank) * 100);
        int slow = Math.round(slowFactorForRank(rank) * 100);
        float dur = slowMsForRank(rank) / 1000f;
        int cd = (int)(cooldownMsForRank(rank) / 1000);
        int pierce = pierceCountForRank(rank);
        return dmg + "% dégâts, traverse " + pierce + " cibles, ralentit " + slow + "% pendant " + dur + "s, Délai " + cd + "s";
    }
}
