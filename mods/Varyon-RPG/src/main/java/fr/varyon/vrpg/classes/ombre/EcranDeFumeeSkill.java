package fr.varyon.vrpg.classes.ombre;

public final class EcranDeFumeeSkill {

    public static final String SKILL_ID       = "ecran_de_fumee";
    public static final String TALENT_NODE_ID = "ombre_4"; // node 4 dans l'arbre

    private static final long[]  DURATION_MS  = {3000, 3500, 4000, 4500, 5000};
    private static final long[]  COOLDOWN_MS  = {40000, 37000, 33000, 30000, 25000};
    private static final float[] STAMINA_COST = {6f, 7f, 8f, 9f, 10f};

    private EcranDeFumeeSkill() {}

    public static int maxRank() { return COOLDOWN_MS.length; }

    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static long durationMsForRank(int rank)  { return DURATION_MS[idx(rank)]; }
    public static long cooldownMsForRank(int rank)   { return COOLDOWN_MS[idx(rank)]; }
    public static float staminaCostForRank(int rank) { return STAMINA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        float sec = durationMsForRank(rank) / 1000f;
        int cd = (int)(cooldownMsForRank(rank) / 1000);
        int stamina = Math.round(staminaCostForRank(rank));
        return "Invisibilité " + sec + "s, Délai " + cd + "s, " + stamina + " endurance";
    }
}
