package fr.varyon.death.command;

import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import fr.varyon.death.combat.SuiviCombat;
import fr.varyon.death.combat.BanqueRecaps;
import fr.varyon.death.config.PreferencesRecap;
import fr.varyon.death.ui.DeathRecapPage;

/**
 * {@code /mort} rouvre le dernier recapitulatif, {@code /mort logs} bascule l'affichage.
 * Utile lorsque la page a ete fermee par megarde juste apres la reapparition.
 */
public final class DeathRecapCommand extends CommandBase {

    public DeathRecapCommand() {
        super("mort", "Rouvre le recapitulatif de votre derniere mort.");
        this.addSubCommand(new ToggleSubCommand());
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        PlayerRef playerRef = resolveSender(context);
        if (playerRef == null) {
            context.sendMessage(Message.raw("Cette commande doit etre utilisee par un joueur."));
            return;
        }
        UUID uuid = playerRef.getUuid();
        if (uuid == null) {
            return;
        }
        if (!PreferencesRecap.estActif(uuid)) {
            context.sendMessage(Message.raw("Vos recapitulatifs de mort sont desactives. Utilisez /mort logs pour les reactiver."));
            return;
        }
        SuiviCombat.Snapshot snapshot = BanqueRecaps.get().peekLast(uuid);
        if (snapshot == null) {
            context.sendMessage(Message.raw("Aucun recapitulatif de mort recent."));
            return;
        }
        // L'ouverture touche au Store : elle doit imperativement s'executer sur le thread du
        // monde, alors que cette methode tourne sur un thread du pool de commandes.
        World world = resolveWorld(playerRef);
        if (world == null) {
            context.sendMessage(Message.raw("Impossible d'ouvrir le recapitulatif pour le moment."));
            return;
        }
        // Si la page est deja ouverte mais repliee (bouton seul), /mort la deplie plutot que
        // de se plaindre qu'elle est "deja ouverte" : c'est une demande explicite de la voir.
        world.execute(() -> DeathRecapPage.openExpandedFor(playerRef, snapshot));
    }

    @Nullable
    private static World resolveWorld(@Nonnull PlayerRef playerRef) {
        Ref<EntityStore> ref = playerRef.getReference();
        Store<EntityStore> store = ref != null && ref.isValid() ? ref.getStore() : null;
        if (store != null && store.getExternalData() != null) {
            World world = store.getExternalData().getWorld();
            if (world != null) {
                return world;
            }
        }
        Universe universe = Universe.get();
        return universe != null ? universe.getDefaultWorld() : null;
    }

    /**
     * Resout l'emetteur SANS toucher au {@code Store} : {@code executeSync} s'execute sur un
     * thread du pool de commandes, alors que {@code Store.getComponent} exige le thread du
     * monde et leve {@code IllegalStateException} ailleurs. {@link Universe#getPlayer} est
     * sur, lui, et donne directement le {@link PlayerRef}.
     */
    @Nullable
    static PlayerRef resolveSender(@Nonnull CommandContext context) {
        if (!context.isPlayer() || context.sender() == null) {
            return null;
        }
        UUID uuid = context.sender().getUuid();
        if (uuid == null) {
            return null;
        }
        Universe universe = Universe.get();
        return universe != null ? universe.getPlayer(uuid) : null;
    }

    private static final class ToggleSubCommand extends CommandBase {

        private ToggleSubCommand() {
            super("logs", "Active ou desactive vos recapitulatifs de mort.");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            PlayerRef playerRef = resolveSender(context);
            if (playerRef == null) {
                context.sendMessage(Message.raw("Cette commande doit etre utilisee par un joueur."));
                return;
            }
            UUID uuid = playerRef.getUuid();
            if (uuid == null) {
                return;
            }
            boolean enabled = PreferencesRecap.toggle(uuid);
            if (enabled) {
                context.sendMessage(Message.raw("Recapitulatifs de mort actives."));
            } else {
                SuiviCombat.get().clear(uuid);
                BanqueRecaps.get().dropAll(uuid);
                context.sendMessage(Message.raw("Recapitulatifs de mort desactives."));
            }
        }
    }
}
