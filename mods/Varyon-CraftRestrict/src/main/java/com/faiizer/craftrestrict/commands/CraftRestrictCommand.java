package com.faiizer.craftrestrict.commands;

import com.faiizer.craftrestrict.Main;
import com.faiizer.craftrestrict.commands.subcommand.GuiSubCommand;
import com.faiizer.craftrestrict.commands.subcommand.HelpSubCommand;
import com.faiizer.craftrestrict.commands.subcommand.ReloadConfigSubCommand;
import com.hypixel.hytale.common.plugin.AuthorInfo;
import com.hypixel.hytale.common.plugin.PluginManifest;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.entity.entities.Player;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CraftRestrictCommand extends AbstractCommand {

    public CraftRestrictCommand() {
        super("craftrestrict", "Manage the CraftRecipe mod");
        this.addSubCommand(new HelpSubCommand());
        this.addSubCommand(new ReloadConfigSubCommand());
        this.addSubCommand(new GuiSubCommand());
    }

    @Nullable
    @Override
    protected String generatePermissionNode() {
        return "craftrestrict.commands.craftrestrict.command.use";
    }

    @Nullable
    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext commandContext) {
        CommandSender sender = commandContext.sender();
        Main plugin = Main.getPluginInstance();
        PluginManifest manifest = plugin.getManifest();
        HytaleLogger logger = plugin.getLogger();
        String message = """

                ====================
                %s v%s
                - Author: %s
                - Description: %s
                Command list: /craftrestrict help
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
