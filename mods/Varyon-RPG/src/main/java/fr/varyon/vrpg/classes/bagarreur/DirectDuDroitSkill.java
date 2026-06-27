package fr.varyon.vrpg.classes.bagarreur;

public final class DirectDuDroitSkill {

    public static final String SKILL_ID       = "direct_du_droit";
    public static final String TALENT_NODE_ID = "bagarreur_2";

    private static final float[] DAMAGE_PCT   = {0.7f, 0.9f, 1.1f, 1.3f, 1.5f};
    private static final long[]  STUN_MS      = {1000, 1200, 1400, 1600, 2000};
    private static final long[]  COOLDOWN_MS  = {14000, 12000, 11000, 10000, 8000};
    private static final float[] STAMINA_COST = {5f, 6f, 7f, 8f, 8f};

    private DirectDuDroitSkill() {}

    public static int    maxRank()                    { return COOLDOWN_MS.length; }
    private static int   idx(int rank)                { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static float  damagePctForRank(int rank)   { return DAMAGE_PCT[idx(rank)]; }
    public static long   stunMsForRank(int rank)      { return STUN_MS[idx(rank)]; }
    public static long   cooldownMsForRank(int rank)  { return COOLDOWN_MS[idx(rank)]; }
    public static float  staminaCostForRank(int rank) { return STAMINA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int pct = Math.round(damagePctForRank(rank) * 100);
        int st  = (int) (stunMsForRank(rank) / 1000);
        int cd  = (int) (cooldownMsForRank(rank) / 1000);
        return pct + "% dégâts arme, étourdit " + st + "s, Délai " + cd + "s";
    }
}
