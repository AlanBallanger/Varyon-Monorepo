package fr.varyon.vrpg.classes.arbaletrier;

public final class CarreauExplosifSkill {

    public static final String SKILL_ID       = "carreau_explosif";
    public static final String TALENT_NODE_ID = "arbaletrier_3";

    private static final float[]  DAMAGE_PCT   = {2.20f, 2.40f, 2.60f, 2.80f, 3.20f};
    private static final double[] RADIUS       = {3.0, 4.0, 4.0, 5.0, 5.0};
    private static final long[]   COOLDOWN_MS  = {27000, 25000, 23000, 21000, 19000};
    private static final float[]  STAMINA_COST = {11f, 12f, 12f, 13f, 14f};

    private CarreauExplosifSkill() {}

    public static int maxRank() { return COOLDOWN_MS.length; }

    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static float  damagePctForRank(int rank)   { return DAMAGE_PCT[idx(rank)]; }
    public static double radiusForRank(int rank)      { return RADIUS[idx(rank)]; }
    public static long   cooldownMsForRank(int rank)  { return COOLDOWN_MS[idx(rank)]; }
    public static float  staminaCostForRank(int rank) { return STAMINA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int dmg = Math.round(damagePctForRank(rank) * 100);
        int cd = (int)(cooldownMsForRank(rank) / 1000);
        return dmg + "% dégâts, rayon " + (int)radiusForRank(rank) + "m, Délai " + cd + "s";
    }
}
