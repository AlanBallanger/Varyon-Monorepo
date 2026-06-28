package fr.varyon.vrpg.classes.ombre;

public final class FrappeFataleSkill {

    public static final String SKILL_ID       = "frappe_fatale";
    public static final String TALENT_NODE_ID = "ombre_6";

    private static final float[] DAMAGE_BONUS = {0.40f, 0.50f, 0.60f, 0.70f, 0.80f};
    private static final long[]  COOLDOWN_MS  = {25000, 23000, 20000, 17000, 15000};
    private static final float[] STAMINA_COST = {6f, 7f, 8f, 9f, 10f};
    private static final long    ARMED_WINDOW = 8_000L;

    private FrappeFataleSkill() {}

    public static int maxRank() { return COOLDOWN_MS.length; }

    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static float damageBonusForRank(int rank) { return DAMAGE_BONUS[idx(rank)]; }
    public static long  cooldownMsForRank(int rank)  { return COOLDOWN_MS[idx(rank)]; }
    public static long  armedWindowMs()              { return ARMED_WINDOW; }
    public static float staminaCostForRank(int rank) { return STAMINA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int pct = Math.round(damageBonusForRank(rank) * 100);
        int cd = (int)(cooldownMsForRank(rank) / 1000);
        int stamina = Math.round(staminaCostForRank(rank));
        return "+" + pct + "% dégâts prochain coup, Délai " + cd + "s, " + stamina + " endurance";
    }
}
