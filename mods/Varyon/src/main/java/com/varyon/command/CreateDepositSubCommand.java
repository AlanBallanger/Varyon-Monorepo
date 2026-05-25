package com.varyon.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.varyon.deposit.DepositBlockManager;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.Color;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class CreateDepositSubCommand extends AbstractAsyncCommand {
    private static final Map<UUID, PendingDepositBind> pendingBinds = new ConcurrentHashMap<>();
    private final DepositBlockManager depositBlockManager;
    
    public static class PendingDepositBind {
        private final long timestamp;
        private final boolean isCreation;
        
        public PendingDepositBind(boolean isCreation) {
            this.timestamp = System.currentTimeMillis();
            this.isCreation = isCreation;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public boolean isCreation() {
            return isCreation;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > 30000; // 30 seconds
        }
    }
    
    public CreateDepositSubCommand(DepositBlockManager depositBlockManager) {
        super("createdeposit", "Set a block as deposit block (click the block after)");
        this.depositBlockManager = depositBlockManager;
        this.requirePermission("varyon.admin");
    }
    
    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext context) {
        CommandSender sender = context.sender();
        if (!(sender instanceof Player player)) {
            context.sendMessage(Message.raw("Commande joueur uniquement.").color(Color.RED));
            return CompletableFuture.completedFuture(null);
        }
        
        pendingBinds.put(player.getUuid(), new PendingDepositBind(true));
        context.sendMessage(Message.raw("Appuyez sur F en visant un bloc pour le configurer comme bloc de dépôt.").color(Color.YELLOW));
        
        return CompletableFuture.completedFuture(null);
    }
    
    public static PendingDepositBind getPendingBind(UUID playerUuid) {
        PendingDepositBind pending = pendingBinds.get(playerUuid);
        if (pending != null && pending.isExpired()) {
            pendingBinds.remove(playerUuid);
            return null;
        }
        return pending;
    }
    
    public static void clearPendingBind(UUID playerUuid) {
        pendingBinds.remove(playerUuid);
    }
}
