package fr.varyon.vrpg.classes.arcaniste;

public final class NovaDeGivreSkill {

    public static final String SKILL_ID       = "nova_de_givre";
    public static final String TALENT_NODE_ID = "arcaniste_6";
    public static final String SLOW_EFFECT    = "Vrpg_Arme_Lourde";

    private static final float[] DAMAGE_PCT  = {0.8f, 1.1f, 1.5f, 2.0f, 2.8f};
    private static final float   RADIUS      = 6.0f;
    private static final long[]  SLOW_MS     = {2000, 2500, 3000, 3500, 4000};
    private static final long[]  COOLDOWN_MS = {14000, 13000, 12000, 11000, 10000};
    private static final float[] MANA_COST   = {12f, 14f, 16f, 18f, 20f};

    private NovaDeGivreSkill() {}

    public static int   maxRank()                    { return COOLDOWN_MS.length; }
    private static int  idx(int rank)               { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static float  damagePctForRank(int rank)  { return DAMAGE_PCT[idx(rank)]; }
    public static float  radius()                    { return RADIUS; }
    public static long   slowMsForRank(int rank)     { return SLOW_MS[idx(rank)]; }
    public static long   cooldownMsForRank(int rank)  { return COOLDOWN_MS[idx(rank)]; }
    public static float  manaCostForRank(int rank)   { return MANA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int pct  = Math.round(damagePctForRank(rank) * 100);
        int slow = (int) (slowMsForRank(rank) / 1000);
        int cd   = (int) (cooldownMsForRank(rank) / 1000);
        int mana = Math.round(manaCostForRank(rank));
        return pct + "% dégâts arme, ralentit " + slow + "s (rayon " + (int) RADIUS + " blocs), " + mana + " mana, CD " + cd + "s";
    }
}
