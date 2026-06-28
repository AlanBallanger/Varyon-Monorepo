package fr.varyon.vrpg.classes.vaudou;

public final class FleauToxiqueSkill {

    public static final String SKILL_ID       = "fleau_toxique";
    public static final String TALENT_NODE_ID = "vaudou_2";

    public static final long   BASE_POISON_DURATION_MS = 5_000L;
    private static final float[] POISON_WEAPON_PCT = {0.64f, 0.84f, 1.04f, 1.30f, 1.60f};
    private static final float[] DAMAGE_REDUCE     = {0.06f, 0.08f, 0.10f, 0.12f, 0.15f};
    private static final long[]  COOLDOWN_MS       = {18000, 17000, 16000, 15000, 14000};
    private static final float[] MANA_COST         = {10f, 12f, 12f, 14f, 16f};

    private FleauToxiqueSkill() {}

    public static int maxRank() { return COOLDOWN_MS.length; }

    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static float  poisonWeaponPctForRank(int rank) { return POISON_WEAPON_PCT[idx(rank)]; }
    public static float  damageReduceForRank(int rank)    { return DAMAGE_REDUCE[idx(rank)]; }
    public static long   cooldownMsForRank(int rank)      { return COOLDOWN_MS[idx(rank)]; }
    public static float  manaCostForRank(int rank)        { return MANA_COST[idx(rank)]; }

    public static long   poisonDurationMsForRank(int rank) { return BASE_POISON_DURATION_MS; }

    public static String statLineForRank(int rank) {
        int pct = Math.round(poisonWeaponPctForRank(rank) * 100);
        int red = Math.round(damageReduceForRank(rank) * 100);
        int cd  = (int) (cooldownMsForRank(rank) / 1000);
        int mn  = Math.round(manaCostForRank(rank));
        return pct + "% arme/s (5s poison), -" + red + "% dégâts cible, " + mn + " mana, Délai " + cd + "s";
    }
}
