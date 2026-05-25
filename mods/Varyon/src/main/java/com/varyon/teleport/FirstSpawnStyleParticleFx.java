package com.varyon.teleport;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.logger.HytaleLogger;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.asset.type.gameplay.GameplayConfig;
import com.hypixel.hytale.server.core.asset.type.gameplay.SpawnConfig;
import com.hypixel.hytale.server.core.asset.type.particle.config.WorldParticle;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public final class FirstSpawnStyleParticleFx {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final double ARRIVAL_Y_OFFSET = 1.0;
    private static final long RETRIGGER_INTERVAL_MS = 10_000L;

    private FirstSpawnStyleParticleFx() {}

    public static void playAt(@Nonnull World world, @Nonnull Vector3d at,
                              @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                              int effectDurationSeconds) {
        try {
            int duration = Math.max(0, effectDurationSeconds);
            int intervalSec = (int) (RETRIGGER_INTERVAL_MS / 1000L);
            int spawnTicks = Math.max(1, duration / intervalSec + 1);

            GameplayConfig gameplayConfig = world.getGameplayConfig();
            if (gameplayConfig == null) {
                return;
            }
            SpawnConfig spawnConfig = gameplayConfig.getSpawnConfig();
            if (spawnConfig == null) {
                return;
            }
            WorldParticle[] particlesTemplate = spawnConfig.getFirstSpawnParticles();
            if (particlesTemplate == null || particlesTemplate.length == 0) {
                particlesTemplate = spawnConfig.getSpawnParticles();
            }
            if (particlesTemplate == null || particlesTemplate.length == 0) {
                return;
            }

            final WorldParticle[] particles = particlesTemplate;
            final double px = at.x;
            final double py = at.y + ARRIVAL_Y_OFFSET;
            final double pz = at.z;

            AtomicInteger count = new AtomicInteger(0);
            ScheduledFuture<?>[] holder = new ScheduledFuture<?>[1];
            final int spawnTicksFinal = spawnTicks;
            holder[0] = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(() -> {
                int k = count.incrementAndGet();
                if (k > spawnTicksFinal) {
                    ScheduledFuture<?> f = holder[0];
                    if (f != null) {
                        f.cancel(false);
                    }
                    return;
                }
                world.execute(() -> {
                    Store<EntityStore> useStore = world.getEntityStore().getStore();
                    spawnBurst(world, px, py, pz, particles, ref, useStore);
                });
            }, 0L, RETRIGGER_INTERVAL_MS, TimeUnit.MILLISECONDS);
        } catch (Throwable t) {
            LOGGER.at(Level.FINE).log("First-spawn-style particles skipped: " + t.getMessage());
        }
    }

    private static void spawnBurst(@Nonnull World world,
                                   double px, double py, double pz,
                                   @Nonnull WorldParticle[] particles,
                                   @Nullable Ref<EntityStore> ref,
                                   @Nullable Store<EntityStore> store) {
        try {
            Store<EntityStore> useStore = store;
            if (useStore == null) {
                useStore = world.getEntityStore().getStore();
            }
            Vector3d position = new Vector3d(px, py, pz);
            SpatialResource<Ref<EntityStore>, EntityStore> playerSpatialResource =
                useStore.getResource(EntityModule.get().getPlayerSpatialResourceType());
            List<Ref<EntityStore>> results = SpatialResource.getThreadLocalReferenceList();
            playerSpatialResource.getSpatialStructure().collect(position, ParticleUtil.DEFAULT_PARTICLE_DISTANCE, results);
            if (ref != null && ref.isValid() && !results.contains(ref)) {
                results.add(ref);
            }
            ParticleUtil.spawnParticleEffects(particles, position, null, results, useStore);
        } catch (Throwable t) {
            LOGGER.at(Level.FINE).log("Particle burst skipped: " + t.getMessage());
        }
    }
}
