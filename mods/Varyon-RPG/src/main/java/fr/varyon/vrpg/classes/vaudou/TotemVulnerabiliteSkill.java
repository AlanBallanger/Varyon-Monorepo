package fr.varyon.vrpg.classes.vaudou;

public final class TotemVulnerabiliteSkill {

    public static final String SKILL_ID       = "totem_vulnerabilite";
    public static final String TALENT_NODE_ID = "vaudou_6";
    public static final String PROJECTILE_CONFIG = "Vrpg_Totem_Vulnerabilite_Throw";
    public static final double THROW_SPEED      = 8.0;

    private static final float[] DAMAGE_TAKEN_BONUS = {0.10f, 0.12f, 0.14f, 0.17f, 0.20f};
    private static final long[]  BASE_DURATION_MS   = {10000, 12000, 14000, 16000, 18000};
    private static final long[]  COOLDOWN_MS         = {28000, 25000, 23000, 21000, 18000};
    private static final float[] MANA_COST           = {16f, 18f, 20f, 22f, 24f};

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
        return "+" + bonus + "% dégâts subis (zone) " + dur + "s, " + mn + " mana, Délai " + cd + "s";
    }
}
