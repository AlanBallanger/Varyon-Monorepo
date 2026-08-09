package fr.varyon.readablebooks.interaction;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.WaitForDataFrom;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.readablebooks.BookManager;
import fr.varyon.readablebooks.commands.BookCommand;
import fr.varyon.readablebooks.data.BookEntry;
import fr.varyon.readablebooks.gui.BookEditorPage;
import fr.varyon.readablebooks.gui.BookReaderPage;

import javax.annotation.Nonnull;

public class ReadableBookInteraction extends SimpleInstantInteraction {

    @Nonnull
    public static final BuilderCodec<ReadableBookInteraction> CODEC;

    static {
        CODEC = BuilderCodec
            .builder(ReadableBookInteraction.class, ReadableBookInteraction::new,
                SimpleInstantInteraction.CODEC)
            .build();
    }

    @Override
    public boolean needsRemoteSync() {
        return false;
    }

    @Nonnull
    @Override
    public WaitForDataFrom getWaitForDataFrom() {
        return WaitForDataFrom.Server;
    }

    @Override
    protected void firstRun(
        @Nonnull InteractionType type,
        @Nonnull InteractionContext context,
        @Nonnull CooldownHandler cooldownHandler
    ) {
        Ref<EntityStore> playerRef = context.getEntity();
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        if (commandBuffer == null || playerRef == null || !playerRef.isValid()) {
            return;
        }

        BlockPosition targetBlock = context.getTargetBlock();
        if (targetBlock == null) {
            return;
        }

        final int bx = targetBlock.x;
        final int by = targetBlock.y;
        final int bz = targetBlock.z;

        commandBuffer.run(store -> {
            Player player = store.getComponent(playerRef, Player.getComponentType());
            PlayerRef playerRefComp = store.getComponent(playerRef, PlayerRef.getComponentType());
            if (player == null || playerRefComp == null || player.getWorld() == null) {
                return;
            }

            String dimension = player.getWorld().getName();
            if (dimension == null || dimension.isBlank()) {
                return;
            }

            BookManager manager = BookManager.getInstance();
            BookEntry entry = manager.getBook(dimension, bx, by, bz);

            boolean mayEdit = manager.isInEditMode(playerRefComp.getUuid())
                && (playerRefComp.hasPermission("*")
                    || playerRefComp.hasPermission(BookCommand.PERMISSION));

            if (mayEdit) {
                String title = entry != null ? entry.title() : "";
                String text = entry != null ? entry.text() : "";
                player.getPageManager().openCustomPage(playerRef, store,
                    new BookEditorPage(playerRefComp, dimension, bx, by, bz, title, text));
                return;
            }

            String title = entry != null ? entry.title() : "";
            String text = entry != null ? entry.text() : "";
            player.getPageManager().openCustomPage(playerRef, store,
                new BookReaderPage(playerRefComp, title, text));
        });
    }
}
