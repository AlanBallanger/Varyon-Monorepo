package fr.varyon.readablebooks.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import fr.varyon.readablebooks.BookManager;
import fr.varyon.readablebooks.data.BookEntry;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BookParticleTickSystem extends EntityTickingSystem<EntityStore> {
    private static final String PARTICLE_ID = "Chest_Sparks";
    private static final int CHECK_INTERVAL = 60;
    private static final double PARTICLE_RANGE_SQ = 24.0 * 24.0;

    private final ComponentType<EntityStore, PlayerRef> playerRefType = PlayerRef.getComponentType();
    private final Map<UUID, Integer> tickCounters = new ConcurrentHashMap<>();

    @Override
    public void tick(
        float deltaTime,
        int index,
        ArchetypeChunk<EntityStore> chunk,
        Store<EntityStore> store,
        CommandBuffer<EntityStore> commandBuffer
    ) {
        PlayerRef playerRef = chunk.getComponent(index, playerRefType);
        if (playerRef == null) return;

        UUID uuid = playerRef.getUuid();
        int tc = tickCounters.merge(uuid, 1, Integer::sum);
        if (tc % CHECK_INTERVAL != 0) return;

        try {
            Ref<EntityStore> ref = chunk.getReferenceTo(index);
            TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
            if (transform == null) return;

            World world = store.getExternalData().getWorld();
            if (world == null || world.getName() == null) return;

            List<BookEntry> books = BookManager.getInstance().getBooksInDimension(world.getName());
            if (books.isEmpty()) return;

            Vector3d playerPos = transform.getPosition();
            for (BookEntry book : books) {
                double gx = book.x() + 0.5;
                double gy = book.y() + 0.5;
                double gz = book.z() + 0.5;

                double dx = playerPos.x - gx;
                double dy = playerPos.y - gy;
                double dz = playerPos.z - gz;
                if (dx * dx + dy * dy + dz * dz > PARTICLE_RANGE_SQ) continue;

                ParticleUtil.spawnParticleEffect(PARTICLE_ID, new Vector3d(gx, gy, gz), store);
            }
        } catch (Exception ignored) {}
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return playerRefType;
    }
}
