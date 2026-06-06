package fr.varyon.vrpg.ui.profession;

import fr.varyon.vrpg.rpg.Profession;

import javax.annotation.Nonnull;
import java.util.Arrays;

import static fr.varyon.vrpg.ui.tree.TalentTreeEdgeLayout.*;
import static fr.varyon.vrpg.ui.tree.TalentTreeTheme.SLOT;

public final class ProfessionSkillTrees {

    public static final String ICON_BASE = "Pages/VaryonRpg/Icons/";
    public static final String[] LEGACY_STATIC_EDGE_IDS = {
        "#SkillTreeEdgeRootStem",
        "#SkillTreeEdgeRootBranch",
        "#SkillTreeEdgeRootToNode6",
        "#SkillTreeEdgeRootToNode7",
        "#SkillTreeEdgeRootToNode1",
        "#SkillTreeEdgeRootToNode2",
        "#SkillTreeEdgeNode6ToNode9",
        "#SkillTreeEdgeNode2ToNode8",
    };

    public static final int PROFESSION_CATALOG_SLOTS = 8;

    public static final Profession[] CATALOG_ORDER = {
        Profession.MINEUR,
        Profession.FERMIER,
        Profession.FORESTIER,
        Profession.CHASSEUR,
        Profession.FORGERON,
        Profession.ALCHIMISTE,
        Profession.ARTISAN,
        Profession.CUISINIER,
    };

    private ProfessionSkillTrees() {}

    public static ProfessionSkillTreeDef forProfession(@Nonnull Profession profession) {
        return switch (profession) {
            case MINEUR -> MINEUR_TREE;
            case FERMIER -> FERMIER_TREE;
            case FORESTIER -> FORESTIER_TREE;
            case CHASSEUR -> CHASSEUR_TREE;
            default -> BASE_TREE;
        };
    }
    private static final int[][] PARENT_GROUPS = new int[][] {
        {}, // 0
        {}, // 1
        {0, 1}, // 2
        {2}, // 3
        {2}, // 4
        {2}, // 5
        {3}, // 6
        {4}, // 7
        {5}, // 8
        {6, 7, 8}, // 9
        {9}, // 10
        {9}, // 11
        {2}, // 12
        {2}, // 13
        {9}, // 14
        {9}, // 15
    };

    private static final int[] NODE_MAX_RANKS = {
        5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, // nodes 0–11
        1, 1, 1, 1                             // nodes 12–15
    };
    private static final int[][] SLOT_LT = {
        {118, 32},  // 0
        {298, 32},  // 1
        {208, 125}, // 2
        {58,  218}, // 3
        {208, 218}, // 4
        {358, 218}, // 5
        {58,  309}, // 6
        {208, 309}, // 7
        {358, 309}, // 8
        {208, 402}, // 9
        {118, 495}, // 10
        {298, 495}, // 11
        {58,  125}, // 12 — gauche de 2
        {358, 125}, // 13 — droite de 2
        {58,  402}, // 14 — gauche de 9
        {358, 402}, // 15 — droite de 9
    };

