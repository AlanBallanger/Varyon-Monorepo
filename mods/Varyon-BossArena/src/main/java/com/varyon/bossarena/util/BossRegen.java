package com.varyon.bossarena.util;

/**
 * Boss regen is flat HP restored every second (not a multiplier).
 * UI range: 0–1000 HP/s.
 */
public final class BossRegen {
    public static final float MIN_HP_PER_SECOND = 0f;
    public static final float MAX_HP_PER_SECOND = 1000f;

    private BossRegen() {}

    public static float normalizeHpPerSecond(float raw) {
        if (!Float.isFinite(raw) || raw <= 0f) {
            return 0f;
        }
        // Legacy multiplier era used ~1.0 as "neutral" (no custom regen).
        // Values slightly above/below 1 that aren't intentional whole HP/s stay off.
        if (raw > 0.09f && raw < 1.0f) {
            return 0f;
        }
        if (raw > MAX_HP_PER_SECOND) {
            return MAX_HP_PER_SECOND;
        }
        return raw;
    }

    public static String formatLabel(float hpPerSecond) {
        float value = normalizeHpPerSecond(hpPerSecond);
        if (value <= 0f) {
            return "0";
        }
        if (Math.abs(value - Math.rint(value)) < 0.05f) {
            return Integer.toString(Math.round(value));
        }
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }
}
