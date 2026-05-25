package fr.varyon.musiczones.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import fr.varyon.musiczones.VaryonMusicZonesPlugin;

import javax.annotation.Nonnull;

public final class ReloadMusicZonesSubCommand extends MusicZoneAdminCommandBase {

    public ReloadMusicZonesSubCommand() {
        super("reload", "Recharger zones.json et régénérer le pack audio");
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        if (!requireMusicZoneAdmin(context)) {
            return;
        }
        VaryonMusicZonesPlugin plugin = VaryonMusicZonesPlugin.getInstance();
        if (plugin == null) {
            context.sendMessage(Message.raw("Plugin non chargé."));
            return;
        }
        plugin.getRepository().load();
        try {
            plugin.rebuildAssetPack();
        } catch (Exception e) {
            context.sendMessage(Message.raw("Échec : " + e.getMessage()));
            return;
        }
        context.sendMessage(Message.raw("MusicZones rechargé (" + plugin.getRepository().getZonesReadOnly().size() + " zone(s))."));
    }
}