    private static final String[][] TREE_NODES = {
        {"0",  "Poches Pleines",           "Passif", "Un vrai mineur ne repart jamais avec un seul caillou.",                    "Chance de doubler les ressources obtenues en minant.",                             "Jobs_Icons/Ore_Loot.png"},
        {"1",  "Front Poussiereux",        "Passif", "Chaque coup de pioche laisse une marque. Certaines deviennent du savoir.", "Augmente l’expérience gagnée en minant.",                                          "Jobs_Icons/Xp_Boost.png"},
        {"2",  "Minerai Fantomatique",      "Passif", "Au fond des galeries, certains minerais brillent d’une lueur qui n’appartient pas à ce monde.", "Chance d’obtenir un Minerai Fantomatique en récoltant.", "Jobs_Icons/Ore_Corrupted.png"},
        {"3",  "Pioche de Vétéran",        "Passif", "Les outils bien entretenus survivent aux mineurs.",                       "Réduit les pertes de durabilité de votre pioche.",                                 "Jobs_Icons/Pickaxe_Durability.png"},
        {"4",  "Minerai Immortel",         "Passif", "Certaines veines refusent simplement de disparaître.",                    "Chance qu’un minerai réapparaîsse immédiatement après récolte.",                  "Jobs_Icons/Ore_Respawn.png"},
        {"5",  "C-C-Combo",                "Passif", "Plus tu frappes vite, plus la montagne te récompense.",                   "Enchainer les minerais rapport de l'XP et du minerai bonus par combo (Max 8 combo)",    "Jobs_Icons/Combo_Mining.png"},
        {"6",  "Incassable !",             "Passif", "Ta pioche a vu pire.",                                                    "Chaque coup a une chance de gagner un point de durabilité plutôt que d’en perdre un.", "Jobs_Icons/Unbreakable.png"},
        {"7",  "Briseur de Roche",         "Passif", "Terre et pierre ne sont plus qu’un simple obstacle.",                    "Chance que les coups sur la roche ne consomment pas la durabilité de votre pioche.", "Jobs_Icons/Pickaxe_Durability_Stone.png"},
        {"8",  "Chant de la Veine",        "Actif",  "Une frappe parfaite suffit à réveiller tout le filon.",                  "Permet de miner instantanément toute une veine de minerai.",                      "Jobs_Icons/Vein_Sing.png"},
        {"9",  "Gardien de Pierre",        "Passif", "Sous certaines montagnes sommeillent encore les anciens protecteurs.",    "Chance d’invoquer un Gardien Minéral laissant un objet légendaire.",              "Jobs_Icons/Keeper_Miner.png"},
        {"10", "Œil du Prospecteur",       "Passif", "Les cristaux rares brillent différemment pour ceux qui savent regarder.", "Détecte les gemmes rares à proximité.",                                          "Jobs_Icons/Radar_Gem.png"},
        {"11", "Wagon Express",            "Actif",  "Tous les tunnels finissent par mener quelque part.",                      "Débloque une commande pour retourner instantanément à la surface.",               "Jobs_Icons/Extract_Drill.png"},
        {"12", "Œil de Taupe",             "Passif", "Dans les profondeurs, la lumière finit toujours par suivre les anciens.", "Équipe un casque de mineur diffusant une lumière permanente autour de vous.",     "Jobs_Icons/Miner_Helmet.png"},
        {"13", "Besace du Foreur",         "Passif", "Même la mort n’ose pas fouiller dans ce sac.",                            "Les minerais placés dans votre sac de mineur sont conservés après votre mort.",  "Jobs_Icons/Bag_Rock.png"},
        {"14", "Deux pour le Prix d’un",   "Passif", "Un coup de pioche rentable, enfin.",                                      "Casser un bloc de roche casse également le bloc de roche en dessous.",            "Jobs_Icons/Multi_Mining.png"},
        {"15", "Diplomatie Minière",       "Actif",  "Quand la roche refuse de bouger, il existe d’autres arguments.",         "Permet de déclencher une explosion contrôlée pour terraformer rapidement la zone.", "Jobs_Icons/Rock_Explosion.png"},
    };

    private static final String[][] MINEUR_NODE_STAT_VALUES = {
        {"5% loot",        "10% loot",        "15% loot",        "20% loot",        "25% loot"},           // 0
        {"5% XP",          "10% XP",          "15% XP",          "20% XP",          "25% XP"},             // 1
        {"1% minerai",     "1.5% minerai",    "2% minerai",      "2.5% minerai",    "3% minerai"},          // 2
        {"7% durabilité",  "14% durabilité",  "21% durabilité",  "28% durabilité",  "35% durabilité"},      // 3
        {"4% repop",       "8% repop",        "12% repop",       "16% repop",       "20% repop"},           // 4
        {"1% / combo (max +8%)", "1.5% / combo (max +12%)", "2% / combo (max +16%)", "2.5% / combo (max +20%)", "3% / combo (max +24%)"}, // 5
        {"5% gain dura",   "10% gain dura",   "15% gain dura",   "20% gain dura",   "25% gain dura"},       // 6
        {"15% durabilité", "30% durabilité",  "45% durabilité",  "60% durabilité",  "75% durabilité"},      // 7
        {"4min 20s recharge", "3min 20s recharge", "2min 20s recharge", "1min 40s recharge", "1min recharge"},  // 8
        {"5% invocation",  "10% invocation", "15% invocation",  "20% invocation", "25% invocation"},
        {"12 blocs",       "18 blocs",        "24 blocs",        "30 blocs",        "36 blocs"},            // 10
        {"3h recharge",    "2h30 recharge",   "2h recharge",     "1h30 recharge",   "1h recharge"},         // 11
        {"Lumière permanente activée"},    // 12
        {"Minerais conservés à la mort"}, // 13
        {"Zone 2×2 débloquée"},           // 14
        {"Explosion contrôlée débloquée"} // 15
    };

    private static final String[][] BASE_TREE_NODES = Arrays.copyOfRange(TREE_NODES, 0, 12);
    private static final int[][] BASE_TREE_SLOT_LT = Arrays.copyOfRange(SLOT_LT, 0, 12);
    private static final int[][] BASE_TREE_PARENT_GROUPS = Arrays.copyOfRange(PARENT_GROUPS, 0, 12);
    private static final int[] BASE_TREE_MAX_RANKS = Arrays.copyOfRange(NODE_MAX_RANKS, 0, 12);

