package fr.varyon.vrpg.classes.duelliste;

public final class DuellistePassifs {

    private DuellistePassifs() {}

    // --- Blessure Ouverte (node 2) ---
    public static final String BLESSURE_NODE   = "duelliste_2";
    private static final float[]  BLEED_CHANCE  = {0.08f, 0.12f, 0.16f, 0.20f, 0.25f};
    public static final float  BLEED_WEAPON_PCT  = 0.30f;
    public static final long   BLEED_DURATION_MS = 5_000L;

    public static float bleedChanceForRank(int rank) {
        return BLEED_CHANCE[Math.max(0, Math.min(rank - 1, BLEED_CHANCE.length - 1))];
    }

    public static int bleedMaxRank() { return BLEED_CHANCE.length; }

    public static String bleedStatLine(int rank) {
        int pct = Math.round(bleedChanceForRank(rank) * 100);
        int weaponPct = Math.round(BLEED_WEAPON_PCT * 100);
        return pct + "% chance, " + weaponPct + "% dégâts arme/s pendant 5s";
    }

    // --- Frappe Précise (node 8) ---
    public static final String FRAPPE_NODE      = "duelliste_8";
    private static final float[] CRIT_BONUS     = {0.05f, 0.08f, 0.12f, 0.15f, 0.20f};

    public static float critBonusForRank(int rank) {
        return CRIT_BONUS[Math.max(0, Math.min(rank - 1, CRIT_BONUS.length - 1))];
    }

    public static int critMaxRank() { return CRIT_BONUS.length; }

    public static String critStatLine(int rank) {
        int pct = Math.round(critBonusForRank(rank) * 100);
        return "+" + pct + "% dégâts sur coup critique";
    }

    // --- Contre-Attaque (node 5) ---
    public static final String CONTRE_NODE      = "duelliste_5";
    private static final float[] CONTRE_BONUS   = {0.10f, 0.20f, 0.30f, 0.40f, 0.50f};

    public static float contreBonusForRank(int rank) {
        return CONTRE_BONUS[Math.max(0, Math.min(rank - 1, CONTRE_BONUS.length - 1))];
    }

    public static int contreMaxRank() { return CONTRE_BONUS.length; }

    public static String contreStatLine(int rank) {
        int pct = Math.round(contreBonusForRank(rank) * 100);
        return "+" + pct + "% dégâts après parade (fenêtre 8s)";
    }

    // --- Esquive du Bretteur (node 7) ---
    public static final String ESQUIVE_NODE     = "duelliste_7";
    private static final float[] DODGE_CHANCE   = {0.03f, 0.06f, 0.09f, 0.12f, 0.15f};

    public static float dodgeChanceForRank(int rank) {
        return DODGE_CHANCE[Math.max(0, Math.min(rank - 1, DODGE_CHANCE.length - 1))];
    }

    public static int dodgeMaxRank() { return DODGE_CHANCE.length; }

    public static String dodgeStatLine(int rank) {
        int pct = Math.round(dodgeChanceForRank(rank) * 100);
        return pct + "% chance d'esquiver (annule les dégâts)";
    }

    // --- Momentum (node 10) ---
    public static final String MOMENTUM_NODE    = "duelliste_10";
    public static final int    MOMENTUM_MAX_STACKS = 5;
    private static final float[] MOMENTUM_PER_STACK = {0.02f, 0.025f, 0.03f, 0.04f, 0.05f};

    public static float momentumBonusPerStack(int rank) {
        return MOMENTUM_PER_STACK[Math.max(0, Math.min(rank - 1, MOMENTUM_PER_STACK.length - 1))];
    }

    public static int momentumMaxRank() { return MOMENTUM_PER_STACK.length; }

    public static String momentumStatLine(int rank) {
        int pct = Math.round(momentumBonusPerStack(rank) * 100 * 10) / 10;
        float raw = momentumBonusPerStack(rank) * 100;
        String pctStr = (raw == Math.floor(raw)) ? String.valueOf((int) raw) : String.valueOf(raw);
        return "+" + pctStr + "% dégâts/cumul (max " + MOMENTUM_MAX_STACKS + " cumuls)";
    }
}
