package fr.varyon.vrpg.classes.bagarreur;

public final class BagarreurPassifs {

    private BagarreurPassifs() {}

    private static int idx(int rank, int len) {
        return Math.max(0, Math.min(rank - 1, len - 1));
    }

    // --- Jusqu'au bout (node 1) — XP bonus si HP < 80% ---
    public static final String JUSQUAU_BOUT_NODE      = "bagarreur_1";
    public static final float  JUSQUAU_BOUT_THRESHOLD = 0.80f;
    private static final float[] JUSQUAU_BOUT_BONUS   = {0.10f, 0.15f, 0.20f, 0.27f, 0.40f};

    public static float jusquAuBoutBonusForRank(int rank) {
        return JUSQUAU_BOUT_BONUS[idx(rank, JUSQUAU_BOUT_BONUS.length)];
    }

    public static String jusquAuBoutStatLine(int rank) {
        int pct = Math.round(jusquAuBoutBonusForRank(rank) * 100);
        return "+" + pct + "% XP si HP < 80%";
    }

    // --- Adrénaline (node 2) — speed boost après dégâts reçus ---
    public static final String ADRENALINE_NODE          = "bagarreur_5";
    private static final float[] ADRENALINE_SPEED       = {0.08f, 0.10f, 0.13f, 0.16f, 0.22f};
    public  static final long    ADRENALINE_DURATION_MS = 3000L;

    public static float adrenalineSpeedForRank(int rank) {
        return ADRENALINE_SPEED[idx(rank, ADRENALINE_SPEED.length)];
    }

    public static String adrenalineStatLine(int rank) {
        int pct = Math.round(adrenalineSpeedForRank(rank) * 100);
        return "+" + pct + "% vitesse pendant " + (ADRENALINE_DURATION_MS / 1000) + "s après avoir reçu des dégâts";
    }

    // --- Garde du boxeur (node 4) — réduction dégâts de face ---
    public static final String GARDE_BOXEUR_NODE     = "bagarreur_4";
    private static final float[] GARDE_BOXEUR_REDUC  = {0.08f, 0.12f, 0.16f, 0.20f, 0.28f};
    public  static final double  GARDE_BOXEUR_ANGLE  = Math.PI / 2.0;

    public static float gardeBoxeurReducForRank(int rank) {
        return GARDE_BOXEUR_REDUC[idx(rank, GARDE_BOXEUR_REDUC.length)];
    }

    public static String gardeBoxeurStatLine(int rank) {
        int pct = Math.round(gardeBoxeurReducForRank(rank) * 100);
        return "-" + pct + "% dégâts reçus de face";
    }

    // --- Acharnement (node 6) — stacks dégâts sur même cible ---
    public static final String ACHARNEMENT_NODE         = "bagarreur_6";
    public  static final int   ACHARNEMENT_MAX_STACKS   = 5;
    private static final float[] ACHARNEMENT_PER_STACK  = {0.04f, 0.06f, 0.08f, 0.10f, 0.14f};
    public  static final long    ACHARNEMENT_WINDOW_MS  = 6000L;

    public static float acharnementBonusPerStack(int rank) {
        return ACHARNEMENT_PER_STACK[idx(rank, ACHARNEMENT_PER_STACK.length)];
    }

    public static String acharnementStatLine(int rank) {
        int pct = Math.round(acharnementBonusPerStack(rank) * 100);
        return "+" + pct + "% dégâts par coup sur la même cible (max " + ACHARNEMENT_MAX_STACKS + " cumuls, " + (ACHARNEMENT_WINDOW_MS / 1000) + "s)";
    }

    // --- Esprit combatif (node 7) — bonus dégâts si HP faibles ---
    public static final String ESPRIT_COMBATIF_NODE      = "bagarreur_7";
    public  static final float ESPRIT_COMBATIF_THRESHOLD = 0.50f;
    private static final float[] ESPRIT_COMBATIF_BONUS   = {0.12f, 0.16f, 0.20f, 0.26f, 0.35f};

    public static float espritCombatifBonusForRank(int rank) {
        return ESPRIT_COMBATIF_BONUS[idx(rank, ESPRIT_COMBATIF_BONUS.length)];
    }

    public static String espritCombatifStatLine(int rank) {
        int pct = Math.round(espritCombatifBonusForRank(rank) * 100);
        return "+" + pct + "% dégâts si HP < 50%";
    }

    // --- Poings d'acier (node 8) — crit → chance de stun ---
    public static final String POINGS_ACIER_NODE      = "bagarreur_8";
    private static final float[] POINGS_ACIER_CHANCE  = {0.02f, 0.04f, 0.06f, 0.08f, 0.10f};
    public  static final long    POINGS_ACIER_STUN_MS = 1000L;

    public static float poingsAcierChanceForRank(int rank) {
        return POINGS_ACIER_CHANCE[idx(rank, POINGS_ACIER_CHANCE.length)];
    }

    public static String poingsAcierStatLine(int rank) {
        int pct = Math.round(poingsAcierChanceForRank(rank) * 100);
        return pct + "% chance d'étourdir " + (POINGS_ACIER_STUN_MS / 1000) + "s sur un coup critique";
    }

    // --- Frappes répétées (passif) → fusionné avec Acharnement ---
    // Gardé comme alias pour la description UI
    public static final String FRAPPES_REPETEES_NODE = ACHARNEMENT_NODE;
}
