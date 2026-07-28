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
        float perRegen = finiteOrDefault(def.perPlayerIncrease != null ? def.perPlayerIncrease.regen : 1.0f, 1.0f);

        int players = Math.max(1, playerCount);

        // final = base × (1 + (perPlayer - 1) × (players - 1)): perPlayer=1.0 is neutral
        // (no scaling regardless of player count); perPlayer=1.5 adds +50% of base per
        // player beyond the first (2 players → ×1.5, 3 players → ×2.0).
        float hp = scaleByPlayers(baseHp, perHp, players);
        float damage = scaleByPlayers(baseDamage, perDamage, players);
        float speed = scaleByPlayers(baseSpeed, perSpeed, players);
        float size = scaleByPlayers(baseSize, perSize, players);
        float attackRate = scaleByPlayers(baseAttackRate, perAttackRate, players);
        float abilityCooldown = scaleByPlayers(baseAbilityCooldown, perAbilityCooldown, players);
        float knockbackGiven = scaleByPlayers(baseKnockbackGiven, perKnockbackGiven, players);
        float knockbackTaken = scaleByPlayers(baseKnockbackTaken, perKnockbackTaken, players);
        float turnRate = scaleByPlayers(baseTurnRate, perTurnRate, players);
        float regen = scaleRegenByPlayers(baseRegen, perRegen, players);

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
     * Multiplies base by {@code 1 + (perPlayer - 1) × (players - 1)}.
     * {@code perPlayer == 1.0} (or {@code <= 0}, legacy/unset) is neutral: no scaling regardless
     * of player count. Values above 1.0 add that fraction of base per player beyond the first.
     */
    private static float scaleByPlayers(float base, float perPlayer, int players) {
        float safeBase = positiveOrDefault(base, 1.0f);
        if (!Float.isFinite(perPlayer) || perPlayer <= 0.0f) {
            return safeBase;
        }
        float extraPlayers = Math.max(0, players - 1);
        float scaleFactor = 1.0f + (perPlayer - 1.0f) * extraPlayers;
        return positiveOrDefault(safeBase * scaleFactor, 1.0f);
    }

    /** Same scaling as {@link #scaleByPlayers} but allows a zero base (0 HP/s regen is valid, unlike other stats). */
    private static float scaleRegenByPlayers(float base, float perPlayer, int players) {
        float safeBase = Float.isFinite(base) && base >= 0.0f ? base : 0.0f;
        if (!Float.isFinite(perPlayer) || perPlayer <= 0.0f) {
            return safeBase;
        }
        float extraPlayers = Math.max(0, players - 1);
        float scaleFactor = 1.0f + (perPlayer - 1.0f) * extraPlayers;
        return Math.max(0.0f, safeBase * scaleFactor);
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
