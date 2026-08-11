package fr.varyon.shop.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.shop.merchant.buyback.BuybackSessionManager;
import fr.varyon.shop.util.EntityApiCompat;

/** "/racheteur" opens the general buyback deposit window. Selling happens via its "Vendre tout" button. */
public final class BuybackCommand extends CommandBase {
    private final BuybackSessionManager sessionManager;

    public BuybackCommand(BuybackSessionManager sessionManager) {
        super("racheteur", "Ouvre le racheteur general");
        this.sessionManager = sessionManager;
    }

    @Override
    protected void executeSync(CommandContext ctx) {
        if (!ctx.isPlayer()) {
            ctx.sendMessage(Message.raw("Cette commande est reservee aux joueurs."));
            return;
        }
        PlayerRef playerRef = EntityApiCompat.senderAsPlayerRef(ctx);
        if (playerRef == null) {
            ctx.sendMessage(Message.raw("Impossible de resoudre le joueur."));
            return;
        }

        Runnable task = () -> {
            if (!sessionManager.open(playerRef)) {
                playerRef.sendMessage(Message.raw("Racheteur General: impossible d'ouvrir la fenetre."));
            }
        };

        if (!runOnPlayerWorld(playerRef, task)) {
            playerRef.sendMessage(Message.raw("Racheteur General: impossible de traiter la demande."));
        }
    }

    private boolean runOnPlayerWorld(PlayerRef playerRef, Runnable task) {
        if (playerRef == null || task == null) {
            return false;
        }
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || ref.getStore() == null) {
            return false;
        }
        EntityStore entityStore = ref.getStore().getExternalData();
        World world = entityStore == null ? null : entityStore.getWorld();
        if (world == null) {
            return false;
        }
        world.execute(task);
        return true;
    }
}
