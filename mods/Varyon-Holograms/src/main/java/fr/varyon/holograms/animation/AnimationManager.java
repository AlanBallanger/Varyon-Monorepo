package fr.varyon.holograms.animation;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.holograms.VaryonHologramsPlugin;
import org.joml.Vector3d;
import org.joml.Vector3f;
import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class AnimationManager {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final float TICK_RATE = 20f;
    private static final long TICK_MS = (long)(1000f / TICK_RATE);

    private final VaryonHologramsPlugin plugin;
    private final Map<UUID, AnimationState> states = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> task;

    public AnimationManager(@Nonnull VaryonHologramsPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "VaryonHolograms-AnimTick");
            t.setDaemon(true);
            return t;
        });
        task = scheduler.scheduleAtFixedRate(this::tick, TICK_MS, TICK_MS, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        if (task != null) task.cancel(false);
        if (scheduler != null) scheduler.shutdownNow();
        states.clear();
    }

    public void registerAnimation(@Nonnull UUID entityId, @Nonnull AnimationData animation,
                                   @Nonnull Vector3d basePosition, @Nonnull Vector3f baseRotation, float baseScale) {
        states.put(entityId, new AnimationState(animation, basePosition, baseRotation, baseScale));
    }

    public void unregisterAnimation(@Nonnull UUID entityId) {
        states.remove(entityId);
    }

    public boolean hasAnimation(@Nonnull UUID entityId) {
        return states.containsKey(entityId);
    }

    private void tick() {
        if (states.isEmpty()) return;
        float delta = TICK_MS / 1000f;
        Universe universe = Universe.get();
        if (universe == null) return;

        for (Map.Entry<UUID, AnimationState> entry : states.entrySet()) {
            UUID entityId = entry.getKey();
            AnimationState state = entry.getValue();
            state.tick(delta);

            for (World world : universe.getWorlds().values()) {
                world.execute(() -> applyAnimation(world, entityId, state));
            }
        }
    }

    private void applyAnimation(@Nonnull World world, @Nonnull UUID entityId, @Nonnull AnimationState state) {
        try {
            Store<EntityStore> store = world.getEntityStore().getStore();
            Ref<EntityStore> ref = ((EntityStore) store.getExternalData()).getRefFromUUID(entityId);
            if (ref == null || !ref.isValid()) return;

            TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
            if (transform != null) {
                Vector3d pos = state.getCurrentPosition();
                transform.getPosition().set(pos.x, pos.y, pos.z);
                Vector3f rot = state.getCurrentRotation();
                transform.getRotation().set(rot.x, rot.y, rot.z);
                transform.markChunkDirty(store);
            }

            EntityScaleComponent scale = store.getComponent(ref, EntityScaleComponent.getComponentType());
            if (scale != null) {
                scale.setScale(state.getCurrentScale());
            }
        } catch (Exception e) {
            LOGGER.at(Level.FINE).log("[Varyon-Holograms] Anim tick error entity=%s: %s", entityId, e.getMessage());
        }
    }
}