    private static final ProfessionSkillTreeDef BASE_TREE = new ProfessionSkillTreeDef(
        BASE_TREE_NODES, BASE_TREE_SLOT_LT, BASE_TREE_PARENT_GROUPS, BASE_TREE_MAX_RANKS,
        (ui, seg, lt) -> {
            seg = layoutMergeTwoToOne(ui, seg, cx(lt[0]), bot(lt[0]), cx(lt[1]), bot(lt[1]), cx(lt[2]), top(lt[2]));
            seg = layoutSplitOneToThree(ui, seg, cx(lt[2]), bot(lt[2]), cx(lt[3]), cx(lt[4]), cx(lt[5]), top(lt[3]));
            seg = layoutVerticalConnector(ui, seg, cx(lt[3]), bot(lt[3]), top(lt[6]));
            seg = layoutVerticalConnector(ui, seg, cx(lt[4]), bot(lt[4]), top(lt[7]));
            seg = layoutVerticalConnector(ui, seg, cx(lt[5]), bot(lt[5]), top(lt[8]));
            seg = layoutMergeThreeToOne(ui, seg, cx(lt[6]), bot(lt[6]), cx(lt[7]), bot(lt[7]), cx(lt[8]), bot(lt[8]), cx(lt[9]), top(lt[9]));
            seg = layoutSplitOneToTwo(ui, seg, cx(lt[9]), bot(lt[9]), cx(lt[10]), cx(lt[11]), top(lt[10]));
            return seg;
        },
        null
    );

    private static final ProfessionSkillTreeDef MINEUR_TREE = new ProfessionSkillTreeDef(
        TREE_NODES, SLOT_LT, PARENT_GROUPS, NODE_MAX_RANKS,
        (ui, seg, lt) -> {
            seg = layoutMergeTwoToOne(ui, seg, cx(lt[0]), bot(lt[0]), cx(lt[1]), bot(lt[1]), cx(lt[2]), top(lt[2]));
            seg = layoutSplitOneToThree(ui, seg, cx(lt[2]), bot(lt[2]), cx(lt[3]), cx(lt[4]), cx(lt[5]), top(lt[3]));
            seg = layoutVerticalConnector(ui, seg, cx(lt[3]), bot(lt[3]), top(lt[6]));
            seg = layoutVerticalConnector(ui, seg, cx(lt[4]), bot(lt[4]), top(lt[7]));
            seg = layoutVerticalConnector(ui, seg, cx(lt[5]), bot(lt[5]), top(lt[8]));
            seg = layoutMergeThreeToOne(ui, seg, cx(lt[6]), bot(lt[6]), cx(lt[7]), bot(lt[7]), cx(lt[8]), bot(lt[8]), cx(lt[9]), top(lt[9]));
            seg = layoutSplitOneToTwo(ui, seg, cx(lt[9]), bot(lt[9]), cx(lt[10]), cx(lt[11]), top(lt[10]));
            seg = layoutHorizontalSiblings(ui, seg, cx(lt[2]), top(lt[2]) + SLOT / 2, cx(lt[12]), cx(lt[13]));
            seg = layoutHorizontalSiblings(ui, seg, cx(lt[9]), top(lt[9]) + SLOT / 2, cx(lt[14]), cx(lt[15]));
            return seg;
        },
        MINEUR_NODE_STAT_VALUES
    );

