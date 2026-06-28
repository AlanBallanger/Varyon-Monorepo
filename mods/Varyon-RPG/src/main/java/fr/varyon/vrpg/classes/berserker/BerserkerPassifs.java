package fr.varyon.vrpg.classes.berserker;

public final class BerserkerPassifs {

    private BerserkerPassifs() {}

    private static int idx(int rank, int len) {
        return Math.max(0, Math.min(rank - 1, len - 1));
    }

    // --- Dernier Souffle (node 10) ---
    public static final String DERNIER_SOUFFLE_NODE = "berserker_10";
    public static final long   DERNIER_SOUFFLE_DURATION_MS = 4_000L;
    private static final long[] DERNIER_SOUFFLE_COOLDOWN_MS = {150_000L, 135_000L, 120_000L, 105_000L, 90_000L};

    public static long dernierSouffleCooldownMsForRank(int rank) {
        return DERNIER_SOUFFLE_COOLDOWN_MS[idx(rank, DERNIER_SOUFFLE_COOLDOWN_MS.length)];
    }

    public static String dernierSouffleStatLine(int rank) {
        int cd = (int) (dernierSouffleCooldownMsForRank(rank) / 1000);
        return "Ignore la mort (Délai " + cd + "s)";
    }

    // --- Ferveur Guerrière (node 7) ---
    public static final String FERVEUR_NODE     = "berserker_7";
    public static final int    FERVEUR_MAX_STACKS = 10;
    private static final float[] FERVEUR_PER_STACK = {0.01f, 0.015f, 0.02f, 0.025f, 0.03f};

    public static float ferveurBonusPerStack(int rank) {
        return FERVEUR_PER_STACK[idx(rank, FERVEUR_PER_STACK.length)];
    }

    public static String ferveurBonusPctDisplay(int rank) {
        float raw = ferveurBonusPerStack(rank) * 100f;
        if (raw == Math.floor(raw)) {
            return String.valueOf((int) raw);
        }
        return String.format(java.util.Locale.FRENCH, "%.1f", raw);
    }

    public static String ferveurStatLine(int rank) {
        return "+" + ferveurBonusPctDisplay(rank) + "% dégâts/s en combat (max " + FERVEUR_MAX_STACKS + " cumuls)";
    }

    // --- Fureur Sanguinaire (node 2) ---
    public static final String FUREUR_NODE = "berserker_2";
    private static final float[] FUREUR_LIFESTEAL_PCT = {0.04f, 0.05f, 0.06f, 0.07f, 0.08f};
    private static final long    FUREUR_DURATION_MS   = 8_000L;

    public static float fureurLifestealPct(int rank) {
        return FUREUR_LIFESTEAL_PCT[idx(rank, FUREUR_LIFESTEAL_PCT.length)];
    }

    public static long fureurDurationMs() { return FUREUR_DURATION_MS; }

    public static String fureurStatLine(int rank) {
        int pct = Math.round(fureurLifestealPct(rank) * 100);
        return "Après un kill, vol de vie +" + pct + "% des dégâts infligés pendant " + (FUREUR_DURATION_MS / 1000) + "s";
    }

    // --- Carnage (node 1) ---
    public static final String CARNAGE_NODE      = "berserker_1";
    public static final int    CARNAGE_MAX_STACKS = 5;
    private static final float[] CARNAGE_XP_BONUS = {0.10f, 0.15f, 0.20f, 0.28f, 0.40f};

    public static float carnageXpBonusPerStack(int rank) {
        return CARNAGE_XP_BONUS[idx(rank, CARNAGE_XP_BONUS.length)];
    }

    public static String carnageStatLine(int rank) {
        int pct = Math.round(carnageXpBonusPerStack(rank) * 100);
        return "+" + pct + "% XP/élim en série (max " + CARNAGE_MAX_STACKS + " cumuls)";
    }

    // --- Blessures Profondes (node 6) ---
    public static final String BLESSURES_NODE = "berserker_6";
    private static final float[] BLEED_CHANCE     = {0.10f, 0.15f, 0.20f, 0.25f, 0.30f};
    private static final float[] BLEED_WEAPON_PCT  = {0.30f, 0.40f, 0.50f, 0.60f, 0.70f};
    public static final long     BLEED_DURATION_MS = 5_000L;

    public static float bleedChanceForRank(int rank) {
        return BLEED_CHANCE[idx(rank, BLEED_CHANCE.length)];
    }

    public static float bleedWeaponPctForRank(int rank) {
        return BLEED_WEAPON_PCT[idx(rank, BLEED_WEAPON_PCT.length)];
    }

    public static String blessuresStatLine(int rank) {
        int pct = Math.round(bleedChanceForRank(rank) * 100);
        int weaponPct = Math.round(bleedWeaponPctForRank(rank) * 100);
        return pct + "% chance de saignement (" + weaponPct + "% arme/s, 5s)";
    }

    // --- Frénésie (node 3) ---
    public static final String FRENESIE_NODE      = "berserker_3";
    public static final int    FRENESIE_MAX_STACKS = 3;
    private static final float[] FRENESIE_DMG_BONUS  = {0.05f, 0.08f, 0.10f, 0.13f, 0.15f};
    private static final float[] FRENESIE_SPD_BONUS  = {0.05f, 0.07f, 0.10f, 0.12f, 0.15f};
    private static final long    FRENESIE_DURATION_MS = 6_000L;

    public static float frenesieDmgBonusPerStack(int rank) {
        return FRENESIE_DMG_BONUS[idx(rank, FRENESIE_DMG_BONUS.length)];
    }

    public static float frenesieSpdBonusPerStack(int rank) {
        return FRENESIE_SPD_BONUS[idx(rank, FRENESIE_SPD_BONUS.length)];
    }

    public static long frenesieDurationMs() { return FRENESIE_DURATION_MS; }

    public static String frenesieStatLine(int rank) {
        int dmgPct = Math.round(frenesieDmgBonusPerStack(rank) * 100);
        int spdPct = Math.round(frenesieSpdBonusPerStack(rank) * 100);
        return "Après kill : +" + dmgPct + "% dégâts, +" + spdPct + "% vitesse/cumul (max 3, " + (FRENESIE_DURATION_MS / 1000) + "s)";
    }
}
