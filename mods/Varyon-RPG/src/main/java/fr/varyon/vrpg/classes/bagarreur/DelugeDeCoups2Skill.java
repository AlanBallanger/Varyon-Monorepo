package fr.varyon.vrpg.classes.bagarreur;

public final class DelugeDeCoups2Skill {

    public static final String SKILL_ID       = "deluge_de_coups_bagarreur";
    public static final String TALENT_NODE_ID = "bagarreur_9";

    private static final int[] HIT_COUNTS     = {3, 4, 5, 6, 7};
    public  static final long  HIT_DELAY_MS   = 125L;

    public static int hitCountForRank(int rank) { return HIT_COUNTS[idx(rank)]; }

    private static final float[] DAMAGE_PER_HIT = {0.4f, 0.5f, 0.6f, 0.7f, 0.9f};
    private static final long[]  COOLDOWN_MS    = {18000, 16000, 14000, 12000, 10000};
    private static final float[] STAMINA_COST   = {8f, 9f, 10f, 11f, 12f};

    private DelugeDeCoups2Skill() {}

    public static int    maxRank()                     { return COOLDOWN_MS.length; }
    private static int   idx(int rank)                 { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static float  damagePerHitForRank(int rank) { return DAMAGE_PER_HIT[idx(rank)]; }
    public static long   cooldownMsForRank(int rank)   { return COOLDOWN_MS[idx(rank)]; }
    public static float  staminaCostForRank(int rank)  { return STAMINA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int pct = Math.round(damagePerHitForRank(rank) * 100);
        int cd  = (int) (cooldownMsForRank(rank) / 1000);
        return hitCountForRank(rank) + " frappes de " + pct + "% dégâts arme, Délai " + cd + "s";
    }
}
