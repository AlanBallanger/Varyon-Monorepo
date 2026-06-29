package fr.varyon.vrpg.classes.rodeur;

public final class MarqueDuChasseurSkill {

    public static final String SKILL_ID       = "marque_du_chasseur";
    public static final String TALENT_NODE_ID = "rodeur_3";

    private static final float[] DAMAGE_BONUS  = {0.10f, 0.15f, 0.20f, 0.25f, 0.30f};
    private static final long[]  DURATION_MS   = {4000, 5000, 6000, 7000, 8000};
    private static final long[]  COOLDOWN_MS   = {28000, 26000, 24000, 22000, 20000};
    private static final float[] STAMINA_COST  = {8f, 8f, 9f, 9f, 10f};
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
        return "+" + dmg + "% dégâts sur cible marquée, " + dur + "s, Délai " + cd + "s";
    }
}
