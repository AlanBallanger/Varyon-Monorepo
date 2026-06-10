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
            new Node("Assaut Éclair",         "Actif",  "Une botte parfaite ne laisse aucune fenêtre à l'adversaire.",             "Ruée vers l'avant, infligeant des dégâts à l'impact.",             DI + "Percée.png",                5),
            new Node("Expert en Duel",        "Passif", "La prudence du bretteur, c'est de toujours avoir l'avantage.",            "Gain d'expérience augmenté lorsque les PV sont supérieurs à 70 %.", DI + "Expert_En_Duel.png",         5),
            new Node("Blessure Ouverte",      "Passif", "Une lame qui accroche, c'est une victoire qui se compte en secondes.",    "Les attaques ont une chance d'infliger un saignement.",             DI + "Blessure_Ouverte.png",       5),
            new Node("Coup d'Estoc",          "Actif",  "Une frappe tranchante qui ouvre le combat — ou le clôt.",                "Frappe en AoE autour du joueur, puis arme le prochain coup d'un multiplicateur de dégâts.", DI + "Coup_D_estoc.png", 5),
            new Node("Riposte Parfaite",      "Actif",  "Absorber le coup pour mieux le rendre — trois fois.",                    "Entre en posture défensive. Si une attaque est reçue, contre-attaque instantanément.", DI + "Riposte_Parfaite.png",  5),
            new Node("Contre-Attaque",        "Passif", "La parade n'est pas une fin — c'est une invitation.",                    "Après une parade réussie, la prochaine attaque inflige davantage de dégâts.", DI + "Contre_Attaque.png",    5),
            new Node("Feinte",                "Actif",  "Montrer une faille pour en créer une vraie — c'est tout l'art du duel.", "Le prochain coup ne peut être ni bloqué ni paré par l'adversaire.", DI + "Feinte.png",                 5),
            new Node("Esquive du Bretteur",   "Passif", "Le sol sous ses pieds n'est qu'un appui — il n'y reste jamais.",         "Augmente les chances d'esquive.",                                   DI + "Esquive_du_bretteur.png",    5),
            new Node("Frappe Précise",        "Passif", "L'endroit précis, au moment précis — l'armure ne compte plus.",          "Les coups critiques infligent des dégâts supplémentaires.",          DI + "Frappe_Précise.png",         5),
            new Node("Désarmement",           "Actif",  "Ôter l'arme, c'est ôter la menace avant même qu'elle frappe.",           "Réduit temporairement les dégâts infligés par la cible.",           DI + "Désarmement.png",            5),
            new Node("Momentum",              "Passif", "Chaque coup sans en recevoir rend le suivant un peu plus dévastateur.",   "Chaque coup consécutif sans en recevoir augmente légèrement les dégâts.", DI + "Momentum.png",           5),
            new Node("Assaut du Bretteur",    "Actif",  "Quand l'élan est là, rien ne peut l'arrêter — pas même l'ennemi.",       "Augmente les dégâts d'attaque et la vitesse de déplacement pendant quelques secondes.", DI + "Assaut_Du_Bretteur.png", 5),
        });

        final String OI = "Classes_Icons/Shadow/";
        SPEC_TREES.put(PlayerSpecialization.OMBRE, new Node[]{
            new Node("Pas des Ténèbres",      "Actif",  "L'ombre n'attend pas — elle frappe et disparaît.",                          "Ruée vers l'arrière suivie d'une courte invisibilité.",                              OI + "Pas_Des_Tenebres.png",      5),
            new Node("Exécution Rapide",      "Passif", "Le prédateur qui hésite perd sa proie.",                                    "Gain d'expérience augmenté si la cible est tuée en moins de 5 secondes.",            OI + "Execution_Rapide.png",      5),
            new Node("Lames Empoisonnées",    "Passif", "Une égratignure anodine — pour l'instant.",                                 "Les attaques ont une chance d'empoisonner la cible.",                                OI + "Lames_Empoisonnees.png",    5),
            new Node("Déluge de Lames",       "Actif",  "Quand une lame ne suffit pas, on en envoie cinq.",                          "Enchaînement rapide de frappes infligeant des dégâts à la cible devant soi.",       OI + "Deluge_De_Lames.png",       5),
            new Node("Écran de Fumée",        "Actif",  "Disparaître n'est pas fuir — c'est choisir le bon moment.",                 "Devient invisible pendant quelques secondes, bloquant les dégâts reçus.",           OI + "Ecran_De_Fumee.png",        5),
            new Node("Embuscade",             "Passif", "La première frappe depuis l'ombre laisse peu de place à la réaction.",      "La première attaque après une invisibilité immobilise brièvement la cible.",         OI + "Embuscade.png",             5),
            new Node("Frappe Fatale",         "Actif",  "Un seul coup, au bon endroit, peut tout changer.",                          "Le prochain coup inflige des dégâts significativement accrus.",                      OI + "Frappe_Fatale.png",         5),
            new Node("Ombre Insaisissable",   "Passif", "Toucher l'ombre, c'est attraper le vent.",                                  "Augmente les chances d'esquive.",                                                    OI + "Ombre_Insaisissable.png",   5),
            new Node("Danse des Lames",       "Passif", "Chaque coup critique est une invitation à accélérer.",                      "Après un coup critique, gagne un bonus de vitesse de déplacement pendant 3 sec.",   OI + "Danse_Des_Lames.png",       5),
            new Node("Pas de l'Ombre",        "Actif",  "Surgir de nulle part, frapper, disparaître — telle est la voie.",           "Se téléporte derrière la cible et frappe. Bonus si la cible est à moins de 30 % HP.", OI + "Pas_De_L_Ombre.png",      5),
            new Node("Instinct de Survie",    "Passif", "Sous 30 %, l'ombre ne meurt pas — elle s'adapte.",                          "Sous 30 % de points de vie, les chances d'esquive sont fortement augmentées.",       OI + "Instict_De_Survie.png",     5),
            new Node("Chasse Ouverte",        "Actif",  "Marquer une proie, c'est déjà la posséder.",                                "Marque une cible — toutes les attaques contre elle sont des coups critiques.",       OI + "Chasse_Ouverte.png",        5),
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
