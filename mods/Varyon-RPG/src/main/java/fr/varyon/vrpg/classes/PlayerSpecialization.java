package fr.varyon.vrpg.classes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum PlayerSpecialization {
    //                                                                                                                                             hp    atk   armor stamina critC critD
    DUELLISTE("duelliste", "Duelliste",         PlayerClass.GUERRIER, "Weapon_Sword_Mithril",           "Mobilite - Ripostes - Critiques", "Expert des duels et des contre-attaques.",      0.95, 1.10, 0.95, 1.05, 1.20, 1.10),
    OMBRE    ("ombre",     "Ombre",             PlayerClass.GUERRIER, "Weapon_Daggers_Doomed",          "Furtivite - Poisons - Assassinat","Frappe dans l'ombre, insaisissable et mortel.", 0.85, 1.20, 0.80, 1.10, 1.15, 1.20),
    REMPART  ("rempart",   "Rempart",          PlayerClass.GUERRIER, "Weapon_Shield_Orbis_Knight",      "Tank - Bouclier - Protection",   "Maitre de la defense et de la resilience.",     1.15, 0.90, 1.20, 1.00, 0.75, 0.75),

    BERSERKER("berserker", "Berserker",         PlayerClass.BARBARE,  "Weapon_Adamantite_Axe",           "Rage - Vitesse - Frenetique",    "Plus il est blesse, plus il devient dangereux.", 1.05, 1.20, 0.85, 0.90, 1.10, 1.15),
    RAVAGEUR ("ravageur",  "Ravageur",          PlayerClass.BARBARE,  "Weapon_Mace_Mithril",             "Arme a deux mains - Executions - Degats","Frappes puissantes et degats massifs.", 1.00, 1.25, 0.90, 0.95, 0.90, 1.30),
    BAGARREUR("bagarreur", "Bagarreur",         PlayerClass.BARBARE,  "Armor_Cloth_Cindercloth_Hands",   "Corps a corps - Etourdissements - Tenace","Combat brutal a mains nues ou en melee.", 1.15, 1.00, 1.10, 1.05, 0.85, 0.90),

    ARCANISTE      ("arcaniste",       "Arcaniste",       PlayerClass.MAGE, "Weapon_Staff_Crystal_Flame",  "Magie - Sorts - Puissance",      "Lance des sorts devastateurs depuis la distance.", 0.80, 1.30, 0.75, 1.10, 1.00, 1.20),
    GARDIEN_DE_GAIA("gardien_de_gaia", "Gardien de Gaia", PlayerClass.MAGE, "Weapon_Staff_Wood",           "Nature - Soin - Invocation",     "Puise dans la nature pour soigner et invoquer.",  1.00, 0.85, 0.90, 1.25, 0.70, 0.70),
    VAUDOU         ("vaudou",          "Vaudou",          PlayerClass.MAGE, "Weapon_Spellbook_Frost",      "Maledictions - Debuffs - Zones", "Affaiblit les ennemis par des maledictions.",     0.85, 1.10, 0.85, 1.20, 0.90, 0.90),

    RODEUR     ("rodeur",      "Rodeur",      PlayerClass.TIREUR, "Weapon_Shortbow_Thorium",     "Arc - Traque - Mobilite",        "Traque ses proies avec agilite et precision.",      0.90, 1.15, 0.85, 1.10, 1.20, 1.10),
    ARBALETRIER("arbaletrier", "Arbaletrier", PlayerClass.TIREUR, "Weapon_Crossbow_Iron",        "Arbalete - Penetration - Lent",  "Tirs lents mais devastateurs a longue portee.",    0.95, 1.25, 0.80, 0.90, 0.90, 1.20),
    LANCIER    ("lancier",     "Lancier",     PlayerClass.TIREUR, "Weapon_Leaf_Spear",           "Lance - Zone - Charge",          "Combat avec une lance, efficace en zone et en charge.", 1.05, 1.10, 1.00, 1.05, 1.00, 1.00);

    private final String id;
    private final String displayName;
    private final PlayerClass parentClass;
    private final String itemId;
    private final String keywords;
    private final String description;
    private final double hpMult;
    private final double atkMult;
    private final double armorMult;
    private final double staminaMult;
    private final double critChanceMult;
    private final double critDamageMult;

    PlayerSpecialization(@Nonnull String id,
                         @Nonnull String displayName,
                         @Nonnull PlayerClass parentClass,
                         @Nonnull String itemId,
                         @Nonnull String keywords,
                         @Nonnull String description,
                         double hpMult,
                         double atkMult,
                         double armorMult,
                         double staminaMult,
                         double critChanceMult,
                         double critDamageMult) {
        this.id = id;
        this.displayName = displayName;
        this.parentClass = parentClass;
        this.itemId = itemId;
        this.keywords = keywords;
        this.description = description;
        this.hpMult = hpMult;
        this.atkMult = atkMult;
        this.armorMult = armorMult;
        this.staminaMult = staminaMult;
        this.critChanceMult = critChanceMult;
        this.critDamageMult = critDamageMult;
    }

    @Nonnull public String getId()             { return id; }
    @Nonnull public String getDisplayName()    { return displayName; }
    @Nonnull public PlayerClass getParentClass() { return parentClass; }
    @Nonnull public String getItemId()          { return itemId; }
    @Nonnull public String getKeywords()       { return keywords; }
    @Nonnull public String getDescription()    { return description; }
    public double getHpMult()         { return hpMult; }
    public double getAtkMult()        { return atkMult; }
    public double getArmorMult()      { return armorMult; }
    public double getStaminaMult()    { return staminaMult; }
    public double getCritChanceMult() { return critChanceMult; }
    public double getCritDamageMult() { return critDamageMult; }

    private static final Map<String, PlayerSpecialization> BY_ID =
        Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(PlayerSpecialization::getId, s -> s));

    @Nullable
    public static PlayerSpecialization fromId(@Nullable String id) {
        return id == null ? null : BY_ID.get(id);
    }
}
