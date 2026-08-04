package fr.varyon.vrpg.classes.rodeur;

public final class FlecheEntravantSkill {

    public static final String SKILL_ID       = "fleche_entravante";
    public static final String TALENT_NODE_ID = "rodeur_8";

    private static final long    TRAP_DURATION_MS = 10_000L;
    private static final long[]  ROOT_MS       = {1000, 1500, 2000, 2500, 3000};
    private static final long[]  COOLDOWN_MS   = {24000, 22000, 20000, 18000, 16000};
    private static final float[] STAMINA_COST  = {8f, 8f, 9f, 9f, 10f};

    private FlecheEntravantSkill() {}

    public static int maxRank() { return COOLDOWN_MS.length; }

    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static long  trapDurationMs()            { return TRAP_DURATION_MS; }
    public static long  rootMsForRank(int rank)     { return ROOT_MS[idx(rank)]; }
    public static long  cooldownMsForRank(int rank) { return COOLDOWN_MS[idx(rank)]; }
    public static float staminaCostForRank(int rank){ return STAMINA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        float root = rootMsForRank(rank) / 1000f;
        int cd = (int)(cooldownMsForRank(rank) / 1000);
        return "Piège au sol " + (TRAP_DURATION_MS / 1000) + "s, immobilise " + root + "s, Délai " + cd + "s";
    }
}
