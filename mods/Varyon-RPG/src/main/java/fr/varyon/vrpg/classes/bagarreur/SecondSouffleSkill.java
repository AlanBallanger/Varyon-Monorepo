package fr.varyon.vrpg.classes.bagarreur;

public final class SecondSouffleSkill {

    public static final String SKILL_ID       = "second_souffle_bagarreur";
    public static final String TALENT_NODE_ID = "bagarreur_10";

    private static final float[] HP_RESTORE_PCT      = {0.08f, 0.10f, 0.12f, 0.15f, 0.20f};
    private static final float[] STAMINA_RESTORE_PCT = {0.50f, 0.50f, 0.50f, 0.50f, 0.50f};
    private static final long[]  COOLDOWN_MS          = {40000, 36000, 32000, 28000, 24000};
    private static final long    CAST_DURATION_MS     = 1500L;

    private SecondSouffleSkill() {}

    public static int    maxRank()                        { return COOLDOWN_MS.length; }
    private static int   idx(int rank)                    { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static float  hpRestorePctForRank(int rank)      { return HP_RESTORE_PCT[idx(rank)]; }
    public static float  staminaRestorePctForRank(int rank) { return STAMINA_RESTORE_PCT[idx(rank)]; }
    public static long   cooldownMsForRank(int rank)        { return COOLDOWN_MS[idx(rank)]; }
    public static float  staminaCostForRank(int rank)       { return 0f; }
    public static long   castDurationMs()                   { return CAST_DURATION_MS; }

    public static String statLineForRank(int rank) {
        int hp  = Math.round(hpRestorePctForRank(rank) * 100);
        int sta = Math.round(staminaRestorePctForRank(rank) * 100);
        int cd  = (int) (cooldownMsForRank(rank) / 1000);
        return "Restaure " + hp + "% PV et " + sta + "% endurance (cast " + (CAST_DURATION_MS / 1000) + "s), Délai " + cd + "s";
    }
}
