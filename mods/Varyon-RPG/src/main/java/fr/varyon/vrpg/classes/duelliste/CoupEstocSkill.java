package fr.varyon.vrpg.classes.duelliste;

public final class CoupEstocSkill {

    public static final String SKILL_ID       = "coup_estoc";
    public static final String TALENT_NODE_ID = "duelliste_3";

    private static final float[] CAST_DAMAGE_FACTOR = {1.0f, 1.35f, 1.7f, 2.1f, 2.5f};
    private static final double  CAST_RADIUS   = 2.5;

    private static final float[] NEXT_HIT_MULT = {1.35f, 1.55f, 1.75f, 1.95f, 2.15f};
    private static final long    ARMED_WINDOW  = 4_000L;

    private static final long[]  COOLDOWN_MS   = {12000, 11000, 10000, 9500, 9000};
    private static final float[] STAMINA_COST  = {8f, 9f, 10f, 11f, 12f};

    private CoupEstocSkill() {}

    public static int maxRank() { return CAST_DAMAGE_FACTOR.length; }

    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, CAST_DAMAGE_FACTOR.length - 1)); }

    public static float castDamageFactorForRank(int rank) { return CAST_DAMAGE_FACTOR[idx(rank)]; }

    public static float castDamageForRank(int rank, com.hypixel.hytale.server.core.universe.PlayerRef playerRef) {
        int weaponDmg = fr.varyon.vrpg.classes.WeaponDamageReader.readHeldWeaponDamage(playerRef);
        int base = weaponDmg > 0 ? weaponDmg : 1;
        return base * CAST_DAMAGE_FACTOR[idx(rank)];
    }

    public static double castRadius()                  { return CAST_RADIUS; }
    public static float  nextHitMultForRank(int rank)  { return NEXT_HIT_MULT[idx(rank)]; }
    public static long   armedWindowMs()               { return ARMED_WINDOW; }
    public static long   cooldownMsForRank(int rank)   { return COOLDOWN_MS[idx(rank)]; }
    public static float  staminaCostForRank(int rank)  { return STAMINA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int pct     = Math.round(CAST_DAMAGE_FACTOR[idx(rank)] * 100);
        int mult    = Math.round((nextHitMultForRank(rank) - 1) * 100);
        int stamina = Math.round(staminaCostForRank(rank));
        int cd      = (int) (cooldownMsForRank(rank) / 1000);
        return pct + "% dégâts arme AoE + prochain coup +" + mult + "% (4s), "
            + stamina + " endurance, Délai " + cd + "s";
    }
}
