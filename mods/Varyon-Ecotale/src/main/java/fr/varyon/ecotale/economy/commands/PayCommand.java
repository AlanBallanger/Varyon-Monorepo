package fr.varyon.ecotale.economy.commands;

import fr.varyon.ecotale.VaryonEcotalePlugin;
import fr.varyon.ecotale.economy.EconomyManager;
import fr.varyon.ecotale.economy.gui.PayGui;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.concurrent.CompletableFuture;

public class PayCommand extends AbstractAsyncCommand {

    private final OptionalArg<PlayerRef> playerArg;
    private final OptionalArg<Double> amountArg;

    public PayCommand() {
        super("pay", "Send money to another player");
        this.playerArg = this.withOptionalArg("player", "The player to send money to", ArgTypes.PLAYER_REF);
        this.amountArg = this.withOptionalArg("amount", "The amount to send", ArgTypes.DOUBLE);
    }

    @NotNull
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext ctx) {
        if (!ctx.isPlayer()) {
            ctx.sendMessage(Message.raw("This command can only be used by players").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        Ref<EntityStore> senderEntity = ctx.senderAsPlayerRef();
        if (senderEntity == null || !senderEntity.isValid()) {
            ctx.sendMessage(Message.raw("Error: Could not get your player data").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        Store<EntityStore> senderStore = senderEntity.getStore();
        var world = senderStore.getExternalData().getWorld();
        if (world == null) return CompletableFuture.completedFuture(null);

        Player player = senderStore.getComponent(senderEntity, Player.getComponentType());
        if (player == null) {
            ctx.sendMessage(Message.raw("Error: Could not get your player data").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void> future = new CompletableFuture<>();
        world.execute(() -> {
            PlayerRef senderRef = senderStore.getComponent(senderEntity, PlayerRef.getComponentType());
            if (senderRef == null) {
                ctx.sendMessage(Message.raw("Error: Could not get player reference").color(Color.RED));
                future.complete(null);
                return;
            }

            PlayerRef targetRef = ctx.get(playerArg);
            Double amount = ctx.get(amountArg);

            if (targetRef == null && amount == null) {
                player.getPageManager().openCustomPage(senderEntity, senderStore, new PayGui(senderRef));
                future.complete(null);
                return;
            }

            if (targetRef == null) {
                ctx.sendMessage(Message.raw("Usage: /pay <player> <amount> or /pay to open GUI").color(Color.GRAY));
                future.complete(null);
                return;
            }

            if (amount == null || amount <= 0) {
                ctx.sendMessage(Message.raw("Amount must be a positive number").color(Color.RED));
                future.complete(null);
                return;
            }

            double minTx = VaryonEcotalePlugin.getInstance().getEconomyConfig().getMinimumTransaction();
            if (amount < minTx) {
                ctx.sendMessage(Message.join(
                    Message.raw("Minimum transaction is ").color(Color.RED),
                    Message.raw(VaryonEcotalePlugin.getInstance().getEconomyConfig().format(minTx)).color(Color.WHITE)
                ));
                future.complete(null);
                return;
            }

            EconomyManager.TransferResult result = VaryonEcotalePlugin.getInstance().getEconomyManager()
                .transfer(senderRef.getUuid(), targetRef.getUuid(), amount, "Player payment");

            switch (result) {
                case SUCCESS -> {
                    double fee = amount * VaryonEcotalePlugin.getInstance().getEconomyConfig().getTransferFee();
                    senderRef.sendMessage(Message.join(
                        Message.raw("Payment sent! ").color(Color.GREEN),
                        Message.raw(VaryonEcotalePlugin.getInstance().getEconomyConfig().format(amount)).color(new Color(50, 205, 50)).bold(true),
                        fee > 0 ? Message.raw(" (Fee: " + VaryonEcotalePlugin.getInstance().getEconomyConfig().format(fee) + ")").color(Color.GRAY) : Message.raw("")
                    ));
                }
                case INSUFFICIENT_FUNDS -> {
                    double balance = VaryonEcotalePlugin.getInstance().getEconomyManager().getBalance(senderRef.getUuid());
                    senderRef.sendMessage(Message.join(
                        Message.raw("Insufficient funds. Your balance: ").color(Color.RED),
                        Message.raw(VaryonEcotalePlugin.getInstance().getEconomyConfig().format(balance)).color(Color.WHITE)
                    ));
                }
                case SELF_TRANSFER ->
                    senderRef.sendMessage(Message.raw("You cannot send money to yourself").color(Color.RED));
                case INVALID_AMOUNT ->
                    senderRef.sendMessage(Message.raw("Invalid amount").color(Color.RED));
                case RECIPIENT_MAX_BALANCE ->
                    senderRef.sendMessage(Message.raw("Recipient has reached maximum balance").color(Color.RED));
            }

            future.complete(null);
        });

        return future;
    }
}
