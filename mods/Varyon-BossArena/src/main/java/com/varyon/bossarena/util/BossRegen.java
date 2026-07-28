package com.varyon.bossarena.util;

/**
 * Boss regen is flat HP restored every second (not a multiplier).
 * UI range: 0–1000 HP/s.
 */
public final class BossRegen {
    public static final float MIN_HP_PER_SECOND = 0f;
    public static final float MAX_HP_PER_SECOND = 1000f;

    /** Only these discrete HP/s values are allowed; any other input snaps to the nearest one. */
    public static final float[] STEPS = {0f, 1f, 5f, 10f, 20f, 50f, 100f, 150f, 200f, 300f, 500f, 1000f};

    private BossRegen() {}

    public static float normalizeHpPerSecond(float raw) {
        if (!Float.isFinite(raw) || raw <= 0f) {
            return 0f;
        }
        float clamped = Math.min(raw, MAX_HP_PER_SECOND);
        float closest = STEPS[0];
        float closestDistance = Math.abs(clamped - closest);
        for (float step : STEPS) {
            float distance = Math.abs(clamped - step);
            if (distance < closestDistance) {
                closest = step;
                closestDistance = distance;
            }
        }
        return closest;
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
