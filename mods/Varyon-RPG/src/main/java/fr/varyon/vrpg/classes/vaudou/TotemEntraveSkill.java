package fr.varyon.vrpg.classes.vaudou;

public final class TotemEntraveSkill {

    public static final String SKILL_ID       = "totem_entrave";
    public static final String TALENT_NODE_ID = "vaudou_3";

    private static final long[]  BASE_DURATION_MS = {10000, 12000, 14000, 16000, 18000};
    private static final long[]  COOLDOWN_MS      = {25000, 23000, 21000, 19000, 17000};
    private static final float[] MANA_COST        = {7f, 8f, 9f, 10f, 11f};

    private TotemEntraveSkill() {}

    public static int maxRank() { return COOLDOWN_MS.length; }

    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static long  baseDurationMsForRank(int rank) { return BASE_DURATION_MS[idx(rank)]; }
    public static long  cooldownMsForRank(int rank)     { return COOLDOWN_MS[idx(rank)]; }
    public static float manaCostForRank(int rank)       { return MANA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int dur = (int) (baseDurationMsForRank(rank) / 1000);
        int cd  = (int) (cooldownMsForRank(rank) / 1000);
        int mn  = Math.round(manaCostForRank(rank));
        return "Ralentit les ennemis " + dur + "s, " + mn + " mana, Délai " + cd + "s";
    }
}
