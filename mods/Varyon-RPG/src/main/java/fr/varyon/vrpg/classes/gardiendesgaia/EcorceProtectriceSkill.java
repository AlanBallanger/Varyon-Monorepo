package fr.varyon.vrpg.classes.gardiendesgaia;

public final class EcorceProtectriceSkill {

    public static final String SKILL_ID       = "ecorce_protectrice";
    public static final String TALENT_NODE_ID = "gardien_de_gaia_3";

    private static final float[] DAMAGE_REDUCTION = {0.08f, 0.10f, 0.12f, 0.14f, 0.16f};
    private static final float[] HEAL_PER_SEC     = {7f,    10f,   14f,   18f,   25f};
    private static final long[]  DURATION_MS      = {5000,  5000,  5000,  5000,  5000};
    private static final long[]  COOLDOWN_MS      = {45000, 40500, 37500, 33000, 30000};
    private static final float[] MANA_COST        = {15f,   16f,   18f,   20f,   22f};

    private EcorceProtectriceSkill() {}

    public static int   maxRank()                        { return COOLDOWN_MS.length; }
    private static int  idx(int rank)                    { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static float damageReductionForRank(int rank) { return DAMAGE_REDUCTION[idx(rank)]; }
    public static float healPerSecForRank(int rank)      { return HEAL_PER_SEC[idx(rank)]; }
    public static long  durationMsForRank(int rank)      { return DURATION_MS[idx(rank)]; }
    public static long  cooldownMsForRank(int rank)      { return COOLDOWN_MS[idx(rank)]; }
    public static float manaCostForRank(int rank)        { return MANA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int reduc = Math.round(damageReductionForRank(rank) * 100);
        float heal = healPerSecForRank(rank);
        int dur  = (int) (durationMsForRank(rank) / 1000);
        int cd   = (int) (cooldownMsForRank(rank) / 1000);
        int mana = Math.round(manaCostForRank(rank));
        return "-" + reduc + "% dégâts + " + heal + " PV/s, " + dur + "s, " + mana + " mana, Délai " + cd + "s";
    }
}
