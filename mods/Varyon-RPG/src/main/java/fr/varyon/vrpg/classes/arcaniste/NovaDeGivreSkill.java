package fr.varyon.vrpg.classes.arcaniste;

public final class NovaDeGivreSkill {

    public static final String SKILL_ID       = "nova_de_givre";
    public static final String TALENT_NODE_ID = "arcaniste_7";
    public static final String SLOW_EFFECT    = "Vrpg_Arme_Lourde";
    public static final String IMPACT_PARTICLE = "Ice_Blast";
    public static final String IMPACT_SOUND    = "SFX_Ice_Bolt_Death";

    private static final float[] DAMAGE_PCT  = {1.2f, 1.65f, 2.25f, 3.0f, 4.2f};
    private static final float[] RADIUS      = {3.0f, 4.0f, 5.0f, 6.0f, 7.0f};
    private static final long[]  SLOW_MS     = {2000, 2500, 3000, 3500, 4000};
    private static final long[]  COOLDOWN_MS = {28000, 26000, 24000, 22000, 20000};
    private static final float[] MANA_COST   = {24f, 28f, 32f, 36f, 40f};

    private NovaDeGivreSkill() {}

    public static int   maxRank()                    { return DAMAGE_PCT.length; }
    private static int  idx(int rank)               { return Math.max(0, Math.min(rank - 1, DAMAGE_PCT.length - 1)); }
    private static int  radiusIdx(int rank)         { return idx(rank); }

    public static float  damagePctForRank(int rank)  { return DAMAGE_PCT[idx(rank)]; }
    public static float  radiusForRank(int rank)     { return RADIUS[radiusIdx(rank)]; }
    public static float  particleScaleForRadius(float radius) { return Math.max(1.0f, radius / 3.0f); }
    public static long   slowMsForRank(int rank)     { return SLOW_MS[idx(rank)]; }
    public static long   cooldownMsForRank(int rank)  { return COOLDOWN_MS[idx(rank)]; }
    public static float  manaCostForRank(int rank)   { return MANA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int pct  = Math.round(damagePctForRank(rank) * 100);
        int slow = (int) (slowMsForRank(rank) / 1000);
        int cd   = (int) (cooldownMsForRank(rank) / 1000);
        int mana = Math.round(manaCostForRank(rank));
        return pct + "% dégâts arme, ralentit " + slow + "s (rayon " + (int) radiusForRank(rank) + " blocs), " + mana + " mana, Délai " + cd + "s";
    }
}
