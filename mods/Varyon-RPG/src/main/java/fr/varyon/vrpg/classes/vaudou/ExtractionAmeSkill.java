package fr.varyon.vrpg.classes.vaudou;

public final class ExtractionAmeSkill {

    public static final String SKILL_ID       = "extraction_ame";
    public static final String TALENT_NODE_ID = "vaudou_10";

    private static final float[] DAMAGE_PCT    = {0.80f, 1.00f, 1.20f, 1.50f, 1.80f};
    private static final float[] LIFESTEAL_PCT = {0.30f, 0.35f, 0.40f, 0.45f, 0.50f};
    private static final long[]  COOLDOWN_MS   = {14000, 13000, 12000, 10000, 8000};
    private static final float[] MANA_COST     = {7f, 8f, 9f, 10f, 11f};

    private ExtractionAmeSkill() {}

    public static int maxRank() { return COOLDOWN_MS.length; }

    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static float damagePctForRank(int rank)   { return DAMAGE_PCT[idx(rank)]; }
    public static float lifestealPctForRank(int rank){ return LIFESTEAL_PCT[idx(rank)]; }
    public static long  cooldownMsForRank(int rank)  { return COOLDOWN_MS[idx(rank)]; }
    public static float manaCostForRank(int rank)    { return MANA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int dmg  = Math.round(damagePctForRank(rank) * 100);
        int life = Math.round(lifestealPctForRank(rank) * 100);
        int cd   = (int) (cooldownMsForRank(rank) / 1000);
        int mn   = Math.round(manaCostForRank(rank));
        return dmg + "% dégâts arme, " + life + "% vol de vie, " + mn + " mana, CD " + cd + "s";
    }
}