    private static final String[][] FERMIER_TREE_NODES = {
        {"0",  "Paniers Trop Pleins",    "Passif", "Les champs donnent davantage à ceux qui savent les écouter.",                     "Chance de doubler les récoltes obtenues.",                                        "Jobs_Icons/Crops_Loot.png"},
        {"1",  "Mains Terreuses",        "Passif", "Plus tes bottes sont sales, plus tu progresses.",                                  "Augmente l'expérience gagnée en récoltant.",                                      "Jobs_Icons/Xp_Boost.png"},
        {"2",  "Maître Arroseur",        "Passif", "Un vrai fermier hydrate ses cultures avec style.",                                  "Débloque les arroseurs automatiques.",                                            "Jobs_Icons/Sprinkler.png"},
        {"3",  "Graines Fantomatiques",  "Passif", "Ces semences translucides ne se révèlent qu'aux mains qui ont vraiment travaillé la terre.", "Chance d'obtenir des Graines Fantomatiques lors des récoltes.",         "Jobs_Icons/Seed_Corrupted.png"},
        {"4",  "Bras Long",              "Passif", "Pourquoi marcher jusqu'au champ quand le champ est déjà à portée ?",           "Plante sur 5 blocs de long.",                                                     "Jobs_Icons/Multiple_Crop.png"},
        {"5",  "Grains Sans Fin",        "Passif", "Le stock de graines devient un concept théorique.",                                "Chance d'obtenir une Graine Éternelle en récoltant n'importe quelle culture.",    "Jobs_Icons/Eternal_Seed_Plus.png"},
        {"6",  "C-C-Combo",              "Passif", "Plus tu récoltes vite, plus les champs te récompensent.",                         "Enchainer les récoltes rapporte de l'XP et du loot bonus par combo (Max 10 combo).", "Jobs_Icons/Combo_Harvsting.png"},
        {"7",  "Seigneur de l'Étable",  "Passif", "Même les bêtes savent reconnaître un maître.",                                  "Augmente les ressources obtenues sur l'élevage.",                                 "Jobs_Icons/Farm_Animals_Loot.png"},
        {"8",  "Faucille Éternelle",    "Passif", "Elle coupe encore. Toujours.",                                                      "Réduit l'usure de votre faucille.",                                               "Jobs_Icons/Sickle_Durability.png"},
        {"9",  "Casse-Croûte Fermier",  "Passif", "Un bon champ nourrit toujours son maître.",                                        "Récolter une culture a une chance de restaurer votre faim ou votre soif.",        "Jobs_Icons/Feed_Hydrate.png"},
        {"10", "Crop Circles",           "Passif", "Les champs commencent à s'organiser sans toi.",                                    "Débloque les semeurs de graines automatiques.",                                   "Jobs_Icons/Crop_Dispenser.png"},
        {"11", "Gardiens des Champs",    "Passif", "Les récoltes les plus riches attirent parfois d'anciens protecteurs.",             "Chance d'invoquer un Gardien des Champs laissant un objet légendaire.",          "Jobs_Icons/Scarecrow.png"},
        {"12", "Terre Nourricière",      "Passif", "La bonne terre, ça s'entretient avant de se mériter.",                          "Débloque des fertilisants de haute qualité boostant la vitesse de croissance des cultures.", "Jobs_Icons/Fertilizer.png"},
        {"16", "Besace du Paysan",       "Passif", "Un bon paysan ne rentre jamais les mains vides — ni sans un sac qui suit.",       "Un sac renforcé permettant de transporter des récoltes sans les perdre à la mort.", "Jobs_Icons/Bag_Seeds.png"},
    };

    private static final int[][] FERMIER_SLOT_LT = {
        {272, 32},
        {452, 32},
        {212, 125},
        {362, 125},
        {512, 125},
        {362, 218},
        {512, 218},
        {362, 309},
        {512, 309},
        {362, 402},
        {272, 495},
        {452, 495},
        {212, 218},
        {212, 309},
    };

    private static final int[][] FERMIER_PARENT_GROUPS = {
        {},
        {},
        {0, 1},
        {0, 1},
        {0, 1},
        {3},
        {4},
        {5},
        {6},
        {7, 8},
        {9},
        {9},
        {2},
        {12},
    };

    private static final int[] FERMIER_MAX_RANKS = {5, 5, 5, 5, 1, 5, 5, 5, 5, 5, 1, 5, 4, 1};

    private static final String[][] FERMIER_NODE_STAT_VALUES = {
        {"5% loot",              "10% loot",             "15% loot",             "20% loot",             "25% loot"},
        {"5% XP",                "10% XP",               "15% XP",               "20% XP",               "25% XP"},
        {"Arroseur en cuivre",   "Arroseur en fer",      "Arroseur en thorium",  "Arroseur en cobalt",   "Arroseur en adamantite"},
        {"1% graines",           "1.5% graines",         "2% graines",           "2.5% graines",         "3% graines"},
        {"Replantation 5 blocs activée"},
        {"0.5% graine éternelle",  "0.75% graine éternelle", "1% graine éternelle",   "1.25% graine éternelle", "1.5% graine éternelle"},
        {"1% / combo (max +10%)", "1.5% / combo (max +15%)", "2% / combo (max +20%)", "2.5% / combo (max +25%)", "3% / combo (max +30%)"},
        {"5% élevage",           "10% élevage",          "15% élevage",          "20% élevage",          "25% élevage"},
        {"15% durabilité",       "30% durabilité",       "45% durabilité",       "60% durabilité",       "75% durabilité"},
        {"1% de chance",         "1.25% de chance",      "1.5% de chance",       "1.75% de chance",      "2% de chance"},
        {"Replantation auto arroseurs activée"},
        {"5% invocation",      "10% invocation",       "15% invocation",       "20% invocation",       "25% invocation"},
        {"Fertilisant Chaux débloqué", "Fertilisant Osseux débloqué", "Fertilisant Coquillage débloqué", "Fertilisant Élite débloqué"},
        {"Besace du Paysan activée"},
    };

