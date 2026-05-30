package fr.varyon.vrpg.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandSender;
import com.hypixel.hytale.server.core.command.system.ParseResult;
import com.hypixel.hytale.server.core.command.system.ParserContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.vrpg.VaryonRpgPlugin;
import fr.varyon.vrpg.ui.RpgMainUI;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public final class VrpgCommand extends AbstractCommandCollection {

    @SuppressWarnings("unused")
    public VrpgCommand(@Nonnull VaryonRpgPlugin plugin) {
        super("vrpg", "Varyon RPG - Classes");
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
                ((com.hypixel.hytale.server.core.universe.world.storage.EntityStore) store.getExternalData())
                    .getWorld().execute(() -> {
                        Player player = store.getComponent(ref, Player.getComponentType());
                        if (player != null) {
                            player.getPageManager().openCustomPage(ref, store,
                                new RpgMainUI(playerRef, "classes"));
                        }
                    });
            }
            return CompletableFuture.completedFuture(null);
        }
        return super.acceptCall(sender, parserContext, parseResult);
    }
}
