package fr.varyon.vrpg.classes.arcaniste;

public final class SurchargeSkill {

    public static final String SKILL_ID       = "surcharge";
    public static final String TALENT_NODE_ID = "arcaniste_8";

    private static final float[] DAMAGE_BONUS  = {0.20f, 0.28f, 0.36f, 0.45f, 0.55f};
    private static final float[] MANA_RESTORE  = {0.15f, 0.18f, 0.22f, 0.27f, 0.33f};
    private static final long[]  DURATION_MS   = {5000, 6000, 7000, 8000, 10000};
    private static final long[]  COOLDOWN_MS   = {25000, 23000, 21000, 19000, 16000};
    private static final float[] MANA_COST     = {5f, 5f, 6f, 6f, 7f};

    private SurchargeSkill() {}

    public static int   maxRank()                          { return COOLDOWN_MS.length; }
    private static int  idx(int rank)                     { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static float  damageBonusForRank(int rank)     { return DAMAGE_BONUS[idx(rank)]; }
    public static float  manaRestorePctForRank(int rank)  { return MANA_RESTORE[idx(rank)]; }
    public static long   durationMsForRank(int rank)      { return DURATION_MS[idx(rank)]; }
    public static long   cooldownMsForRank(int rank)      { return COOLDOWN_MS[idx(rank)]; }
    public static float  manaCostForRank(int rank)        { return MANA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int dmg     = Math.round(damageBonusForRank(rank) * 100);
        int manaRst = Math.round(manaRestorePctForRank(rank) * 100);
        int dur     = (int) (durationMsForRank(rank) / 1000);
        int cd      = (int) (cooldownMsForRank(rank) / 1000);
        int mana    = Math.round(manaCostForRank(rank));
        return "+" + dmg + "% dégâts sorts, +" + manaRst + "% mana restauré, " + dur + "s, " + mana + " mana, Délai " + cd + "s";
    }
}
