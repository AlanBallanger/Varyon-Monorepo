package fr.varyon.vrpg.classes.arcaniste;

public final class SalveDeGivreSkill {

    public static final String SKILL_ID       = "salve_de_givre";
    public static final String TALENT_NODE_ID = "arcaniste_4";
    public static final String SLOW_EFFECT    = "Vrpg_Arme_Lourde";

    private static final int[]   BOLT_COUNTS     = {3, 3, 4, 4, 5};
    private static final float[] DAMAGE_PCT_HIT  = {1.12f, 1.28f, 1.44f, 1.6f, 1.76f};
    private static final long    BOLT_DELAY_MS   = 120L;
    private static final long[]  SLOW_MS         = {2000, 2000, 2500, 2500, 3000};
    private static final long[]  COOLDOWN_MS     = {12000, 11000, 10000, 9000, 8000};
    private static final float[] MANA_COST       = {12f, 14f, 16f, 18f, 20f};

    private SalveDeGivreSkill() {}

    public static int   maxRank()                       { return COOLDOWN_MS.length; }
    private static int  idx(int rank)                  { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static int    boltCountForRank(int rank)     { return BOLT_COUNTS[idx(rank)]; }
    public static float  damagePctPerHitForRank(int rank) { return DAMAGE_PCT_HIT[idx(rank)]; }
    public static long   boltDelayMs()                  { return BOLT_DELAY_MS; }
    public static long   slowMsForRank(int rank)        { return SLOW_MS[idx(rank)]; }
    public static long   cooldownMsForRank(int rank)     { return COOLDOWN_MS[idx(rank)]; }
    public static float  manaCostForRank(int rank)      { return MANA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int bolts = boltCountForRank(rank);
        int pct   = Math.round(damagePctPerHitForRank(rank) * 100);
        int slow  = (int) (slowMsForRank(rank) / 1000);
        int cd    = (int) (cooldownMsForRank(rank) / 1000);
        int mana  = Math.round(manaCostForRank(rank));
        return bolts + " projectiles de " + pct + "% dégâts arme, ralentit " + slow + "s, " + mana + " mana, Délai " + cd + "s";
    }
}
