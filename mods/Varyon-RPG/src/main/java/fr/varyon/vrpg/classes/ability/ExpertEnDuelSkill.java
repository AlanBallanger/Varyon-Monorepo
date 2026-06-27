package fr.varyon.vrpg.classes.ability;

public final class ExpertEnDuelSkill {

    public static final String SKILL_ID = "expert_en_duel";
    public static final String TALENT_NODE_ID = "duelliste_1";

    private static final double[] XP_BONUS = {0.10, 0.15, 0.20, 0.25, 0.30};

    private ExpertEnDuelSkill() {}

    public static int maxRank() {
        return XP_BONUS.length;
    }

    public static double xpBonusForRank(int rank) {
        return XP_BONUS[Math.max(0, Math.min(rank - 1, XP_BONUS.length - 1))];
    }

    public static String statLineForRank(int rank) {
        int pct = (int) Math.round(xpBonusForRank(rank) * 100);
        return "+" + pct + "% XP";
    }
}
