package fr.varyon.vrpg.classes.gardiendesgaia;

public final class BenedictionDeGaiaSkill {

    public static final String SKILL_ID       = "benediction_de_gaia";
    public static final String TALENT_NODE_ID = "gardien_de_gaia_2";

    public static final double HEAL_RADIUS = 10.0;

    private static final float[] HEAL_AMOUNT  = {40f, 60f, 85f, 115f, 150f};
    private static final long[]  COOLDOWN_MS  = {22000, 20000, 18000, 16000, 14000};
    private static final float[] MANA_COST    = {10f, 11f, 12f, 13f, 15f};

    private BenedictionDeGaiaSkill() {}

    public static int   maxRank()                    { return COOLDOWN_MS.length; }
    private static int  idx(int rank)                { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static float healAmountForRank(int rank)  { return HEAL_AMOUNT[idx(rank)]; }
    public static long  cooldownMsForRank(int rank)  { return COOLDOWN_MS[idx(rank)]; }
    public static float manaCostForRank(int rank)    { return MANA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int heal = Math.round(healAmountForRank(rank));
        int cd   = (int) (cooldownMsForRank(rank) / 1000);
        int mana = Math.round(manaCostForRank(rank));
        return "+" + heal + " PV aux alliés à " + (int) HEAL_RADIUS + " blocs, " + mana + " mana, CD " + cd + "s";
    }
}
