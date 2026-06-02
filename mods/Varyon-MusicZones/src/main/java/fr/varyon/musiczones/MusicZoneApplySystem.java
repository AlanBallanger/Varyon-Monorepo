package fr.varyon.musiczones;

import com.hypixel.hytale.builtin.audio.components.ForcedMusicTracker;
import com.hypixel.hytale.builtin.audio.systems.ForcedMusicSystems;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.protocol.ToClientPacket;
import com.hypixel.hytale.protocol.packets.world.UpdateForcedMusic;
import com.hypixel.hytale.server.core.asset.type.ambiencefx.config.AmbienceFX;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MusicZoneApplySystem extends EntityTickingSystem<EntityStore> {

    private static final ComponentType<EntityStore, TransformComponent> TRANSFORM =
            TransformComponent.getComponentType();

    private final VaryonMusicZonesPlugin plugin;
    private final Map<UUID, Integer> lastSentIndex = new ConcurrentHashMap<>();
    private final Query<EntityStore> query;

    public MusicZoneApplySystem(VaryonMusicZonesPlugin plugin) {
        this.plugin = plugin;
        this.query = Archetype.of(
                Player.getComponentType(),
                PlayerRef.getComponentType(),
                ForcedMusicTracker.getComponentType(),
                TRANSFORM);
    }

    @Override
    @Nonnull
    public Set<Dependency<EntityStore>> getDependencies() {
        return Set.of(new SystemDependency<>(Order.AFTER, ForcedMusicSystems.Tick.class));
    }

    @Override
    public void tick(
            float dt,
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        PlayerRef playerRef = archetypeChunk.getComponent(index, PlayerRef.getComponentType());
        Player player = archetypeChunk.getComponent(index, Player.getComponentType());
        ForcedMusicTracker tracker = archetypeChunk.getComponent(index, ForcedMusicTracker.getComponentType());
        TransformComponent transform = archetypeChunk.getComponent(index, TRANSFORM);
        if (playerRef == null || tracker == null || transform == null) {
            return;
        }
        com.hypixel.hytale.server.core.universe.world.World _w =
                com.hypixel.hytale.server.core.universe.Universe.get().getWorld(playerRef.getWorldUuid());
        if (_w == null) {
            return;
        }
        int baseline = 0;
        List<MusicZone> zones = plugin.getRepository().zonesForWorld(_w.getName());
        if (zones.isEmpty()) {
            sendIfChanged(playerRef, tracker, baseline, lastSentIndex);
            return;
        }
        double x = transform.getPosition().x;
        double y = transform.getPosition().y;
        double z = transform.getPosition().z;
        MusicZone best = null;
        double bestVol = Double.MAX_VALUE;
        for (MusicZone zt : zones) {
            if (!zt.contains(x, y, z)) {
                continue;
            }
            double v = zt.volume();
            if (v < bestVol) {
                bestVol = v;
                best = zt;
            }
        }
        if (best == null) {
            sendIfChanged(playerRef, tracker, baseline, lastSentIndex);
            return;
        }
        AmbienceFX ambience = AmbienceFX.getAssetMap().getAsset(best.ambienceAssetId());
        if (ambience == null) {
            sendIfChanged(playerRef, tracker, baseline, lastSentIndex);
            return;
        }
        int idx = ambience.getMusicContainerIndex();
        if (idx < 0) {
            sendIfChanged(playerRef, tracker, baseline, lastSentIndex);
            return;
        }
        sendIfChanged(playerRef, tracker, idx, lastSentIndex);
    }

    private static void sendIfChanged(
            PlayerRef playerRef,
            ForcedMusicTracker tracker,
            int desired,
            Map<UUID, Integer> lastSent) {
        UUID uuid = playerRef.getUuid();
        Integer prev = lastSent.get(uuid);
        if (prev != null
                && prev == desired
                && tracker.getCurrentContainerIndex() == desired) {
            return;
        }
        tracker.setCurrentContainerIndex(desired);
        UpdateForcedMusic pkt = tracker.getMusicPacket();
        pkt.containerIndex = desired;
        playerRef.getPacketHandler().write((ToClientPacket) pkt);
        lastSent.put(uuid, desired);
    }

    public void forget(UUID playerUuid) {
        if (playerUuid != null) {
            lastSentIndex.remove(playerUuid);
        }
    }

    @Override
    @Nonnull
    public Query<EntityStore> getQuery() {
        return query;
    }
}
