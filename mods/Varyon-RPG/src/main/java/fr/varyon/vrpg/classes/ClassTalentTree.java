package fr.varyon.vrpg.classes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public final class ClassTalentTree {

    public record Node(
        @Nonnull String name,
        @Nonnull String type,
        @Nonnull String flavor,
        @Nonnull String description,
        @Nonnull String itemId,
        int maxRank
    ) {}

    private static final Map<PlayerClass, Node[]> TREES = new EnumMap<>(PlayerClass.class);
    private static final Map<PlayerSpecialization, Node[]> SPEC_TREES = new HashMap<>();

    static {
        TREES.put(PlayerClass.GUERRIER, new Node[]{
            new Node("Maîtrise de l'Épée",    "Passif", "Chaque coup porté forge le guerrier autant que sa lame.",              "Augmente les dégâts de mêlée.",                                    "Weapon_Sword_Mithril",       5),
            new Node("Endurance au Combat",   "Passif", "Les cicatrices sont la preuve que tu t'en es sorti.",                  "Augmente les points de vie maximum.",                              "Armor_Iron_Chest",           5),
            new Node("Acier Trempé",          "Passif", "Une lame mithril ne pardonne pas.",                                    "Augmente les dégâts des coups critiques.",                         "Ingredient_Bar_Mithril",     5),
            new Node("Frappe Brisante",       "Passif", "L'armure, comme la volonté, finit par céder.",                         "Réduit la réduction de dégâts de la cible touchée.",               "Weapon_Axe_Mithril",         5),
            new Node("Posture du Rempart",    "Passif", "Tenir sa position vaut mieux que courir.",                             "Réduit les dégâts reçus lorsque tu es immobile.",                  "Weapon_Shield_Mithril",      5),
            new Node("Vitesse de Frappe",     "Passif", "La rapidité d'une lame vaut parfois mieux que sa force.",              "Réduit le temps entre les attaques.",                              "Ingredient_Bar_Cobalt",      5),
            new Node("Coup de Bouclier",      "Actif",  "Un choc bien placé, et l'ennemi voit trente-six chandelles.",          "Étourdit brièvement l'ennemi touché.",                             "Weapon_Shield_Cobalt",       5),
            new Node("Second Souffle",        "Passif", "Le corps sait se battre même quand l'esprit abandonne.",               "Régénère des points de vie lors des coups portés.",                "Potion_Health",              5),
            new Node("Sang-Froid",            "Passif", "La peur est pour les autres.",                                         "Réduit la durée des effets de statut négatifs.",                   "Ingredient_Crystal_Blue",    5),
            new Node("Lame Effilée",          "Passif", "Une lame onyxium passe là où les autres s'arrêtent.",                  "Augmente la pénétration d'armure.",                                "Weapon_Sword_Onyxium",       5),
            new Node("Maître d'Armes",        "Passif", "Tout ce qui coupe est une extension de ta volonté.",                   "Augmente tous les dégâts de mêlée de façon permanente.",           "Weapon_Longsword_Mithril",   5),
            new Node("Volonté de Fer",        "Passif", "On ne tue pas ce qu'on ne peut pas abattre.",                          "Survivre fatalement une fois par combat avec 1 PV.",               "Armor_Adamantite_Head",      5),
        });

        TREES.put(PlayerClass.BARBARE, new Node[]{
            new Node("Fureur Primitive",      "Passif", "Plus la bataille dure, plus la bête se réveille.",                     "Augmente les dégâts à mesure que tes PV diminuent.",               "Weapon_Battleaxe_Mithril",   5),
            new Node("Peau Épaisse",          "Passif", "Les cuirasses sont pour ceux qui ont peur.",                           "Augmente la réduction naturelle de dégâts.",                       "Armor_Leather_Heavy_Chest",  5),
            new Node("Cri de Guerre",         "Actif",  "Un hurlement qui fait trébucher les plus braves.",                     "Ralentit et affaiblit temporairement les ennemis proches.",        "Weapon_Club_Mithril",        5),
            new Node("Rage Incontrôlable",    "Passif", "Quand la rage prend, tout le reste disparaît.",                        "Déclenche un burst de dégâts à faibles PV.",                       "Weapon_Longsword_Mithril",   5),
            new Node("Régén Sauvage",         "Passif", "Le sang des barbares coule vite, mais se coagule encore plus vite.",   "Régénère des PV pendant le combat.",                               "Potion_Health_Greater",      5),
            new Node("Force Brute",           "Passif", "Là où la technique échoue, la force pure suffit.",                    "Augmente les dégâts de mêlée de base.",                            "Ingredient_Bar_Iron",        5),
            new Node("Charge",                "Actif",  "Trois enjambées et l'ennemi vole.",                                    "Charge sur l'ennemi ciblé en infligeant des dégâts.",              "Weapon_Spear_Mithril",       5),
            new Node("Chair Solide",          "Passif", "Blessé mais debout. Toujours debout.",                                 "Augmente les points de vie maximum.",                              "Armor_Mithril_Legs",         5),
            new Node("Instinct Prédateur",    "Passif", "Tu sens l'ennemi avant qu'il te voie.",                                "Augmente la détection des ennemis proches.",                       "Ingredient_Crystal_Red",     5),
            new Node("Déchaînement",          "Passif", "Une hache, deux haches, peu importe — tout saigne.",                   "Enchaîne automatiquement plusieurs frappes.",                      "Weapon_Battleaxe_Onyxium",   5),
            new Node("Indomptable",           "Actif",  "Ni magie ni acier — rien ne peut l'arrêter.",                          "Devient brièvement invulnérable.",                                 "Armor_Adamantite_Chest",     5),
            new Node("Colère Primordiale",    "Passif", "Au fond de chaque barbare dort un titan.",                             "Augmente massivement les dégâts sur la dernière cible frappée.",   "Weapon_Battleaxe_Adamantite",5),
        });

        TREES.put(PlayerClass.MAGE, new Node[]{
            new Node("Maîtrise des Sorts",    "Passif", "Comprendre un sort, c'est déjà à moitié le lancer.",                  "Augmente les dégâts de sorts.",                                    "Weapon_Staff_Mithril",       5),
            new Node("Réserve de Mana",       "Passif", "Un mage à cours de mana est juste un homme avec un bâton.",           "Augmente la réserve de mana maximum.",                             "Potion_Mana",                5),
            new Node("Concentration Arcane",  "Passif", "L'esprit est la véritable arme du mage.",                             "Augmente le multiplicateur de dégâts critiques des sorts.",        "Ingredient_Crystal_Purple",  5),
            new Node("Bouclier Arcanique",    "Actif",  "La magie protège autant qu'elle détruit.",                             "Absorbe les dégâts pendant un bref instant.",                      "Weapon_Wand_Stoneskin",      5),
            new Node("Catalyseur",            "Passif", "Moins de temps à incanter, plus de temps à détruire.",                "Réduit le temps de recharge des sorts.",                           "Ingredient_Crystal_Cyan",    5),
            new Node("Maîtrise du Feu",       "Passif", "Le feu ne brûle pas ceux qui le comprennent.",                        "Augmente les dégâts des sorts de feu.",                            "Ingredient_Fire_Essence",    5),
            new Node("Maîtrise du Froid",     "Passif", "Le gel ralentit tout — sauf lui.",                                    "Augmente les dégâts et la durée des effets de gel.",               "Ingredient_Ice_Essence",     5),
            new Node("Invocation Élémentaire","Actif",  "Appelle les forces primaires à la rescousse.",                        "Invoque une entité élémentaire temporaire.",                       "Ingredient_Crystal_Green",   5),
            new Node("Distorsion Temporelle", "Passif", "Le temps lui-même est un sort comme un autre.",                       "Réduit les temps de recharge globaux.",                            "Ingredient_Crystal_Yellow",  5),
            new Node("Surcharge Magique",     "Passif", "Au-delà des limites, le sort explose.",                               "Augmente les dégâts de sorts en échange de mana supplémentaire.",  "Weapon_Staff_Onyxium",       5),
            new Node("Domination Mentale",    "Actif",  "Une volonté assez forte peut plier celle des autres.",                "Contrôle brièvement un ennemi.",                                   "Weapon_Spellbook_Grimoire_Purple", 5),
            new Node("Archamane",             "Passif", "Là où les autres voient des limites, il voit des portes.",            "Amplifie tous les effets de sorts de façon permanente.",           "Weapon_Staff_Adamantite",    5),
        });

        TREES.put(PlayerClass.TIREUR, new Node[]{
            new Node("Œil de Lynx",           "Passif", "La cible est déjà morte avant que la flèche parte.",                  "Augmente les dégâts à distance.",                                  "Weapon_Shortbow_Mithril",    5),
            new Node("Mobilité Tactique",     "Passif", "Rester immobile, c'est mourir.",                                      "Augmente la vitesse de déplacement.",                              "Armor_Leather_Light_Chest",  5),
            new Node("Flèches Empoisonnées",  "Passif", "Parfois, la lenteur est plus cruelle que la mort directe.",           "Les flèches appliquent un poison progressif.",                     "Ingredient_Sac_Venom",       5),
            new Node("Salve",                 "Actif",  "Trois flèches dans le temps d'une, c'est de la mathématique.",        "Tire plusieurs flèches simultanément.",                             "Weapon_Arrow_Iron",          5),
            new Node("Tir de Précision",      "Passif", "À cette distance, le vent fait la différence.",                       "Augmente les dégâts critiques à distance.",                        "Weapon_Crossbow_Iron",       5),
            new Node("Esquive Réflexe",       "Passif", "Son corps bouge avant même que son esprit décide.",                   "Augmente la chance d'esquiver les attaques.",                      "Armor_Leather_Medium_Chest", 5),
            new Node("Marque de la Proie",    "Actif",  "Une fois marqué, nulle part où se cacher.",                           "Augmente les dégâts sur la cible marquée.",                        "Ingredient_Crystal_Red",     5),
            new Node("Cadence",               "Passif", "La régularité bat la précision. Chaque fois.",                        "Augmente la vitesse de tir.",                                      "Ingredient_Feathers_Light",  5),
            new Node("Traque",                "Passif", "Il suit ses proies sans qu'elles le sachent.",                        "Détecte les ennemis à grande portée.",                             "Ingredient_Crystal_Green",   5),
            new Node("Flèche Perçante",       "Passif", "L'armure n'est qu'une question de pression.",                        "Ignore une partie de la réduction d'armure ennemie.",              "Weapon_Arrow_Deadeye",       5),
            new Node("Embuscade",             "Passif", "Le premier coup depuis l'ombre vaut double.",                         "Augmente les dégâts du premier tir sur un ennemi.",                "Weapon_Daggers_Mithril",     5),
            new Node("Archer Légendaire",     "Passif", "Des siècles après, les bardes chanteront encore ses exploits.",       "Augmente définitivement tous les dégâts à distance.",              "Weapon_Shortbow_Onyxium",    5),
        });

        final String DI = "Classes_Icons/Duellist/";
        SPEC_TREES.put(PlayerSpecialization.DUELLISTE, new Node[]{
            new Node("Assaut Éclair",         "Actif",  "La première touche appartient à celui qui ose avancer.",                 "Ruée vers l'avant, infligeant des dégâts à l'impact.",             DI + "Percée.png",                5),
            new Node("Expert en Duel",        "Passif", "Un duel se gagne souvent avant même le premier échange.",                 "Gain d'expérience augmenté lorsque les PV sont supérieurs à 70 %.", DI + "Expert_En_Duel.png",         5),
            new Node("Blessure Ouverte",      "Passif", "Une lame qui accroche, c'est une victoire qui se compte en secondes.",    "Les attaques ont une chance d'infliger un saignement.",             DI + "Blessure_Ouverte.png",       5),
            new Node("Coup d'Estoc",          "Actif",  "La pointe trouve toujours une faille que l'épée ignore.",                "Frappe en AoE autour du joueur, puis arme le prochain coup d'un multiplicateur de dégâts.", DI + "Coup_D_estoc.png", 5),
            new Node("Riposte Parfaite",      "Actif",  "Absorber le coup pour mieux le rendre — trois fois.",                    "Entre en posture défensive. Si une attaque est reçue, contre-attaque instantanément.", DI + "Riposte_Parfaite.png",  5),
            new Node("Contre-Attaque",        "Passif", "Parer le coup n'était que la première moitié du mouvement.",              "Après une parade réussie, la prochaine attaque inflige davantage de dégâts.", DI + "Contre_Attaque.png",    5),
            new Node("Feinte",                "Actif",  "Montrer une faille pour en créer une vraie — c'est tout l'art du duel.", "Le prochain coup ne peut être ni bloqué ni paré par l'adversaire.", DI + "Feinte.png",                 5),
            new Node("Jeu de Jambes",          "Passif", "Une lame ne touche que ce qu'elle peut atteindre.",                      "Augmente les chances d'esquive.",                                   DI + "Esquive_du_bretteur.png",    5),
            new Node("Point Vital",           "Passif", "L'armure protège le corps, pas ses faiblesses.",                         "Les coups critiques infligent des dégâts supplémentaires.",          DI + "Frappe_Précise.png",         5),
            new Node("Désarmement",           "Actif",  "Ôter l'arme, c'est ôter la menace avant même qu'elle frappe.",           "Réduit temporairement les dégâts infligés par la cible.",           DI + "Désarmement.png",            5),
            new Node("Momentum",              "Passif", "Chaque coup sans en recevoir rend le suivant un peu plus dévastateur.",   "Chaque coup consécutif sans en recevoir augmente légèrement les dégâts.", DI + "Momentum.png",           5),
            new Node("Valse des Lames",        "Actif",  "Quand le rythme est trouvé, chaque mouvement devient naturel.",           "Augmente les dégâts d'attaque et la vitesse de déplacement pendant quelques secondes.", DI + "Assaut_Du_Bretteur.png", 5),
        });

        final String OI = "Classes_Icons/Shadow/";
        SPEC_TREES.put(PlayerSpecialization.OMBRE, new Node[]{
            new Node("Pas des Ténèbres",      "Actif",  "L'ombre n'attend pas — elle frappe et disparaît.",                          "Ruée vers l'arrière suivie d'une courte invisibilité.",                              OI + "Pas_Des_Tenebres.png",      5),
            new Node("Travail Propre",         "Passif", "Les professionnels ne laissent jamais traîner les choses.",                 "Gain d'expérience augmenté si la cible est tuée en moins de 5 secondes.",            OI + "Execution_Rapide.png",      5),
            new Node("Lames Empoisonnées",    "Passif", "Une égratignure anodine — pour l'instant.",                                 "Les attaques ont une chance d'empoisonner la cible.",                                OI + "Lames_Empoisonnees.png",    5),
            new Node("Déluge de Lames",       "Actif",  "Quand une lame ne suffit pas, on en envoie cinq.",                          "Enchaînement rapide de frappes infligeant des dégâts à la cible devant soi.",       OI + "Deluge_De_Lames.png",       5),
            new Node("Écran de Fumée",        "Actif",  "Disparaître n'est pas fuir — c'est choisir le bon moment.",                 "Devient invisible pendant quelques secondes, bloquant les dégâts reçus.",           OI + "Ecran_De_Fumee.png",        5),
            new Node("Embuscade",             "Passif", "La première frappe depuis l'ombre laisse peu de place à la réaction.",      "La première attaque après une invisibilité immobilise brièvement la cible.",         OI + "Embuscade.png",             5),
            new Node("Frappe Fatale",         "Actif",  "Un seul coup, au bon endroit, peut tout changer.",                          "Le prochain coup inflige des dégâts significativement accrus.",                      OI + "Frappe_Fatale.png",         5),
            new Node("Ombre Insaisissable",   "Passif", "Toucher l'ombre, c'est attraper le vent.",                                  "Augmente les chances d'esquive.",                                                    OI + "Ombre_Insaisissable.png",   5),
            new Node("Danse Macabre",          "Passif", "Le premier coup n'est que le signal du début de la chasse.",                "Après un coup critique, gagne un bonus de vitesse de déplacement pendant 3 sec.",   OI + "Danse_Des_Lames.png",       5),
            new Node("Traversée Obscure",      "Actif",  "La distance n'existe plus lorsqu'une cible est choisie.",                   "Se téléporte derrière la cible et frappe. Bonus si la cible est à moins de 30 % HP.", OI + "Pas_De_L_Ombre.png",      5),
            new Node("Instinct de Survie",    "Passif", "Sous 30 %, l'ombre ne meurt pas — elle s'adapte.",                          "Sous 30 % de points de vie, les chances d'esquive sont fortement augmentées.",       OI + "Instict_De_Survie.png",     5),
            new Node("Chasse Ouverte",        "Actif",  "Marquer une proie, c'est déjà la posséder.",                                "Marque une cible — toutes les attaques contre elle sont des coups critiques.",       OI + "Chasse_Ouverte.png",        5),
        });

        final String BI = "Classes_Icons/Berserker/";
        SPEC_TREES.put(PlayerSpecialization.BERSERKER, new Node[]{
            new Node("Assaut Bestial",        "Actif",  "Quand la bête charge, même les murs cèdent.",                              "Bond en avant dans la direction du regard, infligeant des dégâts à l'impact.",                         BI + "Assaut_Bestial.png",         5),
            new Node("Carnage",               "Passif", "Chaque mort est une leçon ensanglantée.",                                 "Gain d'expérience augmenté lors des séries d'éliminations (max 5 cumuls).",                             BI + "Carnage.png",                5),
            new Node("Fureur Sanguinaire",    "Passif", "Le sang ennemi nourrit la rage qui nourrit les coups.",                   "Après un kill, le vol de vie est augmenté pendant quelques secondes.",                                   BI + "Fureur_Sanguinaire.png",     5),
            new Node("Frénésie",              "Passif", "Chaque mort allume une flamme de plus dans ses yeux.",                    "Chaque élimination augmente vitesse et dégâts pendant quelques secondes (max 3 cumuls).",               BI + "Frénésie.png",               5),
            new Node("Dix pour Sang",         "Actif",  "Chaque goutte versée est un gage de puissance.",                          "Frappe puissante en zone qui consomme 10 % de tes propres HP.",                                          BI + "Dix_Pour_Sang.png",          5),
            new Node("Cor de Guerre",         "Actif",  "Le son du cor précède toujours la tempête.",                              "Augmente ta vitesse de déplacement pendant quelques secondes.",                                          BI + "Cor_De_Guerre.png",          5),
            new Node("Blessures Profondes",   "Passif", "Là où sa hache passe, les plaies ne se referment pas.",                   "Les attaques ont une chance d'infliger un saignement progressif.",                                      BI + "Blessures_Profondes.png",    5),
            new Node("Ferveur Guerrière",     "Passif", "Plus la bataille dure, plus la rage se cristallise.",                     "Les dégâts augmentent d'un cran chaque seconde passée en combat (max 15 cumuls).",                      BI + "Ferveur_Guerrière.png",      5),
            new Node("Cri de Ralliement",     "Actif",  "Un hurlement qui fait oublier la peur et décuple la hargne.",             "Augmente temporairement les dégâts de toi et des alliés proches.",                                      BI + "Cri_De_Ralliement.png",      5),
            new Node("Déchiquetage",          "Actif",  "La chair cède, et avec elle une partie de son énergie vitale.",           "Le prochain coup tranche profondément et récupère une partie des dégâts infligés en HP.",               BI + "Eviscération.png",           5),
            new Node("Dernier Souffle",       "Passif", "La mort attendra — il a encore des ennemis à abattre.",                   "Ignore temporairement la mort pendant 4 secondes (CD 90s).",                                            BI + "Dernier_Souffle.png",        5),
            new Node("Exécution Sauvage",     "Actif",  "Pour les blessés, chaque coup est le dernier.",                           "Frappe dévastatrice infligeant davantage de dégâts aux ennemis déjà affaiblis.",                        BI + "Exécution_Sauvage.png",      5),
        });

        final String RVI = "Classes_Icons/Ravager/";
        SPEC_TREES.put(PlayerSpecialization.RAVAGEUR, new Node[]{
            new Node("Bond Écrasant",          "Actif",  "Il ne court pas vers ses ennemis — il s'abat sur eux.",                    "Bond en avant et s'écrase au sol, infligeant des dégâts en zone à l'atterrissage.",               RVI + "Bond_Ecrasant.png",              5),
            new Node("Moissonneur",            "Passif", "Chaque mort est une graine d'expérience.",                                  "Gain d'expérience augmenté lors des séries d'éliminations (max 5 cumuls).",                       RVI + "Moisonneur.png",                 5),
            new Node("Arme Lourde",            "Passif", "Un coup critique qui laisse des traces durables.",                          "Les coups critiques réduisent la vitesse de déplacement de la cible pendant 3 secondes.",          RVI + "Arme_Lourde.png",               5),
            new Node("Peau de Fer",            "Actif",  "Sa peau est moins une chair qu'une armure forgée au combat.",               "Réduit les dégâts subis pendant quelques secondes.",                                              RVI + "Peau_De_Fer.png",               5),
            new Node("Exécuteur",              "Passif", "Les blessés ne méritent que la grâce du dernier coup.",                     "Inflige davantage de dégâts aux ennemis ayant moins de 30 % de points de vie.",                   RVI + "Exécuteur.png",                 5),
            new Node("Déchaînement",           "Actif",  "Quand la rage prend le dessus, les retenues disparaissent.",                "Augmente les dégâts infligés pendant quelques secondes.",                                         RVI + "Dechainement.png",              5),
            new Node("Chasseur de Géant",      "Passif", "Il préfère les proies grandes. Elles offrent plus de résistance.",          "Inflige davantage de dégâts aux ennemis ayant plus de points de vie que vous.",                   RVI + "Chasseur_De_Géant.png",         5),
            new Node("Élan Destructeur",       "Passif", "Un kill n'est qu'un tremplin vers le suivant.",                              "Après avoir éliminé un ennemi, la prochaine attaque inflige davantage de dégâts.",               RVI + "Elan_Destructeur.png",          5),
            new Node("Premier Assaut",         "Actif",  "Frapper fort d'emblée pour ne jamais laisser l'ennemi reprendre son souffle.", "Frappe dévastatrice infligeant davantage de dégâts aux ennemis ayant plus de 70 % de vie.",    RVI + "Premier_Assaut.png",            5),
            new Node("Marteau-Pilon",          "Actif",  "Deux coups, une sentence.",                                                  "Enchaîne deux frappes lourdes. Le second coup étourdit la cible.",                               RVI + "Marteau_Pilon.png",             5),
            new Node("Combattant Infatigable", "Passif", "Chaque blessure reçue avive la flamme plutôt qu'elle ne l'éteint.",         "Les dégâts augmentent en fonction des points de vie manquants.",                                  RVI + "Combattant_Infatigable.png",    5),
            new Node("Rabattage",              "Actif",  "Il ne chasse pas — il ramène le troupeau à lui.",                            "Balayage en arc devant soi, attirant les ennemis touchés et les étourdissant brièvement.",        RVI + "Rabattage.png",                 5),
        });

        final String FI = "Classes_Icons/Fighter/";
        SPEC_TREES.put(PlayerSpecialization.BAGARREUR, new Node[]{
            new Node("Jeu de Jambes",        "Actif",  "Esquiver, c'est déjà gagner.",                                                "Dash rapide sur le côté droit pour éviter les attaques.",                                    FI + "Jeu_De_Jambes.png",              5),
            new Node("Jusqu'au bout",        "Passif", "Plus il saigne, plus il se bat.",                                              "Gain d'expérience augmenté lorsque les points de vie sont inférieurs à 80 %.",               FI + "Jusqu'au bout.png",              5),
            new Node("Direct du Droit",      "Actif",  "Un seul coup bien placé suffit à tout changer.",                              "Coup puissant qui étourdit brièvement la cible.",                                            FI + "Direct_Du_Droit.png",            5),
            new Node("Montée d'Adrénaline",  "Actif",  "Il sent l'intensité du combat lui fouetter le sang.",                         "Augmente les dégâts infligés pendant quelques secondes.",                                    FI + "Montée_D_Adrenaline.png",        5),
            new Node("Garde du Boxeur",      "Passif", "La meilleure défense, c'est de voir venir le coup.",                          "Réduit les dégâts reçus lorsque l'attaquant est dans le cône frontal.",                      FI + "Garde_Du_Boxeur.png",            5),
            new Node("Adrénaline",           "Passif", "La douleur est un carburant.",                                                "Recevoir des dégâts augmente légèrement la vitesse de déplacement pendant quelques secondes.", FI + "Adrénaline.png",                 5),
            new Node("Acharnement",          "Passif", "Chaque coup porte un peu plus que le précédent.",                             "Les dégâts augmentent progressivement à chaque frappe sur la même cible.",                    FI + "Acharnement.png",                5),
            new Node("Esprit Combatif",      "Passif", "Le danger aiguise ses sens.",                                                 "Inflige davantage de dégâts lorsque ses points de vie sont inférieurs à 50 %.",              FI + "Esprit_Combatif.png",            5),
            new Node("Poings d'Acier",       "Passif", "Ses poings sont ses armes, et ses armes ne pardonnent pas.",                  "Les coups critiques ont une chance d'étourdir brièvement la cible.",                         FI + "Frappes_Répétées.png",           5),
            new Node("Déluge de Coups",      "Actif",  "Pas le temps de souffler — pas le temps de répondre.",                       "Enchaîne rapidement cinq frappes successives sur la cible.",                                  FI + "Déluge_De_Coups.png",            5),
            new Node("Second Souffle",       "Actif",  "Quand tout semble perdu, il trouve une réserve cachée.",                     "Restaure une partie des points de vie et de l'endurance.",                                   FI + "Second_Souffle.png",             5),
            new Node("Uppercut",             "Actif",  "Un uppercut qui envoie l'ennemi vers le ciel.",                              "Coup ascendant qui inflige des dégâts et projette la cible en l'air.",                       FI + "Uppercut.png",                   5),
        });

        final String AI = "Classes_Icons/Arcanist/";
        SPEC_TREES.put(PlayerSpecialization.ARCANISTE, new Node[]{
            new Node("Distorsion",          "Actif",  "La magie plie l'espace — et lui permet de disparaître.",              "Téléportation magique en arrière.",                                                         AI + "Distorsion.png",           5),
            new Node("Talent Inné",         "Passif", "La distance est son terrain naturel.",                                  "Gain d'expérience augmenté lorsque tu es à plus de 8 blocs de la cible.",                   AI + "Talent_Inné.png",          5),
            new Node("Boule de Feu",        "Actif",  "Une sphère de flamme qui consume tout ce qu'elle touche.",             "Lance une boule de feu qui explose à l'impact.",                                            AI + "Boule_De_Feu.png",         5),
            new Node("Écho Temporel",       "Passif", "Le temps ralentit pour lui, jamais pour ses ennemis.",                 "Réduit le temps de recharge des compétences actives.",                                      AI + "Echo_Temporel.png",        5),
            new Node("Météore",             "Actif",  "Il appelle l'extinction depuis les cieux.",                            "Lance un projectile explosif qui s'abat sur une zone.",                                     AI + "Météore.png",              5),
            new Node("Puits de Mana",       "Passif", "Son mana est un océan que peu peuvent égaler.",                        "Augmente le mana maximum.",                                                                  AI + "Puits_De_Mana.png",        5),
            new Node("Nova de Givre",       "Actif",  "Le froid jaillit de lui comme une explosion silencieuse.",             "Inflige des dégâts aux ennemis proches et les ralentit.",                                   AI + "Nova_De_Givre.png",        5),
            new Node("Drain Mystique",      "Passif", "Chaque mort nourrit sa réserve arcanique.",                            "Les éliminations restaurent une partie du mana.",                                           AI + "Drain_Mystique.png",       5),
            new Node("Surcharge",           "Actif",  "Trop de pouvoir — et pourtant jamais assez.",                          "Restaure du mana et augmente temporairement les dégâts des sorts.",                        AI + "Surcharge.png",            5),
            new Node("Écho Arcanique",      "Passif", "Parfois, le sort ne consomme rien — comme si la magie se répétait.", "Les sorts ont une chance de ne pas déclencher leur temps de recharge.",                     AI + "Echo_Arcanique.png",       5),
            new Node("Salve de Givre",      "Actif",  "Une tempête de glace qui laisse ses ennemis figés sur place.",        "Lance plusieurs projectiles glacés qui ralentissent les cibles touchées.",                   AI + "Salve_De_Givre.png",       5),
            new Node("Pouvoir Grandissant", "Passif", "L'inviolabilité forge une puissance silencieuse.",                     "Les dégâts des sorts augmentent tant que tu n'as pas subi de dégâts.",                      AI + "Pouvoir_Grandissant.png",  5),
        });

        final String RI = "Classes_Icons/Rampart/";
        SPEC_TREES.put(PlayerSpecialization.REMPART, new Node[]{
            new Node("Charge Lourde",         "Actif",  "Quand l'acier avance, rien ne résiste.",                                    "Charge en ligne droite en repoussant et frappant les ennemis sur le passage.",     RI + "Charge_Lourde.png",         5),
            new Node("Maître du Bouclier",    "Passif", "Le bouclier est son arme autant que son épée.",                             "Gain d'expérience de classe augmenté lorsqu'un bouclier est équipé.",               RI + "Maitre_Du_Bouclier.png",     5),
            new Node("Constitution de Fer",   "Passif", "Certains hommes portent une armure. D'autres en sont une.",                 "Augmente les points de vie maximum.",                                               RI + "Constitution_De_Fer.png",    5),
            new Node("Coup de Bouclier",      "Actif",  "Un choc bien placé, et l'ennemi voit trente-six chandelles.",               "Frappe la cible avec le bouclier, l'étourdissant brièvement.",                      RI + "Coup_De_Bouclier.png",       5),
            new Node("Forteresse",            "Actif",  "Impossible de vous faire bouger désormais.",                                "Réduit massivement les dégâts reçus pendant quelques secondes.",                    RI + "Forteresse.png",             5),
            new Node("Garde Impénétrable",    "Passif", "Bloquer, c'est préparer la riposte.",                                       "Après un blocage réussi, les dégâts reçus sont réduits pendant quelques secondes.", RI + "Garde_Impénétrable.png",     5),
            new Node("Second Souffle",        "Actif",  "Abandonner demande plus d'énergie que continuer.",                          "Restaure instantanément une partie des points de vie.",                             RI + "Second_Souffle.png",         5),
            new Node("Infatigable",           "Passif", "Chaque coup arrêté prouve qu'il peut en encaisser un autre.",               "Chaque blocage réussi restaure une partie des points de vie.",                      RI + "Infatigable.png",            5),
            new Node("Représailles",          "Passif", "Plus l'ennemi insiste, plus la réponse sera douloureuse.",                  "Après un blocage réussi, la prochaine attaque inflige davantage de dégâts.",        RI + "Contre_Offensif.png",        5),
            new Node("Provocation",           "Actif",  "Attire la haine pour protéger les siens.",                                  "Force les ennemis proches à t'attaquer pendant quelques secondes.",                 RI + "Provocation.png",            5),
            new Node("Dernier Bastion",       "Passif", "Sous 30 %, le rempart ne cède pas — il résiste.",                           "Sous 30 % de points de vie, subis moins de dégâts.",                               RI + "Dernier_Bastion.png",        5),
            new Node("Garde Rapprochée",      "Actif",  "Aucun allié ne tombera tant qu'il sera là.",                                "Réduit les dégâts subis par toi et les alliés proches pendant quelques secondes.",  RI + "Garde Rapprochée.png",       5),
        });
    }

    @Nonnull
    public static Node[] getTree(@Nonnull PlayerClass cls) {
        Node[] nodes = TREES.get(cls);
        if (nodes == null) throw new IllegalStateException("No talent tree for class: " + cls);
        return nodes;
    }

    @Nullable
    public static Node[] getSpecTree(@Nonnull PlayerSpecialization spec) {
        return SPEC_TREES.get(spec);
    }

    private ClassTalentTree() {}
}
