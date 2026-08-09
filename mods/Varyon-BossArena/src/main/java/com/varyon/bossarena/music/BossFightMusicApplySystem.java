package com.varyon.bossarena.music;

import com.hypixel.hytale.builtin.audio.components.ForcedMusicTracker;
import com.hypixel.hytale.builtin.audio.systems.ForcedMusicSystems;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.OrderPriority;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.protocol.ToClientPacket;
import com.hypixel.hytale.protocol.packets.world.UpdateForcedMusic;
import com.hypixel.hytale.server.core.asset.type.ambiencefx.config.AmbienceFX;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class BossFightMusicApplySystem extends EntityTickingSystem<EntityStore> {

    private static final Logger LOGGER = Logger.getLogger("Varyon-BossArena");
    private static final ComponentType<EntityStore, TransformComponent> TRANSFORM =
            TransformComponent.getComponentType();

    private final BossFightMusicManager manager;
    private final Map<UUID, Integer> lastSentIndex = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastAppliedGeneration = new ConcurrentHashMap<>();
    /** Bumps when leaving radius / fight so the next start uses a different track copy. */
    /** Restart must span several ticks: clear (0) then apply — same-tick pulse is coalesced/resumed. */
    private final Map<UUID, PendingRestart> pendingRestart = new ConcurrentHashMap<>();
    private final Map<String, Long> missingAmbienceLoggedAt = new ConcurrentHashMap<>();
    private final Query<EntityStore> query;

    public BossFightMusicApplySystem(BossFightMusicManager manager) {
        this.manager = manager;
        this.query = Archetype.of(
                Player.getComponentType(),
                PlayerRef.getComponentType(),
                ForcedMusicTracker.getComponentType(),
                TRANSFORM);
    }

    @Override
    @Nonnull
    public Set<Dependency<EntityStore>> getDependencies() {
        Set<Dependency<EntityStore>> deps = new HashSet<>();
        deps.add(new SystemDependency<>(Order.AFTER, ForcedMusicSystems.Tick.class, OrderPriority.FURTHEST));
        return deps;
    }

    @Override
    public void tick(
            float dt,
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        PlayerRef playerRef = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
        ForcedMusicTracker tracker = archetypeChunk.getComponent(index, ForcedMusicTracker.getComponentType());
        TransformComponent transform = archetypeChunk.getComponent(index, TRANSFORM);
        if (playerRef == null || tracker == null || transform == null) {
            return;
        }
        int baseline = 0;
        UUID playerUuid = playerRef.getUuid();

        PendingRestart pending = pendingRestart.get(playerUuid);
        if (pending != null) {
            if (pending.clearTicksLeft > 0) {
                writeForcedMusic(playerRef, tracker, baseline);
                pending.clearTicksLeft--;
                return;
            }
            writeForcedMusic(playerRef, tracker, pending.containerIndex);
            lastAppliedGeneration.put(playerUuid, pending.generationKey);
            pendingRestart.remove(playerUuid);
            return;
        }

        // Bail out before any world/session lookup when no boss fight music is active anywhere:
        // this is the common case for most players most of the time.
        if (!manager.hasActiveSessions()) {
            releaseForcedMusic(playerRef, tracker, baseline, true);
            lastAppliedGeneration.remove(playerUuid);
            return;
        }
        World world = Universe.get().getWorld(playerRef.getWorldUuid());
        if (world == null) {
            return;
        }
        double x = transform.getPosition().x;
        double y = transform.getPosition().y;
        double z = transform.getPosition().z;

        List<BossFightMusicSession> sessions = manager.sessionsForWorld(world.getName());
        BossFightMusicSession best = null;
        double bestDist = Double.MAX_VALUE;
        for (BossFightMusicSession session : sessions) {
            if (!session.contains(x, y, z)) {
                continue;
            }
            double d2 = session.distanceSquared(x, y, z);
            if (d2 < bestDist) {
                bestDist = d2;
                best = session;
            }
        }
        if (best == null) {
            releaseForcedMusic(playerRef, tracker, baseline, true);
            lastAppliedGeneration.remove(playerUuid);
            return;
        }

        AmbienceFX ambience = AmbienceFX.getAssetMap().getAsset(best.ambienceAssetId());
        if (ambience == null) {
            logMissingAmbience(best);
            releaseForcedMusic(playerRef, tracker, baseline, false);
            lastAppliedGeneration.remove(playerUuid);
            return;
        }
        int idx = ambience.getMusicContainerIndex();
        if (idx < 0) {
            logMissingAmbience(best);
            releaseForcedMusic(playerRef, tracker, baseline, false);
            lastAppliedGeneration.remove(playerUuid);
            return;
        }

        long generationKey = best.getGeneration();
        Long appliedGen = lastAppliedGeneration.get(playerUuid);
        if (appliedGen == null || appliedGen != generationKey) {
            PendingRestart restart = new PendingRestart(generationKey, idx);
            pendingRestart.put(playerUuid, restart);
            writeForcedMusic(playerRef, tracker, baseline);
            restart.clearTicksLeft--;
            return;
        }
        sendIfChanged(playerRef, tracker, idx);
    }

    private void releaseForcedMusic(
            PlayerRef playerRef,
            ForcedMusicTracker tracker,
            int baseline,
            boolean unusedRestartFlag) {
        UUID uuid = playerRef.getUuid();
        pendingRestart.remove(uuid);
        Integer prev = lastSentIndex.get(uuid);
        if (prev == null || prev == baseline) {
            return;
        }
        writeForcedMusic(playerRef, tracker, baseline);
    }

    private void logMissingAmbience(BossFightMusicSession session) {
        String key = session.ambienceAssetId();
        long now = System.currentTimeMillis();
        Long prev = missingAmbienceLoggedAt.get(key);
        if (prev != null && now - prev < 10_000L) {
            return;
        }
        missingAmbienceLoggedAt.put(key, now);
        LOGGER.warning("[BossArena] AmbienceFX manquant pour '" + session.getMusicFileName()
                + "' (id=" + key + "). Relance / rebuild pack musique.");
        manager.rebuildPack();
    }

    private void sendIfChanged(PlayerRef playerRef, ForcedMusicTracker tracker, int desired) {
        UUID uuid = playerRef.getUuid();
        Integer prev = lastSentIndex.get(uuid);
        if (prev != null
                && prev == desired
                && tracker.getCurrentContainerIndex() == desired) {
            return;
        }
        writeForcedMusic(playerRef, tracker, desired);
    }

    /**
     * Always allocate a fresh packet — mutating {@code tracker.getMusicPacket()} and writing
     * it twice in one tick can leave both queued writes with the final index.
     */
    private void writeForcedMusic(PlayerRef playerRef, ForcedMusicTracker tracker, int desired) {
        tracker.setCurrentContainerIndex(desired);
        tracker.setLastSentContainerIndex(desired);
        UpdateForcedMusic pkt = new UpdateForcedMusic(desired);
        playerRef.getPacketHandler().write((ToClientPacket) pkt);
        lastSentIndex.put(playerRef.getUuid(), desired);
    }

    @Override
    @Nonnull
    public Query<EntityStore> getQuery() {
        return query;
    }

    private static final class PendingRestart {
        static final int CLEAR_TICKS = 8;

        final long generationKey;
        final int containerIndex;
        int clearTicksLeft = CLEAR_TICKS;

        PendingRestart(long generationKey, int containerIndex) {
            this.generationKey = generationKey;
            this.containerIndex = containerIndex;
        }
    }
}
