package fr.varyon.vrpg.classes.ombre;

public final class FrappeFataleSkill {

    public static final String SKILL_ID       = "frappe_fatale";
    public static final String TALENT_NODE_ID = "ombre_6";

    private static final float[] DAMAGE_BONUS = {0.20f, 0.30f, 0.40f, 0.50f, 0.60f};
    private static final long[]  COOLDOWN_MS  = {35000, 33000, 30000, 27000, 25000};
    private static final long    ARMED_WINDOW = 8_000L;

    private FrappeFataleSkill() {}

    public static int maxRank() { return COOLDOWN_MS.length; }

    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static float damageBonusForRank(int rank) { return DAMAGE_BONUS[idx(rank)]; }
    public static long  cooldownMsForRank(int rank)  { return COOLDOWN_MS[idx(rank)]; }
    public static long  armedWindowMs()              { return ARMED_WINDOW; }

    public static String statLineForRank(int rank) {
        int pct = Math.round(damageBonusForRank(rank) * 100);
        int cd = (int)(cooldownMsForRank(rank) / 1000);
        return "+" + pct + "% dégâts prochain coup, CD " + cd + "s";
    }
}
