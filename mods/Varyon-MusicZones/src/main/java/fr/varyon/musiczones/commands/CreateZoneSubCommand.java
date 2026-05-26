package fr.varyon.musiczones.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.musiczones.MusicZone;
import fr.varyon.musiczones.VaryonMusicZonesPlugin;

import javax.annotation.Nonnull;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

public final class CreateZoneSubCommand extends MusicZoneAdminCommandBase {

    private final RequiredArg<String> idArg =
            withRequiredArg("id", "Identifiant unique de la zone dans ce monde", (ArgumentType<String>) ArgTypes.STRING);
    private final RequiredArg<String> musicArg =
            withRequiredArg(
                    "musique",
                    "Nom du fichier .ogg dans Varyon-MusicZones/music/",
                    (ArgumentType<String>) ArgTypes.STRING);

    public CreateZoneSubCommand() {
        super("create", "Créer la zone avec pos1/pos2 (même monde) et un fichier musique");
    }

    @Override
    @SuppressWarnings("removal")
    protected void executeSync(@Nonnull CommandContext context) {
        if (!requireMusicZoneAdmin(context)) {
            return;
        }
        if (!context.isPlayer()) {
            context.sendMessage(Message.raw("Joueur uniquement."));
            return;
        }
        Ref<EntityStore> _ref = context.senderAsPlayerRef();
        Store<EntityStore> _store = _ref != null ? _ref.getStore() : null;
        PlayerRef _pr = _store != null ? _store.getComponent(_ref, PlayerRef.getComponentType()) : null;
        VaryonMusicZonesPlugin plugin = VaryonMusicZonesPlugin.getInstance();
        if (plugin == null) {
            context.sendMessage(Message.raw("Plugin non chargé."));
            return;
        }
        String zoneId = idArg.get(context).trim();
        if (zoneId.isEmpty()) {
            context.sendMessage(Message.raw("id vide."));
            return;
        }
        String musicName = musicArg.get(context).trim();
        if (musicName.isEmpty()) {
            context.sendMessage(Message.raw("Nom de musique vide."));
            return;
        }
        Path musicPath = plugin.resolveMusicFile(musicName);
        if (musicPath == null || !Files.isRegularFile(musicPath, LinkOption.NOFOLLOW_LINKS)) {
            context.sendMessage(Message.raw("Fichier introuvable dans music/ : " + musicName + " (extensions .ogg)"));
            return;
        }
        World _w = _pr != null ? Universe.get().getWorld(_pr.getWorldUuid()) : null;
        String worldName = _w != null ? _w.getName() : "";
        java.util.UUID pu = _pr != null ? _pr.getUuid() : null;
        if (pu == null) {
            context.sendMessage(Message.raw("UUID indisponible."));
            return;
        }
        VaryonMusicZonesPlugin.PendingBox box = plugin.takePendingBox(pu, worldName);
        if (box == null) {
            context.sendMessage(Message.raw("Définis d'abord pos1 et pos2 dans ce monde (" + worldName + ")."));
            return;
        }
        double minX = Math.min(box.x1(), box.x2());
        double maxX = Math.max(box.x1(), box.x2());
        double minY = Math.min(box.y1(), box.y2());
        double maxY = Math.max(box.y1(), box.y2());
        double minZ = Math.min(box.z1(), box.z2());
        double maxZ = Math.max(box.z1(), box.z2());
        if (minX == maxX || minY == maxY || minZ == maxZ) {
            context.sendMessage(Message.raw("Volume nul : éloigne pos1 et pos2."));
            return;
        }
        String storedFileName = musicPath.getFileName().toString();
        MusicZone zone = new MusicZone(zoneId, worldName, minX, minY, minZ, maxX, maxY, maxZ, storedFileName);
        plugin.getRepository().addOrReplace(zone);
        plugin.getRepository().save();
        try {
            plugin.rebuildAssetPack();
        } catch (Exception e) {
            context.sendMessage(Message.raw("Zone enregistrée mais échec pack audio : " + e.getMessage()));
            return;
        }
        context.sendMessage(Message.raw(
                "Zone « " + zoneId + " » créée. AmbienceFX : " + zone.ambienceAssetId() + " (reconnexion client si besoin)"));
    }
}
