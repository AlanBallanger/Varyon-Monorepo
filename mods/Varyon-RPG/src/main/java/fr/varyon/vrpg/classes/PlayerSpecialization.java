package fr.varyon.vrpg.classes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public enum PlayerSpecialization {
    //                                                                                                                                             hp    atk   armor stamina critC critD  epee  dague hache 2main lance dist  magie
    DUELLISTE("duelliste", "Duelliste",         PlayerClass.GUERRIER, "Weapon_Sword_Mithril",           "Mobilité - Ripostes - Critiques", "Expert des duels et des contre-attaques.",       0.95, 1.10, 0.90, 1.00, 1.05, 1.10,  1.50, 1.00, 1.00, 1.00, 0.75, 1.00, 0.75),
    OMBRE    ("ombre",     "Ombre",             PlayerClass.GUERRIER, "Weapon_Daggers_Doomed",          "Furtivité - Poisons - Assassinat","Frappe dans l'ombre, insaisissable et mortel.",  0.90, 1.15, 0.80, 1.10, 1.15, 1.20,  1.00, 1.50, 1.00, 0.75, 0.75, 1.00, 1.00),
    REMPART  ("rempart",   "Rempart",           PlayerClass.GUERRIER, "Weapon_Shield_Orbis_Knight",     "Tank - Bouclier - Protection",   "Maître de la défense et de la résilience.",      1.20, 0.85, 1.15, 1.10, 0.75, 0.75,  1.25, 0.75, 1.25, 1.00, 0.75, 1.00, 1.00),

    BERSERKER("berserker", "Berserker",         PlayerClass.BARBARE,  "Weapon_Axe_Adamantite",          "Rage - Vitesse - Frénétique",    "Plus il est blessé, plus il devient dangereux.", 1.05, 1.25, 0.85, 0.90, 1.05, 1.15,  1.00, 0.75, 1.50, 1.00, 0.75, 1.00, 0.75),
    RAVAGEUR ("ravageur",  "Ravageur",          PlayerClass.BARBARE,  "Weapon_Longsword_Adamantite",    "Arme à deux mains - Exécutions - Dégâts","Frappes puissantes et dégâts massifs.", 1.10, 1.30, 1.00, 1.10, 0.90, 0.90,  1.00, 0.75, 1.00, 1.50, 0.75, 1.00, 0.75),
    BAGARREUR("bagarreur", "Bagarreur",         PlayerClass.BARBARE,  "Weapon_Knuckles_Iron",           "Gants de combat - Étourdissements - Tenace","Combat brutal au poing américain.", 1.15, 1.50, 1.10, 0.90, 0.80, 0.50,  0.10, 0.10, 0.10, 0.10, 0.10, 0.10, 0.10),

    ARCANISTE      ("arcaniste",       "Arcaniste",       PlayerClass.MAGE, "Weapon_Staff_Crystal_Flame",  "Magie - Sorts - Puissance",      "Lance des sorts dévastateurs depuis la distance.", 0.90, 1.20, 0.85, 0.85, 0.90, 1.05,  1.00, 1.00, 1.00, 0.75, 0.75, 1.00, 1.50),
    GARDIEN_DE_GAIA("gardien_de_gaia", "Gardien de Gaïa", PlayerClass.MAGE, "Weapon_Staff_Wood",           "Nature - Soin - Invocation",     "Puise dans la nature pour soigner et invoquer.",   0.90, 0.65, 0.90, 1.25, 0.70, 0.70,  1.00, 1.00, 1.00, 0.75, 0.75, 1.00, 1.50),
    VAUDOU         ("vaudou",          "Vaudou",          PlayerClass.MAGE, "Weapon_Spellbook_Frost",      "Malédictions - Debuffs - Zones", "Affaiblit les ennemis par des malédictions.",      1.10, 0.85, 1.15, 1.20, 0.90, 0.90,  1.00, 0.75, 1.00, 1.00, 0.75, 1.00, 1.50),

    RODEUR     ("rodeur",      "Rodeur",      PlayerClass.TIREUR, "Weapon_Shortbow_Thorium",     "Arc - Traque - Mobilité",        "Traque ses proies avec agilité et précision.",      0.90, 1.15, 0.85, 1.30, 1.05, 1.10,  1.00, 1.00, 1.00, 0.75, 0.75, 1.50, 0.75),
    ARBALETRIER("arbaletrier", "Arbalétrier", PlayerClass.TIREUR, "Weapon_Crossbow_Iron",        "Arbalète - Pénétration - Lent",  "Tirs lents mais dévastateurs à longue portée.",    0.90, 1.05, 0.85, 1.25, 1.15, 1.20,  1.00, 1.00, 1.00, 0.75, 0.75, 1.50, 0.75),
    LANCIER    ("lancier",     "Lancier",     PlayerClass.TIREUR, "Weapon_Spear_Iron",           "Lance - Zone - Charge",          "Combat avec une lance, efficace en zone et en charge.", 1.15, 1.15, 1.00, 1.15, 1.10, 0.90,  1.00, 1.00, 1.00, 0.75, 1.50, 1.00, 0.75);

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
    private final double weaponEpeeMult;
    private final double weaponDagueMult;
    private final double weaponHacheMult;
    private final double weaponDeuxMainsMult;
    private final double weaponLanceMult;
    private final double weaponDistanceMult;
    private final double weaponMagieMult;

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
                         double critDamageMult,
                         double weaponEpeeMult,
                         double weaponDagueMult,
                         double weaponHacheMult,
                         double weaponDeuxMainsMult,
                         double weaponLanceMult,
                         double weaponDistanceMult,
                         double weaponMagieMult) {
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
        this.weaponEpeeMult = weaponEpeeMult;
        this.weaponDagueMult = weaponDagueMult;
        this.weaponHacheMult = weaponHacheMult;
        this.weaponDeuxMainsMult = weaponDeuxMainsMult;
        this.weaponLanceMult = weaponLanceMult;
        this.weaponDistanceMult = weaponDistanceMult;
        this.weaponMagieMult = weaponMagieMult;
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
    public double getWeaponEpeeMult()       { return weaponEpeeMult; }
    public double getWeaponDagueMult()      { return weaponDagueMult; }
    public double getWeaponHacheMult()      { return weaponHacheMult; }
    public double getWeaponDeuxMainsMult()  { return weaponDeuxMainsMult; }
    public double getWeaponLanceMult()      { return weaponLanceMult; }
    public double getWeaponDistanceMult()   { return weaponDistanceMult; }
    public double getWeaponMagieMult()      { return weaponMagieMult; }

    private static final Map<String, PlayerSpecialization> BY_ID =
        Arrays.stream(values()).collect(Collectors.toUnmodifiableMap(PlayerSpecialization::getId, s -> s));

    @Nullable
    public static PlayerSpecialization fromId(@Nullable String id) {
        return id == null ? null : BY_ID.get(id);
    }
}
