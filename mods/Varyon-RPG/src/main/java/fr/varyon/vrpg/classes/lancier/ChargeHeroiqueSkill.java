package fr.varyon.vrpg.classes.lancier;

import com.hypixel.hytale.server.core.universe.PlayerRef;

public final class ChargeHeroiqueSkill {

    public static final String SKILL_ID       = "charge_heroique";
    public static final String TALENT_NODE_ID = "lancier_2";

    private static final float[] DAMAGE_FACTOR  = {0.53f, 0.67f, 0.83f, 1.0f, 1.27f};
    private static final long[]  STUN_MS        = {1200, 1500, 1800, 2100, 2500};
    private static final long[]  COOLDOWN_MS    = {22000, 20000, 18000, 16000, 14000};
    private static final float[] STAMINA_COST   = {8f, 8f, 9f, 9f, 10f};
    private static final double  CHARGE_DISTANCE = 6.0;
    private static final double  HIT_RADIUS      = 1.5;

    private ChargeHeroiqueSkill() {}

    public static int maxRank() { return DAMAGE_FACTOR.length; }

    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, DAMAGE_FACTOR.length - 1)); }

    public static float  damageFactor(int rank)       { return DAMAGE_FACTOR[idx(rank)]; }
    public static long   stunMsForRank(int rank)      { return STUN_MS[idx(rank)]; }
    public static long   cooldownMsForRank(int rank)  { return COOLDOWN_MS[idx(rank)]; }
    public static float  staminaCostForRank(int rank) { return STAMINA_COST[idx(rank)]; }
    public static double chargeDistance()             { return CHARGE_DISTANCE; }
    public static double hitRadius()                  { return HIT_RADIUS; }

    public static float computeDamage(int rank, @javax.annotation.Nonnull PlayerRef playerRef) {
        int weaponDmg = fr.varyon.vrpg.classes.WeaponDamageReader.readHeldWeaponDamage(playerRef);
        int base = weaponDmg > 0 ? weaponDmg : 1;
        return base * damageFactor(rank);
    }

    public static String statLineForRank(int rank) {
        int dmg = Math.round(damageFactor(rank) * 100);
        int cd = (int) (cooldownMsForRank(rank) / 1000);
        return dmg + "% dégâts, Délai " + cd + "s";
    }
}
