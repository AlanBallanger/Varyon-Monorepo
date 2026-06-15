package fr.varyon.vrpg.classes.gardiendesgaia;

public final class EtreinteDeGaiaSkill {

    public static final String SKILL_ID       = "etreinte_de_gaia";
    public static final String TALENT_NODE_ID = "gardien_de_gaia_6";

    public static final String PROJECTILE_CONFIG = "Vrpg_EtreinteDeGaia";

    private static final float[] DAMAGE_FACTOR = {0.50f, 0.65f, 0.80f, 1.00f, 1.25f};
    private static final double  ROOT_RADIUS   = 4.0;
    private static final long[]  ROOT_DURATION = {2000, 2500, 3000, 3500, 4000};
    private static final long[]  COOLDOWN_MS   = {20000, 18000, 16000, 14000, 12000};
    private static final float[] MANA_COST     = {9f, 10f, 11f, 12f, 13f};

    private EtreinteDeGaiaSkill() {}

    public static int    maxRank()                      { return COOLDOWN_MS.length; }
    private static int   idx(int rank)                  { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static float  damageFactor(int rank)         { return DAMAGE_FACTOR[idx(rank)]; }
    public static double rootRadius()                   { return ROOT_RADIUS; }
    public static long   rootDurationMs(int rank)       { return ROOT_DURATION[idx(rank)]; }
    public static long   cooldownMsForRank(int rank)    { return COOLDOWN_MS[idx(rank)]; }
    public static float  manaCostForRank(int rank)      { return MANA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int dmg  = Math.round(damageFactor(rank) * 100);
        int root = (int) (rootDurationMs(rank) / 1000);
        int cd   = (int) (cooldownMsForRank(rank) / 1000);
        int mana = Math.round(manaCostForRank(rank));
        return dmg + "% ATK, immobilise " + root + "s (rayon " + (int) ROOT_RADIUS + " blocs), " + mana + " mana, CD " + cd + "s";
    }
}
