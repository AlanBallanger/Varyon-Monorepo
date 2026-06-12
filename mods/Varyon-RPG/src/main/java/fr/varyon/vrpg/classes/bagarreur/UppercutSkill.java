package fr.varyon.vrpg.classes.bagarreur;

public final class UppercutSkill {

    public static final String SKILL_ID       = "uppercut";
    public static final String TALENT_NODE_ID = "bagarreur_11";

    private static final float[] DAMAGE_PCT   = {0.7f, 0.9f, 1.1f, 1.4f, 1.8f};
    private static final float   LAUNCH_Y     = 40.0f;
    private static final long[]  COOLDOWN_MS  = {16000, 14000, 12000, 10000, 8000};
    private static final float[] STAMINA_COST = {7f, 8f, 9f, 10f, 10f};

    private UppercutSkill() {}

    public static int    maxRank()                    { return COOLDOWN_MS.length; }
    private static int   idx(int rank)                { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static float  damagePctForRank(int rank)   { return DAMAGE_PCT[idx(rank)]; }
    public static float  launchY()                    { return LAUNCH_Y; }
    public static long   cooldownMsForRank(int rank)  { return COOLDOWN_MS[idx(rank)]; }
    public static float  staminaCostForRank(int rank) { return STAMINA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int pct = Math.round(damagePctForRank(rank) * 100);
        int cd  = (int) (cooldownMsForRank(rank) / 1000);
        return pct + "% dégâts arme, projette la cible en l'air, CD " + cd + "s";
    }
}
