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
import com.hypixel.hytale.logger.HytaleLogger;
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

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final ComponentType<EntityStore, TransformComponent> TRANSFORM =
            TransformComponent.getComponentType();

    private final VaryonMusicZonesPlugin plugin;
    private final Map<UUID, Integer> lastSentIndex = new ConcurrentHashMap<>();
    private final Map<UUID, String> lastLoggedState = new ConcurrentHashMap<>();
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
            LOGGER.atInfo().log("[MusicZones] composant manquant playerRef=" + (playerRef != null)
                    + " tracker=" + (tracker != null) + " transform=" + (transform != null));
            return;
        }
        UUID uuid = playerRef.getUuid();
        com.hypixel.hytale.server.core.universe.world.World _w =
                com.hypixel.hytale.server.core.universe.Universe.get().getWorld(playerRef.getWorldUuid());
        if (_w == null) {
            logOnChange(uuid, "no-world", "worldUuid=" + playerRef.getWorldUuid());
            return;
        }
        int baseline = 0;
        List<MusicZone> zones = plugin.getRepository().zonesForWorld(_w.getName());
        if (zones.isEmpty()) {
            logOnChange(uuid, "no-zones", "world=" + _w.getName());
            sendIfChanged(playerRef, tracker, baseline);
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
            logOnChange(uuid, "outside-zone", "world=" + _w.getName() + " pos=" + x + "," + y + "," + z);
            sendIfChanged(playerRef, tracker, baseline);
            return;
        }
        AmbienceFX ambience = AmbienceFX.getAssetMap().getAsset(best.ambienceAssetId());
        if (ambience == null) {
            logOnChange(uuid, "no-ambience:" + best.getId(),
                    "ambienceAssetId=" + best.ambienceAssetId() + " introuvable (pack non chargé ou id incorrect)");
            sendIfChanged(playerRef, tracker, baseline);
            return;
        }
        int idx = ambience.getMusicContainerIndex();
        if (idx < 0) {
            logOnChange(uuid, "bad-index:" + best.getId(),
                    "musicContainerIndex invalide (" + idx + ") pour ambienceAssetId=" + best.ambienceAssetId());
            sendIfChanged(playerRef, tracker, baseline);
            return;
        }
        logOnChange(uuid, "in-zone:" + best.getId() + ":" + idx,
                "zone=" + best.getId() + " containerIndex=" + idx);
        sendIfChanged(playerRef, tracker, idx);
    }

    private void logOnChange(UUID uuid, String state, String detail) {
        String prev = lastLoggedState.put(uuid, state);
        if (!state.equals(prev)) {
            LOGGER.atInfo().log("[MusicZones] joueur=" + uuid + " état=" + state + " (" + detail + ")");
        }
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

    // Toujours allouer un packet neuf : muter tracker.getMusicPacket() et l'écrire deux fois
    // dans le même tick peut laisser les deux écritures en file avec le même index final.
    // Mettre à jour lastSentContainerIndex nous-mêmes évite la désynchro avec le système
    // vanilla ForcedMusicSystems.Tick qui compare current/lastSent à chaque tick.
    private void writeForcedMusic(PlayerRef playerRef, ForcedMusicTracker tracker, int desired) {
        LOGGER.atInfo().log("[MusicZones] writeForcedMusic joueur=" + playerRef.getUuid()
                + " ancienCurrent=" + tracker.getCurrentContainerIndex()
                + " ancienLastSent=" + tracker.getLastSentContainerIndex()
                + " -> desired=" + desired);
        tracker.setCurrentContainerIndex(desired);
        tracker.setLastSentContainerIndex(desired);
        UpdateForcedMusic pkt = new UpdateForcedMusic(desired);
        playerRef.getPacketHandler().write((ToClientPacket) pkt);
        lastSentIndex.put(playerRef.getUuid(), desired);
    }

    public void forget(UUID playerUuid) {
        if (playerUuid != null) {
            lastSentIndex.remove(playerUuid);
        }
    }

    // Après un rebuild du pack (ex. changement d'intensité d'une zone), le client garde en
    // cache l'ancien MusicContainer. Oublier l'index envoyé force le prochain tick à réémettre
    // UpdateForcedMusic, ce qui pousse le client à relire le container et son nouveau Volume.
    public void forgetAll() {
        lastSentIndex.clear();
        lastLoggedState.clear();
    }

    @Override
    @Nonnull
    public Query<EntityStore> getQuery() {
        return query;
    }
}
