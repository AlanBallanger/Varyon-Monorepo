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

    // --- Ricochet (node 4) — les flèches normales ricochent sur une cible proche ---
    public static final String RICOCHET_NODE   = "rodeur_4";
    private static final float[] RICOCHET_PCT  = {0.50f, 0.60f, 0.70f, 0.80f, 1.00f};
    public static final double   RICOCHET_RANGE = 5.0;

    public static float ricochetPctForRank(int rank) {
        return RICOCHET_PCT[idx(rank, RICOCHET_PCT.length)];
    }

    public static int ricochetBouncesForRank(int rank) {
        return rank >= RICOCHET_PCT.length ? 2 : 1;
    }

    public static String ricochetStatLine(int rank) {
        int bounces = ricochetBouncesForRank(rank);
        String bounceText = bounces > 1 ? "Ricoche sur " + bounces + " cibles proches" : "Ricoche sur une cible proche";
        return bounceText + " : " + Math.round(ricochetPctForRank(rank) * 100) + "% des dégâts";
    }

    // --- Précision mortelle (node 5) — bonus dégâts < 50% HP ---
    public static final String PRECISION_MORTELLE_NODE = "rodeur_5";
    private static final float[] PRECISION_BONUS      = {0.08f, 0.12f, 0.16f, 0.20f, 0.25f};
    public static final float    PRECISION_HP_THRESH  = 0.50f;

    public static float precisionBonusForRank(int rank) {
        return PRECISION_BONUS[idx(rank, PRECISION_BONUS.length)];
    }

    public static String precisionStatLine(int rank) {
        return "+" + Math.round(precisionBonusForRank(rank) * 100) + "% dégâts si cible < 50% PV";
    }

    // --- Foulée du Rodeur (node 9) — bonus de vitesse au tir d'une flèche normale ---
    public static final String FOULEE_RODEUR_NODE    = "rodeur_9";
    private static final float[] FOULEE_SPEED_BONUS  = {0.30f, 0.35f, 0.40f, 0.45f, 0.50f};
    public static final long     FOULEE_DURATION_MS  = 2_000L;

    public static float fouleeSpeedBonusForRank(int rank) {
        return FOULEE_SPEED_BONUS[idx(rank, FOULEE_SPEED_BONUS.length)];
    }

    public static String fouleeStatLine(int rank) {
        return "+" + Math.round(fouleeSpeedBonusForRank(rank) * 100) + "% vitesse pendant 2s au tir d'une flèche";
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
