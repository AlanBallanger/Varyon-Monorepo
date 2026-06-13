package fr.varyon.vrpg.classes.ability;

import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

public final class ClassSkillMana {

    private static Integer manaIdx = null;

    private ClassSkillMana() {}

    public static boolean hasEnough(@Nonnull PlayerRef playerRef, float cost) {
        if (cost <= 0f) return true;
        int idx = manaIndex();
        if (idx < 0) return true;
        EntityStatMap stats = playerRef.getComponent(EntityStatMap.getComponentType());
        if (stats == null) return true;
        var stat = stats.get(idx);
        if (stat == null) return true;
        return stat.get() >= cost;
    }

    public static boolean consume(@Nonnull PlayerRef playerRef, float cost) {
        if (cost <= 0f) return true;
        int idx = manaIndex();
        if (idx < 0) return true;
        EntityStatMap stats = playerRef.getComponent(EntityStatMap.getComponentType());
        if (stats == null) return false;
        if (!hasEnough(playerRef, cost)) return false;
        stats.addStatValue(idx, -cost);
        return true;
    }

    public static void restoreByPct(@Nonnull PlayerRef playerRef, float pct) {
        if (pct <= 0f) return;
        int idx = manaIndex();
        if (idx < 0) return;
        EntityStatMap stats = playerRef.getComponent(EntityStatMap.getComponentType());
        if (stats == null) return;
        var stat = stats.get(idx);
        if (stat == null) return;
        restore(playerRef, stat.getMax() * pct);
    }

    public static void restore(@Nonnull PlayerRef playerRef, float amount) {
        if (amount <= 0f) return;
        int idx = manaIndex();
        if (idx < 0) return;
        EntityStatMap stats = playerRef.getComponent(EntityStatMap.getComponentType());
        if (stats == null) return;
        var stat = stats.get(idx);
        if (stat == null) return;
        float capped = Math.min(amount, stat.getMax() - stat.get());
        if (capped > 0f) stats.addStatValue(idx, capped);
    }

    public static float getMaxMana(@Nonnull PlayerRef playerRef) {
        int idx = manaIndex();
        if (idx < 0) return 0f;
        EntityStatMap stats = playerRef.getComponent(EntityStatMap.getComponentType());
        if (stats == null) return 0f;
        var stat = stats.get(idx);
        return stat != null ? stat.getMax() : 0f;
    }

    private static int manaIndex() {
        if (manaIdx == null) {
            try {
                manaIdx = DefaultEntityStatTypes.getMana();
            } catch (Exception e) {
                manaIdx = -1;
            }
        }
        return manaIdx;
    }
}
