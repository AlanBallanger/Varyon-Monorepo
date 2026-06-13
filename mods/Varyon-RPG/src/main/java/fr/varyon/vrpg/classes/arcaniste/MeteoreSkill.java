package fr.varyon.vrpg.classes.arcaniste;

public final class MeteoreSkill {

    public static final String SKILL_ID       = "meteore";
    public static final String TALENT_NODE_ID = "arcaniste_4";

    private static final float[] DAMAGE_PCT  = {2.5f, 3.5f, 5.0f, 7.0f, 10.0f};
    private static final float   IMPACT_RADIUS = 5.0f;
    private static final long    DROP_DELAY_MS = 2500L;
    private static final float   DROP_HEIGHT   = 46.0f;
    private static final long[]  COOLDOWN_MS  = {30000, 27000, 24000, 21000, 18000};
    private static final float[] MANA_COST    = {20f, 22f, 24f, 26f, 28f};

    private MeteoreSkill() {}

    public static int   maxRank()                    { return COOLDOWN_MS.length; }
    private static int  idx(int rank)               { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static float  damagePctForRank(int rank)  { return DAMAGE_PCT[idx(rank)]; }
    public static float  impactRadius()              { return IMPACT_RADIUS; }
    public static long   dropDelayMs()               { return DROP_DELAY_MS; }
    public static float  dropHeight()                { return DROP_HEIGHT; }
    public static long   cooldownMsForRank(int rank)  { return COOLDOWN_MS[idx(rank)]; }
    public static float  manaCostForRank(int rank)   { return MANA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int pct  = Math.round(damagePctForRank(rank) * 100);
        int cd   = (int) (cooldownMsForRank(rank) / 1000);
        int mana = Math.round(manaCostForRank(rank));
        return pct + "% dégâts arme (rayon " + (int) IMPACT_RADIUS + " blocs), " + mana + " mana, CD " + cd + "s";
    }
}
