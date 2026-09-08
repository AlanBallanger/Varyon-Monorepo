package fr.varyon.musiczones.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.musiczones.VaryonMusicZonesPlugin;
import fr.varyon.musiczones.gui.MusicZoneListPage;

import javax.annotation.Nonnull;

public final class OpenUiSubCommand extends AbstractPlayerCommand {

    public OpenUiSubCommand() {
        super("ui", "Ouvrir l'interface de gestion des zones musicales");
        requireNoPermission();
    }

    @Override
    protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef, @Nonnull World world) {
        if (!(playerRef.hasPermission("*") || playerRef.hasPermission("varyon.musiczones.admin"))) {
            context.sendMessage(Message.raw("Op ou permission varyon.musiczones.admin requise.").color("#FF5555"));
            return;
        }
        VaryonMusicZonesPlugin plugin = VaryonMusicZonesPlugin.getInstance();
        if (plugin == null) {
            context.sendMessage(Message.raw("Plugin non chargé.").color("#FF5555"));
            return;
        }
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        player.getPageManager().openCustomPage(ref, store, new MusicZoneListPage(playerRef, plugin));
    }
}