    private static final ProfessionSkillTreeDef FERMIER_TREE = new ProfessionSkillTreeDef(
        FERMIER_TREE_NODES, FERMIER_SLOT_LT, FERMIER_PARENT_GROUPS, FERMIER_MAX_RANKS,
        (ui, seg, lt) -> {
            seg = layoutFanTwoToThree(ui, seg, cx(lt[0]), bot(lt[0]), cx(lt[1]), bot(lt[1]),
                cx(lt[2]), cx(lt[3]), cx(lt[4]), top(lt[2]));
            seg = layoutVerticalConnector(ui, seg, cx(lt[2]), bot(lt[2]), top(lt[12]));
            seg = layoutVerticalConnector(ui, seg, cx(lt[3]), bot(lt[3]), top(lt[5]));
            seg = layoutVerticalConnector(ui, seg, cx(lt[4]), bot(lt[4]), top(lt[6]));
            seg = layoutVerticalConnector(ui, seg, cx(lt[5]), bot(lt[5]), top(lt[7]));
            seg = layoutVerticalConnector(ui, seg, cx(lt[6]), bot(lt[6]), top(lt[8]));
            seg = layoutMergeTwoToOne(ui, seg, cx(lt[7]), bot(lt[7]), cx(lt[8]), bot(lt[8]), cx(lt[9]), top(lt[9]));
            seg = layoutSplitOneToTwo(ui, seg, cx(lt[9]), bot(lt[9]), cx(lt[10]), cx(lt[11]), top(lt[10]));
            seg = layoutVerticalConnector(ui, seg, cx(lt[12]), bot(lt[12]), top(lt[13]));
            return seg;
        },
        FERMIER_NODE_STAT_VALUES
    );

    private static final String[][] FORESTIER_TREE_NODES = {
        {"0",  "Bûches Bien Lourdes",       "Passif", "Un arbre vide, c'est juste du mobilier.",                           "Chance de doubler les ressources obtenues en coupant des arbres.",                                           "Jobs_Icons/Logs_Loot.png"},
        {"1",  "Mains Écorchées",           "Passif", "L'expérience pousse rarement sans échardes.",                       "Augmente l'expérience gagnée en coupant des arbres et en récoltant dans la nature.",                        "Jobs_Icons/Xp_Boost.png"},
        {"2",  "Bûches Fantomatiques",       "Passif", "Certains arbres anciens laissent derrière eux plus que du bois.",    "Chance d'obtenir une Bûche Fantomatique en coupant des arbres.",                                              "Jobs_Icons/Log_Corrupted.png"},
        {"3",  "Hache du Survivant",        "Passif", "Elle coupe encore. Toujours.",                                      "Réduit l'usure de votre hache.",                                                                              "Jobs_Icons/Hatchet_Durability.png"},
        {"4",  "Cueilleur des Sous-Bois",   "Passif", "Les meilleures trouvailles poussent loin des chemins.",             "Augmente les ressources obtenues sur les fleurs et champignons.",                                             "Jobs_Icons/Mushroom_Loot.png"},
        {"5",  "C-C-Combo",                  "Passif", "Quand le rythme part, la forêt suit.",                             "Couper plusieurs arbres rapidement déclenche un combo augmentant les gains.",                                  "Jobs_Icons/Combo_Logging.png"},
        {"6",  "Rôdeur Sylvestre",          "Passif", "La forêt finit toujours par reconnaître les siens.",                "Augmente votre vitesse dans les forêts et réduit les dégâts de chute.",                                       "Jobs_Icons/Forest_Runner.png"},
        {"7",  "Pêche Miraculeuse",         "Passif", "Même les poissons veulent finir dans ton sac.",                     "Augmente les loot en pêchant.",                                                                               "Jobs_Icons/Fishs_Loot.png"},
        {"8",  "Yeux de Hibou",             "Passif", "La nuit appartient à ceux qui voient encore.",                      "Améliore votre vision nocturne dans les forêts.",                                                             "Jobs_Icons/Night_Vision.png"},
        {"9",  "Équipement Tridimensionnel","Actif",  "Le sol devient optionnel.",                                         "Débloque un grappin forestier permettant de se déplacer rapidement entre les arbres.",                         "Jobs_Icons/Grappling_Hook.png"},
        {"10", "Gardien Sylvestre",         "Passif", "Les forêts anciennes n'abandonnent jamais leurs protecteurs.",      "Chance d'invoquer un Gardien Sylvestre laissant un objet légendaire à sa mort.",                              "Jobs_Icons/Forester_Guardian.png"},
        {"12", "Retour aux Racines",        "Passif", "Chaque arbre tombé mérite un héritier.",                            "Replante automatiquement un arbre après l'avoir coupé.",                                                      "Jobs_Icons/Tree_Regrowth.png"},
        {"13", "Besace du Forestier",       "Passif", "Le bois coupé ne se perd pas avec le souffle.",                     "Un sac renforcé permettant de transporter du bois sans le perdre à la mort.",                                 "Jobs_Icons/Bag_Wood.png"},
        {"14", "Poumons de Loutre",         "Passif", "Tu passes plus de temps sous l'eau qu'au sec.",                     "Augmente le temps de respiration sous l'eau. Rang max : durée doublée (+100%).",                              "Jobs_Icons/Water_Breathing.png"},
        {"15", "Lit de Fortune",            "Passif", "Même les rôdeurs doivent dormir un jour.",                          "Les lits d'appoint restaurent davantage de vie et d'énergie.",                                               "Jobs_Icons/Bed_Regen.png"},
    };

