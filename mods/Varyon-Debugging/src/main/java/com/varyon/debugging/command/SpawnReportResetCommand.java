package com.varyon.debugging.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.varyon.debugging.SpawnRegistry;
import javax.annotation.Nonnull;

/** /spawnreport reset - clears counters, to compare a fresh window (e.g. "5 min in town"). */
public final class SpawnReportResetCommand extends CommandBase {

    public SpawnReportResetCommand() {
        super("reset", "Remet a zero les compteurs de spawn.");
        this.requirePermission("varyon.admin");
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        SpawnRegistry.get().reset();
        context.sendMessage(Message.raw("Compteurs de spawn remis a zero."));
    }
}
