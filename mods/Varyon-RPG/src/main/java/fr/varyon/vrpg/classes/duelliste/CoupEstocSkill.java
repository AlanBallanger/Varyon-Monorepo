package fr.varyon.vrpg.classes.duelliste;

public final class CoupEstocSkill {

    public static final String SKILL_ID       = "coup_estoc";
    public static final String TALENT_NODE_ID = "3";

    // Cast AoE damage : facteur × dégâts arme tenue
    private static final float[] CAST_DAMAGE_FACTOR = {1.2f, 1.6f, 2.0f, 2.6f, 3.2f};
    private static final double  CAST_RADIUS   = 2.5;

    // Prochain coup boosté
    private static final float[] NEXT_HIT_MULT = {1.5f, 1.75f, 2.0f, 2.25f, 2.5f};
    private static final long    ARMED_WINDOW  = 4_000L;

    private static final long[]  COOLDOWN_MS   = {8000, 7500, 7000, 6500, 6000};

    private CoupEstocSkill() {}

    public static int maxRank() { return CAST_DAMAGE_FACTOR.length; }

    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, CAST_DAMAGE_FACTOR.length - 1)); }

    public static float castDamageForRank(int rank, com.hypixel.hytale.server.core.universe.PlayerRef playerRef) {
        int weaponDmg = fr.varyon.vrpg.classes.WeaponDamageReader.readHeldWeaponDamage(playerRef);
        int base = weaponDmg > 0 ? weaponDmg : 1;
        return base * CAST_DAMAGE_FACTOR[idx(rank)];
    }
    public static double castRadius()                  { return CAST_RADIUS; }
    public static float  nextHitMultForRank(int rank)  { return NEXT_HIT_MULT[idx(rank)]; }
    public static long   armedWindowMs()               { return ARMED_WINDOW; }
    public static long   cooldownMsForRank(int rank)   { return COOLDOWN_MS[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int pct  = Math.round(CAST_DAMAGE_FACTOR[idx(rank)] * 100);
        int mult = Math.round((nextHitMultForRank(rank) - 1) * 100);
        int cd   = (int) (cooldownMsForRank(rank) / 1000);
        return pct + "% dégâts arme AoE + prochain coup +" + mult + "% (4s), CD " + cd + "s";
    }
}
