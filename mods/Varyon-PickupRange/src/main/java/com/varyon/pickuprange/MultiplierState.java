package com.varyon.pickuprange;

/**
 * Live multiplier values shared between the plugin, the command and the two systems.
 *
 * <p>Both values are {@code volatile} so an in-game {@code /vpr} change is picked up by the systems
 * without a restart. The systems only read; the plugin and command write.
 */
public final class MultiplierState {

    private volatile float pickupMultiplier;
    private volatile float dropThrowMultiplier;

    public MultiplierState(float pickupMultiplier, float dropThrowMultiplier) {
        this.pickupMultiplier = pickupMultiplier;
        this.dropThrowMultiplier = dropThrowMultiplier;
    }

    public float getPickupMultiplier() {
        return this.pickupMultiplier;
    }

    public void setPickupMultiplier(float value) {
        this.pickupMultiplier = value;
    }

    public float getDropThrowMultiplier() {
        return this.dropThrowMultiplier;
    }

    public void setDropThrowMultiplier(float value) {
        this.dropThrowMultiplier = value;
    }
}
