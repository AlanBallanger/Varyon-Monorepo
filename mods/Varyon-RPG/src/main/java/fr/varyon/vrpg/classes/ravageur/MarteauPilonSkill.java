package fr.varyon.vrpg.classes.ravageur;

public final class MarteauPilonSkill {

    public static final String SKILL_ID       = "marteau_pilon";
    public static final String TALENT_NODE_ID = "ravageur_9";

    private static final float[] DAMAGE_PCT_1  = {0.8f, 0.9f, 1.0f, 1.2f, 1.4f};
    private static final float[] DAMAGE_PCT_2  = {1.2f, 1.4f, 1.6f, 1.9f, 2.3f};
    private static final long[]  STUN_MS       = {1200, 1400, 1600, 1800, 2200};
    private static final long[]  COOLDOWN_MS   = {18000, 16000, 14000, 12000, 10000};
    private static final float[] STAMINA_COST  = {8f, 9f, 10f, 11f, 12f};
    public  static final long    SECOND_HIT_DELAY_MS = 500L;

    private MarteauPilonSkill() {}

    public static int    maxRank()                    { return COOLDOWN_MS.length; }
    private static int   idx(int rank)                { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static float  damagePct1ForRank(int rank)  { return DAMAGE_PCT_1[idx(rank)]; }
    public static float  damagePct2ForRank(int rank)  { return DAMAGE_PCT_2[idx(rank)]; }
    public static long   stunMsForRank(int rank)      { return STUN_MS[idx(rank)]; }
    public static long   cooldownMsForRank(int rank)  { return COOLDOWN_MS[idx(rank)]; }
    public static float  staminaCostForRank(int rank) { return STAMINA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int p1  = Math.round(damagePct1ForRank(rank) * 100);
        int p2  = Math.round(damagePct2ForRank(rank) * 100);
        int st  = (int) (stunMsForRank(rank) / 1000);
        int cd  = (int) (cooldownMsForRank(rank) / 1000);
        return "2 frappes : " + p1 + "% puis " + p2 + "% dégâts arme, 2e frappe étourdit " + st + "s, Délai " + cd + "s";
    }
}
