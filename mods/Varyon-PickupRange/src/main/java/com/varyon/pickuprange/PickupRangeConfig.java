package com.varyon.pickuprange;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

/**
 * Persisted config for Varyon-PickupRange (written to {@code <data dir>/PickupRange.json}).
 *
 * <ul>
 *   <li>{@code PickupMultiplier} — dropped items get their pickup radius multiplied by this. Clamped to
 *       {@link #MIN_MULTIPLIER}..{@link #MAX_MULTIPLIER}. {@code 1.0} = vanilla.</li>
 *   <li>{@code DropThrowMultiplier} — how far items fly when a player drops/throws them, relative to
 *       vanilla. Clamped to {@link #MIN_MULTIPLIER}..{@link #MAX_MULTIPLIER}.</li>
 *   <li>{@code Locked} — when true, {@code /vpr} can only report the current values; set/reset are refused.</li>
 * </ul>
 */
public final class PickupRangeConfig {

    public static final double MIN_MULTIPLIER = 1.0d;
    public static final double MAX_MULTIPLIER = 50.0d;

    public static final double DEFAULT_PICKUP_MULTIPLIER = 1.75d;
    public static final double DEFAULT_DROP_THROW_MULTIPLIER = 1.0d;

    public static final BuilderCodec<PickupRangeConfig> CODEC = BuilderCodec.builder(PickupRangeConfig.class, PickupRangeConfig::new)
            .append(new KeyedCodec<>("PickupMultiplier", Codec.DOUBLE),
                    (config, value, extraInfo) -> config.pickupMultiplier = value,
                    (config, extraInfo) -> config.pickupMultiplier).add()
            .append(new KeyedCodec<>("DropThrowMultiplier", Codec.DOUBLE),
                    (config, value, extraInfo) -> config.dropThrowMultiplier = value,
                    (config, extraInfo) -> config.dropThrowMultiplier).add()
            .append(new KeyedCodec<>("Locked", Codec.BOOLEAN),
                    (config, value, extraInfo) -> config.locked = value,
                    (config, extraInfo) -> config.locked).add()
            .build();

    private double pickupMultiplier = DEFAULT_PICKUP_MULTIPLIER;
    private double dropThrowMultiplier = DEFAULT_DROP_THROW_MULTIPLIER;
    private boolean locked = false;

    public static double clampMultiplier(double raw) {
        if (Double.isNaN(raw)) {
            return DEFAULT_PICKUP_MULTIPLIER;
        }
        return Math.max(MIN_MULTIPLIER, Math.min(MAX_MULTIPLIER, raw));
    }

    /** @return the pickup radius multiplier, already clamped to a sane range. */
    public double getPickupMultiplier() {
        return clampMultiplier(this.pickupMultiplier);
    }

    public void setPickupMultiplier(double value) {
        this.pickupMultiplier = clampMultiplier(value);
    }

    /** @return the drop/throw speed multiplier, already clamped to a sane range. */
    public double getDropThrowMultiplier() {
        return clampMultiplier(this.dropThrowMultiplier);
    }

    public void setDropThrowMultiplier(double value) {
        this.dropThrowMultiplier = clampMultiplier(value);
    }

    public boolean isLocked() {
        return this.locked;
    }

    public void setLocked(boolean value) {
        this.locked = value;
    }
}