    private static final int[][] FORESTIER_SLOT_LT = {
        {272, 32},
        {452, 32},
        {362, 125},
        {212, 218},
        {362, 218},
        {512, 218},
        {272, 309},
        {452, 309},
        {362, 402},
        {272, 495},
        {452, 495},
        {212, 125},
        {512, 125},
        {212, 402},
        {512, 402},
    };

    private static final int[][] FORESTIER_PARENT_GROUPS = {
        {},
        {},
        {0, 1},
        {2},
        {2},
        {2},
        {3, 4},
        {4, 5},
        {6, 7},
        {8},
        {8},
        {2},
        {2},
        {8},
        {8},
    };

    private static final int[] FORESTIER_MAX_RANKS = {5, 5, 5, 5, 5, 5, 5, 5, 5, 3, 5, 1, 1, 5, 5};

    private static final String[][] FORESTIER_NODE_STAT_VALUES = {
        {"5% loot",              "10% loot",              "15% loot",              "20% loot",              "25% loot"},
        {"5% XP",                "10% XP",                "15% XP",                "20% XP",                "25% XP"},
        {"1% bûche",             "1.5% bûche",            "2% bûche",              "2.5% bûche",            "3% bûche"},
        {"15% durabilité",       "30% durabilité",        "45% durabilité",        "60% durabilité",        "75% durabilité"},
        {"5% fleurs/champi",     "10% fleurs/champi",     "15% fleurs/champi",     "20% fleurs/champi",     "25% fleurs/champi"},
        {"1% / combo (max +10%)", "1.5% / combo (max +15%)", "2% / combo (max +20%)", "2.5% / combo (max +25%)", "3% / combo (max +30%)"},
        {"10% vit / 10% chute",  "15% vit / 15% chute",  "20% vit / 20% chute",  "25% vit / 25% chute",  "30% vit / 30% chute"},
        {"5% loot rare",         "10% loot rare",         "15% loot rare",         "20% loot rare",         "25% loot rare"},
        {"Vision faible",        "Vision modérée",        "Vision renforcée",      "Vision avancée",        "Vision parfaite"},
        {"Grappin Fer, Émeraude, Diamant, Rubis, Saphir, Topaze, Zéphyr débloqués", "Grappin Thorium & Cobalt débloqués", "Grappin Adamantite débloqué"},
        {"5% invocation",      "10% invocation",        "15% invocation",        "20% invocation",         "25% invocation"},
        {"Replantation auto activée"},
        {"Déracinage total activé"},
        {"+20% respiration",     "+40% respiration",      "+60% respiration",      "+80% respiration",      "+100% respiration"},
        {"5% récupération",      "10% récupération",      "15% récupération",      "20% récupération",      "25% récupération"},
    };

