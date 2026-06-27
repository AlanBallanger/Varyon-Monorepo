package fr.varyon.vrpg.classes.rodeur;

public final class MarqueDuChasseurSkill {

    public static final String SKILL_ID       = "marque_du_chasseur";
    public static final String TALENT_NODE_ID = "rodeur_3";

    private static final float[] DAMAGE_BONUS  = {0.10f, 0.15f, 0.20f, 0.25f, 0.30f};
    private static final long[]  DURATION_MS   = {8000, 10000, 12000, 14000, 16000};
    private static final long[]  COOLDOWN_MS   = {25000, 23000, 21000, 19000, 17000};
    private static final float[] STAMINA_COST  = {5f, 5f, 6f, 6f, 7f};
    private static final int[]   MAX_CONSUMES  = {3, 3, 4, 4, 5};

    private MarqueDuChasseurSkill() {}

    public static int maxRank() { return COOLDOWN_MS.length; }

    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static float damageBonusForRank(int rank)  { return DAMAGE_BONUS[idx(rank)]; }
    public static long  durationMsForRank(int rank)   { return DURATION_MS[idx(rank)]; }
    public static long  cooldownMsForRank(int rank)   { return COOLDOWN_MS[idx(rank)]; }
    public static float staminaCostForRank(int rank)  { return STAMINA_COST[idx(rank)]; }
    public static int   maxConsumesForRank(int rank)  { return MAX_CONSUMES[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int dmg = Math.round(damageBonusForRank(rank) * 100);
        float dur = durationMsForRank(rank) / 1000f;
        int cd = (int)(cooldownMsForRank(rank) / 1000);
        int consumes = maxConsumesForRank(rank);
        return "+" + dmg + "% dégâts sur cible, " + consumes + " hits, " + dur + "s, Délai " + cd + "s";
    }
}
