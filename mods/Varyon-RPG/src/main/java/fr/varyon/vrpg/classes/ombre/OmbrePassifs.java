package fr.varyon.vrpg.classes.ombre;

public final class OmbrePassifs {

    private OmbrePassifs() {}

    // --- Exécution Rapide (node à définir) ---
    public static final String EXECUTION_RAPIDE_NODE = "ombre_1";
    private static final float[] EXECUTION_XP_BONUS = {0.10f, 0.15f, 0.20f, 0.25f, 0.30f};
    private static final long   EXECUTION_WINDOW_MS  = 5_000L;

    public static float executionXpBonusForRank(int rank) {
        return EXECUTION_XP_BONUS[idx(rank, EXECUTION_XP_BONUS.length)];
    }

    public static long executionWindowMs() { return EXECUTION_WINDOW_MS; }

    public static String executionStatLine(int rank) {
        return "+" + Math.round(executionXpBonusForRank(rank) * 100) + "% XP si cible tuée en moins de 5s";
    }

    // --- Danse des Lames (node à définir) ---
    public static final String DANSE_LAMES_NODE = "ombre_8";
    private static final float[] DANSE_SPEED_BONUS = {0.04f, 0.08f, 0.12f, 0.16f, 0.20f};
    private static final long   DANSE_DURATION_MS  = 3_000L;

    public static float danseSpeedBonusForRank(int rank) {
        return DANSE_SPEED_BONUS[idx(rank, DANSE_SPEED_BONUS.length)];
    }

    public static long danseDurationMs() { return DANSE_DURATION_MS; }

    public static String danseStatLine(int rank) {
        return "+" + Math.round(danseSpeedBonusForRank(rank) * 100) + "% vitesse pendant 3s après crit";
    }

    // --- Lames Empoisonnées (node à définir) ---
    public static final String LAMES_EMPOISONNEES_NODE = "ombre_2";
    private static final float[] POISON_CHANCE      = {0.08f, 0.12f, 0.16f, 0.20f, 0.25f};
    private static final float[] POISON_WEAPON_PCT  = {1.20f, 1.50f, 1.80f, 2.10f, 2.40f};
    public static final long     POISON_DURATION_MS = 5_000L;

    public static float poisonChanceForRank(int rank) {
        return POISON_CHANCE[idx(rank, POISON_CHANCE.length)];
    }

    public static float poisonWeaponPctForRank(int rank) {
        return POISON_WEAPON_PCT[idx(rank, POISON_WEAPON_PCT.length)];
    }

    public static String poisonStatLine(int rank) {
        return Math.round(poisonChanceForRank(rank) * 100) + "% chance, "
            + Math.round(poisonWeaponPctForRank(rank) * 100) + "% dégâts arme/s pendant 5s";
    }

    // --- Embuscade (node à définir) ---
    public static final String EMBUSCADE_NODE = "ombre_5";
    private static final float[] EMBUSCADE_DURATION_SEC = {0.5f, 1.0f, 1.5f, 2.0f, 2.5f};

    public static long embuscadeDurationMs(int rank) {
        return (long)(EMBUSCADE_DURATION_SEC[idx(rank, EMBUSCADE_DURATION_SEC.length)] * 1000);
    }

    public static String embuscadeStatLine(int rank) {
        return "Immobilise la cible pendant " + EMBUSCADE_DURATION_SEC[idx(rank, EMBUSCADE_DURATION_SEC.length)] + "s après sortie d'invisibilité";
    }

    // --- Ombre Insaisissable (node à définir) ---
    public static final String OMBRE_INSAISISSABLE_NODE = "ombre_7";
    private static final float[] DODGE_BONUS = {0.03f, 0.06f, 0.09f, 0.12f, 0.15f};

    public static float dodgeBonusForRank(int rank) {
        return DODGE_BONUS[idx(rank, DODGE_BONUS.length)];
    }

    public static String ombreInsaisissableStatLine(int rank) {
        return "+" + Math.round(dodgeBonusForRank(rank) * 100) + "% chance d'esquive";
    }

    // --- Instinct de Survie (node à définir) ---
    public static final String INSTINCT_SURVIE_NODE = "ombre_10";
    private static final float[] SURVIE_DODGE_BONUS = {0.05f, 0.10f, 0.15f, 0.20f, 0.25f};
    private static final float   SURVIE_HP_THRESHOLD = 0.30f;

    public static float survieDodgeBonusForRank(int rank) {
        return SURVIE_DODGE_BONUS[idx(rank, SURVIE_DODGE_BONUS.length)];
    }

    public static float survieHpThreshold() { return SURVIE_HP_THRESHOLD; }

    public static String instinctSurvieStatLine(int rank) {
        return "+" + Math.round(survieDodgeBonusForRank(rank) * 100) + "% esquive supplémentaire sous 30% PV";
    }

    private static int idx(int rank, int len) {
        return Math.max(0, Math.min(rank - 1, len - 1));
    }
}
