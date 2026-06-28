package fr.varyon.vrpg.classes.gardiendesgaia;

public final class MarqueDeRenaissanceSkill {

    public static final String SKILL_ID       = "marque_de_renaissance";
    public static final String TALENT_NODE_ID = "gardien_de_gaia_11";

    private static final long[]  DURATION_MS        = {4000, 4500, 5000, 6000, 7000};
    private static final float[] SPECIAL_GAUGE_BONUS = {10f, 15f, 20f, 25f, 30f};
    private static final long[]  COOLDOWN_MS         = {60000, 55000, 50000, 45000, 40000};
    private static final float[] MANA_COST           = {18f, 19f, 20f, 22f, 24f};

    private MarqueDeRenaissanceSkill() {}

    public static int   maxRank()                        { return COOLDOWN_MS.length; }
    private static int  idx(int rank)                    { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static long  durationMsForRank(int rank)      { return DURATION_MS[idx(rank)]; }
    public static float specialGaugeBonusForRank(int rank) { return SPECIAL_GAUGE_BONUS[idx(rank)]; }
    public static long  cooldownMsForRank(int rank)      { return COOLDOWN_MS[idx(rank)]; }
    public static float manaCostForRank(int rank)        { return MANA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int dur   = (int) (durationMsForRank(rank) / 1000);
        int gauge = Math.round(specialGaugeBonusForRank(rank));
        int cd    = (int) (cooldownMsForRank(rank) / 1000);
        int mana  = Math.round(manaCostForRank(rank));
        return "Protection mort " + dur + "s, +" + gauge + " jauge spéciale, " + mana + " mana, Délai " + cd + "s";
    }
}
