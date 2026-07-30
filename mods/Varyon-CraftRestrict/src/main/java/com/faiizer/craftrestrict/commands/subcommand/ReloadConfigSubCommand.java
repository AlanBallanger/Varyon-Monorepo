package com.faiizer.craftrestrict.commands.subcommand;

import com.faiizer.craftrestrict.Main;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ReloadConfigSubCommand extends AbstractAsyncCommand {

    public ReloadConfigSubCommand() {
        super("reloadconfig", "Reload the mod config");
        this.addAliases(new String[]{"reload", "rl"});
    }

    @Nullable
    @Override
    protected String generatePermissionNode() {
        return "craftrestrict.commands.craftrestrict.reloadconfig.use";
    }

    @Nonnull
    @Override
    protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext commandContext) {
        CommandSender sender = commandContext.sender();
        Main plugin = Main.getPluginInstance();
        boolean success = plugin.reloadConfig();
        if (sender instanceof Player) {
            if (success) {
                sender.sendMessage(Message.raw("Config reloaded successfully."));
            } else {
                sender.sendMessage(Message.raw("Config reload failed, see console logs."));
            }
        }
        return CompletableFuture.completedFuture(null);
    }
}
