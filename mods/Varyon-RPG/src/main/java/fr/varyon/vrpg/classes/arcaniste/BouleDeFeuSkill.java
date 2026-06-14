package fr.varyon.vrpg.classes.arcaniste;

public final class BouleDeFeuSkill {

    public static final String SKILL_ID       = "boule_de_feu";
    public static final String TALENT_NODE_ID = "arcaniste_2";
    public static final String PROJECTILE_CONFIG = "Vrpg_Fireball";

    private static final float[] DAMAGE_PCT  = {2.0f, 2.25f, 2.5f, 2.75f, 3.0f};
    private static final float   DAMAGE_RADIUS = 3.0f;
    private static final long[]  COOLDOWN_MS = {10000, 9000, 8000, 7000, 6000};
    private static final float[] MANA_COST   = {10f, 12f, 14f, 16f, 18f};

    private BouleDeFeuSkill() {}

    public static int   maxRank()                    { return COOLDOWN_MS.length; }
    private static int  idx(int rank)               { return Math.max(0, Math.min(rank - 1, COOLDOWN_MS.length - 1)); }

    public static float  damagePctForRank(int rank)  { return DAMAGE_PCT[idx(rank)]; }
    public static float  damageRadius()              { return DAMAGE_RADIUS; }
    public static long   cooldownMsForRank(int rank)  { return COOLDOWN_MS[idx(rank)]; }
    public static float  manaCostForRank(int rank)   { return MANA_COST[idx(rank)]; }

    public static String statLineForRank(int rank) {
        int pct  = Math.round(damagePctForRank(rank) * 100);
        int cd   = (int) (cooldownMsForRank(rank) / 1000);
        int mana = Math.round(manaCostForRank(rank));
        return pct + "% dégâts arme (rayon " + (int) DAMAGE_RADIUS + " blocs), " + mana + " mana, CD " + cd + "s";
    }
}
