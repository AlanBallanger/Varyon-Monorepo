package fr.varyon.ecotale.coins.commands;

import fr.varyon.ecotale.coins.currency.BankManager;
import fr.varyon.ecotale.coins.currency.CoinManager;
import fr.varyon.ecotale.coins.transaction.SecureTransaction;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BankCommand extends AbstractAsyncCommand {

    public BankCommand() {
        super("bank", "Manage your bank account");
        this.addSubCommand(new BankDepositCommand());
        this.addSubCommand(new BankWithdrawCommand());
    }

    @NotNull
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
        if (!ctx.isPlayer()) {
            ctx.sendMessage(Message.raw("This command can only be used by players").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        Ref<EntityStore> ref = ctx.senderAsPlayerRef();
        if (ref == null || !ref.isValid()) {
            ctx.sendMessage(Message.raw("Error: Could not get your player data").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();
        if (world == null) return CompletableFuture.completedFuture(null);

        CompletableFuture<Void> future = new CompletableFuture<>();
        world.execute(() -> {
            Player player = store.getComponent(ref, Player.getComponentType());
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

            if (player == null || playerRef == null) {
                ctx.sendMessage(Message.raw("Error: Could not get your player data").color(Color.RED));
                future.complete(null);
                return;
            }

            if (!playerRef.hasPermission("ecotale.ecotalecoins.command.bank")) {
                ctx.sendMessage(Message.raw("You don't have permission to use the bank.").color(Color.RED));
                future.complete(null);
                return;
            }

            player.getPageManager().openCustomPage(ref, store, new fr.varyon.ecotale.coins.gui.BankGui(playerRef));
            future.complete(null);
        });
        return future;
    }

    // ========== Deposit Subcommand ==========
    private static class BankDepositCommand extends AbstractAsyncCommand {
        private final RequiredArg<String> amountArg;

        public BankDepositCommand() {
            super("deposit", "Deposit coins to your bank");
            this.addAliases("d");
            this.amountArg = this.withRequiredArg("amount", "Amount or 'all'", ArgTypes.STRING);
        }

        @NotNull
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            if (!ctx.isPlayer()) {
                ctx.sendMessage(Message.raw("This command can only be used by players").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            Ref<EntityStore> ref = ctx.senderAsPlayerRef();
            if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);

            Store<EntityStore> store = ref.getStore();
            World world = store.getExternalData().getWorld();
            if (world == null) return CompletableFuture.completedFuture(null);

            Player player = store.getComponent(ref, Player.getComponentType());
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

            if (player == null || playerRef == null) return CompletableFuture.completedFuture(null);

            if (!playerRef.hasPermission("ecotale.ecotalecoins.command.bank")) {
                CompletableFuture<Void> denied = new CompletableFuture<>();
                world.execute(() -> {
                    ctx.sendMessage(Message.raw("You don't have permission to use the bank.").color(Color.RED));
                    denied.complete(null);
                });
                return denied;
            }

            String amountStr = ctx.get(amountArg);
            final Player finalPlayer = player;
            final PlayerRef finalPlayerRef = playerRef;

            return CompletableFuture.runAsync(() -> {
                UUID playerUuid = finalPlayerRef.getUuid();
                long amount;
                if (amountStr.equalsIgnoreCase("all")) {
                    amount = CoinManager.countCoins(finalPlayer);
                } else {
                    try {
                        amount = Long.parseLong(amountStr);
                    } catch (NumberFormatException e) {
                        ctx.sendMessage(Message.raw("Invalid amount. Use a number or 'all'").color(Color.RED));
                        return;
                    }
                }

                if (amount <= 0) {
                    ctx.sendMessage(Message.raw("Amount must be positive").color(Color.RED));
                    return;
                }

                long currentPhysical = CoinManager.countCoins(finalPlayer);
                if (currentPhysical < amount) {
                    ctx.sendMessage(Message.join(
                        Message.raw("Not enough coins. You have: ").color(Color.RED),
                        Message.raw(formatLong(currentPhysical)).color(Color.WHITE)
                    ));
                    return;
                }

                SecureTransaction.TransactionResult result = SecureTransaction.executeSecureDeposit(finalPlayer, playerUuid, amount);
                if (result.isSuccess()) {
                    long newBankBalance = BankManager.getBankBalance(playerUuid);
                    ctx.sendMessage(Message.join(
                        Message.raw("Deposited ").color(Color.GREEN),
                        Message.raw(formatLong(amount)).color(new Color(50, 205, 50)).bold(true),
                        Message.raw(" coins. Bank: ").color(Color.GREEN),
                        Message.raw(formatLong(newBankBalance)).color(Color.WHITE)
                    ));
                } else {
                    ctx.sendMessage(Message.raw(result.getMessage()).color(Color.RED));
                }
            }, world);
        }
    }

    // ========== Withdraw Subcommand ==========
    private static class BankWithdrawCommand extends AbstractAsyncCommand {
        private final RequiredArg<String> amountArg;

        public BankWithdrawCommand() {
            super("withdraw", "Withdraw coins from your bank");
            this.addAliases("w");
            this.amountArg = this.withRequiredArg("amount", "Amount or 'all'", ArgTypes.STRING);
        }

        @NotNull
        @Override
        protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
            if (!ctx.isPlayer()) {
                ctx.sendMessage(Message.raw("This command can only be used by players").color(Color.RED));
                return CompletableFuture.completedFuture(null);
            }

            Ref<EntityStore> ref = ctx.senderAsPlayerRef();
            if (ref == null || !ref.isValid()) return CompletableFuture.completedFuture(null);

            Store<EntityStore> store = ref.getStore();
            World world = store.getExternalData().getWorld();
            if (world == null) return CompletableFuture.completedFuture(null);

            Player player = store.getComponent(ref, Player.getComponentType());
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

            if (player == null || playerRef == null) return CompletableFuture.completedFuture(null);

            if (!playerRef.hasPermission("ecotale.ecotalecoins.command.bank")) {
                CompletableFuture<Void> denied = new CompletableFuture<>();
                world.execute(() -> {
                    ctx.sendMessage(Message.raw("You don't have permission to use the bank.").color(Color.RED));
                    denied.complete(null);
                });
                return denied;
            }

            String amountStr = ctx.get(amountArg);
            final Player finalPlayer = player;
            final PlayerRef finalPlayerRef = playerRef;

            return CompletableFuture.runAsync(() -> {
                UUID playerUuid = finalPlayerRef.getUuid();
                long amount;
                if (amountStr.equalsIgnoreCase("all")) {
                    amount = BankManager.getBankBalance(playerUuid);
                } else {
                    try {
                        amount = Long.parseLong(amountStr);
                    } catch (NumberFormatException e) {
                        ctx.sendMessage(Message.raw("Invalid amount. Use a number or 'all'").color(Color.RED));
                        return;
                    }
                }

                if (amount <= 0) {
                    ctx.sendMessage(Message.raw("Amount must be positive").color(Color.RED));
                    return;
                }

                long currentBank = BankManager.getBankBalance(playerUuid);
                if (currentBank < amount) {
                    ctx.sendMessage(Message.join(
                        Message.raw("Not enough in bank. You have: ").color(Color.RED),
                        Message.raw(formatLong(currentBank)).color(Color.WHITE)
                    ));
                    return;
                }

                SecureTransaction.TransactionResult result = SecureTransaction.executeSecureWithdraw(finalPlayer, playerUuid, amount);
                if (result.isSuccess()) {
                    long newBankBalance = BankManager.getBankBalance(playerUuid);
                    ctx.sendMessage(Message.join(
                        Message.raw("Withdrew ").color(Color.GREEN),
                        Message.raw(formatLong(amount)).color(new Color(50, 205, 50)).bold(true),
                        Message.raw(" coins. Bank: ").color(Color.GREEN),
                        Message.raw(formatLong(newBankBalance)).color(Color.WHITE)
                    ));
                } else if (result.isMoneySafe() && result.getTxHash() != null) {
                    ctx.sendMessage(Message.raw(result.getMessage()).color(Color.YELLOW));
                } else {
                    ctx.sendMessage(Message.raw(result.getMessage()).color(Color.RED));
                }
            }, world);
        }
    }

    private static String formatLong(long value) {
        if (value >= 1_000_000_000) return String.format("%.2fB", value / 1_000_000_000.0);
        if (value >= 1_000_000) return String.format("%.2fM", value / 1_000_000.0);
        if (value >= 1_000) return String.format("%.1fK", value / 1_000.0);
        return String.valueOf(value);
    }
}
