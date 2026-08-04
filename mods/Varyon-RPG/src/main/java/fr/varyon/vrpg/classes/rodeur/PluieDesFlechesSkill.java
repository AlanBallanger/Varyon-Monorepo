package fr.varyon.vrpg.classes.rodeur;

public final class PluieDesFlechesSkill {

    public static final String SKILL_ID       = "pluie_des_fleches";
    public static final String TALENT_NODE_ID = "rodeur_2";

    private static final float[] DAMAGE_PCT   = {1.50f, 1.75f, 2.00f, 2.25f, 2.50f};
    private static final int[]   ARROW_COUNT  = {6, 7, 8, 9, 10};
    private static final long    DURATION_MS  = 1000L;
    private static final double  SPREAD_RADIUS = 1.0;
    private static final double  AIM_RANGE     = 20.0;
    private static final double  FALL_HEIGHT  = 10.0;
    private static final long    ARROW_FALL_MS = 500L;
    private static final long    ROOT_MS       = 1000L;
    private static final double  ROOT_RADIUS   = 3.0;
    public static final String   PROJECTILE_ID = "Vrpg_Fleche_Pluie2";
    private static final long[]  COOLDOWN_MS  = {30000, 28000, 26000, 24000, 22000};
    private static final float[] STAMINA_COST = {11f, 11f, 12f, 12f, 13f};

    private PluieDesFlechesSkill() {}

    public static int maxRank() { return COOLDOWN_MS.length; }

    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static float  damagePctForRank(int rank)  { return DAMAGE_PCT[idx(rank)]; }
    public static int    arrowCountForRank(int rank) { return ARROW_COUNT[idx(rank)]; }
    public static long   durationMs()               { return DURATION_MS; }
    public static double spreadRadius()              { return SPREAD_RADIUS; }
    public static double aimRange()                  { return AIM_RANGE; }
    public static double fallHeight()                { return FALL_HEIGHT; }
    public static long   arrowFallMs()              { return ARROW_FALL_MS; }
    public static long   cooldownMsForRank(int rank) { return COOLDOWN_MS[idx(rank)]; }
    public static float  staminaCostForRank(int rank){ return STAMINA_COST[idx(rank)]; }
    public static long   rootMs()                   { return ROOT_MS; }
    public static double rootRadius()               { return ROOT_RADIUS; }

    public static String statLineForRank(int rank) {
        int dmg = Math.round(damagePctForRank(rank) * 100);
        int arrows = arrowCountForRank(rank);
        int cd = (int)(cooldownMsForRank(rank) / 1000);
        return "Entrave en zone " + (ROOT_MS / 1000f) + "s, " + arrows + " flèches, "
            + dmg + "% dégâts/flèche, " + (DURATION_MS / 1000f) + "s, Délai " + cd + "s";
    }
}
