package fr.varyon.vrpg.classes.vaudou;

public final class VaudouPassifs {

    private VaudouPassifs() {}

    // --- Féticheur (node 1) — XP bonus si cible meurt < 5m ---
    public static final String FETICHEUR_NODE = "vaudou_1";
    private static final float[] FETICHEUR_XP_BONUS = {0.10f, 0.15f, 0.20f, 0.25f, 0.30f};
    public static final double FETICHEUR_RANGE = 5.0;

    public static float feticheurXpBonusForRank(int rank) {
        return FETICHEUR_XP_BONUS[idx(rank, FETICHEUR_XP_BONUS.length)];
    }

    public static String feticheurStatLine(int rank) {
        return "+" + Math.round(feticheurXpBonusForRank(rank) * 100) + "% XP si cible meurt à moins de 5 blocs";
    }

    // --- Présence Oppressante (node 11) — ennemis proches infligent moins ---
    public static final String PRESENCE_NODE = "vaudou_11";
    private static final float[] PRESENCE_REDUCTION = {0.04f, 0.08f, 0.12f, 0.16f, 0.20f};
    public static final double PRESENCE_RADIUS = 6.0;

    public static float presenceReductionForRank(int rank) {
        return PRESENCE_REDUCTION[idx(rank, PRESENCE_REDUCTION.length)];
    }

    public static String presenceStatLine(int rank) {
        return "-" + Math.round(presenceReductionForRank(rank) * 100) + "% dégâts des ennemis dans un rayon de 6 blocs";
    }

    // --- Toxines (node 5) — poison dure plus longtemps ---
    public static final String TOXINES_NODE = "vaudou_5";
    private static final float[] TOXINES_DURATION_BONUS = {0.10f, 0.20f, 0.30f, 0.40f, 0.50f};

    public static float toxinesDurationBonusForRank(int rank) {
        return TOXINES_DURATION_BONUS[idx(rank, TOXINES_DURATION_BONUS.length)];
    }

    public static String toxinesStatLine(int rank) {
        return "+" + Math.round(toxinesDurationBonusForRank(rank) * 100) + "% durée du poison";
    }

    // --- Ancrage Rituel (node 7) — totems durent plus longtemps ---
    public static final String ANCRAGE_NODE = "vaudou_8";
    private static final float[] ANCRAGE_DURATION_BONUS = {0.15f, 0.25f, 0.35f, 0.50f, 0.70f};

    public static float ancrageTotemBonusForRank(int rank) {
        return ANCRAGE_DURATION_BONUS[idx(rank, ANCRAGE_DURATION_BONUS.length)];
    }

    public static String ancrageStatLine(int rank) {
        return "+" + Math.round(ancrageTotemBonusForRank(rank) * 100) + "% durée des totems";
    }

    // --- Rituel Interdit (node 9) — +dégâts sur cible maudite ---
    public static final String RITUEL_NODE = "vaudou_9";
    private static final float[] RITUEL_BONUS = {0.05f, 0.08f, 0.12f, 0.16f, 0.20f};

    public static float rituelBonusForRank(int rank) {
        return RITUEL_BONUS[idx(rank, RITUEL_BONUS.length)];
    }

    public static String rituelStatLine(int rank) {
        return "+" + Math.round(rituelBonusForRank(rank) * 100) + "% dégâts sur les ennemis dans un totem";
    }

    // --- Parasite Spirituel (node 7) — frappe cible maudite → soin ---
    public static final String PARASITE_NODE = "vaudou_7";
    private static final float[] PARASITE_HEAL_PCT = {0.02f, 0.03f, 0.04f, 0.06f, 0.08f};

    public static float parasiteHealPctForRank(int rank) {
        return PARASITE_HEAL_PCT[idx(rank, PARASITE_HEAL_PCT.length)];
    }

    public static String parasiteStatLine(int rank) {
        return "Frapper un ennemi dans un totem restaure " + Math.round(parasiteHealPctForRank(rank) * 100) + "% des dégâts infligés en PV";
    }

    private static int idx(int rank, int len) {
        return Math.max(0, Math.min(rank - 1, len - 1));
    }
}
