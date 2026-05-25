package fr.varyon.musiczones;

import com.hypixel.hytale.builtin.ambience.components.AmbienceTracker;
import com.hypixel.hytale.builtin.ambience.resources.AmbienceResource;
import com.hypixel.hytale.builtin.ambience.systems.ForcedMusicSystems;
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
import com.hypixel.hytale.protocol.packets.world.UpdateEnvironmentMusic;
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
                AmbienceTracker.getComponentType(),
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
        AmbienceTracker tracker = archetypeChunk.getComponent(index, AmbienceTracker.getComponentType());
        TransformComponent transform = archetypeChunk.getComponent(index, TRANSFORM);
        if (playerRef == null || player == null || player.getWorld() == null || tracker == null || transform == null) {
            return;
        }
        AmbienceResource ambienceResource = store.getResource(AmbienceResource.getResourceType());
        int baseline = ambienceResource != null ? ambienceResource.getForcedMusicIndex() : 0;
        List<MusicZone> zones = plugin.getRepository().zonesForWorld(player.getWorld().getName());
        if (zones.isEmpty()) {
            sendIfChanged(playerRef, tracker, baseline, lastSentIndex);
            return;
        }
        double x = transform.getPosition().getX();
        double y = transform.getPosition().getY();
        double z = transform.getPosition().getZ();
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
        int idx = AmbienceFX.getAssetMap().getIndex(best.ambienceAssetId());
        if (idx < 0) {
            sendIfChanged(playerRef, tracker, baseline, lastSentIndex);
            return;
        }
        sendIfChanged(playerRef, tracker, idx, lastSentIndex);
    }

    private static void sendIfChanged(
            PlayerRef playerRef,
            AmbienceTracker tracker,
            int desired,
            Map<UUID, Integer> lastSent) {
        UUID uuid = playerRef.getUuid();
        Integer prev = lastSent.get(uuid);
        if (prev != null
                && prev == desired
                && tracker.getForcedMusicIndex() == desired) {
            return;
        }
        tracker.setForcedMusicIndex(desired);
        UpdateEnvironmentMusic pkt = tracker.getMusicPacket();
        pkt.environmentIndex = desired;
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
