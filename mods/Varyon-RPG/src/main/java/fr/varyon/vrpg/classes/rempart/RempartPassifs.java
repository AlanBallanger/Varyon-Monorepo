package fr.varyon.vrpg.classes.rempart;

public final class RempartPassifs {

    private RempartPassifs() {}

    // --- Maître du Bouclier (node 1) — XP bonus avec bouclier ---
    public static final String MAITRE_BOUCLIER_NODE = "rempart_1";
    private static final float[] BOUCLIER_XP_BONUS = {0.10f, 0.15f, 0.20f, 0.25f, 0.30f};

    public static float bouclierXpBonusForRank(int rank) {
        return BOUCLIER_XP_BONUS[idx(rank, BOUCLIER_XP_BONUS.length)];
    }

    public static String maitreBouclierStatLine(int rank) {
        return "+" + Math.round(bouclierXpBonusForRank(rank) * 100) + "% XP classe avec bouclier équipé";
    }

    // --- Constitution de Fer (node 2) — HP max bonus ---
    public static final String CONSTITUTION_NODE = "rempart_2";
    private static final float[] CONSTITUTION_HP_BONUS = {0.05f, 0.08f, 0.11f, 0.14f, 0.18f};

    public static float constitutionHpBonusForRank(int rank) {
        return CONSTITUTION_HP_BONUS[idx(rank, CONSTITUTION_HP_BONUS.length)];
    }

    public static String constitutionStatLine(int rank) {
        return "+" + Math.round(constitutionHpBonusForRank(rank) * 100) + "% HP maximum";
    }

    // --- Garde Impénétrable (node 5) — réduction dégâts après blocage ---
    public static final String GARDE_IMPENETRABLE_NODE = "rempart_5";
    private static final float[] GARDE_REDUCTION    = {0.10f, 0.13f, 0.16f, 0.20f, 0.25f};
    private static final long    GARDE_DURATION_MS  = 3000L;

    public static float gardeReductionForRank(int rank) {
        return GARDE_REDUCTION[idx(rank, GARDE_REDUCTION.length)];
    }

    public static long gardeDurationMs() { return GARDE_DURATION_MS; }

    public static String gardeImpenetrableStatLine(int rank) {
        return "-" + Math.round(gardeReductionForRank(rank) * 100) + "% dégâts reçus pendant " + (GARDE_DURATION_MS / 1000) + "s après blocage parfait";
    }

    // --- Infatigable (node 7) — soin après blocage ---
    public static final String INFATIGABLE_NODE = "rempart_7";
    private static final float[] INFATIGABLE_HEAL_PCT = {0.02f, 0.03f, 0.04f, 0.05f, 0.07f};

    public static float infatigableHealPctForRank(int rank) {
        return INFATIGABLE_HEAL_PCT[idx(rank, INFATIGABLE_HEAL_PCT.length)];
    }

    public static String infatigableStatLine(int rank) {
        return "Les blocages parfaits restaurent " + Math.round(infatigableHealPctForRank(rank) * 100) + "% HP max";
    }

    // --- Contre Offensif (node 8) — bonus dégâts après blocage ---
    public static final String CONTRE_OFFENSIF_NODE = "rempart_8";
    private static final float[] CONTRE_DAMAGE_BONUS = {0.20f, 0.30f, 0.40f, 0.50f, 0.65f};
    private static final long    CONTRE_WINDOW_MS    = 5000L;

    public static float contreDamageBonusForRank(int rank) {
        return CONTRE_DAMAGE_BONUS[idx(rank, CONTRE_DAMAGE_BONUS.length)];
    }

    public static long contreWindowMs() { return CONTRE_WINDOW_MS; }

    public static String contreOffensifStatLine(int rank) {
        return "+" + Math.round(contreDamageBonusForRank(rank) * 100) + "% dégâts sur le prochain coup après blocage parfait";
    }

    // --- Dernier Bastion (node 10) — réduction sous 30% HP ---
    public static final String DERNIER_BASTION_NODE = "rempart_10";
    private static final float[] BASTION_REDUCTION  = {0.10f, 0.15f, 0.20f, 0.25f, 0.30f};
    private static final float   BASTION_THRESHOLD  = 0.30f;

    public static float bastionReductionForRank(int rank) {
        return BASTION_REDUCTION[idx(rank, BASTION_REDUCTION.length)];
    }

    public static float bastionThreshold() { return BASTION_THRESHOLD; }

    public static String dernierBastionStatLine(int rank) {
        return "-" + Math.round(bastionReductionForRank(rank) * 100) + "% dégâts reçus sous 30% HP";
    }

    private static int idx(int rank, int len) {
        return Math.max(0, Math.min(rank - 1, len - 1));
    }
}
