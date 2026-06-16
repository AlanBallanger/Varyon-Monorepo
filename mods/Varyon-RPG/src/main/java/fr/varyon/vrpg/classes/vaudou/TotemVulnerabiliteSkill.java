package fr.varyon.vrpg.classes.vaudou;

public final class TotemVulnerabiliteSkill {

    public static final String SKILL_ID       = "totem_vulnerabilite";
    public static final String TALENT_NODE_ID = "vaudou_6";

    private static final float[] DAMAGE_TAKEN_BONUS = {0.08f, 0.10f, 0.12f, 0.15f, 0.18f};
    private static final long[]  BASE_DURATION_MS   = {10000, 12000, 14000, 16000, 18000};
    private static final long[]  COOLDOWN_MS         = {28000, 25000, 23000, 21000, 18000};
    private static final float[] MANA_COST           = {8f, 9f, 10f, 11f, 12f};

    private TotemVulnerabiliteSkill() {}

    public static int maxRank() { return COOLDOWN_MS.length; }

    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static float damageTakenBonusForRank(int rank) { return DAMAGE_TAKEN_BONUS[idx(rank)]; }
    public static long  baseDurationMsForRank(int rank)   { return BASE_DURATION_MS[idx(rank)]; }
    public static long  cooldownMsForRank(int rank)       { return COOLDOWN_MS[idx(rank)]; }
    public static float manaCostForRank(int rank)         { return MANA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int bonus = Math.round(damageTakenBonusForRank(rank) * 100);
        int dur   = (int) (baseDurationMsForRank(rank) / 1000);
        int cd    = (int) (cooldownMsForRank(rank) / 1000);
        int mn    = Math.round(manaCostForRank(rank));
        return "+" + bonus + "% dégâts subis (zone) " + dur + "s, " + mn + " mana, CD " + cd + "s";
    }
}
