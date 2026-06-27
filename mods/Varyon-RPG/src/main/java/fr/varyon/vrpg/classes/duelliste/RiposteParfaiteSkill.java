package fr.varyon.vrpg.classes.duelliste;

public final class RiposteParfaiteSkill {

    public static final String SKILL_ID       = "riposte_parfaite";
    public static final String TALENT_NODE_ID = "duelliste_4";

    private static final long[]  WINDOW_MS    = {3000, 3000, 4000, 4000, 5000};
    private static final float[] DMG_BONUS    = {0.50f, 0.75f, 1.00f, 1.25f, 1.50f};
    private static final long[]  COOLDOWN_MS  = {35000, 32000, 30000, 27000, 25000};
    private static final float[] STAMINA_COST = {7f, 8f, 9f, 10f, 11f};

    private RiposteParfaiteSkill() {}

    public static int maxRank() { return COOLDOWN_MS.length; }

    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static long windowMsForRank(int rank)   { return WINDOW_MS[idx(rank)]; }
    public static float dmgBonusForRank(int rank)   { return DMG_BONUS[idx(rank)]; }
    public static long cooldownMsForRank(int rank)  { return COOLDOWN_MS[idx(rank)]; }
    public static float staminaCostForRank(int rank) { return STAMINA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int dmg = Math.round(dmgBonusForRank(rank) * 100);
        int win = (int) (windowMsForRank(rank) / 1000);
        int stamina = Math.round(staminaCostForRank(rank));
        int cd  = (int) (cooldownMsForRank(rank) / 1000);
        return "Posture " + win + "s, contre-attaque +" + dmg + "%, "
            + stamina + " endurance, Délai " + cd + "s";
    }
}
