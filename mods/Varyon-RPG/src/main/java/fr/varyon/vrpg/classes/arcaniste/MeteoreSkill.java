package fr.varyon.vrpg.classes.arcaniste;

public final class MeteoreSkill {

    public static final String SKILL_ID       = "meteore";
    public static final String TALENT_NODE_ID = "arcaniste_10";

    public static final String TELEGRAPH_PARTICLE = "Vrpg_Meteor_Telegraph";
    public static final String IMPACT_PARTICLE    = "Vrpg_Meteor_Impact";
    public static final String FALLING_PROJECTILE = "Vrpg_Meteor_Falling";
    public static final String IMPACT_SOUND         = "SFX_Goblin_Lobber_Bomb_Death";

    private static final float[] DAMAGE_PCT  = {5.0f, 6.0f, 7.5f, 8.5f, 10.0f};
    private static final float   IMPACT_RADIUS = 8.0f;
    private static final long    DROP_DELAY_MS = 2500L;
    private static final float   DROP_HEIGHT   = 45.0f;
    private static final float   GROUND_Y_OFFSET = 0.15f;
    private static final double  FALL_GRAVITY  = 15.0;
    private static final double  MAX_TARGET_DISTANCE = 30.0;
    private static final long[]  COOLDOWN_MS  = {30000, 27000, 24000, 21000, 18000};
    private static final float[] MANA_COST    = {20f, 22f, 24f, 26f, 28f};

    private MeteoreSkill() {}

    public static int   maxRank()                    { return COOLDOWN_MS.length; }
    private static int  idx(int rank)               { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static float  damagePctForRank(int rank)  { return DAMAGE_PCT[idx(rank)]; }
    public static float  impactRadius()              { return IMPACT_RADIUS; }
    public static long   dropDelayMs()               { return DROP_DELAY_MS; }
    public static float  dropHeight()                { return DROP_HEIGHT; }
    public static float  groundYOffset()           { return GROUND_Y_OFFSET; }
    public static double fallGravity()               { return FALL_GRAVITY; }
    public static double maxTargetDistance()         { return MAX_TARGET_DISTANCE; }
    public static long   cooldownMsForRank(int rank)  { return COOLDOWN_MS[idx(rank)]; }
    public static float  manaCostForRank(int rank)   { return MANA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int pct  = Math.round(damagePctForRank(rank) * 100);
        int cd   = (int) (cooldownMsForRank(rank) / 1000);
        int mana = Math.round(manaCostForRank(rank));
        return pct + "% dégâts arme (rayon " + (int) IMPACT_RADIUS + " blocs), " + mana + " mana, CD " + cd + "s";
    }
}
