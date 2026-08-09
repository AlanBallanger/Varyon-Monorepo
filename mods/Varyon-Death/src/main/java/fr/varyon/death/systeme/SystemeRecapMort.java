package fr.varyon.death.systeme;

import java.util.UUID;
import java.util.logging.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import fr.varyon.death.combat.SuiviCombat;
import fr.varyon.death.combat.BanqueRecaps;
import fr.varyon.death.config.PreferencesRecap;

/**
 * Capture le recapitulatif final a la mort reelle.
 *
 * <p>La page est desormais ouverte des la mise a terre par {@code SystemeInterceptionMort},
 * avant meme que ce {@link DeathComponent} n'apparaisse. Ce systeme se contente de figer le
 * recapitulatif definitif (apres saignement) dans {@link BanqueRecaps}, pour que {@code /mort}
 * puisse toujours rouvrir le dernier recap une fois la mort consommee.
 */
public final class SystemeRecapMort extends DeathSystems.OnDeathSystem {

    private static final HytaleLogger LOGGER =
            HytaleLogger.getLogger().getSubLogger("VaryonDeathRecap");

    @Override
    @Nonnull
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }

    @Override
    public void onComponentAdded(@Nonnull Ref<EntityStore> ref,
                                 @Nonnull DeathComponent component,
                                 @Nonnull Store<EntityStore> store,
                                 @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        UUID uuid = resolveUuid(store, commandBuffer, ref);
        LOGGER.at(Level.INFO).log("DeathComponent ajoute (joueur=%s).", uuid);
        if (uuid == null) {
            return;
        }
        if (!PreferencesRecap.estActif(uuid)) {
            SuiviCombat.get().clear(uuid);
            return;
        }
        try {
            SuiviCombat.Snapshot snapshot = SuiviCombat.get().consommerALaMort(uuid);
            if (snapshot == null) {
                LOGGER.at(Level.INFO).log(
                        "Mort de %s : aucune session de combat (expiree ou inexistante), pas de recap.", uuid);
                return;
            }
            if (snapshot.topThreats().isEmpty()) {
                LOGGER.at(Level.INFO).log("Mort de %s : session sans menace identifiee.", uuid);
                return;
            }
            LOGGER.at(Level.INFO).log("Mort de %s : recap capture (%d menaces, %.0f degats).",
                    uuid, snapshot.topThreats().size(), snapshot.totalDamageTaken());
            BanqueRecaps.get().put(uuid, snapshot);
        } catch (RuntimeException e) {
            LOGGER.at(Level.WARNING).log("Echec capture du recapitulatif pour %s: %s", uuid, e.getMessage());
        }
    }

    @Nullable
    private static UUID resolveUuid(@Nullable Store<EntityStore> store,
                                    @Nullable CommandBuffer<EntityStore> commandBuffer,
                                    @Nonnull Ref<EntityStore> ref) {
        PlayerRef playerRef = resolvePlayerRef(store, commandBuffer, ref);
        return playerRef != null && playerRef.isValid() ? playerRef.getUuid() : null;
    }

    @Nullable
    private static PlayerRef resolvePlayerRef(@Nullable Store<EntityStore> store,
                                              @Nullable CommandBuffer<EntityStore> commandBuffer,
                                              @Nonnull Ref<EntityStore> ref) {
        try {
            if (commandBuffer != null) {
                PlayerRef fromBuffer = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
                if (fromBuffer != null) {
                    return fromBuffer;
                }
            }
            return store != null ? store.getComponent(ref, PlayerRef.getComponentType()) : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
