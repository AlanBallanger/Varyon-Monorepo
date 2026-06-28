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
            new Node("Déchaînement",          "Passif", "Une hache, deux haches, peu importe, tout saigne.",                   "Enchaîne automatiquement plusieurs frappes.",                      "Weapon_Battleaxe_Onyxium",   5),
            new Node("Indomptable",           "Actif",  "Ni magie ni acier, rien ne peut l'arrêter.",                          "Devient brièvement invulnérable.",                                 "Armor_Adamantite_Chest",     5),
            new Node("Colère Primordiale",    "Passif", "Au fond de chaque barbare dort un titan.",                             "Augmente massivement les dégâts sur la dernière cible frappée.",   "Weapon_Battleaxe_Adamantite",5),
        });

        TREES.put(PlayerClass.MAGE, new Node[]{
            new Node("Maîtrise des Sorts",    "Passif", "Comprendre un sort, c'est déjà à moitié le lancer.",                  "Augmente les dégâts de sorts.",                                    "Weapon_Staff_Mithril",       5),
            new Node("Réserve de Mana",       "Passif", "Un mage à cours de mana est juste un homme avec un bâton.",           "Augmente la réserve de mana maximum.",                             "Potion_Mana",                5),
            new Node("Concentration Arcane",  "Passif", "L'esprit est la véritable arme du mage.",                             "Augmente le multiplicateur de dégâts critiques des sorts.",        "Ingredient_Crystal_Purple",  5),
            new Node("Bouclier Arcanique",    "Actif",  "La magie protège autant qu'elle détruit.",                             "Absorbe les dégâts pendant un bref instant.",                      "Weapon_Wand_Stoneskin",      5),
            new Node("Catalyseur",            "Passif", "Moins de temps à incanter, plus de temps à détruire.",                "Réduit le temps de recharge des sorts.",                           "Ingredient_Crystal_Cyan",    5),
            new Node("Maîtrise du Feu",       "Passif", "Le feu ne brûle pas ceux qui le comprennent.",                        "Augmente les dégâts des sorts de feu.",                            "Ingredient_Fire_Essence",    5),
            new Node("Maîtrise du Froid",     "Passif", "Le gel ralentit tout, sauf lui.",                                    "Augmente les dégâts et la durée des effets de gel.",               "Ingredient_Ice_Essence",     5),
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
            new Node("Entaille Profonde",      "Passif", "Même une petite entaille peut décider d'un duel.",                         "Les attaques ont une chance d'infliger un saignement.",             DI + "Blessure_Ouverte.png",       5),
            new Node("Coup d'Estoc",          "Actif",  "La pointe trouve toujours une faille que l'épée ignore.",                "Frappe en AoE autour du joueur, puis arme le prochain coup d'un multiplicateur de dégâts.", DI + "Coup_D_estoc.png", 5),
            new Node("Riposte Parfaite",      "Actif",  "La meilleure ouverture est celle qu'on vous offre.",                     "Entre en posture défensive. Si une attaque est reçue, contre-attaque instantanément.", DI + "Riposte_Parfaite.png",  5),
            new Node("Ascendant",              "Passif", "Un duel est plus beau sans y laisser une goutte de sang.",                "Après une parade réussie, la prochaine attaque inflige davantage de dégâts.", DI + "Contre_Attaque.png",    5),
            new Node("Feinte",                "Actif",  "Montrer une faille pour en créer une vraie, c'est tout l'art du duel.", "Le prochain coup ne peut être ni bloqué ni paré par l'adversaire.", DI + "Feinte.png",                 5),
            new Node("Jeu de Jambes",          "Passif", "Une lame ne touche que ce qu'elle peut atteindre.",                      "Augmente les chances d'esquive.",                                   DI + "Esquive_du_bretteur.png",    5),
            new Node("Frappe Précise",          "Passif", "Il suffit parfois d'un seul coup bien placé.",                            "Les coups critiques infligent des dégâts supplémentaires.",          DI + "Frappe_Précise.png",         5),
            new Node("Désarmement",           "Actif",  "Ôter l'arme, c'est ôter la menace avant même qu'elle frappe.",           "Réduit temporairement les dégâts infligés par la cible.",           DI + "Désarmement.png",            5),
            new Node("Momentum",              "Passif", "Chaque coup sans en recevoir rend le suivant un peu plus dévastateur.",   "Chaque coup consécutif sans en recevoir augmente les dégâts.", DI + "Momentum.png",           5),
            new Node("Déferlante",             "Actif",  "Une fois lancé, rien ne l'arrête.",                                       "Augmente les dégâts d'attaque et la vitesse de déplacement pendant quelques secondes.", DI + "Assaut_Du_Bretteur.png", 5),
        });

        final String OI = "Classes_Icons/Shadow/";
        SPEC_TREES.put(PlayerSpecialization.OMBRE, new Node[]{
            new Node("Pas des Ténèbres",      "Actif",  "L'ombre n'attend pas, elle frappe et disparaît.",                          "Ruée vers l'arrière suivie d'une courte invisibilité.",                              OI + "Pas_Des_Tenebres.png",      5),
            new Node("Mort Éclair",            "Passif", "Tu t'appelles Flash ?",                                                     "Gain d'expérience augmenté si la cible est tuée en moins de 5 secondes.",            OI + "Execution_Rapide.png",      5),
            new Node("Lames Empoisonnées",    "Passif", "Rien de tel qu'une mort lente et douloureuse.",                              "Les attaques ont une chance d'empoisonner la cible.",                                OI + "Lames_Empoisonnees.png",    5),
            new Node("Déluge de Lames",       "Actif",  "Quand une lame ne suffit pas, on en envoie cinq.",                          "Enchaînement rapide de frappes infligeant des dégâts à la cible devant soi.",       OI + "Deluge_De_Lames.png",       5),
            new Node("Écran de Fumée",        "Actif",  "Disparaître règle bien des problèmes.",                                      "Devient invisible pendant quelques secondes, bloquant les dégâts reçus.",           OI + "Ecran_De_Fumee.png",        5),
            new Node("Embuscade",             "Passif", "Votre charisme est tel qu'il paralyse vos cibles.",                          "La première attaque après une invisibilité immobilise brièvement la cible.",         OI + "Embuscade.png",             5),
            new Node("Frappe Parfaite",        "Actif",  "Un coup peut parfois être décisif.",                                         "Le prochain coup inflige des dégâts significativement accrus.",                      OI + "Frappe_Fatale.png",         5),
            new Node("Silhouette Fantôme",    "Passif", "Le mouvement évite bien des blessures.",                                     "Augmente les chances d'esquive.",                                                    OI + "Ombre_Insaisissable.png",   5),
            new Node("Traque",                 "Passif", "L'adrénaline aime les coups bien placés.",                                  "Après un coup critique, gagne un bonus de vitesse de déplacement pendant 3 sec.",   OI + "Danse_Des_Lames.png",       5),
            new Node("Traversée Obscure",      "Actif",  "La distance n'existe plus lorsqu'une cible est choisie.",                   "Se téléporte derrière la cible et frappe. Bonus si la cible est à moins de 30 % PV.", OI + "Pas_De_L_Ombre.png",      5),
            new Node("Instinct de Survie",    "Passif", "Sous 30 %, l'ombre ne meurt pas, elle s'adapte.",                          "Sous 30 % de points de vie, les chances d'esquive sont fortement augmentées.",       OI + "Instict_De_Survie.png",     5),
            new Node("Verrouillage Mortel",    "Actif",  "Toi, je ne te lâche plus.",                                                  "Marque une cible : toutes les attaques contre elle sont des coups critiques.",       OI + "Chasse_Ouverte.png",        5),
        });

        final String BI = "Classes_Icons/Berserker/";
        SPEC_TREES.put(PlayerSpecialization.BERSERKER, new Node[]{
            new Node("Assaut Bestial",        "Actif",  "Quand la bête charge, même les murs cèdent.",                              "Bond en avant dans la direction du regard, infligeant des dégâts à l'impact.",                         BI + "Assaut_Bestial.png",         5),
            new Node("Carnage",               "Passif", "Chaque mort est une leçon ensanglantée.",                                 "Gain d'expérience augmenté lors des séries d'éliminations (max 5 cumuls).",                             BI + "Carnage.png",                5),
            new Node("Fureur Sanguinaire",    "Passif", "Le sang ennemi nourrit la rage qui nourrit les coups.",                   "Après un kill, le vol de vie est augmenté pendant quelques secondes.",                                   BI + "Fureur_Sanguinaire.png",     5),
            new Node("Frénésie",              "Passif", "Chaque mort allume une flamme de plus dans ses yeux.",                    "Chaque élimination augmente vitesse et dégâts pendant quelques secondes (max 3 cumuls).",               BI + "Frénésie.png",               5),
            new Node("Dix pour Sang",         "Actif",  "Chaque goutte versée est un gage de puissance.",                          "Frappe puissante en zone qui consomme 10% de tes PV.",                                          BI + "Dix_Pour_Sang.png",          5),
            new Node("Cor de Guerre",         "Actif",  "Le son du cor précède toujours la tempête.",                              "Augmente ta vitesse de déplacement pendant quelques secondes.",                                          BI + "Cor_De_Guerre.png",          5),
            new Node("Blessures Profondes",   "Passif", "Là où sa hache passe, les plaies ne se referment pas.",                   "Les attaques ont une chance d'infliger un saignement.",                                      BI + "Blessures_Profondes.png",    5),
            new Node("Ferveur Guerrière",     "Passif", "Plus la bataille dure, plus la rage se cristallise.",                     "Les dégâts augmentent d'un cran chaque seconde passée en combat (max 10 cumuls).",                      BI + "Ferveur_Guerrière.png",      5),
            new Node("Cri de Ralliement",     "Actif",  "Un hurlement qui fait oublier la peur et décuple la hargne.",             "Augmente temporairement les dégâts de toi et des alliés proches.",                                      BI + "Cri_De_Ralliement.png",      5),
            new Node("Déchiquetage",          "Actif",  "La chair cède, et avec elle une partie de son énergie vitale.",           "Le prochain coup tranche profondément et récupère une partie des dégâts infligés en PV.",               BI + "Eviscération.png",           5),
            new Node("Dernier Souffle",       "Passif", "La mort attendra, il a encore des ennemis à abattre.",                   "Ignore temporairement la mort.",                                            BI + "Dernier_Souffle.png",        5),
            new Node("Exécution Sauvage",     "Actif",  "Pour les blessés, chaque coup est le dernier.",                           "Frappe dévastatrice infligeant davantage de dégâts aux cibles sous 50 % PV.",                        BI + "Exécution_Sauvage.png",      5),
        });

        final String RVI = "Classes_Icons/Ravager/";
        SPEC_TREES.put(PlayerSpecialization.RAVAGEUR, new Node[]{
            new Node("Bond Écrasant",          "Actif",  "Il ne court pas vers ses ennemis, il s'abat sur eux.",                    "Bond en avant et s'écrase au sol, infligeant des dégâts en zone à l'atterrissage.",               RVI + "Bond_Ecrasant.png",              5),
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
            new Node("Rabattage",              "Actif",  "Il ne chasse pas, il ramène le troupeau à lui.",                            "Balayage en arc devant soi, attirant les ennemis touchés et les étourdissant brièvement.",        RVI + "Rabattage.png",                 5),
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
            new Node("Déluge de Coups",      "Actif",  "Pas le temps de souffler, pas le temps de répondre.",                       "Enchaîne rapidement cinq frappes successives sur la cible.",                                  FI + "Déluge_De_Coups.png",            5),
            new Node("Second Souffle",       "Actif",  "Quand tout semble perdu, il trouve une réserve cachée.",                     "Restaure une partie des points de vie et de l'endurance.",                                   FI + "Second_Souffle.png",             5),
            new Node("Uppercut",             "Actif",  "Un uppercut qui envoie l'ennemi vers le ciel.",                              "Coup ascendant qui inflige des dégâts et projette la cible en l'air.",                       FI + "Uppercut.png",                   5),
        });

        final String GGI = "Classes_Icons/Guardian/";
        SPEC_TREES.put(PlayerSpecialization.GARDIEN_DE_GAIA, new Node[]{
            new Node("Évasion Sylvestre",     "Actif",  "La forêt s'ouvre pour laisser passage, et se referme aussitôt.",        "Téléportation magique en arrière.",                                                         GGI + "Evasion_Sylvestre.png",          5),
            new Node("Harmonie Naturelle",    "Passif", "La nature récompense ceux qui restent en bonne santé.",                  "Gain d'expérience augmenté lorsque les PV sont supérieurs à 80 %.",                         GGI + "Harmonie_Naturelle.png",         5),
            new Node("Bénédiction de Gaïa",  "Actif",  "La terre s'éveille et verse sa vie sur ceux qui la servent.",            "Soigne instantanément les alliés dans une zone.",                                           GGI + "Bénédiction_De_Gaia.png",       5),
            new Node("Écorce Protectrice",   "Actif",  "La vie de la forêt enveloppe la cible comme une armure vivante.",        "Réduit les dégâts reçus par la cible pendant quelques secondes et la soigne.",              GGI + "Ecorce_Protectrice.png",         5),
            new Node("Appel du Tréant",      "Actif",  "D'une graine plantée dans le sol naît un gardien millénaire.",           "Invoque un gardien végétal qui combat à vos côtés.",                                        GGI + "Appel_Du_Tréant.png",           5),
            new Node("Lien Spirituel",       "Passif", "Ce que fait l'invocation résonne dans l'âme du druide.",                  "Une partie des dégâts infligés par les invocations vous soigne.",                           GGI + "Lien_Spirituel.png",             5),
            new Node("Étreinte de Gaïa",     "Actif",  "La terre elle-même saisit les ennemis pour les retenir.",                "Lance un projectile qui en explosant immobilise les ennemis dans une zone.",                 GGI + "Etreinte_De_Gaia.png",           5),
            new Node("Gardien de la Nature", "Passif", "Les créatures de la forêt sont plus robustes sous sa protection.",       "Augmente les points de vie des invocations.",                                                GGI + "Gardien_De_La_Nature.png",       5),
            new Node("Cycle de Vie",         "Passif", "Soigner, c'est recevoir en retour, le cycle ne s'arrête jamais.",       "Soigner un allié restaure une petite quantité de mana.",                                    GGI + "Cycle_De_Vie.png",              5),
            new Node("Grâce de Gaïa",        "Passif", "Gaïa donne plus à ceux qui en ont le plus besoin.",                      "Les soins sont plus efficaces sur les cibles ayant peu de points de vie.",                   GGI + "Grace_De_Gaia.png",              5),
            new Node("Souffle de la Nature", "Passif", "Un vent tiède qui redonne forces et vigueur à ceux qui l'écoutent.",     "Régénère la vie et l'endurance des alliés proches en permanence.",                          GGI + "Souffle_De_La_Nature.png",       5),
            new Node("Marque de Renaissance","Actif",  "La mort elle-même hésite devant le sceau de Gaïa.",                      "Protège la cible de la mort pendant une courte durée et lui octroie de la jauge spéciale.",  GGI + "Marque_De_Renaissance.png",      5),
        });

        final String VI = "Classes_Icons/Voodoo/";
        SPEC_TREES.put(PlayerSpecialization.VAUDOU, new Node[]{
            new Node("Passage Éthéré",        "Actif",  "L'âme quitte le corps, et le retrouve de l'autre côté.",          "Se téléporte à la position symétrique de la cible par rapport à soi, face à elle.",  VI + "Passage_Ethere.png",           5),
            new Node("Féticheur",             "Passif", "La mort dans les bras, l'expérience afflue.",                       "Gain d'expérience augmenté si la cible meurt à moins de 5 blocs.",                  VI + "Feticheur.png",                5),
            new Node("Fléau Toxique",         "Actif",  "Un venin lent, mais inexorable.",                                  "Empoisonne la cible et réduit ses dégâts infligés.",                                 VI + "Fleau_Toxique.png",            5),
            new Node("Totem d'Entrave",       "Actif",  "Le totem chuchote, et les jambes refusent d'avancer.",            "Déploie un totem qui ralentit les ennemis dans sa zone.",                            VI + "Totem_Entrave.png",            5),
            new Node("Attaque Perfide",       "Actif",  "L'honnêteté est pour ceux qui perdent.",                           "Frappe de mêlée, bonus de 50 % si dans le dos de la cible.",                       VI + "Attaque_Perfide.png",          5),
            new Node("Toxines",               "Passif", "Ses poisons résistent au temps mieux qu'à la chair.",              "Le poison dure plus longtemps.",                                                     VI + "Toxines.png",                  5),
            new Node("Totem de Vulnérabilité","Actif",  "Sous l'influence du totem, chaque coup résonne plus fort.",        "Déploie un totem qui augmente les dégâts subis par les ennemis dans la zone.",      VI + "Totem_Vulnerabilite.png",      5),
            new Node("Parasite Spirituel",    "Passif", "Les malédictions ne font pas que blesser, elles nourrissent.",    "Frapper une cible maudite restaure des PV.",                                         VI + "Parasite_Spirituel.png",       5),
            new Node("Ancrage Rituel",        "Passif", "Le rituel gravé dans le sol tient plus longtemps.",                "Les totems restent actifs plus longtemps.",                                          VI + "Ancrage_Rituel.png",           5),
            new Node("Rituel Interdit",       "Passif", "Les anciens avaient raison d'interdire cela.",                     "Inflige davantage de dégâts aux cibles affectées par une malédiction.",              VI + "Rituel_Interdit.png",          5),
            new Node("Extraction d'Âme",     "Actif",  "Arracher un fragment d'âme, et s'en nourrir.",                   "Frappe de mêlée qui récupère une partie des dégâts infligés en points de vie.",     VI + "Extraction_Ame.png",           5),
            new Node("Présence Oppressante",  "Passif", "Simplement être là suffit à briser la confiance de l'adversaire.", "Les ennemis proches infligent moins de dégâts.",                                     VI + "Presence_Oppressante.png",     5),
        });

        final String AI = "Classes_Icons/Arcanist/";
        SPEC_TREES.put(PlayerSpecialization.ARCANISTE, new Node[]{
            new Node("Distorsion",          "Actif",  "La magie plie l'espace, et lui permet de disparaître.",              "Téléportation magique en arrière.",                                                         AI + "Distorsion.png",           5),
            new Node("Talent Inné",         "Passif", "La distance est son terrain naturel.",                                  "Gain d'expérience augmenté lorsque tu es à plus de 8 blocs de la cible.",                   AI + "Talent_Inné.png",          5),
            new Node("Boule de Feu",        "Actif",  "Une sphère de flamme qui consume tout ce qu'elle touche.",             "Lance une boule de feu qui explose à l'impact.",                                            AI + "Boule_De_Feu.png",         5),
            new Node("Écho Arcanique",      "Passif", "Parfois, le sort ne consomme rien, comme si la magie se répétait.", "Les sorts ont une chance de ne pas déclencher leur temps de recharge.",                     AI + "Echo_Arcanique.png",       5),
            new Node("Salve de Givre",      "Actif",  "Une tempête de glace qui laisse ses ennemis figés sur place.",        "Lance plusieurs projectiles glacés qui ralentissent les cibles touchées.",                   AI + "Salve_De_Givre.png",       5),
            new Node("Puits de Mana",       "Passif", "Son mana est un océan que peu peuvent égaler.",                        "Augmente le mana maximum.",                                                                  AI + "Puits_De_Mana.png",        5),
            new Node("Écho Temporel",       "Passif", "Le temps ralentit pour lui, jamais pour ses ennemis.",                 "Réduit le temps de recharge des compétences actives.",                                      AI + "Echo_Temporel.png",        5),
            new Node("Nova de Givre",       "Actif",  "Le froid jaillit de lui comme une explosion silencieuse.",             "Inflige des dégâts aux ennemis proches et les ralentit.",                                   AI + "Nova_De_Givre.png",        5),
            new Node("Surcharge",           "Actif",  "Trop de pouvoir, et pourtant jamais assez.",                          "Restaure du mana et augmente temporairement les dégâts des sorts.",                        AI + "Surcharge.png",            5),
            new Node("Drain Mystique",      "Passif", "Chaque mort nourrit sa réserve arcanique.",                            "Les éliminations restaurent une partie du mana.",                                           AI + "Drain_Mystique.png",       5),
            new Node("Météore",             "Actif",  "Il appelle l'extinction depuis les cieux.",                            "Lance un projectile explosif qui s'abat sur une zone.",                                     AI + "Météore.png",              5),
            new Node("Pouvoir Grandissant", "Passif", "L'inviolabilité forge une puissance silencieuse.",                     "Les dégâts des sorts augmentent tant que tu n'as pas subi de dégâts.",                      AI + "Pouvoir_Grandissant.png",  5),
        });

        final String ROI = "Classes_Icons/Rodder/";
        SPEC_TREES.put(PlayerSpecialization.RODEUR, new Node[]{
            new Node("Recul Stratégique",    "Actif",  "Fuir n'est pas une faiblesse, c'est une tactique.",                 "Ruée vers l'arrière puis gagne temporairement de la vitesse.",                          ROI + "Recul_Stratégique.png",    5),
            new Node("Œil du Chasseur",      "Passif", "À cette distance, la cible n'a aucune chance.",                      "Gain d'expérience augmenté lorsque la cible est à plus de 10 mètres.",                  ROI + "Oeil_Du_Chasseur.png",     5),
            new Node("Pluie de Flèches",     "Actif",  "Quand une flèche ne suffit pas, le ciel en envoie une douzaine.",    "Bombarde une zone de projectiles pendant quelques secondes.",                           ROI + "Pluie_De_Flèches.png",     5),
            new Node("Marque du Chasseur",   "Actif",  "Une fois marqué, l'ennemi devient une proie.",                       "Marque une cible, augmentant les dégâts qu'elle subit.",                                ROI + "Marque_Du_Chasseur.png",   5),
            new Node("Flèche de Recul",      "Actif",  "Parfois, créer de la distance vaut mieux que d'avancer.",            "Tire une flèche qui repousse fortement la cible.",                                      ROI + "Flèche_De_Recul.png",      5),
            new Node("Flèche Entravante",    "Actif",  "Une flèche au bon endroit cloue les pieds au sol.",                  "Tire une flèche qui immobilise brièvement la cible.",                                   ROI + "Flèche_Entravante.png",    5),
            new Node("Flèches Toxiques",     "Passif", "Chaque égratignure laisse une trace invisible.",                     "Les attaques ont une chance d'empoisonner la cible.",                                   ROI + "Flèches_Toxiques.png",     5),
            new Node("Instinct de Survie",   "Passif", "Le chasseur sent le danger avant qu'il arrive.",                     "Augmente les chances d'esquiver les attaques.",                                         ROI + "Instinct_De_Survie.png",   5),
            new Node("Précision Mortelle",   "Passif", "Les blessés tombent plus vite.",                                     "Inflige plus de dégâts aux ennemis ayant moins de 50 % de PV.",                        ROI + "Précision_Mortelle.png",   5),
            new Node("Traque Mobile",        "Passif", "Il ne s'arrête jamais, et ses dégâts non plus.",                    "Augmente les dégâts lorsque le personnage est en déplacement.",                         ROI + "Traque_Mobile.png",        5),
            new Node("Traque sans Fin",      "Passif", "Une proie tuée libère la chasse pour une autre.",                    "Tuer une cible marquée réinitialise le Délai de Marque du Chasseur.",                     ROI + "Traque_Sans_Fin.png",      5),
            new Node("Rafale",               "Actif",  "Quand le moment est venu, on ne tire qu'une seule fois, plusieurs.", "Tire plusieurs flèches en succession rapide.",                                         ROI + "Rafale.png",               5),
        });

        final String ABI = "Classes_Icons/Crossbowman/";
        SPEC_TREES.put(PlayerSpecialization.ARBALETRIER, new Node[]{
            new Node("Recul Tactique",       "Actif",  "Reculer pour mieux frapper.",                                        "Ruée vers l'arrière, le prochain carreau inflige des dégâts augmentés.",                                                          ABI + "Recul_Tactique.png",       5),
            new Node("Tireur d'Élite",       "Passif", "La portée, c'est l'avantage.",                                       "Gain d'expérience augmenté lorsque la cible est à plus de 15 mètres.",                                                            ABI + "Tireur_D_Elite.png",       5),
            new Node("Carreau Lourd",        "Actif",  "Un carreau, une sentence.",                                          "Le prochain carreau inflige des dégâts considérablement augmentés.",                                                               ABI + "Carreau_Lourd.png",        5),
            new Node("Carreau Explosif",     "Actif",  "L'explosion fait plus de dégâts que la pointe.",                     "Tire un carreau qui explose à l'impact, infligeant des dégâts de zone.",                                                           ABI + "Carreau_Explosif.png",     5),
            new Node("Carreau Transperçant", "Actif",  "L'armure ne fait que ralentir ce qui est inévitable.",               "Tire un carreau qui traverse les ennemis sur sa trajectoire et ralentit fortement les cibles.",                                     ABI + "Carreau_Transpercant.png", 5),
            new Node("Coup de Botte",        "Actif",  "Quand le carreau est rechargé, le pied parle.",                      "Met un violent coup de pied qui fait reculer les adversaires.",                                                                     ABI + "Coup_De_Botte.png",        5),
            new Node("Chasseur de Colosses", "Passif", "Plus la proie est grande, plus la chasse est belle.",                "Inflige davantage de dégâts aux ennemis ayant plus de points de vie que vous.",                                                     ABI + "Chasseur_De_Colosses.png", 5),
            new Node("Tireur Embusqué",      "Passif", "L'immobilité est une arme.",                                         "Les dégâts augmentent après être resté immobile pendant quelques secondes.",                                                        ABI + "Tireur_Embusqué.png",      5),
            new Node("Viseur Expérimenté",   "Passif", "Plus la cible est loin, plus le tir est précis.",                    "Les dégâts augmentent plus la cible est éloignée.",                                                                                 ABI + "Viseur_Expérimenté.png",   5),
            new Node("Carreaux Lacérants",   "Passif", "Chaque carreau laisse une marque qui saigne.",                       "Les attaques appliquent un saignement.",                                                                                             ABI + "Carreau_Lacérant.png",     5),
            new Node("Réflexes Affûtés",     "Passif", "Son instinct le fait bouger avant même qu'il décide.",               "Augmente les chances d'esquive.",                                                                                                   ABI + "Réflexes_Affutés.png",     5),
            new Node("Mise en Joue",         "Actif",  "Prendre le temps d'ajuster, pour ne jamais manquer.",               "Prend le temps d'ajuster son tir. Le prochain carreau est garanti critique et inflige des dégâts considérablement augmentés.",     ABI + "Mise_En_Joue.png",         5),
        });

        final String LCI = "Classes_Icons/Lancer/";
        SPEC_TREES.put(PlayerSpecialization.LANCIER, new Node[]{
            new Node("Percée",                "Actif",  "Reculer pour mieux piquer.",                                          "Ruée vers l'arrière.",                                                                       LCI + "Percée.png",                5),
            new Node("Discipline",            "Passif", "L'expérience forge ceux qui restent debout.",                          "Gain d'expérience augmenté lorsque les PV sont supérieurs à 50 %.",                          LCI + "Discipline.png",            5),
            new Node("Charge Héroïque",       "Actif",  "Il ne charge pas, il renverse.",                                     "Charge une cible et l'étourdit brièvement.",                                                 LCI + "Charge_Héroique.png",       5),
            new Node("Garde du Lancier",      "Actif",  "Un instant de calme avant la tempête.",                               "Adopte une posture défensive. La prochaine attaque reçue dans les 3 secondes est annulée et arme un coup dévastateur.", LCI + "Garde_Du_Lancier.png", 5),
            new Node("Harponnage",            "Actif",  "La lance revient toujours à son lanceur, avec la proie.",            "Lance sa lance, inflige des dégâts et attire la cible vers soi.",                            LCI + "Harponnage.png",            5),
            new Node("Formation de Piques",   "Actif",  "Un mur de pointes que l'ennemi doit traverser.",                     "Crée une zone devant soi. Les ennemis qui la traversent subissent des dégâts et sont ralentis.", LCI + "Formation_De_Piques.png",   5),
            new Node("Posture Dominante",     "Passif", "Toucher d'abord, c'est dicter les règles du combat.",                 "Après avoir touché un ennemi en mêlée, les dégâts reçus sont réduits pendant quelques secondes.", LCI + "Posture_Dominante.png",    5),
            new Node("Perce-Cœur",            "Passif", "Un coup critique bien placé laisse une blessure qui dure.",           "Les coups critiques infligent un saignement.",                                               LCI + "Perce_Coeur.png",           5),
            new Node("Chasseur de Géants",    "Passif", "La taille est un avantage, jusqu'à ce que tu te retrouves face à lui.", "Inflige davantage de dégâts aux ennemis ayant plus de PV maximum que vous.",             LCI + "Chasseur_De_Géants.png",    5),
            new Node("Briseur de Ligne",      "Passif", "Repousser, attirer, étourdir, puis frapper.",                       "Les ennemis repoussés, attirés ou étourdis subissent davantage de dégâts pendant quelques secondes.", LCI + "Controle_De_L_Espace.png", 5),
            new Node("Portée Maîtrisée",      "Passif", "La lance a deux portées idéales, et toutes les deux font mal.",     "Inflige davantage de dégâts aux ennemis à moins de 3 mètres ou à plus de 10 mètres.",       LCI + "Portée_Maitrisée.png",      5),
            new Node("Empalement",            "Actif",  "La lance entre, la proie ne sort plus.",                              "Coup de lance précis qui inflige un saignement et immobilise brièvement la cible.",           LCI + "Empalement.png",            5),
        });

        final String RI = "Classes_Icons/Rampart/";
        SPEC_TREES.put(PlayerSpecialization.REMPART, new Node[]{
            new Node("Collision",              "Actif",  "Mieux vaut ne pas être sur son passage.",                                   "Charge en ligne droite en repoussant et frappant les ennemis sur le passage.",     RI + "Charge_Lourde.png",         5),
            new Node("Protecteur",             "Passif", "L'expérience nous apprend à bien choisir son équipement.",                  "Gain d'expérience de classe augmenté lorsqu'un bouclier est équipé.",               RI + "Maitre_Du_Bouclier.png",     5),
            new Node("Santé de Fer",           "Passif", "Certains hommes portent une armure. D'autres en sont une.",                 "Augmente les points de vie maximum.",                                               RI + "Constitution_De_Fer.png",    5),
            new Node("Brise Crâne",            "Actif",  "Un bouclier a plus d'un usage.",                                            "Frappe la cible avec le bouclier, l'étourdissant brièvement.",                      RI + "Coup_De_Bouclier.png",       5),
            new Node("Forteresse",            "Actif",  "Impossible de vous faire bouger désormais.",                                "Réduit massivement les dégâts reçus pendant quelques secondes.",                    RI + "Forteresse.png",             5),
            new Node("Fortifications",        "Passif", "Chaque coup absorbé renforce le rempart.",                                   "Après un blocage réussi, les dégâts reçus sont réduits pendant quelques secondes.", RI + "Garde_Impénétrable.png",     5),
            new Node("Second Souffle",        "Actif",  "Abandonner demande plus d'énergie que continuer.",                          "Restaure instantanément une partie des points de vie.",                             RI + "Second_Souffle.png",         5),
            new Node("Infatigable",           "Passif", "Chaque coup arrêté prouve qu'il peut en encaisser un autre.",               "Chaque blocage réussi restaure une partie des points de vie.",                      RI + "Infatigable.png",            5),
            new Node("Riposte Lourde",         "Passif", "Il n'avait qu'à pas commencer.",                                            "Après un blocage réussi, la prochaine attaque inflige davantage de dégâts.",        RI + "Contre_Offensif.png",        5),
            new Node("Provocation",           "Actif",  "Serrez les rangs ! Je vous couvre !",                                       "Force les ennemis proches à t'attaquer pendant quelques secondes.",                 RI + "Provocation.png",            5),
            new Node("Dernier Bastion",       "Passif", "Sous 30 %, le rempart ne cède pas, il résiste.",                           "Sous 30 % de points de vie, subis moins de dégâts.",                               RI + "Dernier_Bastion.png",        5),
            new Node("Garde Rapprochée",      "Actif",  "Là où se dresse le rempart, les siens trouvent refuge.",                    "Réduit les dégâts subis par toi et les alliés proches pendant quelques secondes.",  RI + "Garde Rapprochée.png",       5),
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