    private static final ProfessionSkillTreeDef FORESTIER_TREE = new ProfessionSkillTreeDef(
        FORESTIER_TREE_NODES, FORESTIER_SLOT_LT, FORESTIER_PARENT_GROUPS, FORESTIER_MAX_RANKS,
        (ui, seg, lt) -> {
            seg = layoutMergeTwoToOne(ui, seg, cx(lt[0]), bot(lt[0]), cx(lt[1]), bot(lt[1]), cx(lt[2]), top(lt[2]));
            seg = layoutSplitOneToThree(ui, seg, cx(lt[2]), bot(lt[2]), cx(lt[3]), cx(lt[4]), cx(lt[5]), top(lt[3]));
            seg = layoutFanThreeToTwo(ui, seg, cx(lt[3]), bot(lt[3]), cx(lt[4]), bot(lt[4]), cx(lt[5]), bot(lt[5]),
                cx(lt[6]), cx(lt[7]), top(lt[6]));
            seg = layoutMergeTwoToOne(ui, seg, cx(lt[6]), bot(lt[6]), cx(lt[7]), bot(lt[7]), cx(lt[8]), top(lt[8]));
            seg = layoutSplitOneToTwo(ui, seg, cx(lt[8]), bot(lt[8]), cx(lt[9]), cx(lt[10]), top(lt[9]));
            seg = layoutHorizontalSiblings(ui, seg, cx(lt[2]), top(lt[2]) + SLOT / 2, cx(lt[11]), cx(lt[12]));
            seg = layoutHorizontalSiblings(ui, seg, cx(lt[8]), top(lt[8]) + SLOT / 2, cx(lt[13]), cx(lt[14]));
            return seg;
        },
        FORESTIER_NODE_STAT_VALUES
    );

    private static final String[][] CHASSEUR_TREE_NODES = {
        {"0",  "Mains du Boucher",     "Passif", "Un vrai chasseur sait où couper.",                                          "Augmente les ressources obtenues sur la viande, le cuir et les plumes.",                        "Jobs_Icons/Leather_Loot.png"},
        {"1",  "Instinct Sauvage",     "Passif", "Plus la traque dure, plus le prédateur apprend.",                           "Augmente l'expérience gagnée en chassant.",                                                      "Jobs_Icons/Xp_Boost.png"},
        {"2",  "Cuir Fantomatique",    "Passif", "Les créatures les plus rares laissent parfois une dépouille translucide.", "Chance d'obtenir du Cuir Fantomatique sur les créatures.",                                       "Jobs_Icons/Hide_Corrupted.png"},
        {"3",  "Kit Renforcé",         "Passif", "Un bon arc encaisse autant que son porteur.",                               "Réduit l'usure de vos armes de chasse.",                                                         "Jobs_Icons/Hunter_Weapon_Durability.png"},
        {"4",  "Dépouilleur",          "Passif", "Les meilleures prises ne se laissent jamais partir sans récompense.",       "Chance de doubler les ressources obtenues sur les créatures.",                                   "Jobs_Icons/Monster_Loot.png"},
        {"5",  "Chasse Frénétique",    "Passif", "Quand la poursuite commence, difficile de s'arrêter.",                     "Éliminer plusieurs créatures rapidement déclenche un combo augmentant les gains.",               "Jobs_Icons/Combo_Killing.png"},
        {"6",  "Rôdeur des Dunes",     "Passif", "Le désert finit toujours par respecter ceux qui le traversent.",           "Augmente votre vitesse dans les zones désertiques et réduit les dégâts de chute.",               "Jobs_Icons/Forest_Runner.png"},
        {"7",  "Matériaux Exotiques",  "Passif", "Les créatures rares laissent rarement des matériaux ordinaires.",          "Augmente les chances d'obtenir de la chitine, du venin, des os et de la laine.",                 "Jobs_Icons/Exotic_Loot.png"},
        {"8",  "Second Souffle",       "Passif", "Un chasseur fatigué devient une proie.",                                    "Augmente votre endurance maximale.",                                                              "Jobs_Icons/Stamina_Max.png"},
        {"9",  "Yeux de Lynx",         "Passif", "La nuit cache les faibles, pas les chasseurs.",                            "Améliore votre vision nocturne.",                                                                 "Jobs_Icons/Night_Vision.png"},
        {"10", "Dompteur de Monstres", "Actif",  "Certaines créatures préfèrent obéir plutôt que mourir.",                   "Permet d'apprivoiser certaines créatures agressives.",                                           "Jobs_Icons/Tame_Tool.png"},
        {"11", "Prédateur Alpha",      "Passif", "Même les monstres savent reconnaître le sommet de la chaîne alimentaire.", "Chance d'invoquer un Prédateur Alpha laissant un objet légendaire à sa mort.",                  "Jobs_Icons/Rex.png"},
        {"12", "Chasseur_12",          "Passif", "À définir.",                                                                "À définir.",                                                                                      ""},
        {"13", "Bourse du Traqueur",   "Passif", "Un bon chasseur garde toujours ses trophées près de lui.",                 "Les ressources placées dans votre sac de chasse sont conservées après votre mort.",             "Jobs_Icons/Bag_Meat.png"},
        {"14", "Chasseur_14",          "Passif", "À définir.",                                                                "À définir.",                                                                                      ""},
        {"15", "Chasseur_15",          "Passif", "À définir.",                                                                "À définir.",                                                                                      ""},
    };

