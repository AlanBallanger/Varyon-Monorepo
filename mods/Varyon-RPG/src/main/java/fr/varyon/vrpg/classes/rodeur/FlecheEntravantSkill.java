package fr.varyon.vrpg.classes.rodeur;

public final class FlecheEntravantSkill {

    public static final String SKILL_ID       = "fleche_entravante";
    public static final String TALENT_NODE_ID = "rodeur_5";

    private static final float[] DAMAGE_PCT    = {0.70f, 0.80f, 0.90f, 1.00f, 1.10f};
    private static final long[]  ROOT_MS       = {1000, 1500, 2000, 2500, 3000};
    private static final long[]  COOLDOWN_MS   = {18000, 16000, 15000, 14000, 12000};
    private static final float[] STAMINA_COST  = {5f, 5f, 6f, 6f, 7f};

    private FlecheEntravantSkill() {}

    public static int maxRank() { return COOLDOWN_MS.length; }

    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static float damagePctForRank(int rank)  { return DAMAGE_PCT[idx(rank)]; }
    public static long  rootMsForRank(int rank)     { return ROOT_MS[idx(rank)]; }
    public static long  cooldownMsForRank(int rank) { return COOLDOWN_MS[idx(rank)]; }
    public static float staminaCostForRank(int rank){ return STAMINA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int dmg = Math.round(damagePctForRank(rank) * 100);
        float root = rootMsForRank(rank) / 1000f;
        int cd = (int)(cooldownMsForRank(rank) / 1000);
        return dmg + "% dégâts, immobilise " + root + "s, Délai " + cd + "s";
    }
}
