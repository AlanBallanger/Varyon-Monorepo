package fr.varyon.vrpg.classes.rodeur;

public final class RodeurPassifs {

    private RodeurPassifs() {}

    // --- Œil du chasseur (node 1) — XP bonus à distance ---
    public static final String OEIL_CHASSEUR_NODE  = "rodeur_1";
    private static final float[] OEIL_XP_BONUS     = {0.10f, 0.15f, 0.20f, 0.25f, 0.30f};
    public static final double   OEIL_DISTANCE_MIN = 10.0;

    public static float oeilXpBonusForRank(int rank) {
        return OEIL_XP_BONUS[idx(rank, OEIL_XP_BONUS.length)];
    }

    public static String oeilStatLine(int rank) {
        return "+" + Math.round(oeilXpBonusForRank(rank) * 100) + "% XP si cible à plus de 10m";
    }

    // --- Flèches toxiques (node 6) — poison passif ---
    public static final String FLECHES_TOXIQUES_NODE = "rodeur_6";
    private static final float[] POISON_CHANCE       = {0.08f, 0.12f, 0.16f, 0.20f, 0.25f};
    public static final float    POISON_WEAPON_PCT   = 0.60f;
    public static final long     POISON_DURATION_MS  = 5_000L;

    public static float poisonChanceForRank(int rank) {
        return POISON_CHANCE[idx(rank, POISON_CHANCE.length)];
    }

    public static String poisonStatLine(int rank) {
        return Math.round(poisonChanceForRank(rank) * 100) + "% chance, "
            + Math.round(POISON_WEAPON_PCT * 100) + "% dégâts arme/s pendant 5s";
    }

    // --- Instinct de survie (node 7) — esquive passive ---
    public static final String INSTINCT_SURVIE_NODE  = "rodeur_7";
    private static final float[] DODGE_CHANCE        = {0.04f, 0.07f, 0.10f, 0.13f, 0.16f};

    public static float dodgeChanceForRank(int rank) {
        return DODGE_CHANCE[idx(rank, DODGE_CHANCE.length)];
    }

    public static String instinctStatLine(int rank) {
        return "+" + Math.round(dodgeChanceForRank(rank) * 100) + "% chance d'esquive";
    }

    // --- Précision mortelle (node 8) — bonus dégâts < 50% HP ---
    public static final String PRECISION_MORTELLE_NODE = "rodeur_8";
    private static final float[] PRECISION_BONUS      = {0.08f, 0.12f, 0.16f, 0.20f, 0.25f};
    public static final float    PRECISION_HP_THRESH  = 0.50f;

    public static float precisionBonusForRank(int rank) {
        return PRECISION_BONUS[idx(rank, PRECISION_BONUS.length)];
    }

    public static String precisionStatLine(int rank) {
        return "+" + Math.round(precisionBonusForRank(rank) * 100) + "% dégâts si cible < 50% PV";
    }

    // --- Traque mobile (node 9) — bonus dégâts en déplacement ---
    public static final String TRAQUE_MOBILE_NODE    = "rodeur_9";
    private static final float[] TRAQUE_BONUS        = {0.05f, 0.08f, 0.11f, 0.14f, 0.18f};

    public static float traqueBonusForRank(int rank) {
        return TRAQUE_BONUS[idx(rank, TRAQUE_BONUS.length)];
    }

    public static String traqueMobileStatLine(int rank) {
        return "+" + Math.round(traqueBonusForRank(rank) * 100) + "% dégâts en déplacement";
    }

    // --- Traque sans fin (node 10) — réduit Délai marque après kill ---
    public static final String TRAQUE_SANS_FIN_NODE   = "rodeur_10";
    private static final float[] TRAQUE_CD_REDUCTION  = {0.30f, 0.40f, 0.50f, 0.60f, 0.80f};

    public static float traqueCdReductionForRank(int rank) {
        return TRAQUE_CD_REDUCTION[idx(rank, TRAQUE_CD_REDUCTION.length)];
    }

    public static String traqueSansFinStatLine(int rank) {
        int pct = Math.round(traqueCdReductionForRank(rank) * 100);
        return "Tuer une cible marquée réduit le délai de Marque du Chasseur de " + pct + "%";
    }

    private static int idx(int rank, int len) {
        return Math.max(0, Math.min(rank - 1, len - 1));
    }
}
