package com.varyon.pickuprange;

import com.hypixel.hytale.component.system.ISystem;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;

import javax.annotation.Nonnull;
import java.util.logging.Level;

/**
 * Varyon-PickupRange — multiplies the item pickup radius (and optionally the drop/throw speed) by a
 * configurable factor.
 *
 * <p>Design vs. the mod this replaces: the multiplier is applied <b>once per dropped item</b> from a
 * {@link PickupRangeApplySystem} that reacts to {@code ItemComponent} being added, instead of a
 * per-tick system that reflectively rewrote every ground item's radius on every tick. That per-tick
 * loop (single-threaded, non-parallel, two reflective writes + a full recompute per item per tick)
 * was what spent CPU and stalled the tick under item load.
 */
public final class PickupRangePlugin extends JavaPlugin {

    private final Config<PickupRangeConfig> config = this.withConfig("PickupRange", PickupRangeConfig.CODEC);

    private volatile MultiplierState state;

    public PickupRangePlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        super.setup();

        this.config.load().join();
        this.config.save();

        PickupRangeConfig cfg = this.config.get();
        this.state = new MultiplierState(
                (float) cfg.getPickupMultiplier(),
                (float) cfg.getDropThrowMultiplier());

        if (!PickupRangeField.available()) {
            getLogger().at(Level.WARNING).log(
                    "[Varyon-PickupRange] ItemComponent.pickupRange is not reachable on this server build; "
                            + "the pickup multiplier will do nothing. Drop/throw multiplier still works.");
        }

        this.getEntityStoreRegistry().registerSystem((ISystem) new PickupRangeApplySystem(this.state));
        this.getEntityStoreRegistry().registerSystem((ISystem) new DropThrowBoostSystem(this.state));
        this.getCommandRegistry().registerCommand((AbstractCommand) new PickupRangeCommand(this));

        getLogger().at(Level.INFO).log(
                "[Varyon-PickupRange] Ready. pickup x%.2f, throw x%.2f%s",
                cfg.getPickupMultiplier(), cfg.getDropThrowMultiplier(),
                cfg.isLocked() ? " (locked)" : "");
    }

    /** @return the live config (already range-clamped by its getters). */
    public PickupRangeConfig getConfig() {
        return this.config.get();
    }

    /**
     * Set the pickup-radius multiplier, clamp it, push it to the running systems and persist it.
     *
     * @return the value that was actually applied after clamping.
     */
    public double setPickupMultiplier(double raw) {
        PickupRangeConfig cfg = this.config.get();
        cfg.setPickupMultiplier(raw);
        double applied = cfg.getPickupMultiplier();
        this.state.setPickupMultiplier((float) applied);
        saveQuietly();
        return applied;
    }

    /**
     * Set the drop/throw-speed multiplier, clamp it, push it to the running systems and persist it.
     *
     * @return the value that was actually applied after clamping.
     */
    public double setDropThrowMultiplier(double raw) {
        PickupRangeConfig cfg = this.config.get();
        cfg.setDropThrowMultiplier(raw);
        double applied = cfg.getDropThrowMultiplier();
        this.state.setDropThrowMultiplier((float) applied);
        saveQuietly();
        return applied;
    }

    /** Restore both multipliers to their defaults and persist. */
    public void resetMultipliers() {
        PickupRangeConfig cfg = this.config.get();
        cfg.setPickupMultiplier(PickupRangeConfig.DEFAULT_PICKUP_MULTIPLIER);
        cfg.setDropThrowMultiplier(PickupRangeConfig.DEFAULT_DROP_THROW_MULTIPLIER);
        this.state.setPickupMultiplier((float) cfg.getPickupMultiplier());
        this.state.setDropThrowMultiplier((float) cfg.getDropThrowMultiplier());
        saveQuietly();
    }

    private void saveQuietly() {
        try {
            this.config.save();
        } catch (RuntimeException e) {
            getLogger().at(Level.WARNING).withCause(e).log("[Varyon-PickupRange] Could not save config");
        }
    }
}
