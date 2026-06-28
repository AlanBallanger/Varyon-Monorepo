package fr.varyon.vrpg.classes.berserker;

public final class DechiquetageSkill {

    public static final String SKILL_ID       = "dechiquetage";
    public static final String TALENT_NODE_ID = "berserker_9";

    private static final float[] DAMAGE_PCT    = {1.0f, 1.15f, 1.3f, 1.5f, 1.75f};
    private static final float[] LIFESTEAL_PCT = {0.15f, 0.20f, 0.25f, 0.30f, 0.40f};
    private static final long[]  COOLDOWN_MS   = {17000, 15500, 14000, 12500, 11000};
    private static final float[] STAMINA_COST  = {10f, 12f, 14f, 16f, 18f};
    private static final long    ARMED_WINDOW_MS = 6000L;

    private DechiquetageSkill() {}

    public static int maxRank() { return COOLDOWN_MS.length; }
    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static float  damagePctForRank(int rank)    { return DAMAGE_PCT[idx(rank)]; }
    public static float  lifestealPctForRank(int rank) { return LIFESTEAL_PCT[idx(rank)]; }
    public static long   cooldownMsForRank(int rank)   { return COOLDOWN_MS[idx(rank)]; }
    public static float  staminaCostForRank(int rank)  { return STAMINA_COST[idx(rank)]; }
    public static long   armedWindowMs()               { return ARMED_WINDOW_MS; }

    public static String statLineForRank(int rank) {
        int pct = Math.round(damagePctForRank(rank) * 100);
        int ls  = Math.round(lifestealPctForRank(rank) * 100);
        int cd  = (int) (cooldownMsForRank(rank) / 1000);
        return pct + "% dégâts arme, récupère " + ls + "% des dégâts en PV, Délai " + cd + "s";
    }
}
