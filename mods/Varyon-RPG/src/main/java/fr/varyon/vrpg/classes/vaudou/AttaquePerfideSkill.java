package fr.varyon.vrpg.classes.vaudou;

public final class AttaquePerfideSkill {

    public static final String SKILL_ID       = "attaque_perfide";
    public static final String TALENT_NODE_ID = "vaudou_4";

    private static final float[] DAMAGE_PCT     = {0.60f, 0.75f, 0.90f, 1.10f, 1.30f};
    private static final float   BEHIND_BONUS   = 0.50f;
    private static final float[] HP_COST_PCT    = {0.03f, 0.03f, 0.04f, 0.04f, 0.05f};
    private static final long[]  COOLDOWN_MS    = {10000, 9000, 8000, 7000, 6000};
    private static final float[] MANA_COST      = {4f, 5f, 5f, 6f, 7f};

    private AttaquePerfideSkill() {}

    public static int maxRank() { return COOLDOWN_MS.length; }

    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static float damagePctForRank(int rank)  { return DAMAGE_PCT[idx(rank)]; }
    public static float behindBonus()               { return BEHIND_BONUS; }
    public static float hpCostPctForRank(int rank)  { return HP_COST_PCT[idx(rank)]; }
    public static long  cooldownMsForRank(int rank) { return COOLDOWN_MS[idx(rank)]; }
    public static float manaCostForRank(int rank)   { return MANA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int pct  = Math.round(damagePctForRank(rank) * 100);
        int hp   = Math.round(hpCostPctForRank(rank) * 100);
        int cd   = (int) (cooldownMsForRank(rank) / 1000);
        int mn   = Math.round(manaCostForRank(rank));
        return pct + "% dégâts arme (+50% dos), " + hp + "% HP max, " + mn + " mana, Délai " + cd + "s";
    }
}
