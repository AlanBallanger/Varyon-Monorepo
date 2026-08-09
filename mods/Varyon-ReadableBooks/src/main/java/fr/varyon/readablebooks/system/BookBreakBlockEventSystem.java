package fr.varyon.readablebooks.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.readablebooks.BookManager;

import java.util.Set;
import javax.annotation.Nonnull;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public final class BookBreakBlockEventSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {
    /** Every readable variant shares this id prefix, so one check covers them all. */
    private static final String READABLE_BOOK_BLOCK_PREFIX = "Varyon_Readable_";

    public BookBreakBlockEventSystem() {
        super(BreakBlockEvent.class);
    }

    public void handle(
        int index,
        @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
        @Nonnull Store<EntityStore> store,
        @Nonnull CommandBuffer<EntityStore> commandBuffer,
        @Nonnull BreakBlockEvent event
    ) {
        String blockId = event.getBlockType().getId();
        if (blockId == null || !blockId.contains(READABLE_BOOK_BLOCK_PREFIX)) {
            return;
        }

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null || player.getWorld() == null) {
            return;
        }

        String dimension = player.getWorld().getName();
        if (dimension == null || dimension.isBlank()) {
            return;
        }

        BookManager.getInstance().removeBook(
            dimension,
            event.getTargetBlock().x,
            event.getTargetBlock().y,
            event.getTargetBlock().z
        );
    }

    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }

    @NonNullDecl
    public Set<Dependency<EntityStore>> getDependencies() {
        return Set.of(RootDependency.first());
    }
}
