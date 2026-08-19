package fr.varyon.stacktiers.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.ParseResult;
import com.hypixel.hytale.server.core.command.system.ParserContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import fr.varyon.stacktiers.research.ResearchManager;
import fr.varyon.stacktiers.ui.ResearchTreeUI;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

/** Ouvre l'arbre de recherche pour le joueur. Ouvert à tous, aucune permission requise. */
public final class VaryonResearchCommand extends AbstractCommandCollection {
    private final ResearchManager researchManager;

    public VaryonResearchCommand(@Nonnull ResearchManager researchManager) {
        super("varyonresearch", "Ouvrir l'arbre de recherche");
        this.researchManager = researchManager;
    }

    @Override
    @Nullable
    public CompletableFuture<Void> acceptCall(@Nonnull CommandSender sender,
                                               @Nonnull ParserContext parserContext,
                                               @Nonnull ParseResult parseResult) {
        if (sender instanceof PlayerRef playerRef) {
            Ref<EntityStore> ref = playerRef.getReference();
            if (ref != null && ref.isValid()) {
                Store<EntityStore> store = ref.getStore();
                ((EntityStore) store.getExternalData()).getWorld().execute(() -> {
                    Player player = store.getComponent(ref, Player.getComponentType());
                    if (player != null) {
                        player.getPageManager().openCustomPage(ref, store, new ResearchTreeUI(playerRef, researchManager));
                    }
                });
            }
            return CompletableFuture.completedFuture(null);
        }
        return super.acceptCall(sender, parserContext, parseResult);
    }
}
