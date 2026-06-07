package fr.varyon.vrpg.classes.ability;

import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

public final class ClassSkillStamina {

    private static int staminaIdx = Integer.MIN_VALUE;

    private ClassSkillStamina() {}

    public static boolean hasEnough(@Nonnull PlayerRef playerRef, float cost) {
        if (cost <= 0f) return true;
        int idx = staminaIndex();
        if (idx < 0) return true;
        EntityStatMap stats = playerRef.getComponent(EntityStatMap.getComponentType());
        if (stats == null) return true;
        var stat = stats.get(idx);
        if (stat == null) return true;
        return stat.get() >= cost;
    }

    public static boolean consume(@Nonnull PlayerRef playerRef, float cost) {
        if (cost <= 0f) return true;
        int idx = staminaIndex();
        if (idx < 0) return true;
        EntityStatMap stats = playerRef.getComponent(EntityStatMap.getComponentType());
        if (stats == null) return false;
        if (!hasEnough(playerRef, cost)) return false;
        stats.addStatValue(idx, -cost);
        return true;
    }

    private static int staminaIndex() {
        if (staminaIdx == Integer.MIN_VALUE) {
            try {
                staminaIdx = DefaultEntityStatTypes.getStamina();
            } catch (Exception e) {
                staminaIdx = -1;
            }
        }
        return staminaIdx;
    }
}
