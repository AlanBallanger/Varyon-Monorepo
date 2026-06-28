package fr.varyon.vrpg.classes.vaudou;

public final class PassageEthereSkill {

    public static final String SKILL_ID       = "passage_ethere";
    public static final String TALENT_NODE_ID = "vaudou_0";

    private static final double[] RANGE_BLOCKS = {8.0, 9.0, 10.0, 11.0, 12.0};
    private static final long[]   COOLDOWN_MS  = {20000, 18000, 16000, 14000, 12000};
    private static final float[]  MANA_COST    = {6f, 7f, 7f, 8f, 8f};

    private PassageEthereSkill() {}

    public static int maxRank() { return COOLDOWN_MS.length; }

    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static double rangeForRank(int rank)    { return RANGE_BLOCKS[idx(rank)]; }
    public static long   cooldownMsForRank(int rank){ return COOLDOWN_MS[idx(rank)]; }
    public static float  manaCostForRank(int rank)  { return MANA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int range = (int) rangeForRank(rank);
        int cd    = (int) (cooldownMsForRank(rank) / 1000);
        int mana  = Math.round(manaCostForRank(rank));
        return "Portée " + range + " blocs, " + mana + " mana, Délai " + cd + "s";
    }
}
