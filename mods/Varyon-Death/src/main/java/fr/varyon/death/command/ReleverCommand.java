package fr.varyon.death.command;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import fr.varyon.death.etat.EtatATerre;
import fr.varyon.death.etat.GestionnaireATerre;
import fr.varyon.death.systeme.SystemeReleve;

/**
 * {@code /relever <joueur>} releve immediatement un joueur a terre, sans passer par un
 * soigneur ni consommer de potion. Reservee aux administrateurs, pour debloquer une
 * situation coincee (allies absents, bug de progression) sans attendre le saignement.
 */
public final class ReleverCommand extends CommandBase {

    private final RequiredArg<PlayerRef> joueurArg =
            this.withRequiredArg("joueur", "Joueur a terre a relever.", ArgTypes.PLAYER_REF);

    public ReleverCommand() {
        super("relever", "Releve immediatement un joueur a terre.");
        this.requirePermission("varyon.admin");
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        PlayerRef cibleRef = joueurArg.get(context);
        if (cibleRef == null || !cibleRef.isValid()) {
            context.sendMessage(Message.raw("Ce joueur n'est pas dans le monde."));
            return;
        }
        GestionnaireATerre gestionnaire = GestionnaireATerre.get();
        if (gestionnaire == null) {
            context.sendMessage(Message.raw("Le systeme d'etat a terre n'est pas disponible."));
            return;
        }
        EtatATerre etat = gestionnaire.getEtat(cibleRef.getUuid());
        if (etat == null) {
            context.sendMessage(Message.raw(nomDe(cibleRef) + " n'est pas a terre."));
            return;
        }

        // Le relevement touche au Store : il doit imperativement s'executer sur le thread du
        // monde, alors que executeSync tourne sur un thread du pool de commandes.
        World world = resolveWorld(cibleRef);
        if (world == null) {
            context.sendMessage(Message.raw("Impossible de relever ce joueur pour le moment."));
            return;
        }
        String nomCible = nomDe(cibleRef);
        world.execute(() -> {
            Ref<EntityStore> ref = cibleRef.getReference();
            if (ref == null || !ref.isValid()) {
                return;
            }
            Store<EntityStore> store = ref.getStore();
            if (store == null) {
                return;
            }
            SystemeReleve.releverImmediatement(gestionnaire, etat, cibleRef, store);
        });
        context.sendMessage(Message.raw(nomCible + " a ete releve."));
    }

    @Nullable
    private static World resolveWorld(@Nonnull PlayerRef playerRef) {
        Ref<EntityStore> ref = playerRef.getReference();
        Store<EntityStore> store = ref != null && ref.isValid() ? ref.getStore() : null;
        if (store != null && store.getExternalData() != null) {
            return store.getExternalData().getWorld();
        }
        return null;
    }

    @Nonnull
    private static String nomDe(@Nonnull PlayerRef playerRef) {
        try {
            String nom = playerRef.getUsername();
            return nom == null ? "?" : nom;
        } catch (RuntimeException ignore) {
            return "?";
        }
    }
}
