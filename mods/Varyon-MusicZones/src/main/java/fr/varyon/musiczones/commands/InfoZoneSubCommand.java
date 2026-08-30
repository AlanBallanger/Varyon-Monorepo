package fr.varyon.musiczones.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.musiczones.MusicZone;
import fr.varyon.musiczones.VaryonMusicZonesPlugin;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class InfoZoneSubCommand extends MusicZoneAdminCommandBase {

    public InfoZoneSubCommand() {
        super("info", "Afficher les infos de la zone musicale à ta position");
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
        return onWorld(context, () -> {
            Ref<EntityStore> ref = context.senderAsPlayerRef();
            Store<EntityStore> store = ref != null ? ref.getStore() : null;
            PlayerRef pr = store != null ? store.getComponent(ref, PlayerRef.getComponentType()) : null;
            Object tcObj = store != null ? store.getComponent(ref, TransformComponent.getComponentType()) : null;
            if (!(tcObj instanceof TransformComponent t)) {
                context.sendMessage(Message.raw("Transform indisponible."));
                return;
            }
            World w = pr != null ? Universe.get().getWorld(pr.getWorldUuid()) : null;
            String worldName = w != null ? w.getName() : "";
            double x = t.getPosition().x;
            double y = t.getPosition().y;
            double z = t.getPosition().z;

            List<MusicZone> zones = plugin.getRepository().zonesForWorld(worldName);
            List<MusicZone> matching = new ArrayList<>();
            for (MusicZone zone : zones) {
                if (zone.contains(x, y, z)) {
                    matching.add(zone);
                }
            }

            if (matching.isEmpty()) {
                context.sendMessage(Message.raw("Aucune zone musicale à ta position (" + fmt(x) + ", " + fmt(y) + ", " + fmt(z) + ")."));
                return;
            }

            matching.sort(Comparator.comparingDouble(MusicZone::volume));
            MusicZone active = matching.get(0);

            double sizeX = active.getMaxX() - active.getMinX();
            double sizeY = active.getMaxY() - active.getMinY();
            double sizeZ = active.getMaxZ() - active.getMinZ();

            context.sendMessage(Message.raw("=== Zone musicale active ==="));
            context.sendMessage(Message.raw("ID       : " + active.getId()));
            context.sendMessage(Message.raw("Musique  : " + active.getMusicFileName()));
            context.sendMessage(Message.raw("Asset    : " + active.ambienceAssetId()));
            context.sendMessage(Message.raw(String.format(
                    "Intensité: %.0f%% (%.1f dB)",
                    MusicZone.dbToPercent(active.getVolumeDb()), active.getVolumeDb())));
            context.sendMessage(Message.raw("Taille   : " + fmt(sizeX) + " x " + fmt(sizeY) + " x " + fmt(sizeZ)));
            context.sendMessage(Message.raw("Coins    : [" + fmt(active.getMinX()) + ", " + fmt(active.getMinY()) + ", " + fmt(active.getMinZ()) + "] → [" + fmt(active.getMaxX()) + ", " + fmt(active.getMaxY()) + ", " + fmt(active.getMaxZ()) + "]"));
            if (matching.size() > 1) {
                context.sendMessage(Message.raw("Chevauchements : " + (matching.size() - 1) + " autre(s) zone(s) à cette position."));
            }
        });
    }

    private static String fmt(double v) {
        return String.format("%.1f", v);
    }
}
