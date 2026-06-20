package fr.varyon.vrpg.classes.lancier;

public final class LancierPassifs {

    private LancierPassifs() {}

    // node 1  = Discipline (passif - XP > 80% HP)
    // node 6  = Posture dominante (passif - réduction dégâts reçus après mêlée)
    // node 7  = Perce-cœur (passif - saignement sur crit)
    // node 8  = Chasseur de géants (passif - dégâts si cible a plus de HP)
    // node 9  = Briseur de ligne (passif - dégâts bonus après CC)
    // node 10 = Portée maîtrisée (passif - dégâts selon distance)

    // --- Discipline (node 1) — XP bonus si HP > 80% ---
    public static final String DISCIPLINE_NODE    = "lancier_1";
    private static final float[] DISCIPLINE_BONUS = {0.10f, 0.15f, 0.20f, 0.25f, 0.30f};
    public static final double   DISCIPLINE_HP_THRESHOLD = 0.50;

    public static float disciplineBonusForRank(int rank) {
        return DISCIPLINE_BONUS[idx(rank, DISCIPLINE_BONUS.length)];
    }

    public static String disciplineStatLine(int rank) {
        return "+" + Math.round(disciplineBonusForRank(rank) * 100) + "% XP si PV > 50%";
    }

    // --- Posture dominante (node 6) — réduction dégâts reçus après touche mêlée ---
    public static final String POSTURE_NODE        = "lancier_6";
    private static final float[] POSTURE_REDUCTION = {0.08f, 0.11f, 0.14f, 0.17f, 0.20f};
    public static final long     POSTURE_DURATION_MS = 4_000L;

    public static float postureReductionForRank(int rank) {
        return POSTURE_REDUCTION[idx(rank, POSTURE_REDUCTION.length)];
    }

    public static String postureStatLine(int rank) {
        return "-" + Math.round(postureReductionForRank(rank) * 100) + "% dégâts reçus pendant 4s après touche mêlée";
    }

    // --- Perce-cœur (node 7) — saignement sur critique ---
    public static final String PERCE_COEUR_NODE     = "lancier_7";
    private static final float[] PERCE_BLEED_PCT    = {0.20f, 0.25f, 0.30f, 0.35f, 0.40f};
    public static final long     PERCE_BLEED_DURATION_MS = 4_000L;

    public static float perceBleedPctForRank(int rank) {
        return PERCE_BLEED_PCT[idx(rank, PERCE_BLEED_PCT.length)];
    }

    public static String perceCoeurStatLine(int rank) {
        return "Coups critiques : saignement à " + Math.round(perceBleedPctForRank(rank) * 100) + "% dégâts/s pendant 4s";
    }

    // --- Chasseur de géants (node 8) — bonus si cible a plus de HP max que soi ---
    public static final String CHASSEUR_GEANTS_NODE  = "lancier_8";
    private static final float[] GEANTS_BONUS        = {0.06f, 0.10f, 0.14f, 0.18f, 0.22f};

    public static float geantsBonusForRank(int rank) {
        return GEANTS_BONUS[idx(rank, GEANTS_BONUS.length)];
    }

    public static String geantsStatLine(int rank) {
        return "+" + Math.round(geantsBonusForRank(rank) * 100) + "% dégâts si cible a plus de HP max que soi";
    }

    // --- Briseur de ligne (node 9) — bonus dégâts après CC (repousse/attire/étourdissement) ---
    public static final String CONTROLE_NODE         = "lancier_9";
    private static final float[] CONTROLE_BONUS      = {0.10f, 0.14f, 0.18f, 0.22f, 0.28f};
    public static final long     CONTROLE_DURATION_MS = 4_000L;

    public static float controleBonusForRank(int rank) {
        return CONTROLE_BONUS[idx(rank, CONTROLE_BONUS.length)];
    }

    public static String controleStatLine(int rank) {
        return "+" + Math.round(controleBonusForRank(rank) * 100) + "% dégâts pendant 4s après CC infligé";
    }

    // --- Portée maîtrisée (node 10) — bonus si < 3m ou > 10m ---
    public static final String PORTEE_NODE          = "lancier_10";
    private static final float[] PORTEE_BONUS_CLOSE = {0.10f, 0.13f, 0.16f, 0.20f, 0.25f};
    private static final float[] PORTEE_BONUS_FAR   = {0.10f, 0.13f, 0.16f, 0.20f, 0.25f};
    public static final double   PORTEE_CLOSE_MAX   = 3.0;
    public static final double   PORTEE_FAR_MIN     = 10.0;

    public static float porteeBonusCloseForRank(int rank) {
        return PORTEE_BONUS_CLOSE[idx(rank, PORTEE_BONUS_CLOSE.length)];
    }

    public static float porteeBonusFarForRank(int rank) {
        return PORTEE_BONUS_FAR[idx(rank, PORTEE_BONUS_FAR.length)];
    }

    public static String porteeStatLine(int rank) {
        int close = Math.round(porteeBonusCloseForRank(rank) * 100);
        int far   = Math.round(porteeBonusFarForRank(rank) * 100);
        return "+" + close + "% dégâts si < 3m ou +" + far + "% si > 10m";
    }

    private static int idx(int rank, int len) {
        return Math.max(0, Math.min(rank - 1, len - 1));
    }
}
