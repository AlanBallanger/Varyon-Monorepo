package com.varyon.craftrestrict.commands.subcommand;

import com.varyon.craftrestrict.Main;
import com.hypixel.hytale.common.plugin.AuthorInfo;
import com.hypixel.hytale.common.plugin.PluginManifest;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class HelpSubCommand extends AbstractAsyncCommand {

    public HelpSubCommand() {
        super("help", "Shows all available commands");
    }

    @Nullable
    @Override
    protected String generatePermissionNode() {
        return "craftrestrict.commands.craftrestrict.help.use";
    }

    @Nonnull
    @Override
    protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext commandContext) {
        CommandSender sender = commandContext.sender();
        Main plugin = Main.getPluginInstance();
        PluginManifest manifest = plugin.getManifest();
        HytaleLogger logger = plugin.getLogger();
        String message = """

                ====================
                HELP - CraftRestrict
                ====================
                /craftrestrict help -> Show this message.
                /craftrestrict reloadconfig/reload/rl -> Reload the mod config.
                /craftrestrict gui -> Open the restriction management UI.
                ====================
                """.stripTrailing().formatted(
                manifest.getName(),
                manifest.getVersion(),
                manifest.getAuthors().getFirst().getName(),
                manifest.getDescription());
        if (sender instanceof Player) {
            sender.sendMessage(Message.raw(message));
            return CompletableFuture.completedFuture(null);
        }
        logger.at(Level.INFO).log(message);
        return CompletableFuture.completedFuture(null);
    }
}
