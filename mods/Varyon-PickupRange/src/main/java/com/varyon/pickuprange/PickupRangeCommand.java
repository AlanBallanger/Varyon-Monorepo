package com.varyon.pickuprange;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

import javax.annotation.Nonnull;

/**
 * {@code /vpr} — inspect and change the multipliers at runtime.
 *
 * <ul>
 *   <li>{@code /vpr} — print the current pickup and drop/throw multipliers.</li>
 *   <li>{@code /vpr pickup <x>} — set the pickup-radius multiplier (1.0 .. 50.0).</li>
 *   <li>{@code /vpr throw <x>} — set the drop/throw-speed multiplier (1.0 .. 50.0).</li>
 *   <li>{@code /vpr reset} — restore both to their defaults.</li>
 * </ul>
 *
 * All mutating subcommands are refused when {@code Locked} is true in the config, and persist the new
 * value via {@code PickupRangePlugin}.
 */
public final class PickupRangeCommand extends CommandBase {

    private final PickupRangePlugin plugin;

    public PickupRangeCommand(PickupRangePlugin plugin) {
        super("vpr", "Varyon-PickupRange: view or change the pickup / throw multipliers");
        this.plugin = plugin;
        this.requirePermission("varyon.admin");
        this.addSubCommand(new SetPickupCommand(plugin));
        this.addSubCommand(new SetThrowCommand(plugin));
        this.addSubCommand(new ResetCommand(plugin));
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        PickupRangeConfig config = plugin.getConfig();
        context.sendMessage(Message.raw(String.format(
                "[Varyon-PickupRange] pickup x%.2f, throw x%.2f%s",
                config.getPickupMultiplier(),
                config.getDropThrowMultiplier(),
                config.isLocked() ? " (locked)" : "")));
        context.sendMessage(Message.raw("Usage: /vpr pickup <1.0-50.0> | /vpr throw <1.0-50.0> | /vpr reset"));
    }

    private static boolean refuseIfLocked(CommandContext context, PickupRangePlugin plugin) {
        if (plugin.getConfig().isLocked()) {
            context.sendMessage(Message.raw("[Varyon-PickupRange] Locked by the server config; change 'Locked' to false to edit in-game."));
            return true;
        }
        return false;
    }

    private static final class SetPickupCommand extends CommandBase {

        private final PickupRangePlugin plugin;
        private final RequiredArg<Double> valueArg =
                this.withRequiredArg("multiplier", "Pickup radius multiplier, 1.0 to 50.0", ArgTypes.DOUBLE);

        private SetPickupCommand(PickupRangePlugin plugin) {
            super("pickup", "Set the pickup-radius multiplier");
            this.plugin = plugin;
            this.requirePermission("varyon.admin");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            if (refuseIfLocked(context, plugin)) {
                return;
            }
            double applied = plugin.setPickupMultiplier(this.valueArg.get(context));
            context.sendMessage(Message.raw(String.format(
                    "[Varyon-PickupRange] Pickup multiplier set to x%.2f. New ground items use it immediately; existing ones keep their current radius until re-dropped.",
                    applied)));
        }
    }

    private static final class SetThrowCommand extends CommandBase {

        private final PickupRangePlugin plugin;
        private final RequiredArg<Double> valueArg =
                this.withRequiredArg("multiplier", "Drop/throw speed multiplier, 1.0 to 50.0", ArgTypes.DOUBLE);

        private SetThrowCommand(PickupRangePlugin plugin) {
            super("throw", "Set the drop/throw speed multiplier");
            this.plugin = plugin;
            this.requirePermission("varyon.admin");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            if (refuseIfLocked(context, plugin)) {
                return;
            }
            double applied = plugin.setDropThrowMultiplier(this.valueArg.get(context));
            context.sendMessage(Message.raw(String.format(
                    "[Varyon-PickupRange] Drop/throw multiplier set to x%.2f.", applied)));
        }
    }

    private static final class ResetCommand extends CommandBase {

        private final PickupRangePlugin plugin;

        private ResetCommand(PickupRangePlugin plugin) {
            super("reset", "Restore both multipliers to their defaults");
            this.plugin = plugin;
            this.requirePermission("varyon.admin");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            if (refuseIfLocked(context, plugin)) {
                return;
            }
            plugin.resetMultipliers();
            PickupRangeConfig config = plugin.getConfig();
            context.sendMessage(Message.raw(String.format(
                    "[Varyon-PickupRange] Reset: pickup x%.2f, throw x%.2f",
                    config.getPickupMultiplier(),
                    config.getDropThrowMultiplier())));
        }
    }
}
