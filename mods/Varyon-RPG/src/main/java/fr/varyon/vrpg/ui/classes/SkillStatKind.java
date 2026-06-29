package fr.varyon.vrpg.ui.classes;

public enum SkillStatKind {
    WEAPON_DAMAGE("Icons/attack.png", "Dégâts"),
    DAMAGE_BONUS("Icons/attack.png", "Dégâts"),
    COOLDOWN("Icons/cooldown.png", "Délai"),
    STAMINA("Icons/stamina.png", "Endu."),
    MANA("Icons/mana.png", "Mana"),
    DURATION("Icons/time.png", "Durée"),
    MOVE_SPEED("Icons/movement_speed.png", "Vit."),
    RANGE("Icons/range.png", "Portée"),
    DODGE("Icons/dodge.png", "Esquive"),
    XP("Icons/xp2.png", "XP"),
    RATE("Icons/rate.png", "Chance"),
    HEAL("Icons/health.png", "PV"),
    DEFENSE("Icons/defense.png", "Réduc.");

    private final String iconPath;
    private final String defaultLabel;

    SkillStatKind(String iconPath, String defaultLabel) {
        this.iconPath = iconPath;
        this.defaultLabel = defaultLabel;
    }

    public String iconPath() {
        return iconPath;
    }

    public String defaultLabel() {
        return defaultLabel;
    }
}
