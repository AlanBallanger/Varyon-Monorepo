package fr.varyon.vrpg.classes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum PlayerSpecialization {
    REMPART  ("rempart",   "Rempart",          PlayerClass.GUERRIER, "Jobs_Icons/Shield.png",    "Tank - Bouclier - Protection",  "Maitre de la defense et de la resilience."),
    DUELLISTE("duelliste", "Duelliste",         PlayerClass.GUERRIER, "Jobs_Icons/Sword.png",     "Mobilite - Ripostes - Critiques","Expert des duels et des contre-attaques."),
    OMBRE    ("ombre",     "Ombre",             PlayerClass.GUERRIER, "Jobs_Icons/Shadow.png",    "Furtivite - Poisons - Assassinat","Frappe dans l'ombre, insaisissable et mortel."),

    BERSERKER("berserker", "Berserker",         PlayerClass.BARBARE,  "Jobs_Icons/Axe.png",       "Rage - Vitesse - Frenetique",   "Plus il est blesse, plus il devient dangereux."),
    RAVAGEUR ("ravageur",  "Ravageur",          PlayerClass.BARBARE,  "Jobs_Icons/DoubleAxe.png", "Arme a deux mains - Executions - Degats","Frappes puissantes et degats massifs."),
    BAGARREUR("bagarreur", "Bagarreur",         PlayerClass.BARBARE,  "Jobs_Icons/Fist.png",      "Corps a corps - Etourdissements - Tenace","Combat brutal a mains nues ou en melee."),

    ARCANISTE      ("arcaniste",       "Arcaniste",       PlayerClass.MAGE, "Jobs_Icons/Staff.png",    "Magie - Sorts - Puissance",     "Lance des sorts devastateurs depuis la distance."),
    GARDIEN_DE_GAIA("gardien_de_gaia", "Gardien de Gaia", PlayerClass.MAGE, "Jobs_Icons/Nature.png",   "Nature - Soin - Invocation",    "Puise dans la nature pour soigner et invoquer."),
    VAUDOU         ("vaudou",          "Vaudou",          PlayerClass.MAGE, "Jobs_Icons/Voodoo.png",   "Maledictions - Debuffs - Zones","Affaiblit les ennemis par des maledictions."),

    RODEUR     ("rodeur",      "Rodeur",      PlayerClass.TIREUR, "Jobs_Icons/Bow.png",       "Arc - Traque - Mobilite",       "Traque ses proies avec agilite et precision."),
    ARBALETRIER("arbaletrier", "Arbaletrier", PlayerClass.TIREUR, "Jobs_Icons/Crossbow.png",  "Arbalete - Penetration - Lent", "Tirs lents mais devastateurs a longue portee."),
    LANCIER    ("lancier",     "Lancier",     PlayerClass.TIREUR, "Jobs_Icons/Spear.png",     "Lance - Zone - Charge",         "Combat avec une lance, efficace en zone et en charge.");

    private final String id;
    private final String displayName;
    private final PlayerClass parentClass;
    private final String iconPath;
    private final String keywords;
    private final String description;

    PlayerSpecialization(@Nonnull String id,
                         @Nonnull String displayName,
                         @Nonnull PlayerClass parentClass,
                         @Nonnull String iconPath,
                         @Nonnull String keywords,
                         @Nonnull String description) {
        this.id = id;
        this.displayName = displayName;
        this.parentClass = parentClass;
        this.iconPath = iconPath;
        this.keywords = keywords;
        this.description = description;
    }

    @Nonnull public String getId()             { return id; }
    @Nonnull public String getDisplayName()    { return displayName; }
    @Nonnull public PlayerClass getParentClass() { return parentClass; }
    @Nonnull public String getIconPath()       { return iconPath; }
    @Nonnull public String getKeywords()       { return keywords; }
    @Nonnull public String getDescription()    { return description; }

    private static final Map<String, PlayerSpecialization> BY_ID =
        Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(PlayerSpecialization::getId, s -> s));

    @Nullable
    public static PlayerSpecialization fromId(@Nullable String id) {
        return id == null ? null : BY_ID.get(id);
    }
}
