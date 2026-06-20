package fr.varyon.vrpg.classes.lancier;

import com.hypixel.hytale.server.core.universe.PlayerRef;

public final class EmpalementSkill {

    public static final String SKILL_ID       = "empalement";
    public static final String TALENT_NODE_ID = "lancier_11";

    private static final float[] DAMAGE_FACTOR    = {2.0f, 2.5f, 3.0f, 3.7f, 4.5f};
    private static final float[] BLEED_PCT_PER_S  = {0.25f, 0.30f, 0.35f, 0.40f, 0.50f};
    private static final long[]  BLEED_DURATION_MS = {4000, 4000, 5000, 5000, 6000};
    private static final long[]  ROOT_DURATION_MS  = {1500, 1800, 2100, 2400, 2800};
    private static final long[]  COOLDOWN_MS       = {22000, 20000, 18000, 16000, 14000};
    private static final float[] STAMINA_COST      = {7f, 7f, 8f, 8f, 9f};
    private static final double  RANGE             = 4.0;

    private EmpalementSkill() {}

    public static int maxRank() { return DAMAGE_FACTOR.length; }

    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, DAMAGE_FACTOR.length - 1)); }

    public static float  damageFactor(int rank)          { return DAMAGE_FACTOR[idx(rank)]; }
    public static float  bleedPctPerSForRank(int rank)   { return BLEED_PCT_PER_S[idx(rank)]; }
    public static long   bleedDurationMsForRank(int rank){ return BLEED_DURATION_MS[idx(rank)]; }
    public static long   rootDurationMsForRank(int rank) { return ROOT_DURATION_MS[idx(rank)]; }
    public static long   cooldownMsForRank(int rank)     { return COOLDOWN_MS[idx(rank)]; }
    public static float  staminaCostForRank(int rank)    { return STAMINA_COST[idx(rank)]; }
    public static double range()                         { return RANGE; }

    public static float computeDamage(int rank, @javax.annotation.Nonnull PlayerRef playerRef) {
        int weaponDmg = fr.varyon.vrpg.classes.WeaponDamageReader.readHeldWeaponDamage(playerRef);
        int base = weaponDmg > 0 ? weaponDmg : 1;
        return base * damageFactor(rank);
    }

    public static String statLineForRank(int rank) {
        int dmg    = Math.round(damageFactor(rank) * 100);
        int bleedPct = Math.round(bleedPctPerSForRank(rank) * 100);
        int bleedDur = (int)(bleedDurationMsForRank(rank) / 1000);
        int root   = (int)(rootDurationMsForRank(rank) / 100) * 100 / 1000;
        int cd     = (int)(cooldownMsForRank(rank) / 1000);
        return dmg + "% dégâts, saignement " + bleedPct + "%/s " + bleedDur + "s, immob. " + root + "s, CD " + cd + "s";
    }
}
