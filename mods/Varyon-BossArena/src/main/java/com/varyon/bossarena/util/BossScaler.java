package com.varyon.bossarena.util;

import com.varyon.bossarena.data.BossDefinition;
import com.varyon.bossarena.boss.BossModifiers;

public final class BossScaler {

    private BossScaler() {}

    public static BossModifiers calculateModifiers(BossDefinition def, int playerCount) {
        if (def == null) {
            return defaultModifiers();
        }

        float baseHp = positiveOrDefault(def.modifiers != null ? def.modifiers.hp : 1.0f, 1.0f);
        float baseDamage = positiveOrDefault(def.modifiers != null ? def.modifiers.damage : 1.0f, 1.0f);
        float baseSpeed = positiveOrDefault(def.modifiers != null ? def.modifiers.movementSpeed : 1.0f, 1.0f);
        float baseSize = positiveOrDefault(def.modifiers != null ? def.modifiers.size : 1.0f, 1.0f);
        float baseAttackRate = positiveOrDefault(def.modifiers != null ? def.modifiers.attackRate : 1.0f, 1.0f);
        float baseAbilityCooldown = positiveOrDefault(def.modifiers != null ? def.modifiers.abilityCooldown : 1.0f, 1.0f);
        float baseKnockbackGiven = positiveOrDefault(def.modifiers != null ? def.modifiers.knockbackGiven : 1.0f, 1.0f);
        float baseKnockbackTaken = positiveOrDefault(def.modifiers != null ? def.modifiers.knockbackTaken : 1.0f, 1.0f);
        float baseTurnRate = positiveOrDefault(def.modifiers != null ? def.modifiers.turnRate : 1.0f, 1.0f);
        // regen = flat HP restored every second (discrete steps on the stored values).
        float baseRegen = BossRegen.normalizeHpPerSecond(def.modifiers != null ? def.modifiers.regen : 0.0f);

        float perHp = finiteOrDefault(def.perPlayerIncrease != null ? def.perPlayerIncrease.hp : 1.0f, 1.0f);
        float perDamage = finiteOrDefault(def.perPlayerIncrease != null ? def.perPlayerIncrease.damage : 1.0f, 1.0f);
        float perSpeed = finiteOrDefault(def.perPlayerIncrease != null ? def.perPlayerIncrease.movementSpeed : 1.0f, 1.0f);
        float perSize = finiteOrDefault(def.perPlayerIncrease != null ? def.perPlayerIncrease.size : 1.0f, 1.0f);
        float perAttackRate = finiteOrDefault(def.perPlayerIncrease != null ? def.perPlayerIncrease.attackRate : 1.0f, 1.0f);
        float perAbilityCooldown = finiteOrDefault(def.perPlayerIncrease != null ? def.perPlayerIncrease.abilityCooldown : 1.0f, 1.0f);
        float perKnockbackGiven = finiteOrDefault(def.perPlayerIncrease != null ? def.perPlayerIncrease.knockbackGiven : 1.0f, 1.0f);
        float perKnockbackTaken = finiteOrDefault(def.perPlayerIncrease != null ? def.perPlayerIncrease.knockbackTaken : 1.0f, 1.0f);
        float perTurnRate = finiteOrDefault(def.perPlayerIncrease != null ? def.perPlayerIncrease.turnRate : 1.0f, 1.0f);
        float perRegen = BossRegen.normalizeHpPerSecond(def.perPlayerIncrease != null ? def.perPlayerIncrease.regen : 0.0f);

        int players = Math.max(1, playerCount);

        // final = base × (perPlayer × playerCount), e.g. 1.5 with 2 players → ×3
        float hp = scaleByPlayers(baseHp, perHp, players);
        float damage = scaleByPlayers(baseDamage, perDamage, players);
        float speed = scaleByPlayers(baseSpeed, perSpeed, players);
        float size = scaleByPlayers(baseSize, perSize, players);
        float attackRate = scaleByPlayers(baseAttackRate, perAttackRate, players);
        float abilityCooldown = scaleByPlayers(baseAbilityCooldown, perAbilityCooldown, players);
        float knockbackGiven = scaleByPlayers(baseKnockbackGiven, perKnockbackGiven, players);
        float knockbackTaken = scaleByPlayers(baseKnockbackTaken, perKnockbackTaken, players);
        float turnRate = scaleByPlayers(baseTurnRate, perTurnRate, players);
        // Regen stays flat HP/s added once per present player.
        float regen = Math.max(0.0f, baseRegen + (perRegen * players));

        return new BossModifiers(
                hp,
                damage,
                speed,
                size,
                attackRate,
                abilityCooldown,
                knockbackGiven,
                knockbackTaken,
                turnRate,
                regen
        );
    }

    /**
     * Multiplies base by {@code perPlayer × playerCount}.
     * Legacy {@code perPlayer <= 0} keeps base unchanged (scaling off).
     */
    private static float scaleByPlayers(float base, float perPlayer, int players) {
        float safeBase = positiveOrDefault(base, 1.0f);
        if (!Float.isFinite(perPlayer) || perPlayer <= 0.0f) {
            return safeBase;
        }
        return positiveOrDefault(safeBase * perPlayer * players, 1.0f);
    }

    private static BossModifiers defaultModifiers() {
        return new BossModifiers(
                1.0f,
                1.0f,
                1.0f,
                1.0f,
                1.0f,
                1.0f,
                1.0f,
                1.0f,
                1.0f,
                0.0f
        );
    }

    private static float finiteOrDefault(float value, float fallback) {
        return Float.isFinite(value) ? value : fallback;
    }

    private static float positiveOrDefault(float value, float fallback) {
        if (!Float.isFinite(value) || value <= 0.0f) {
            return fallback;
        }
        return value;
    }
}
