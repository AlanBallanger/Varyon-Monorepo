package fr.varyon.musiczones.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;

public abstract class MusicZoneAdminCommandBase extends CommandBase {

    protected MusicZoneAdminCommandBase(String name, String description) {
        super(name, description);
        setPermissionGroup(null);
    }

    protected static boolean isMusicZoneAdmin(CommandContext context) {
        return context.sender().hasPermission("*") || context.sender().hasPermission("varyon.musiczones.admin");
    }

    protected boolean requireMusicZoneAdmin(CommandContext context) {
        if (isMusicZoneAdmin(context)) {
            return true;
        }
        context.sendMessage(Message.raw("Op ou permission varyon.musiczones.admin requise."));
        return false;
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }
}
