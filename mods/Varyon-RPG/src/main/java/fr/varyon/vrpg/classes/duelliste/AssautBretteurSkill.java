package fr.varyon.vrpg.classes.duelliste;

public final class AssautBretteurSkill {

    public static final String SKILL_ID      = "assaut_bretteur";
    public static final String TALENT_NODE_ID = "duelliste_11";

    private static final float[]  DMG_BONUS    = {0.08f, 0.10f, 0.12f, 0.15f, 0.20f};
    private static final float[]  SPEED_BONUS  = {0.08f, 0.10f, 0.12f, 0.15f, 0.20f};
    private static final long[]   DURATION_MS  = {5000, 5000, 6000, 6000, 7000};
    private static final long[]   COOLDOWN_MS  = {40000, 38000, 35000, 32000, 30000};

    private AssautBretteurSkill() {}

    public static int maxRank() { return DMG_BONUS.length; }

    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, DMG_BONUS.length - 1)); }

    public static float  damageBonusForRank(int rank)  { return DMG_BONUS[idx(rank)]; }
    public static float  speedBonusForRank(int rank)   { return SPEED_BONUS[idx(rank)]; }
    public static long   durationMsForRank(int rank)   { return DURATION_MS[idx(rank)]; }
    public static long   cooldownMsForRank(int rank)   { return COOLDOWN_MS[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int dmg   = Math.round(damageBonusForRank(rank) * 100);
        int spd   = Math.round(speedBonusForRank(rank) * 100);
        int dur   = (int) (durationMsForRank(rank) / 1000);
        int cd    = (int) (cooldownMsForRank(rank) / 1000);
        return "+" + dmg + "% dégâts, +" + spd + "% vitesse, " + dur + "s, CD " + cd + "s";
    }
}
