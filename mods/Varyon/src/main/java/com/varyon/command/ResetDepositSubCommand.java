package com.varyon.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.varyon.deposit.DepositBlockManager;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.awt.Color;
import java.util.concurrent.CompletableFuture;

public class ResetDepositSubCommand extends AbstractAsyncCommand {
    private final DepositBlockManager depositBlockManager;
    
    public ResetDepositSubCommand(DepositBlockManager depositBlockManager) {
        super("resetdeposit", "Reset all deposit blocks");
        this.depositBlockManager = depositBlockManager;
        this.requirePermission("varyon.admin");
    }
    
    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext context) {
        int count = depositBlockManager.clearAll();
        context.sendMessage(Message.raw("Tous les blocs de dépôt ont été supprimés (" + count + " bloc(s)).").color(Color.GREEN));
        return CompletableFuture.completedFuture(null);
    }
}
