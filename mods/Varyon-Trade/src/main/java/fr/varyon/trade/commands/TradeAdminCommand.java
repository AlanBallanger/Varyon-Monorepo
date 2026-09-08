package fr.varyon.trade.commands;

import fr.varyon.trade.commands.subcommands.IgnoreCommand;
import fr.varyon.trade.commands.subcommands.ReloadCommand;
import fr.varyon.trade.commands.subcommands.SettingsCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public class TradeAdminCommand extends AbstractCommandCollection
{
    public TradeAdminCommand()
    {
        super("varyontrade", "Base command.");
        requireNoPermission();

        addSubCommand(new SettingsCommand());
        addSubCommand(new ReloadCommand());
        addSubCommand(new IgnoreCommand());
    }
}
