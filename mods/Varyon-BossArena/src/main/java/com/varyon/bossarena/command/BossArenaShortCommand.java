package com.varyon.bossarena.command;

import com.varyon.bossarena.BossArenaPlugin;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

public final class BossArenaShortCommand extends AbstractCommand {

    private final BossArenaPlugin plugin;

    public BossArenaShortCommand(BossArenaPlugin plugin) {
        super("ba", "Shortcut for /bossarena");
        this.plugin = plugin;
        requirePermission(BossArenaCommand.ADMIN_PERMISSION);

        // Register all the same subcommands as BossArenaCommand
        addSubCommand(new BossArenaCommand.SpawnBoss(plugin));
        addSubCommand(new BossArenaCommand.Reload(plugin));
        addSubCommand(new BossArenaCommand.ShopRoot(plugin));
        addSubCommand(new BossArenaCommand.Cleanup(plugin));
    }

    /** Bare /ba opens the config GUI, same as /bossarena. */
    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext ctx) {
        return BossArenaCommand.openConfigGui(ctx, plugin);
    }
}
