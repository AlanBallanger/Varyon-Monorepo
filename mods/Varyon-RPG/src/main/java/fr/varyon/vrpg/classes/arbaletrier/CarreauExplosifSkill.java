package fr.varyon.vrpg.classes.arbaletrier;

public final class CarreauExplosifSkill {

    public static final String SKILL_ID       = "carreau_explosif";
    public static final String TALENT_NODE_ID = "arbaletrier_3";

    private static final float[]  DAMAGE_PCT   = {1.20f, 1.40f, 1.60f, 1.80f, 2.00f};
    private static final double   RADIUS       = 4.0;
    private static final long[]   COOLDOWN_MS  = {22000, 20000, 18000, 16000, 14000};
    private static final float[]  STAMINA_COST = {6f, 7f, 7f, 8f, 8f};

    private CarreauExplosifSkill() {}

    public static int maxRank() { return COOLDOWN_MS.length; }

    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static float  damagePctForRank(int rank)   { return DAMAGE_PCT[idx(rank)]; }
    public static double radius()                     { return RADIUS; }
    public static long   cooldownMsForRank(int rank)  { return COOLDOWN_MS[idx(rank)]; }
    public static float  staminaCostForRank(int rank) { return STAMINA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int dmg = Math.round(damagePctForRank(rank) * 100);
        int cd = (int)(cooldownMsForRank(rank) / 1000);
        return dmg + "% dégâts, rayon " + (int)radius() + "m, Délai " + cd + "s";
    }
}
