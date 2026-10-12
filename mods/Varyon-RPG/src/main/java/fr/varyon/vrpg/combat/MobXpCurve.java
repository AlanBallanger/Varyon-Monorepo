package fr.varyon.vrpg.combat;

/**
 * Class XP awarded for killing a mob, scaled by the mob's level (1-100).
 *
 * Curve through the anchor points (1, 15), (10, 28), (56, 190), (100, 650):
 *   xp(l) = BASE + LINEAR * l + POWER_COEF * l^POWER_EXP
 * Constants rounded from a least-squares fit; they match the exact fit within +/-1 XP.
 *
 * The high end is deliberately steep (a level-100 mob is worth ~31x a level-5 one) so that
 * farming low-level mobs once outleveled is never worth it.
 *
 * A level-0 (unscaled) mob falls back to the level-1 value.
 */
public final class MobXpCurve {

    private static double base = 14.0;
    private static double linear = 1.3;
    private static double powerCoef = 0.0016;
    private static double powerExp = 2.75;

    private MobXpCurve() {}

    public static void configure(double base, double linear, double powerCoef, double powerExp) {
        MobXpCurve.base = base;
        MobXpCurve.linear = linear;
        MobXpCurve.powerCoef = powerCoef;
        MobXpCurve.powerExp = powerExp;
    }

    public static double xpForMobLevel(int mobLevel) {
        int l = mobLevel < 1 ? 1 : mobLevel;
        double xp = base + linear * l + powerCoef * Math.pow(l, powerExp);
        return Math.max(1.0, Math.round(xp));
    }
}