    private static final int[][] CHASSEUR_SLOT_LT = {
        {272, 32},
        {452, 32},
        {362, 125},
        {212, 218},
        {362, 218},
        {512, 218},
        {212, 309},
        {362, 309},
        {512, 309},
        {362, 402},
        {272, 495},
        {452, 495},
        {212, 125},
        {512, 125},
        {212, 402},
        {512, 402},
    };

    private static final int[][] CHASSEUR_PARENT_GROUPS = {
        {},
        {},
        {0, 1},
        {2},
        {2},
        {2},
        {3},
        {4},
        {5},
        {6, 7, 8},
        {9},
        {9},
        {2},
        {2},
        {9},
        {9},
    };

    private static final int[] CHASSEUR_MAX_RANKS = {5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 1, 5, 1, 1, 1, 1};

    private static final String[][] CHASSEUR_NODE_STAT_VALUES = {
        {"5% viande/cuir/plumes",      "10% viande/cuir/plumes",      "15% viande/cuir/plumes",      "20% viande/cuir/plumes",      "25% viande/cuir/plumes"},
        {"5% XP",                      "10% XP",                      "15% XP",                      "20% XP",                      "25% XP"},
        {"1% cuir",                    "1.5% cuir",                   "2% cuir",                     "2.5% cuir",                   "3% cuir"},
        {"15% durabilité",             "30% durabilité",              "45% durabilité",              "60% durabilité",              "75% durabilité"},
        {"5% loot",                    "10% loot",                    "15% loot",                    "20% loot",                    "25% loot"},
        {"5% XP & loot",               "10% XP & loot",               "15% XP & loot",               "20% XP & loot",               "25% XP & loot"},
        {"4% vit / 10% chute",         "8% vit / 20% chute",          "12% vit / 30% chute",         "16% vit / 40% chute",         "20% vit / 50% chute"},
        {"5% exotiques",               "10% exotiques",               "15% exotiques",               "20% exotiques",               "25% exotiques"},
        {"5% endurance",               "10% endurance",               "15% endurance",               "20% endurance",               "25% endurance"},
        {"Vision faible",              "Vision modérée",              "Vision renforcée",            "Vision avancée",              "Vision parfaite"},
        {"Apprivoisement activé"},
        {"5% invocation",            "10% invocation",              "15% invocation",              "20% invocation",              "25% invocation"},
        {"À définir"},
        {"Bourse de chasse activée"},
        {"À définir"},
        {"À définir"},
    };

    private static final ProfessionSkillTreeDef CHASSEUR_TREE = new ProfessionSkillTreeDef(
        CHASSEUR_TREE_NODES, CHASSEUR_SLOT_LT, CHASSEUR_PARENT_GROUPS, CHASSEUR_MAX_RANKS,
        (ui, seg, lt) -> {
            seg = layoutMergeTwoToOne(ui, seg, cx(lt[0]), bot(lt[0]), cx(lt[1]), bot(lt[1]), cx(lt[2]), top(lt[2]));
            seg = layoutSplitOneToThree(ui, seg, cx(lt[2]), bot(lt[2]), cx(lt[3]), cx(lt[4]), cx(lt[5]), top(lt[3]));
            seg = layoutVerticalConnector(ui, seg, cx(lt[3]), bot(lt[3]), top(lt[6]));
            seg = layoutVerticalConnector(ui, seg, cx(lt[4]), bot(lt[4]), top(lt[7]));
            seg = layoutVerticalConnector(ui, seg, cx(lt[5]), bot(lt[5]), top(lt[8]));
            seg = layoutMergeThreeToOne(ui, seg, cx(lt[6]), bot(lt[6]), cx(lt[7]), bot(lt[7]), cx(lt[8]), bot(lt[8]), cx(lt[9]), top(lt[9]));
            seg = layoutSplitOneToTwo(ui, seg, cx(lt[9]), bot(lt[9]), cx(lt[10]), cx(lt[11]), top(lt[10]));
            seg = layoutHorizontalSiblings(ui, seg, cx(lt[2]), top(lt[2]) + SLOT / 2, cx(lt[12]), cx(lt[13]));
            seg = layoutHorizontalSiblings(ui, seg, cx(lt[9]), top(lt[9]) + SLOT / 2, cx(lt[14]), cx(lt[15]));
            return seg;
        },
        CHASSEUR_NODE_STAT_VALUES
    );

}