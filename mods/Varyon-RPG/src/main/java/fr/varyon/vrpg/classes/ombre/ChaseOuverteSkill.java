package fr.varyon.vrpg.classes.ombre;

public final class ChaseOuverteSkill {

    public static final String SKILL_ID       = "chasse_ouverte";
    public static final String TALENT_NODE_ID = "ombre_11";

    private static final long[]  DURATION_MS   = {2500, 3000, 3500, 4000, 5000};
    private static final long[]  COOLDOWN_MS   = {35000, 32000, 30000, 27000, 25000};
    private static final float[] STAMINA_COST  = {6f, 7f, 8f, 9f, 10f};

    private ChaseOuverteSkill() {}

    public static int maxRank() { return COOLDOWN_MS.length; }

    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static long  durationMsForRank(int rank)   { return DURATION_MS[idx(rank)]; }
    public static long  cooldownMsForRank(int rank)   { return COOLDOWN_MS[idx(rank)]; }
    public static float staminaCostForRank(int rank)  { return STAMINA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        float dur = durationMsForRank(rank) / 1000f;
        int cd = (int)(cooldownMsForRank(rank) / 1000);
        int stamina = Math.round(staminaCostForRank(rank));
        return "Marque " + dur + "s, Délai " + cd + "s, " + stamina + " endurance";
    }
}
