package fr.varyon.musiczones.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import fr.varyon.musiczones.VaryonMusicZonesPlugin;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

public final class ReloadMusicZonesSubCommand extends MusicZoneAdminCommandBase {

    public ReloadMusicZonesSubCommand() {
        super("reload", "Recharger zones.json et régénérer le pack audio");
    }

    @Override
    @Nonnull
    protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext context) {
        if (!requireMusicZoneAdmin(context)) {
            return CompletableFuture.completedFuture(null);
        }
        VaryonMusicZonesPlugin plugin = VaryonMusicZonesPlugin.getInstance();
        if (plugin == null) {
            context.sendMessage(Message.raw("Plugin non chargé."));
            return CompletableFuture.completedFuture(null);
        }
        plugin.getRepository().load();
        try {
            plugin.rebuildAssetPack();
            plugin.getApplySystem().beginPostRebuildGrace();
        } catch (Exception e) {
            context.sendMessage(Message.raw("Échec : " + e.getMessage()));
            return CompletableFuture.completedFuture(null);
        }
        context.sendMessage(Message.raw("MusicZones rechargé (" + plugin.getRepository().getZonesReadOnly().size() + " zone(s))."));
        return CompletableFuture.completedFuture(null);
    }
}
