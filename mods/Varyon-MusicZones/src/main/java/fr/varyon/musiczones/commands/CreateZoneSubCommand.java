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
import java.util.concurrent.CompletableFuture;

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
    @Nonnull
    protected CompletableFuture<Void> executeAsync(@Nonnull CommandContext context) {
        if (!requireMusicZoneAdmin(context)) {
            return CompletableFuture.completedFuture(null);
        }
        if (!context.isPlayer()) {
            context.sendMessage(Message.raw("Joueur uniquement."));
            return CompletableFuture.completedFuture(null);
        }
        VaryonMusicZonesPlugin plugin = VaryonMusicZonesPlugin.getInstance();
        if (plugin == null) {
            context.sendMessage(Message.raw("Plugin non chargé."));
            return CompletableFuture.completedFuture(null);
        }
        String zoneId = idArg.get(context).trim();
        if (zoneId.isEmpty()) {
            context.sendMessage(Message.raw("id vide."));
            return CompletableFuture.completedFuture(null);
        }
        String musicName = musicArg.get(context).trim();
        if (musicName.isEmpty()) {
            context.sendMessage(Message.raw("Nom de musique vide."));
            return CompletableFuture.completedFuture(null);
        }
        Path musicPath = plugin.resolveMusicFile(musicName);
        if (musicPath == null || !Files.isRegularFile(musicPath, LinkOption.NOFOLLOW_LINKS)) {
            context.sendMessage(Message.raw("Fichier introuvable dans music/ : " + musicName + " (extensions .ogg)"));
            return CompletableFuture.completedFuture(null);
        }
        String storedFileName = musicPath.getFileName().toString();

        // Étape 1 (thread-monde) : lire l'entity store / la box en attente, écrire la zone dans
        // le repo. Aucun IO d'asset ici.
        String[] createdAmbId = new String[1];
        CompletableFuture<Void> resolve = onWorld(context, () -> {
            Ref<EntityStore> ref = context.senderAsPlayerRef();
            Store<EntityStore> store = ref != null ? ref.getStore() : null;
            PlayerRef pr = store != null ? store.getComponent(ref, PlayerRef.getComponentType()) : null;
            World w = pr != null ? Universe.get().getWorld(pr.getWorldUuid()) : null;
            String worldName = w != null ? w.getName() : "";
            java.util.UUID pu = pr != null ? pr.getUuid() : null;
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
            MusicZone zone = new MusicZone(zoneId, worldName, minX, minY, minZ, maxX, maxY, maxZ, storedFileName);
            plugin.getRepository().addOrReplace(zone);
            plugin.getRepository().save();
            createdAmbId[0] = zone.ambienceAssetId();
        });

        // Étape 2 (pool async, JAMAIS le thread-monde) : rebuild du pack (copie des .ogg,
        // loadCommonAssets, rechargement des stores). loadCommonAssets/loadAssetsFromDirectory
        // prennent des locks d'AssetStore partagés avec le file-watcher : les appeler depuis le
        // thread-tick du monde fige le serveur.
        return resolve.thenRunAsync(() -> {
            if (createdAmbId[0] == null) {
                return;
            }
            try {
                plugin.rebuildAssetPack();
            } catch (Exception e) {
                context.sendMessage(Message.raw("Zone enregistrée mais échec pack audio : " + e.getMessage()));
                return;
            }
            context.sendMessage(Message.raw(
                    "Zone « " + zoneId + " » créée. AmbienceFX : " + createdAmbId[0] + " (reconnexion client si besoin)"));
        });
    }
}
