package fr.varyon.ecotale.economy.commands;

import fr.varyon.ecotale.VaryonEcotalePlugin;
import fr.varyon.ecotale.economy.PlayerBalance;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.concurrent.CompletableFuture;

public class BalanceCommand extends AbstractAsyncCommand {

    public BalanceCommand() {
        super("bal", "Check your balance");
        this.addAliases("balance", "money");
    }

    @NotNull
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext commandContext) {
        if (!commandContext.isPlayer()) {
            commandContext.sendMessage(Message.raw("This command can only be used by players.").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        Ref<EntityStore> ref = commandContext.senderAsPlayerRef();
        if (ref == null || !ref.isValid()) {
            commandContext.sendMessage(Message.raw("Error: Could not get player data.").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }

        Store<EntityStore> store = ref.getStore();

        var world = store.getExternalData().getWorld();
        final Ref<EntityStore> finalRef = ref;

        return CompletableFuture.runAsync(() -> {
            PlayerRef playerRef = store.getComponent(finalRef, PlayerRef.getComponentType());
            if (playerRef == null) {
                commandContext.sendMessage(Message.raw("Error: Could not get player data.").color(Color.RED));
                return;
            }

            VaryonEcotalePlugin.getInstance().getEconomyManager().ensureAccount(playerRef.getUuid());
            PlayerBalance balance = VaryonEcotalePlugin.getInstance().getEconomyManager().getPlayerBalance(playerRef.getUuid());

            if (balance == null) {
                playerRef.sendMessage(Message.raw("Error: Could not load balance.").color(Color.RED));
                return;
            }

            String formattedBalance = VaryonEcotalePlugin.getInstance().getEconomyConfig().format(balance.getBalance());
            String earnedStr = VaryonEcotalePlugin.getInstance().getEconomyConfig().formatShort(balance.getTotalEarned());
            String spentStr = VaryonEcotalePlugin.getInstance().getEconomyConfig().formatShort(balance.getTotalSpent());

            playerRef.sendMessage(Message.raw("----- Your Balance -----").color(new Color(255, 215, 0)));
            playerRef.sendMessage(Message.join(
                Message.raw("  Balance: ").color(Color.GRAY),
                Message.raw(formattedBalance).color(new Color(50, 205, 50)).bold(true)
            ));

        }, world);
    }
}
