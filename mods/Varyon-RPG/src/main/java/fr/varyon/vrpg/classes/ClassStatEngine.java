package fr.varyon.vrpg.classes;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class ClassStatEngine {

    private static final HytaleLogger LOGGER = HytaleLogger.getLogger().getSubLogger("VaryonRPG-ClassStats");

    private static final String MOD_KEY_HP      = "vrpg.class.hp";
    private static final String MOD_KEY_STAMINA = "vrpg.class.stamina";

    private final ConcurrentHashMap<UUID, ClassPlayerStats> cache = new ConcurrentHashMap<>();

    private int healthIdx  = Integer.MIN_VALUE;
    private int staminaIdx = Integer.MIN_VALUE;

    private int getHealthIdx() {
        if (healthIdx == Integer.MIN_VALUE) {
            try { healthIdx = DefaultEntityStatTypes.getHealth(); }
            catch (Exception e) { healthIdx = -1; }
        }
        return healthIdx;
    }

    private int getStaminaIdx() {
        if (staminaIdx == Integer.MIN_VALUE) {
            try { staminaIdx = DefaultEntityStatTypes.getStamina(); }
            catch (Exception e) { staminaIdx = -1; }
        }
        return staminaIdx;
    }

    @Nullable
    public ClassPlayerStats getStats(UUID uuid) {
        return cache.get(uuid);
    }

    public ClassPlayerStats computeAndApply(UUID uuid, PlayerRef playerRef, ClassAccount acc) {
        if (acc == null) return null;

        PlayerClass activeClass = acc.getActiveClass();
        if (activeClass == null) {
            removeModifiers(playerRef);
            cache.remove(uuid);
            return null;
        }

        ClassProgress progress = acc.getProgress(activeClass);
        int level = progress.getLevel();
        PlayerSpecialization spec = progress.getActiveSpec();

        ClassPlayerStats stats = ClassStatDefinition.compute(level, spec);
        cache.put(uuid, stats);

        applyEntityStats(playerRef, stats);

        LOGGER.at(Level.INFO).log("[ClassStatEngine] %s class=%s spec=%s Nv.%d → HP=%d ATK=%d ARM=%d%% STA=%d crit=%d%% critDmg=+%d%%",
            uuid.toString().substring(0, 8),
            activeClass.getId(), spec != null ? spec.getId() : "none", level,
            stats.maxHp(), stats.atk(), stats.armorPct(), stats.maxStamina(),
            stats.critChancePct(), stats.critDamagePct());

        return stats;
    }

    private void applyEntityStats(PlayerRef playerRef, ClassPlayerStats stats) {
        try {
            if (playerRef == null || !playerRef.isValid()) return;
            EntityStatMap statMap = playerRef.getComponent(EntityStatMap.getComponentType());
            if (statMap == null) return;

            int hIdx = getHealthIdx();
            int sIdx = getStaminaIdx();

            clearModifier(statMap, hIdx, MOD_KEY_HP);
            clearModifier(statMap, sIdx, MOD_KEY_STAMINA);

            // HP: multiplicateur VRPG appliqué sur le max HP actuel du joueur (base + équipement).
            // combinedHpMult = hpLevelMult(level) * specHpMult
            // Le modificateur MULTIPLICATIF fait en sorte que le moteur multiplie tout le max HP.
            if (hIdx >= 0) {
                double mult = stats.hpMult();
                if (Math.abs(mult - 1.0) > 0.001) {
                    statMap.putModifier(EntityStatMap.Predictable.ALL, hIdx, MOD_KEY_HP,
                        new StaticModifier(Modifier.ModifierTarget.MAX,
                            StaticModifier.CalculationType.MULTIPLICATIVE, (float) mult));
                }
                var hpStat = statMap.get(hIdx);
                if (hpStat != null) {
                    float newMax = hpStat.getMax();
                    if (newMax > 0f) {
                        statMap.setStatValue(EntityStatMap.Predictable.ALL, hIdx, newMax);
                    }
                }
            }

            // Stamina: VRPG définit la stamina complète (remplace la base de 100)
            if (sIdx >= 0) {
                double baseStamina = readStatMax(statMap, sIdx, 100.0);
                float staminaBonus = (float)(stats.maxStamina() - baseStamina);
                if (Math.abs(staminaBonus) > 0.1f) {
                    statMap.putModifier(EntityStatMap.Predictable.ALL, sIdx, MOD_KEY_STAMINA,
                        new StaticModifier(Modifier.ModifierTarget.MAX,
                            StaticModifier.CalculationType.ADDITIVE, staminaBonus));
                }
            }
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("[ClassStatEngine] applyEntityStats: " + e.getMessage());
        }
    }

    private double readStatMax(EntityStatMap statMap, int idx, double fallback) {
        try {
            if (idx < 0 || statMap == null) return fallback;
            var stat = statMap.get(idx);
            if (stat == null) return fallback;
            double max = stat.getMax();
            return max > 0.5 ? max : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    public void removeModifiers(PlayerRef playerRef) {
        try {
            if (playerRef == null || !playerRef.isValid()) return;
            EntityStatMap statMap = playerRef.getComponent(EntityStatMap.getComponentType());
            if (statMap == null) return;
            clearModifier(statMap, getHealthIdx(), MOD_KEY_HP);
            clearModifier(statMap, getStaminaIdx(), MOD_KEY_STAMINA);
        } catch (Exception ignored) {}
    }

    private void clearModifier(EntityStatMap statMap, int idx, String key) {
        if (idx < 0 || statMap == null) return;
        try { statMap.removeModifier(EntityStatMap.Predictable.NONE, idx, key); } catch (Throwable ignored) {}
        try { statMap.removeModifier(EntityStatMap.Predictable.ALL,  idx, key); } catch (Throwable ignored) {}
    }

    public void cleanup(UUID uuid) {
        cache.remove(uuid);
    }
}
