package fr.varyon.vrpg.classes.gardiendesgaia;

public final class AppelDuTreantSkill {

    public static final String SKILL_ID       = "appel_du_treant";
    public static final String TALENT_NODE_ID = "gardien_de_gaia_4";

    private static final long[]  DURATION_MS   = {20000, 30000, 40000, 50000, 60000};
    private static final float[] HP_FACTOR     = {1.0f,  1.25f, 1.5f,  1.75f, 2.0f};
    private static final float[] DAMAGE_FACTOR = {1.0f,  1.25f, 1.5f,  1.75f, 2.0f};
    private static final long[]  COOLDOWN_MS   = {60000, 60000, 60000, 60000, 60000};
    private static final float[] MANA_COST     = {26f, 28f, 30f, 32f, 34f};

    private AppelDuTreantSkill() {}

    public static int   maxRank()                    { return COOLDOWN_MS.length; }
    private static int  idx(int rank)                { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static long  durationMsForRank(int rank)  { return DURATION_MS[idx(rank)]; }
    public static float hpFactorForRank(int rank)    { return HP_FACTOR[idx(rank)]; }
    public static float damageFactor(int rank)       { return DAMAGE_FACTOR[idx(rank)]; }
    public static long  cooldownMsForRank(int rank)  { return COOLDOWN_MS[idx(rank)]; }
    public static float manaCostForRank(int rank)    { return MANA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int dur  = (int) (durationMsForRank(rank) / 1000);
        int pct  = Math.round(damageFactor(rank) * 100);
        int cd   = (int) (cooldownMsForRank(rank) / 1000);
        int mana = Math.round(manaCostForRank(rank));
        return "Tréant " + dur + "s, " + pct + "% stats lanceur, " + mana + " mana, Délai " + cd + "s";
    }
}
