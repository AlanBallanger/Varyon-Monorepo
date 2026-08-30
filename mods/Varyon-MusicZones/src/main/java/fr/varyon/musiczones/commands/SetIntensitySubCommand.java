package fr.varyon.musiczones.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgumentType;
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

public final class SetIntensitySubCommand extends MusicZoneAdminCommandBase {

    private final RequiredArg<Float> percentArg =
            withRequiredArg(
                    "pourcentage",
                    "Intensité 0–100 (100 = nominal, 50 ≈ -6 dB, 0 = muet)",
                    (ArgumentType<Float>) ArgTypes.FLOAT);
    private final OptionalArg<String> idArg =
            withOptionalArg("id", "Zone ciblée (par défaut : la zone à ta position)", ArgTypes.STRING);

    public SetIntensitySubCommand() {
        super("intensity", "Régler l'intensité (volume) de la musique d'une zone");
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
        float percent = percentArg.get(context);
        if (Float.isNaN(percent) || percent < 0f || percent > 100f) {
            context.sendMessage(Message.raw("Pourcentage hors bornes : attendu 0–100."));
            return CompletableFuture.completedFuture(null);
        }
        String explicitId = idArg.provided(context) ? idArg.get(context).trim() : null;
        double db = MusicZone.percentToDb(percent);

        // Étape 1 (thread-monde) : lire l'entity store pour trouver la zone visée, la remplacer
        // dans le repo avec le nouveau volume. Rien d'IO/asset ici.
        String[] resolvedId = new String[1];
        CompletableFuture<Void> resolve = onWorld(context, () -> {
            Ref<EntityStore> ref = context.senderAsPlayerRef();
            Store<EntityStore> store = ref != null ? ref.getStore() : null;
            PlayerRef pr = store != null ? store.getComponent(ref, PlayerRef.getComponentType()) : null;
            World w = pr != null ? Universe.get().getWorld(pr.getWorldUuid()) : null;
            String worldName = w != null ? w.getName() : "";

            MusicZone target;
            if (explicitId != null && !explicitId.isEmpty()) {
                target = plugin.getRepository().zonesForWorld(worldName).stream()
                        .filter(z -> explicitId.equals(z.getId()))
                        .findFirst()
                        .orElse(null);
                if (target == null) {
                    context.sendMessage(Message.raw("Aucune zone « " + explicitId + " » dans " + worldName + "."));
                    return;
                }
            } else {
                Object tcObj = store != null ? store.getComponent(ref, TransformComponent.getComponentType()) : null;
                if (!(tcObj instanceof TransformComponent t)) {
                    context.sendMessage(Message.raw("Transform indisponible."));
                    return;
                }
                double x = t.getPosition().x;
                double y = t.getPosition().y;
                double z = t.getPosition().z;
                List<MusicZone> matching = new ArrayList<>();
                for (MusicZone zone : plugin.getRepository().zonesForWorld(worldName)) {
                    if (zone.contains(x, y, z)) {
                        matching.add(zone);
                    }
                }
                if (matching.isEmpty()) {
                    context.sendMessage(Message.raw("Aucune zone musicale à ta position. Précise un id : /mz intensity <pct> <id>"));
                    return;
                }
                matching.sort(Comparator.comparingDouble(MusicZone::volume));
                target = matching.get(0);
            }

            plugin.getRepository().addOrReplace(target.withVolumeDb(db));
            plugin.getRepository().save();
            resolvedId[0] = target.getId();
        });

        // Étape 2 (pool async, JAMAIS le thread-monde) : régénérer les JSON MusicContainer /
        // AmbienceFX et recharger ces stores. loadAssetsFromDirectory prend des locks d'AssetStore
        // que le file-watcher détient aussi ; l'appeler depuis le thread-tick du monde bloque le
        // serveur. forgetAll() ensuite fait réémettre UpdateForcedMusic au tick suivant.
        return resolve.thenRunAsync(() -> {
            if (resolvedId[0] == null) {
                return;
            }
            try {
                plugin.rebuildContainersOnly();
            } catch (Exception e) {
                context.sendMessage(Message.raw("Intensité enregistrée mais échec pack audio : " + e.getMessage()));
                return;
            }
            plugin.getApplySystem().forgetAll();
            context.sendMessage(Message.raw(String.format(
                    "Zone « %s » : intensité %.0f%% (%.1f dB). Réémission en cours pour les joueurs.",
                    resolvedId[0], (double) percent, db)));
        });
    }
}
