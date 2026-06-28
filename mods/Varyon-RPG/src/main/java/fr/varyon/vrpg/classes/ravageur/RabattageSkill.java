package fr.varyon.vrpg.classes.ravageur;

public final class RabattageSkill {

    public static final String SKILL_ID       = "rabattage";
    public static final String TALENT_NODE_ID = "ravageur_11";

    private static final float[] DAMAGE_PCT   = {0.6f, 0.7f, 0.8f, 1.0f, 1.2f};
    private static final double  SWEEP_RADIUS = 10.0;
    private static final long[]  STUN_MS      = {800, 1000, 1200, 1400, 1600};
    private static final long[]  COOLDOWN_MS  = {25000, 24000, 23000, 22000, 21000};
    private static final float[] STAMINA_COST = {7f, 8f, 9f, 10f, 11f};
    public  static final double  PULL_SPEED   = 60.0;

    private RabattageSkill() {}

    public static int    maxRank()                    { return COOLDOWN_MS.length; }
    private static int   idx(int rank)                { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static float  damagePctForRank(int rank)   { return DAMAGE_PCT[idx(rank)]; }
    public static double sweepRadius()                { return SWEEP_RADIUS; }
    public static long   stunMsForRank(int rank)      { return STUN_MS[idx(rank)]; }
    public static long   cooldownMsForRank(int rank)  { return COOLDOWN_MS[idx(rank)]; }
    public static float  staminaCostForRank(int rank) { return STAMINA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int pct = Math.round(damagePctForRank(rank) * 100);
        int st  = (int) (stunMsForRank(rank) / 1000);
        int cd  = (int) (cooldownMsForRank(rank) / 1000);
        return "Balayage " + (int) SWEEP_RADIUS + " blocs, " + pct + "% dégâts, attire et étourdit " + st + "s, Délai " + cd + "s";
    }
}
