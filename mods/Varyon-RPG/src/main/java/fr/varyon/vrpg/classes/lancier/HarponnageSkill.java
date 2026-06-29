package fr.varyon.vrpg.classes.lancier;

import com.hypixel.hytale.server.core.universe.PlayerRef;

public final class HarponnageSkill {

    public static final String SKILL_ID       = "harponnage";
    public static final String TALENT_NODE_ID = "lancier_4";

    private static final float[] DAMAGE_FACTOR = {0.47f, 0.57f, 0.70f, 0.87f, 1.07f};
    private static final double  RANGE         = 16.0;
    private static final long[]  COOLDOWN_MS   = {18000, 16000, 14000, 12000, 10000};
    private static final float[] STAMINA_COST  = {8f, 8f, 9f, 9f, 10f};
    private static final double  PULL_STRENGTH = 55.0;

    private HarponnageSkill() {}

    public static int maxRank() { return DAMAGE_FACTOR.length; }

    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, DAMAGE_FACTOR.length - 1)); }

    public static float  damageFactor(int rank)       { return DAMAGE_FACTOR[idx(rank)]; }
    public static double range()                      { return RANGE; }
    public static long   cooldownMsForRank(int rank)  { return COOLDOWN_MS[idx(rank)]; }
    public static float  staminaCostForRank(int rank) { return STAMINA_COST[idx(rank)]; }
    public static double pullStrength()               { return PULL_STRENGTH; }

    public static float computeDamage(int rank, @javax.annotation.Nonnull PlayerRef playerRef) {
        int weaponDmg = fr.varyon.vrpg.classes.WeaponDamageReader.readHeldWeaponDamage(playerRef);
        int base = weaponDmg > 0 ? weaponDmg : 1;
        return base * damageFactor(rank);
    }

    public static String statLineForRank(int rank) {
        int dmg = Math.round(damageFactor(rank) * 100);
        int cd  = (int)(cooldownMsForRank(rank) / 1000);
        return dmg + "% dégâts, attire la cible, Délai " + cd + "s";
    }
}
