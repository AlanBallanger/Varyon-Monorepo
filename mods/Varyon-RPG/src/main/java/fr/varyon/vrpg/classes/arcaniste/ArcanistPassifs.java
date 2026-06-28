package fr.varyon.vrpg.classes.arcaniste;

public final class ArcanistPassifs {

    private ArcanistPassifs() {}

    private static int idx(int rank, int len) {
        return Math.max(0, Math.min(rank - 1, len - 1));
    }

    // --- Talent Inné (node 1) — XP bonus si > 8 blocs de la cible ---
    public static final String TALENT_INNE_NODE     = "arcaniste_1";
    public static final double TALENT_INNE_DISTANCE = 8.0;
    private static final float[] TALENT_INNE_BONUS  = {0.10f, 0.15f, 0.20f, 0.27f, 0.40f};

    public static float talentInneBonusForRank(int rank) {
        return TALENT_INNE_BONUS[idx(rank, TALENT_INNE_BONUS.length)];
    }
    public static String talentInneStatLine(int rank) {
        int pct = Math.round(talentInneBonusForRank(rank) * 100);
        return "+" + pct + "% XP si à plus de " + (int) TALENT_INNE_DISTANCE + " blocs de la cible";
    }

    // --- Écho Temporel (node 3) — réduit les cooldowns ---
    public static final String ECHO_TEMPOREL_NODE    = "arcaniste_6";
    private static final float[] ECHO_TEMPOREL_REDUC = {0.05f, 0.08f, 0.11f, 0.15f, 0.20f};

    public static float echoTemporelReducForRank(int rank) {
        return ECHO_TEMPOREL_REDUC[idx(rank, ECHO_TEMPOREL_REDUC.length)];
    }
    public static String echoTemporelStatLine(int rank) {
        int pct = Math.round(echoTemporelReducForRank(rank) * 100);
        return "-" + pct + "% temps de recharge des compétences actives";
    }

    // --- Puits de Mana (node 5) — augmente le mana max ---
    public static final String PUITS_MANA_NODE     = "arcaniste_5";
    private static final float[] PUITS_MANA_BONUS  = {0.10f, 0.15f, 0.20f, 0.27f, 0.35f};

    public static float puitsManaBonus(int rank) {
        return PUITS_MANA_BONUS[idx(rank, PUITS_MANA_BONUS.length)];
    }
    public static String puitsManaStatLine(int rank) {
        int pct = Math.round(puitsManaBonus(rank) * 100);
        return "+" + pct + "% mana maximum";
    }

    // --- Drain Mystique (node 7) — éliminations → mana restauré ---
    public static final String DRAIN_MYSTIQUE_NODE    = "arcaniste_9";
    private static final float[] DRAIN_MYSTIQUE_BONUS = {0.05f, 0.08f, 0.11f, 0.15f, 0.20f};

    public static float drainMystiqueBonusForRank(int rank) {
        return DRAIN_MYSTIQUE_BONUS[idx(rank, DRAIN_MYSTIQUE_BONUS.length)];
    }
    public static String drainMystiqueStatLine(int rank) {
        int pct = Math.round(drainMystiqueBonusForRank(rank) * 100);
        return "+" + pct + "% mana restauré par élimination";
    }

    // --- Écho Arcanique (node 9) — chance de ne pas déclencher le Délai ---
    public static final String ECHO_ARCANIQUE_NODE     = "arcaniste_3";
    private static final float[] ECHO_ARCANIQUE_CHANCE = {0.05f, 0.08f, 0.11f, 0.15f, 0.20f};

    public static float echoArcanicChanceForRank(int rank) {
        return ECHO_ARCANIQUE_CHANCE[idx(rank, ECHO_ARCANIQUE_CHANCE.length)];
    }
    public static String echoArcanicStatLine(int rank) {
        int pct = Math.round(echoArcanicChanceForRank(rank) * 100);
        return pct + "% de chance de ne pas déclencher le temps de recharge";
    }

    // --- Pouvoir Grandissant (node 11) — bonus dégâts sorts si pas touché depuis 10s ---
    public static final String POUVOIR_GRANDISSANT_NODE    = "arcaniste_11";
    public static final long   POUVOIR_GRANDISSANT_DELAY_MS = 10_000L;
    private static final float[] POUVOIR_GRANDISSANT_BONUS = {0.10f, 0.15f, 0.20f, 0.28f, 0.40f};

    public static float pouvoirGrandissantBonusForRank(int rank) {
        return POUVOIR_GRANDISSANT_BONUS[idx(rank, POUVOIR_GRANDISSANT_BONUS.length)];
    }
    public static long pouvoirGrandissantDelayMs() {
        return POUVOIR_GRANDISSANT_DELAY_MS;
    }
    public static String pouvoirGrandissantStatLine(int rank) {
        int pct = Math.round(pouvoirGrandissantBonusForRank(rank) * 100);
        int sec = (int) (POUVOIR_GRANDISSANT_DELAY_MS / 1000);
        return "+" + pct + "% dégâts sorts après " + sec + "s sans subir de dégâts (reset au prochain coup)";
    }
}
