package fr.varyon.vrpg.classes.ombre;

public final class DelugeDeGamesSkill {

    public static final String SKILL_ID       = "deluge_de_lames";
    public static final String TALENT_NODE_ID = "ombre_3";

    private static final int[]   STRIKE_COUNT     = {3, 3, 4, 4, 5};
    private static final float[] DAMAGE_PCT       = {2.0f, 2.2f, 2.4f, 2.6f, 3.0f};
    private static final long[]  COOLDOWN_MS      = {30000, 28000, 25000, 22000, 20000};
    private static final float[] STAMINA_COST  = {6f, 7f, 8f, 9f, 10f};
    private static final long    INTERVAL_MS      = 150L;
    private static final float   LOW_HP_THRESHOLD = 0.30f;
    private static final float   LOW_HP_BONUS     = 0.30f;

    private DelugeDeGamesSkill() {}

    public static int maxRank() { return COOLDOWN_MS.length; }

    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static int   strikeCountForRank(int rank) { return STRIKE_COUNT[idx(rank)]; }
    public static float damagePctForRank(int rank)   { return DAMAGE_PCT[idx(rank)]; }
    public static long  cooldownMsForRank(int rank)  { return COOLDOWN_MS[idx(rank)]; }
    public static long  intervalMs()                 { return INTERVAL_MS; }
    public static float lowHpThreshold()             { return LOW_HP_THRESHOLD; }
    public static float lowHpBonus()                 { return LOW_HP_BONUS; }
    public static float staminaCostForRank(int rank) { return STAMINA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int strikes = strikeCountForRank(rank);
        int pct = Math.round(damagePctForRank(rank) * 100);
        int cd = (int)(cooldownMsForRank(rank) / 1000);
        int stamina = Math.round(staminaCostForRank(rank));
        return pct + "% dégâts arme × " + strikes + " frappes (+30% si cible <30% PV), Délai " + cd + "s, "
            + stamina + " endurance";
    }
}
