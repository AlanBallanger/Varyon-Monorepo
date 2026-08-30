package fr.varyon.holograms.carousel;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.holograms.VaryonHologramsPlugin;
import fr.varyon.holograms.hologram.CarouselTransition;
import fr.varyon.holograms.hologram.Hologram;
import fr.varyon.holograms.hologram.HologramLayout;
import fr.varyon.holograms.hologram.HologramManager;
import org.joml.Vector3d;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class CarouselManager {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final long TICK_MS = 50;

    private final VaryonHologramsPlugin plugin;
    private final Map<UUID, CarouselRuntime> runtimes = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> task;

    public CarouselManager(@Nonnull VaryonHologramsPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "VaryonHolograms-Carousel");
            t.setDaemon(true);
            return t;
        });
        task = scheduler.scheduleAtFixedRate(this::tick, TICK_MS, TICK_MS, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        if (task != null) task.cancel(false);
        if (scheduler != null) scheduler.shutdownNow();
        runtimes.clear();
    }

    public void register(@Nonnull Hologram hologram) {
        if (!hologram.isCarouselActive()) {
            unregister(hologram.getId());
            return;
        }
        CarouselRuntime runtime = new CarouselRuntime();
        runtime.displayTimer = hologram.getCarouselIntervalSeconds();
        runtimes.put(hologram.getId(), runtime);
    }

    public void unregister(@Nonnull UUID hologramId) {
        runtimes.remove(hologramId);
    }

    private void tick() {
        if (runtimes.isEmpty()) return;
        HologramManager manager = plugin.getHologramManager();
        float delta = TICK_MS / 1000f;

        for (Map.Entry<UUID, CarouselRuntime> entry : runtimes.entrySet()) {
            Hologram hologram = manager.getHologram(entry.getKey());
            if (hologram == null || !hologram.isCarouselActive()) {
                runtimes.remove(entry.getKey());
                continue;
            }
            World world = findWorld(hologram.getWorldId());
            if (world == null) continue;

            CarouselRuntime runtime = entry.getValue();
            try {
                if (!runtime.transitioning) {
                    runtime.displayTimer -= delta;
                    if (runtime.displayTimer <= 0f) {
                        world.execute(() -> beginTransition(hologram, runtime, manager));
                    }
                } else {
                    runtime.transitionTimer += delta;
                    float progress = runtime.transitionTimer / CarouselTransition.DURATION_SECONDS;
                    if (progress >= 1f) {
                        world.execute(() -> finishTransition(hologram, runtime, manager));
                    } else {
                        world.execute(() -> applyTransition(hologram, runtime, progress, manager));
                    }
                }
            } catch (Exception e) {
                LOGGER.at(Level.FINE).log("[Varyon-Holograms] Carousel tick error %s: %s",
                    hologram.getName(), e.getMessage());
            }
        }
    }

    private void beginTransition(@Nonnull Hologram hologram, @Nonnull CarouselRuntime runtime,
                                  @Nonnull HologramManager manager) {
        if (runtime.transitioning) return;
        int nextPage = (runtime.currentPage + 1) % hologram.getPageCount();
        runtime.nextPage = nextPage;
        runtime.transitioning = true;
        runtime.transitionTimer = 0f;
        plugin.getAnimationManager().unregisterHologramAnimation(hologram.getId());

        CarouselTransition transition = hologram.getCarouselTransition();
        HologramLayout layout = hologram.getLayout();

        if (transition == CarouselTransition.INSTANT) {
            manager.despawnActivePage(hologram);
            runtime.currentPage = nextPage;
            manager.spawnActivePage(hologram, nextPage, new Vector3d());
            runtime.transitioning = false;
            runtime.displayTimer = hologram.getCarouselIntervalSeconds();
            return;
        }

        runtime.incomingOffsetStart = transition.incomingStart(layout);
        runtime.outgoingOffsetEnd = transition.outgoingEnd(layout);
        runtime.outgoingEntities = new ArrayList<>(hologram.getLineEntityIds());
        runtime.incomingEntities = manager.spawnPageEntities(
            hologram, nextPage, runtime.incomingOffsetStart, false);
    }

    private void applyTransition(@Nonnull Hologram hologram, @Nonnull CarouselRuntime runtime, float progress,
                                  @Nonnull HologramManager manager) {
        World world = findWorld(hologram.getWorldId());
        if (world == null) return;
        Vector3d outOffset = lerp(new Vector3d(), runtime.outgoingOffsetEnd, progress);
        Vector3d inOffset = lerp(runtime.incomingOffsetStart, new Vector3d(), progress);
        moveEntities(world, runtime.outgoingEntities, hologram, outOffset, manager);
        moveEntities(world, runtime.incomingEntities, hologram, inOffset, manager);
    }

    private void finishTransition(@Nonnull Hologram hologram, @Nonnull CarouselRuntime runtime,
                                   @Nonnull HologramManager manager) {
        manager.despawnEntityIds(hologram, runtime.outgoingEntities);
        hologram.clearLineEntityIds();
        for (UUID id : runtime.incomingEntities) {
            hologram.addLineEntityId(id);
        }
        runtime.currentPage = runtime.nextPage;
        runtime.transitioning = false;
        runtime.displayTimer = hologram.getCarouselIntervalSeconds();
        runtime.outgoingEntities.clear();
        runtime.incomingEntities.clear();
        manager.registerPageAnimation(hologram, runtime.currentPage);
    }

    private static void moveEntities(@Nonnull World world, @Nonnull List<UUID> entityIds, @Nonnull Hologram hologram,
                                      @Nonnull Vector3d slideOffset, @Nonnull HologramManager manager) {
        Store<EntityStore> store = world.getEntityStore().getStore();
        Map<UUID, Vector3d> bases = manager.getCarouselBasePositions(hologram.getId());
        for (UUID entityId : entityIds) {
            Vector3d base = bases.get(entityId);
            if (base == null) continue;
            Ref<EntityStore> ref = ((EntityStore) store.getExternalData()).getRefFromUUID(entityId);
            if (ref == null || !ref.isValid()) continue;
            TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
            if (transform == null) continue;
            transform.teleportPosition(new Vector3d(
                base.x + slideOffset.x,
                base.y + slideOffset.y,
                base.z + slideOffset.z));
        }
    }

    @Nonnull
    private static Vector3d lerp(@Nonnull Vector3d from, @Nonnull Vector3d to, float t) {
        return new Vector3d(
            from.x + (to.x - from.x) * t,
            from.y + (to.y - from.y) * t,
            from.z + (to.z - from.z) * t);
    }

    @javax.annotation.Nullable
    private World findWorld(@Nonnull UUID worldId) {
        Universe universe = Universe.get();
        return universe != null ? universe.getWorld(worldId) : null;
    }

    private static final class CarouselRuntime {
        int currentPage;
        float displayTimer;
        boolean transitioning;
        float transitionTimer;
        int nextPage;
        Vector3d incomingOffsetStart = new Vector3d();
        Vector3d outgoingOffsetEnd = new Vector3d();
        List<UUID> outgoingEntities = new ArrayList<>();
        List<UUID> incomingEntities = new ArrayList<>();
    }
}
