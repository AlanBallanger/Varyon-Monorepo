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

    private static final int[] BASE_STAMINA = {
        100, 102, 104, 106, 108, 110, 113, 116, 119, 122,
        125, 128, 131, 134, 138, 142, 146, 150, 154, 158,
        162, 166, 170, 175, 180, 185, 190, 195, 200, 206
    };

    private static final double BASE_CRIT_CHANCE_PCT = 5.0;
    private static final double BASE_CRIT_DAMAGE_PCT = 50.0;

    private static int clamp(int level) {
        return Math.max(1, Math.min(30, level));
    }

    public static int baseHp(int level)      { return BASE_HP     [clamp(level) - 1]; }
    public static int baseAtk(int level)     { return BASE_ATK    [clamp(level) - 1]; }
    public static int baseStamina(int level) { return BASE_STAMINA[clamp(level) - 1]; }

    public static double armorPctForLevel(int level) {
        return (clamp(level) / 30.0) * 66.0;
    }

    public static ClassPlayerStats compute(int level, @Nullable PlayerSpecialization spec) {
        double hpMult      = spec != null ? spec.getHpMult()         : 1.0;
        double atkMult     = spec != null ? spec.getAtkMult()        : 1.0;
        double armorMult   = spec != null ? spec.getArmorMult()      : 1.0;
        double staminaMult = spec != null ? spec.getStaminaMult()    : 1.0;
        double critCMult   = spec != null ? spec.getCritChanceMult() : 1.0;
        double critDMult   = spec != null ? spec.getCritDamageMult() : 1.0;

        int hp      = (int) Math.round(baseHp(level)            * hpMult);
        int atk     = (int) Math.round(baseAtk(level)           * atkMult);
        int stamina = (int) Math.round(baseStamina(level)       * staminaMult);
        int armor   = (int) Math.round(armorPctForLevel(level)  * armorMult);
        int critC   = (int) Math.round(BASE_CRIT_CHANCE_PCT     * critCMult);
        int critD   = (int) Math.round(BASE_CRIT_DAMAGE_PCT     * critDMult);

        return new ClassPlayerStats(hp, atk, armor, stamina, critC, critD);
    }
}
