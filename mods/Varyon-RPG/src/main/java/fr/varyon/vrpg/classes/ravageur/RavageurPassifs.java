package fr.varyon.vrpg.classes.ravageur;

public final class RavageurPassifs {

    private RavageurPassifs() {}

    private static int idx(int rank, int len) {
        return Math.max(0, Math.min(rank - 1, len - 1));
    }

    // --- Moissonneur (node 1) — XP bonus en série de kills ---
    public static final String MOISSONNEUR_NODE      = "ravageur_1";
    public static final int    MOISSONNEUR_MAX_STACKS = 5;
    private static final float[] MOISSONNEUR_XP_BONUS = {0.10f, 0.15f, 0.20f, 0.28f, 0.40f};

    public static float moissonneurXpBonusPerStack(int rank) {
        return MOISSONNEUR_XP_BONUS[idx(rank, MOISSONNEUR_XP_BONUS.length)];
    }

    public static String moissonneurStatLine(int rank) {
        int pct = Math.round(moissonneurXpBonusPerStack(rank) * 100);
        return "+" + pct + "% XP/élim en série (max " + MOISSONNEUR_MAX_STACKS + " cumuls)";
    }

    // --- Arme lourde (node 2) — critiques ralentissent la cible ---
    public static final String ARME_LOURDE_NODE      = "ravageur_2";
    private static final float[] ARME_LOURDE_SLOW    = {0.15f, 0.20f, 0.25f, 0.30f, 0.40f};
    private static final long    ARME_LOURDE_DURATION_MS = 3000L;

    public static float armeLourdeSlowForRank(int rank) {
        return ARME_LOURDE_SLOW[idx(rank, ARME_LOURDE_SLOW.length)];
    }

    public static long armeLourdeDurationMs() { return ARME_LOURDE_DURATION_MS; }

    public static String armeLourdeStatLine(int rank) {
        int pct = Math.round(armeLourdeSlowForRank(rank) * 100);
        return "Critique : -" + pct + "% vitesse cible pendant " + (ARME_LOURDE_DURATION_MS / 1000) + "s";
    }

    // --- Exécuteur (node 4) — bonus dégâts si cible < seuil HP ---
    public static final String EXECUTEUR_NODE        = "ravageur_4";
    public static final float  EXECUTEUR_THRESHOLD   = 0.30f;
    private static final float[] EXECUTEUR_BONUS     = {0.10f, 0.15f, 0.20f, 0.27f, 0.35f};

    public static float executeurBonusForRank(int rank) {
        return EXECUTEUR_BONUS[idx(rank, EXECUTEUR_BONUS.length)];
    }

    public static String executeurStatLine(int rank) {
        int pct = Math.round(executeurBonusForRank(rank) * 100);
        return "+" + pct + "% dégâts si la cible a <30% PV";
    }

    // --- Chasseur de géant (node 6) — bonus dégâts si cible a PLUS de HP que soi ---
    public static final String CHASSEUR_GEANT_NODE    = "ravageur_6";
    private static final float[] CHASSEUR_GEANT_BONUS = {0.08f, 0.12f, 0.16f, 0.22f, 0.30f};

    public static float chasseurGeantBonusForRank(int rank) {
        return CHASSEUR_GEANT_BONUS[idx(rank, CHASSEUR_GEANT_BONUS.length)];
    }

    public static String chasseurGeantStatLine(int rank) {
        int pct = Math.round(chasseurGeantBonusForRank(rank) * 100);
        return "+" + pct + "% dégâts si la cible a plus de PV que vous";
    }

    // --- Élan destructeur (node 7) — prochain coup armé bonus après un kill ---
    public static final String ELAN_DESTRUCTEUR_NODE    = "ravageur_7";
    private static final float[] ELAN_BONUS              = {0.15f, 0.20f, 0.25f, 0.32f, 0.40f};
    public  static final long    ELAN_WINDOW_MS          = 5000L;

    public static float elanBonusForRank(int rank) {
        return ELAN_BONUS[idx(rank, ELAN_BONUS.length)];
    }

    public static String elanDestructeurStatLine(int rank) {
        int pct = Math.round(elanBonusForRank(rank) * 100);
        return "Après un kill, prochain coup +" + pct + "% dégâts (fenêtre " + (ELAN_WINDOW_MS / 1000) + "s)";
    }

    // --- Combattant infatigable (node 10) — bonus dégâts selon HP manquants ---
    public static final String COMBATTANT_INFATIGABLE_NODE    = "ravageur_10";
    private static final float[] COMBATTANT_BONUS_PER_10PCT   = {0.01f, 0.015f, 0.02f, 0.025f, 0.03f};

    public static float combattantBonusPer10PctForRank(int rank) {
        return COMBATTANT_BONUS_PER_10PCT[idx(rank, COMBATTANT_BONUS_PER_10PCT.length)];
    }

    public static String combattantInfatigableStatLine(int rank) {
        float pct = combattantBonusPer10PctForRank(rank) * 100;
        return "+" + String.format("%.1f", pct) + "% dégâts par tranche de 10% PV manquants (max 100%)";
    }
}
