package fr.varyon.vrpg.classes;

import javax.annotation.Nullable;

public final class ClassStatDefinition {

    private ClassStatDefinition() {}

    private static final int[] BASE_HP = {
        100, 105, 110, 115, 120, 126, 132, 138, 144, 150,
        157, 164, 171, 178, 186, 194, 202, 210, 218, 227,
        236, 245, 254, 264, 274, 284, 294, 305, 316, 328
    };

    private static final int[] BASE_ATK = {
        10, 11, 12, 13, 14, 15, 16, 17, 18, 20,
        21, 22, 23, 24, 26, 27, 28, 30, 31, 33,
        34, 36, 37, 39, 41, 42, 44, 46, 48, 50
    };

    private static final double LEVEL_EASE       = 1.3;
    private static final double ATK_MAX_LVL30   = 3.5;
    private static final double CRIT_DMG_LVL1   = 120.0;
    private static final double CRIT_DMG_RANGE  = 80.0;

    private static int clamp(int level) {
        return Math.max(1, Math.min(30, level));
    }

    public static int baseHp(int level) {
        return BASE_HP[clamp(level) - 1];
    }

    public static double hpLevelMult(int level) {
        int L = clamp(level);
        double m;
        if (L <= 10) m = 1.0 + (L - 1) / 9.0;
        else if (L <= 20) m = 2.0 + (L - 10) / 10.0;
        else m = 3.0 + (L - 20) / 10.0;
        return 1.0 + (m - 1.0) * (2.0 / 3.0);
    }

    public static int baseAtk(int level) {
        return BASE_ATK[clamp(level) - 1];
    }

    public static int baseStamina(int level) {
        int L = clamp(level);
        if (L <= 10) return (int) Math.round(10.0 + (L - 1) * (20.0 / 9.0));
        if (L <= 20) return 30 + (L - 10) * 2;
        return 50 + (L - 20) * 2;
    }

    public static double armorPctForLevel(int level) {
        return (clamp(level) / 30.0) * 66.0;
    }

    public static double atkLevelMultiplier(int level) {
        int L = clamp(level);
        double t = (L - 1) / 29.0;
        return 1.0 + (ATK_MAX_LVL30 - 1.0) * Math.pow(t, LEVEL_EASE);
    }

    public static double atkDisplayMultiplier(int level, @Nullable PlayerSpecialization spec) {
        double specMult = spec != null ? spec.getAtkMult() : 1.0;
        double levelAtk = atkLevelMultiplier(level);
        return (1.0 + (levelAtk - 1.0) * specMult);
    }

    public static double critChancePctForLevel(int level) {
        int L = clamp(level);
        return (L - 1) / 29.0 * 50.0;
    }

    public static double critDamagePctForLevel(int level) {
        int L = clamp(level);
        double t = (L - 1) / 29.0;
        return CRIT_DMG_LVL1 + CRIT_DMG_RANGE * Math.pow(t, LEVEL_EASE);
    }

    public static ClassPlayerStats compute(int level, @Nullable PlayerSpecialization spec) {
        double hpMult      = spec != null ? spec.getHpMult()         : 1.0;
        double atkMult     = spec != null ? spec.getAtkMult()        : 1.0;
        double armorMult   = spec != null ? spec.getArmorMult()      : 1.0;
        double staminaMult = spec != null ? spec.getStaminaMult()    : 1.0;
        double critCMult   = spec != null ? spec.getCritChanceMult() : 1.0;
        double critDMult   = spec != null ? spec.getCritDamageMult() : 1.0;

        double combinedHpMult = hpLevelMult(level) * hpMult;

        int hp      = (int) Math.round(BASE_HP[clamp(level) - 1]    * combinedHpMult);
        int atk     = (int) Math.round(baseAtk(level)               * atkMult);
        int stamina = (int) Math.round(baseStamina(level)           * staminaMult);
        int armor   = (int) Math.round(armorPctForLevel(level)      * armorMult);
        int critC   = (int) Math.round(critChancePctForLevel(level)  * critCMult);
        int critD   = (int) Math.round(critDamagePctForLevel(level)  * critDMult);

        return new ClassPlayerStats(hp, atk, armor, stamina, critC, critD, combinedHpMult);
    }
}
