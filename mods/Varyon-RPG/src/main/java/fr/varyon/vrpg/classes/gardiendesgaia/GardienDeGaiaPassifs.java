package fr.varyon.vrpg.classes.gardiendesgaia;

public final class GardienDeGaiaPassifs {

    private GardienDeGaiaPassifs() {}

    private static int idx(int rank, int len) {
        return Math.max(0, Math.min(rank - 1, len - 1));
    }

    // --- Harmonie Naturelle (node 1) — XP bonus si PV > 80% ---
    public static final String HARMONIE_NODE     = "gardien_de_gaia_1";
    public static final float  HARMONIE_HP_SEUIL = 0.80f;
    private static final float[] HARMONIE_BONUS  = {0.10f, 0.15f, 0.20f, 0.27f, 0.40f};

    public static float harmonieBonusForRank(int rank) {
        return HARMONIE_BONUS[idx(rank, HARMONIE_BONUS.length)];
    }
    public static String harmonieStatLine(int rank) {
        int pct = Math.round(harmonieBonusForRank(rank) * 100);
        return "+" + pct + "% XP si PV > 80 %";
    }

    // --- Souffle de la Nature (node 10) — regen HP + endurance alliés proches ---
    public static final String  SOUFFLE_NODE      = "gardien_de_gaia_10";
    public static final double  SOUFFLE_RADIUS    = 8.0;
    private static final float[] SOUFFLE_HP_REGEN  = {0.5f, 0.8f, 1.1f, 1.5f, 2.0f};
    private static final float[] SOUFFLE_STA_REGEN = {0.2f, 0.4f, 0.6f, 0.8f, 1.0f};

    public static float souffleHpRegenForRank(int rank) {
        return SOUFFLE_HP_REGEN[idx(rank, SOUFFLE_HP_REGEN.length)];
    }
    public static float souffleStaRegenForRank(int rank) {
        return SOUFFLE_STA_REGEN[idx(rank, SOUFFLE_STA_REGEN.length)];
    }
    public static String souffleStatLine(int rank) {
        return "+" + souffleHpRegenForRank(rank) + " PV/s et +" + souffleStaRegenForRank(rank) + " END/s aux alliés à " + (int) SOUFFLE_RADIUS + " blocs";
    }

    // --- Lien Spirituel (node 5) — dégâts invocations → soin joueur ---
    public static final String LIEN_NODE     = "gardien_de_gaia_5";
    private static final float[] LIEN_RATIO  = {0.04f, 0.045f, 0.05f, 0.055f, 0.06f};

    public static float lienRatioForRank(int rank) {
        return LIEN_RATIO[idx(rank, LIEN_RATIO.length)];
    }
    public static String lienStatLine(int rank) {
        int pct = Math.round(lienRatioForRank(rank) * 100);
        return pct + "% des dégâts des invocations vous soigne";
    }

    // --- Gardien de la Nature (node 7) — PV invocations augmentés ---
    public static final String GARDIEN_NODE     = "gardien_de_gaia_7";
    private static final float[] GARDIEN_HP_BONUS = {0.50f, 0.60f, 0.70f, 0.80f, 1.00f};

    public static float gardienHpBonusForRank(int rank) {
        return GARDIEN_HP_BONUS[idx(rank, GARDIEN_HP_BONUS.length)];
    }
    public static String gardienStatLine(int rank) {
        int pct = Math.round(gardienHpBonusForRank(rank) * 100);
        return "+" + pct + "% PV des invocations";
    }

    // --- Grâce de Gaïa (node 9) — soins amplifiés sur cibles à bas PV ---
    public static final String  GRACE_NODE       = "gardien_de_gaia_9";
    public static final float   GRACE_HP_SEUIL   = 0.40f;
    private static final float[] GRACE_BONUS     = {0.15f, 0.22f, 0.30f, 0.40f, 0.55f};

    public static float graceBonusForRank(int rank) {
        return GRACE_BONUS[idx(rank, GRACE_BONUS.length)];
    }
    public static String graceStatLine(int rank) {
        int pct = Math.round(graceBonusForRank(rank) * 100);
        return "+" + pct + "% soins sous 40% PV";
    }

    // --- Cycle de Vie (node 8) — soigner un allié restaure du mana ---
    public static final String CYCLE_NODE     = "gardien_de_gaia_8";
    private static final float[] CYCLE_MANA  = {5f, 6f, 7f, 8f, 10f};

    public static float cycleManaForRank(int rank) {
        return CYCLE_MANA[idx(rank, CYCLE_MANA.length)];
    }
    public static String cycleStatLine(int rank) {
        return "+" + cycleManaForRank(rank) + " mana restauré en soignant un allié";
    }
}
