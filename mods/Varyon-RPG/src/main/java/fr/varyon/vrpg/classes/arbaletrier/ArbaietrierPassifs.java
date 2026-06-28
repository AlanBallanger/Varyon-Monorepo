package fr.varyon.vrpg.classes.arbaletrier;

public final class ArbaietrierPassifs {

    private ArbaietrierPassifs() {}

    // node 0 = ReculTactiqueSkill (actif)
    // node 1 = Tireur d'élite (passif - XP)
    // node 2 = CarreauLourdSkill (actif)
    // node 3 = CarreauExplosifSkill (actif)
    // node 4 = CarreauTranspercantSkill (actif)
    // node 5 = CoupDeBotteSkill (actif)
    // node 6 = Chasseur de colosses (passif)
    // node 7 = Tireur embusqué (passif - immobilité → dégâts)
    // node 8 = Viseur expérimenté (passif - dégâts/distance)
    // node 9 = Carreaux lacérants (passif - saignement)
    // node 10 = Réflexes affûtés (passif - esquive)
    // node 11 = MiseEnJouSkill (actif)

    // --- Tireur d'élite (node 1) — XP bonus à distance ---
    public static final String TIREUR_ELITE_NODE    = "arbaletrier_1";
    private static final float[] ELITE_XP_BONUS    = {0.10f, 0.15f, 0.20f, 0.25f, 0.30f};
    public static final double   ELITE_DISTANCE_MIN = 15.0;

    public static float eliteXpBonusForRank(int rank) {
        return ELITE_XP_BONUS[idx(rank, ELITE_XP_BONUS.length)];
    }

    public static String eliteStatLine(int rank) {
        return "+" + Math.round(eliteXpBonusForRank(rank) * 100) + "% XP si cible à plus de 15m";
    }

    // --- Chasseur de colosses (node 6) — bonus si cible a plus de HP que soi ---
    public static final String CHASSEUR_COLOSSES_NODE = "arbaletrier_6";
    private static final float[] COLOSSES_BONUS       = {0.06f, 0.10f, 0.14f, 0.18f, 0.22f};

    public static float colossesBonusForRank(int rank) {
        return COLOSSES_BONUS[idx(rank, COLOSSES_BONUS.length)];
    }

    public static String colossesStatLine(int rank) {
        return "+" + Math.round(colossesBonusForRank(rank) * 100) + "% dégâts si cible a plus de PV que soi";
    }

    // --- Tireur embusqué (node 7) — bonus dégâts après immobilité ---
    public static final String TIREUR_EMBUSQUE_NODE = "arbaletrier_7";
    private static final float[] EMBUSQUE_BONUS     = {0.08f, 0.12f, 0.16f, 0.20f, 0.25f};
    public static final long     EMBUSQUE_DELAY_MS  = 3_000L;

    public static float embusqueBonusForRank(int rank) {
        return EMBUSQUE_BONUS[idx(rank, EMBUSQUE_BONUS.length)];
    }

    public static String embusqueStatLine(int rank) {
        return "+" + Math.round(embusqueBonusForRank(rank) * 100) + "% dégâts après 3s d'immobilité";
    }

    // --- Viseur expérimenté (node 8) — bonus dégâts selon distance ---
    public static final String VISEUR_NODE           = "arbaletrier_8";
    private static final float[] VISEUR_BONUS_PER_M = {0.010f, 0.013f, 0.016f, 0.020f, 0.025f};
    public static final double   VISEUR_MIN_DIST     = 5.0;
    public static final double   VISEUR_MAX_DIST     = 30.0;

    public static float viseurBonusPerMeterForRank(int rank) {
        return VISEUR_BONUS_PER_M[idx(rank, VISEUR_BONUS_PER_M.length)];
    }

    public static float viseurTotalBonusForRank(int rank, double distance) {
        double clamped = Math.max(0, Math.min(distance - VISEUR_MIN_DIST, VISEUR_MAX_DIST - VISEUR_MIN_DIST));
        return (float)(clamped * viseurBonusPerMeterForRank(rank));
    }

    public static String viseurStatLine(int rank) {
        int bonusPer5m = Math.round(viseurBonusPerMeterForRank(rank) * 5 * 100);
        return "+" + bonusPer5m + "% dégâts tous les 5m de distance (max " + (int)VISEUR_MAX_DIST + "m)";
    }

    // --- Carreaux lacérants (node 9) — saignement passif ---
    public static final String CARREAUX_LACERANTS_NODE = "arbaletrier_9";
    private static final float[] BLEED_CHANCE          = {0.08f, 0.12f, 0.16f, 0.20f, 0.25f};
    public static final float    BLEED_WEAPON_PCT      = 0.25f;
    public static final long     BLEED_DURATION_MS     = 5_000L;

    public static float bleedChanceForRank(int rank) {
        return BLEED_CHANCE[idx(rank, BLEED_CHANCE.length)];
    }

    public static String bleedStatLine(int rank) {
        return Math.round(bleedChanceForRank(rank) * 100) + "% chance, "
            + Math.round(BLEED_WEAPON_PCT * 100) + "% dégâts arme/s pendant 5s";
    }

    // --- Réflexes affûtés (node 10) — esquive passive ---
    public static final String REFLEXES_AFFUTES_NODE = "arbaletrier_10";
    private static final float[] DODGE_CHANCE        = {0.04f, 0.07f, 0.10f, 0.13f, 0.16f};

    public static float dodgeChanceForRank(int rank) {
        return DODGE_CHANCE[idx(rank, DODGE_CHANCE.length)];
    }

    public static String reflexesStatLine(int rank) {
        return "+" + Math.round(dodgeChanceForRank(rank) * 100) + "% chance d'esquive";
    }

    private static int idx(int rank, int len) {
        return Math.max(0, Math.min(rank - 1, len - 1));
    }
}
