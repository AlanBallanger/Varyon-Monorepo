package fr.varyon.vrpg.classes.berserker;

public final class CriRalliementSkill {

    public static final String SKILL_ID       = "cri_ralliement";
    public static final String TALENT_NODE_ID = "berserker_8";

    private static final float[] DAMAGE_BONUS  = {0.10f, 0.14f, 0.18f, 0.22f, 0.28f};
    private static final long[]  DURATION_MS   = {3000, 3500, 4000, 4500, 5000};
    private static final long[]  COOLDOWN_MS   = {35000, 32000, 29000, 26000, 22000};
    private static final float[] STAMINA_COST  = {5f, 6f, 7f, 8f, 9f};
    private static final double  ALLY_RADIUS   = 8.0;

    private CriRalliementSkill() {}

    public static int maxRank() { return COOLDOWN_MS.length; }
    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static float  damageBonusForRank(int rank) { return DAMAGE_BONUS[idx(rank)]; }
    public static long   durationMsForRank(int rank)  { return DURATION_MS[idx(rank)]; }
    public static long   cooldownMsForRank(int rank)  { return COOLDOWN_MS[idx(rank)]; }
    public static float  staminaCostForRank(int rank) { return STAMINA_COST[idx(rank)]; }
    public static double allyRadius()                 { return ALLY_RADIUS; }

    public static String statLineForRank(int rank) {
        int pct = Math.round(damageBonusForRank(rank) * 100);
        int dur = (int) (durationMsForRank(rank) / 1000);
        int cd  = (int) (cooldownMsForRank(rank) / 1000);
        return "+" + pct + "% dégâts alliés (" + ALLY_RADIUS + " blocs) pendant " + dur + "s, Délai " + cd + "s";
    }
}
