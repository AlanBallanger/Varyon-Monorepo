package fr.varyon.ecotale.economy.commands;

import fr.varyon.ecotale.VaryonEcotalePlugin;
import fr.varyon.ecotale.economy.PlayerBalance;
import fr.varyon.ecotale.economy.gui.EcoAdminGui;
import fr.varyon.ecotale.economy.hud.BalanceHud;
import fr.varyon.ecotale.economy.systems.BalanceHudSystem;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class EcoAdminCommand extends AbstractAsyncCommand {

    public EcoAdminCommand() {
        super("eco", "Economy administration commands");
        this.addAliases("economy", "ecoadmin");
        this.addSubCommand(new EcoSetCommand());
        this.addSubCommand(new EcoGiveCommand());
        this.addSubCommand(new EcoTakeCommand());
        this.addSubCommand(new EcoResetCommand());
        this.addSubCommand(new EcoTopCommand());
        this.addSubCommand(new EcoSaveCommand());
        this.addSubCommand(new EcoHudCommand());
        this.addSubCommand(new EcoMetricsCommand());
    }

    @NotNull
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext commandContext) {
        if (commandContext.isPlayer()) {
            Ref<EntityStore> ref = commandContext.senderAsPlayerRef();
            if (ref != null && ref.isValid()) {
                Store<EntityStore> store = ref.getStore();
                var world = store.getExternalData().getWorld();
                if (world == null) return CompletableFuture.completedFuture(null);
                CompletableFuture<Void> future = new CompletableFuture<>();
                world.execute(() -> {
                    Player player = store.getComponent(ref, Player.getComponentType());
                    PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                    if (player != null && playerRef != null) {
                        player.getPageManager().openCustomPage(ref, store, new EcoAdminGui(playerRef));
                    }
                    future.complete(null);
                });
                return future;
            }
        }

        commandContext.sendMessage(Message.raw("=== Ecotale Economy Admin ===").color(new Color(255, 215, 0)));
        commandContext.sendMessage(Message.raw("  /eco set <amount> - Set your balance").color(Color.GRAY));
        commandContext.sendMessage(Message.raw("  /eco give <amount> - Add to balance").color(Color.GRAY));
        commandContext.sendMessage(Message.raw("  /eco take <amount> - Remove from balance").color(Color.GRAY));
        commandContext.sendMessage(Message.raw("  /eco reset - Reset to starting balance").color(Color.GRAY));
        commandContext.sendMessage(Message.raw("  /eco top - Show top balances").color(Color.GRAY));
        commandContext.sendMessage(Message.raw("  /eco metrics - Show performance stats").color(Color.GRAY));
        commandContext.sendMessage(Message.raw("  /eco save - Force save data").color(Color.GRAY));
        return CompletableFuture.completedFuture(null);
    }

    private static Ref<EntityStore> getPlayerRef(CommandContext ctx) {
        if (!ctx.isPlayer()) return null;
        return ctx.senderAsPlayerRef();
    }

    // ========== SET COMMAND ==========
    private static class EcoSetCommand extends AbstractAsyncCommand {
        private final RequiredArg<Double> amountArg;

        public EcoSetCommand() {
            super("set", "Set your balance to a specific amount");
            this.amountArg = this.withRequiredArg("amount", "The amount to set", ArgTypes.DOUBLE);
        }

        @NotNull
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            Ref<EntityStore> ref = getPlayerRef(ctx);
            if (ref == null || !ref.isValid()) {
                ctx.sendMessage(Message.raw("This command can only be used by players").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            Double amount = amountArg.get(ctx);
            if (amount == null || amount < 0) {
                ctx.sendMessage(Message.raw("Amount must be positive").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            Store<EntityStore> store = ref.getStore();
            var world = store.getExternalData().getWorld();

            return CompletableFuture.runAsync(() -> {
                PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                if (playerRef == null) return;

                double oldBalance = VaryonEcotalePlugin.getInstance().getEconomyManager().getBalance(playerRef.getUuid());
                VaryonEcotalePlugin.getInstance().getEconomyManager().setBalance(playerRef.getUuid(), amount, "Admin set");
                updateHud(playerRef.getUuid(), amount);

                playerRef.sendMessage(Message.join(
                    Message.raw("Balance set: ").color(Color.GREEN),
                    Message.raw(VaryonEcotalePlugin.getInstance().getEconomyConfig().format(oldBalance)).color(Color.GRAY),
                    Message.raw(" -> ").color(Color.WHITE),
                    Message.raw(VaryonEcotalePlugin.getInstance().getEconomyConfig().format(amount)).color(new Color(50, 205, 50))
                ));
            }, world);
        }
    }

    // ========== GIVE COMMAND ==========
    private static class EcoGiveCommand extends AbstractAsyncCommand {
        private final RequiredArg<Double> amountArg;

        public EcoGiveCommand() {
            super("give", "Add money to your balance");
            this.addAliases("add");
            this.amountArg = this.withRequiredArg("amount", "The amount to add", ArgTypes.DOUBLE);
        }

        @NotNull
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            Ref<EntityStore> ref = getPlayerRef(ctx);
            if (ref == null || !ref.isValid()) {
                ctx.sendMessage(Message.raw("This command can only be used by players").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            Double amount = amountArg.get(ctx);
            if (amount == null || amount <= 0) {
                ctx.sendMessage(Message.raw("Amount must be positive").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            Store<EntityStore> store = ref.getStore();
            var world = store.getExternalData().getWorld();

            return CompletableFuture.runAsync(() -> {
                PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                if (playerRef == null) return;

                VaryonEcotalePlugin.getInstance().getEconomyManager().deposit(playerRef.getUuid(), amount, "Admin give");
                double newBalance = VaryonEcotalePlugin.getInstance().getEconomyManager().getBalance(playerRef.getUuid());
                updateHud(playerRef.getUuid(), newBalance);

                playerRef.sendMessage(Message.join(
                    Message.raw("Added ").color(Color.GREEN),
                    Message.raw("+" + VaryonEcotalePlugin.getInstance().getEconomyConfig().format(amount)).color(new Color(50, 205, 50)),
                    Message.raw(" | New balance: ").color(Color.GRAY),
                    Message.raw(VaryonEcotalePlugin.getInstance().getEconomyConfig().format(newBalance)).color(Color.WHITE)
                ));
            }, world);
        }
    }

    // ========== TAKE COMMAND ==========
    private static class EcoTakeCommand extends AbstractAsyncCommand {
        private final RequiredArg<Double> amountArg;

        public EcoTakeCommand() {
            super("take", "Remove money from your balance");
            this.addAliases("remove");
            this.amountArg = this.withRequiredArg("amount", "The amount to remove", ArgTypes.DOUBLE);
        }

        @NotNull
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            Ref<EntityStore> ref = getPlayerRef(ctx);
            if (ref == null || !ref.isValid()) {
                ctx.sendMessage(Message.raw("This command can only be used by players").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            Double amount = amountArg.get(ctx);
            if (amount == null || amount <= 0) {
                ctx.sendMessage(Message.raw("Amount must be positive").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            Store<EntityStore> store = ref.getStore();
            var world = store.getExternalData().getWorld();

            return CompletableFuture.runAsync(() -> {
                PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                if (playerRef == null) return;

                boolean success = VaryonEcotalePlugin.getInstance().getEconomyManager().withdraw(playerRef.getUuid(), amount, "Admin take");
                double newBalance = VaryonEcotalePlugin.getInstance().getEconomyManager().getBalance(playerRef.getUuid());
                updateHud(playerRef.getUuid(), newBalance);

                if (success) {
                    playerRef.sendMessage(Message.join(
                        Message.raw("Removed ").color(Color.YELLOW),
                        Message.raw("-" + VaryonEcotalePlugin.getInstance().getEconomyConfig().format(amount)).color(new Color(255, 99, 71)),
                        Message.raw(" | New balance: ").color(Color.GRAY),
                        Message.raw(VaryonEcotalePlugin.getInstance().getEconomyConfig().format(newBalance)).color(Color.WHITE)
                    ));
                } else {
                    playerRef.sendMessage(Message.raw("Insufficient funds").color(Color.RED));
                }
            }, world);
        }
    }

    // ========== RESET COMMAND ==========
    private static class EcoResetCommand extends AbstractAsyncCommand {
        public EcoResetCommand() {
            super("reset", "Reset balance to starting amount");
        }

        @NotNull
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            Ref<EntityStore> ref = getPlayerRef(ctx);
            if (ref == null || !ref.isValid()) {
                ctx.sendMessage(Message.raw("This command can only be used by players").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            Store<EntityStore> store = ref.getStore();
            var world = store.getExternalData().getWorld();

            return CompletableFuture.runAsync(() -> {
                PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
                if (playerRef == null) return;

                double startingBalance = VaryonEcotalePlugin.getInstance().getEconomyConfig().getStartingBalance();
                VaryonEcotalePlugin.getInstance().getEconomyManager().setBalance(playerRef.getUuid(), startingBalance, "Admin reset");
                updateHud(playerRef.getUuid(), startingBalance);

                playerRef.sendMessage(Message.join(
                    Message.raw("Balance reset to ").color(Color.GREEN),
                    Message.raw(VaryonEcotalePlugin.getInstance().getEconomyConfig().format(startingBalance)).color(new Color(50, 205, 50))
                ));
            }, world);
        }
    }

    // ========== TOP COMMAND ==========
    private static class EcoTopCommand extends AbstractAsyncCommand {
        public EcoTopCommand() {
            super("top", "Show top balances");
        }

        @NotNull
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            Map<UUID, PlayerBalance> balances = VaryonEcotalePlugin.getInstance().getEconomyManager().getAllBalances();

            if (balances.isEmpty()) {
                ctx.sendMessage(Message.raw("No player balances found").color(Color.GRAY));
                return CompletableFuture.completedFuture(null);
            }

            ctx.sendMessage(Message.raw("=== Top Balances ===").color(new Color(255, 215, 0)));

            var h2Storage = VaryonEcotalePlugin.getInstance().getEconomyManager().getH2Storage();

            List<PlayerBalance> top10 = balances.values().stream()
                .sorted(Comparator.comparingDouble(PlayerBalance::getBalance).reversed())
                .limit(10)
                .toList();

            List<CompletableFuture<String>> nameFutures = top10.stream()
                .map(balance -> {
                    if (h2Storage != null) {
                        return h2Storage.getPlayerNameAsync(balance.getPlayerUuid())
                            .thenApply(name -> name != null ? name : balance.getPlayerUuid().toString().substring(0, 8) + "...");
                    } else {
                        return CompletableFuture.completedFuture(balance.getPlayerUuid().toString().substring(0, 8) + "...");
                    }
                })
                .toList();

            return CompletableFuture.allOf(nameFutures.toArray(new CompletableFuture[0]))
                .thenAccept(v -> {
                    for (int i = 0; i < top10.size(); i++) {
                        PlayerBalance balance = top10.get(i);
                        String displayName = nameFutures.get(i).join();
                        String formatted = VaryonEcotalePlugin.getInstance().getEconomyConfig().format(balance.getBalance());
                        ctx.sendMessage(Message.join(
                            Message.raw("#" + (i + 1) + " ").color(Color.GRAY),
                            Message.raw(displayName).color(Color.WHITE),
                            Message.raw(" - ").color(Color.GRAY),
                            Message.raw(formatted).color(new Color(50, 205, 50))
                        ));
                    }
                });
        }
    }

    // ========== SAVE COMMAND ==========
    private static class EcoSaveCommand extends AbstractAsyncCommand {
        public EcoSaveCommand() {
            super("save", "Force save all data");
        }

        @NotNull
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            VaryonEcotalePlugin.getInstance().getEconomyManager().forceSave();
            ctx.sendMessage(Message.raw("Economy data saved successfully").color(Color.GREEN));
            return CompletableFuture.completedFuture(null);
        }
    }

    // ========== HUD COMMAND ==========
    private static class EcoHudCommand extends AbstractAsyncCommand {
        public EcoHudCommand() {
            super("hud", "Toggle HUD display on/off");
        }

        @NotNull
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            var config = VaryonEcotalePlugin.getInstance().getEconomyConfig();
            boolean newValue = !config.isEnableHudDisplay();
            config.setEnableHudDisplay(newValue);

            ctx.sendMessage(Message.raw("HUD Display: " + (newValue ? "Enabled" : "Disabled")).color(newValue ? Color.GREEN : Color.RED));
            ctx.sendMessage(Message.raw("Use /eco save to persist this change").color(Color.GRAY));
            return CompletableFuture.completedFuture(null);
        }
    }

    // ========== METRICS COMMAND ==========
    private static class EcoMetricsCommand extends AbstractAsyncCommand {
        public EcoMetricsCommand() {
            super("metrics", "Show performance and scaling metrics");
            this.addAliases("stats", "perf");
        }

        @NotNull
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            var monitor = fr.varyon.ecotale.economy.util.PerformanceMonitor.getInstance();
            if (monitor != null) {
                Color gold = new Color(255, 215, 0);
                Color white = Color.WHITE;
                Color green = new Color(50, 205, 50);
                ctx.sendMessage(Message.raw("--- Ecotale Economy Metrics ---").color(gold));
                ctx.sendMessage(Message.join(
                    Message.raw("Cached Balances: ").color(white),
                    Message.raw(monitor.getCachedPlayers() + " / 1000").color(green)
                ));
            } else {
                ctx.sendMessage(Message.raw("Performance monitor is not active.").color(Color.RED));
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    private static void updateHud(UUID playerUuid, double newBalance) {
        BalanceHud hud = BalanceHudSystem.getHud(playerUuid);
        if (hud != null) {
            hud.updateBalance(newBalance);
        }
    }
}
