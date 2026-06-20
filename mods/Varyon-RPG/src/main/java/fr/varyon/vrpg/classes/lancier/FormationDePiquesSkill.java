package fr.varyon.vrpg.classes.lancier;

import com.hypixel.hytale.server.core.universe.PlayerRef;

public final class FormationDePiquesSkill {

    public static final String SKILL_ID       = "formation_de_piques";
    public static final String TALENT_NODE_ID = "lancier_5";

    private static final float[] DAMAGE_FACTOR  = {0.5f, 0.65f, 0.80f, 1.0f, 1.25f};
    private static final float[] SLOW_FACTOR    = {0.35f, 0.40f, 0.45f, 0.50f, 0.55f};
    private static final long[]  DURATION_MS    = {5000, 5000, 6000, 6000, 7000};
    private static final double[] ZONE_WIDTH    = {3.0, 3.5, 4.0, 4.5, 5.0};
    private static final double  ZONE_DEPTH     = 6.0;
    private static final long[]  COOLDOWN_MS    = {24000, 22000, 20000, 18000, 16000};
    private static final float[] STAMINA_COST   = {7f, 7f, 8f, 8f, 9f};
    private static final long    TICK_INTERVAL_MS = 1000L;

    private FormationDePiquesSkill() {}

    public static int maxRank() { return DAMAGE_FACTOR.length; }

    private static int idx(int rank) { return Math.max(0, Math.min(rank - 1, DAMAGE_FACTOR.length - 1)); }

    public static float  damageFactor(int rank)       { return DAMAGE_FACTOR[idx(rank)]; }
    public static float  slowFactor(int rank)         { return SLOW_FACTOR[idx(rank)]; }
    public static long   durationMsForRank(int rank)  { return DURATION_MS[idx(rank)]; }
    public static double zoneWidthForRank(int rank)   { return ZONE_WIDTH[idx(rank)]; }
    public static double zoneDepth()                  { return ZONE_DEPTH; }
    public static long   cooldownMsForRank(int rank)  { return COOLDOWN_MS[idx(rank)]; }
    public static float  staminaCostForRank(int rank) { return STAMINA_COST[idx(rank)]; }
    public static long   tickIntervalMs()             { return TICK_INTERVAL_MS; }

    public static float computeDamagePerTick(int rank, @javax.annotation.Nonnull PlayerRef playerRef) {
        int weaponDmg = fr.varyon.vrpg.classes.WeaponDamageReader.readHeldWeaponDamage(playerRef);
        int base = weaponDmg > 0 ? weaponDmg : 1;
        return base * damageFactor(rank);
    }

    public static String statLineForRank(int rank) {
        int dmg  = Math.round(damageFactor(rank) * 100);
        int slow = Math.round(slowFactor(rank) * 100);
        int dur  = (int)(durationMsForRank(rank) / 1000);
        int cd   = (int)(cooldownMsForRank(rank) / 1000);
        return dmg + "% dégâts/s, -" + slow + "% vitesse, " + dur + "s, CD " + cd + "s";
    }
}
