package fr.varyon.vrpg.ui.profession;

import fr.varyon.vrpg.rpg.Profession;

public final class ProfessionBonusData {

    public static final String[] NODE_IDS = {"bonus_0", "bonus_1", "bonus_2"};
    public static final String TALENT_SOUND_ICON_ON = "Elements/Sound.png";
    public static final String TALENT_SOUND_ICON_OFF = "Elements/No_Sound.png";

    private static final String[] MINEUR_NAMES = {"Sang Bouillonnant", "Extracteur des Profondeurs", "Impulsion Souterraine"};
    private static final String[] MINEUR_DESCS = {
        "Résiste aux flammes et à la lave.",
        "Les profondeurs te protègent.",
        "L'immobilité forge l'élan."
    };
    private static final String[] MINEUR_STATS = {"-30% dégâts de feu / lave", "-15% dégâts reçus sous la couche 100", "+30% vitesse après 3s immobile"};
    private static final String[] MINEUR_ICONS = {"Jobs_Icons/Lava_Damage.png", "Jobs_Icons/Deep_Mining.png", "Jobs_Icons/Movement_Speed.png"};

    private static final String[] FORESTIER_NAMES = {"Bras de Bûcheron", "Enfant de la Forêt", "Vigueur Sylvestre"};
    private static final String[] FORESTIER_DESCS = {
        "L'abattage libère l'énergie.",
        "La forêt te berce.",
        "La sève te nourrit."
    };
    private static final String[] FORESTIER_STATS = {"+30% vitesse pendant 10s après un abattage", "-15% dégâts reçus en zone 1 & 2", "+15% endurance maximale"};
    private static final String[] FORESTIER_ICONS = {"Jobs_Icons/Movement_Speed.png", "Jobs_Icons/Green_Shield.png", "Jobs_Icons/Stamina_Max.png"};

    private static final String[] FERMIER_NAMES = {"Semis-Marathon", "Tournesol Né", "Aura du Fermier"};
    private static final String[] FERMIER_DESCS = {
        "La moisson t'insuffle de l'énergie.",
        "Le soleil veille sur toi.",
        "La terre te régénère."
    };
    private static final String[] FERMIER_STATS = {"+30% vitesse pendant 30s après une récolte", "-15% dégâts reçus de jour", "+1 PV toutes les 5 secondes"};
    private static final String[] FERMIER_ICONS = {"Jobs_Icons/Movement_Speed.png", "Jobs_Icons/Sunflower.png", "Jobs_Icons/Health_Regen.png"};

    private static final String[] CHASSEUR_NAMES = {"Camouflage Nocturne", "Instinct Affûté", "Traque"};
    private static final String[] CHASSEUR_DESCS = {
        "La nuit est ton alliée.",
        "Le sang versé décuple tes sens.",
        "Ta proie ne peut fuir."
    };
    private static final String[] CHASSEUR_STATS = {"-15% dégâts reçus la nuit", "+30% vitesse pendant 10s après un kill", "+10% dégâts infligés"};
    private static final String[] CHASSEUR_ICONS = {"Jobs_Icons/Night_Defense.png", "Jobs_Icons/Movement_Speed.png", "Jobs_Icons/Attack_Buff.png"};

    private ProfessionBonusData() {}

    public static String[] icons(Profession prof) {
        return switch (prof) {
            case MINEUR -> MINEUR_ICONS;
            case FORESTIER -> FORESTIER_ICONS;
            case FERMIER -> FERMIER_ICONS;
            case CHASSEUR -> CHASSEUR_ICONS;
            default -> new String[]{"", "", ""};
        };
    }

    public static String[] names(Profession prof) {
        return switch (prof) {
            case MINEUR -> MINEUR_NAMES;
            case FORESTIER -> FORESTIER_NAMES;
            case FERMIER -> FERMIER_NAMES;
            case CHASSEUR -> CHASSEUR_NAMES;
            default -> new String[]{"Maîtrise I", "Maîtrise II", "Maîtrise III"};
        };
    }

    public static String[] descs(Profession prof) {
        return switch (prof) {
            case MINEUR -> MINEUR_DESCS;
            case FORESTIER -> FORESTIER_DESCS;
            case FERMIER -> FERMIER_DESCS;
            case CHASSEUR -> CHASSEUR_DESCS;
            default -> new String[]{"", "", ""};
        };
    }

    public static String[] stats(Profession prof) {
        return switch (prof) {
            case MINEUR -> MINEUR_STATS;
            case FORESTIER -> FORESTIER_STATS;
            case FERMIER -> FERMIER_STATS;
            case CHASSEUR -> CHASSEUR_STATS;
            default -> new String[]{"", "", ""};
        };
    }
}
