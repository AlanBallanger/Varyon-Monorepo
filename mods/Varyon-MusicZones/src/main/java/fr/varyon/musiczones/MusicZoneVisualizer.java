package fr.varyon.musiczones;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.protocol.DebugFlags;
import com.hypixel.hytale.protocol.DebugShape;
import com.hypixel.hytale.protocol.ToClientPacket;
import com.hypixel.hytale.protocol.packets.player.DisplayDebug;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Matrix4d;
import org.joml.Vector3f;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MusicZoneVisualizer extends EntityTickingSystem<EntityStore> {

    private static final float CHECK_INTERVAL_SECONDS = 0.5f;
    public static final String VOLUME_ID_PREFIX = "VaryonMZ_Preview_";

    private static final float EDGE_THICKNESS = 0.15f;
    private static final float LINE_TTL = 1.2f;
    private static final float OPACITY = 0.85f;
    private static final byte FLAGS = (byte) (1 << DebugFlags.NoWireframe.getValue());
    private static final Vector3f COLOR = new Vector3f(1.0f, 0.85f, 0.0f);

    private final VaryonMusicZonesPlugin plugin;
    private final Set<UUID> activePreviewPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Float> timers = new ConcurrentHashMap<>();

    private final Query<EntityStore> query;

    public MusicZoneVisualizer(VaryonMusicZonesPlugin plugin) {
        this.plugin = plugin;
        this.query = Archetype.of(
                Player.getComponentType(),
                PlayerRef.getComponentType(),
                TransformComponent.getComponentType());
    }

    public boolean togglePreview(UUID uuid) {
        if (activePreviewPlayers.contains(uuid)) {
            activePreviewPlayers.remove(uuid);
            timers.remove(uuid);
            return false;
        } else {
            activePreviewPlayers.add(uuid);
            timers.put(uuid, CHECK_INTERVAL_SECONDS);
            return true;
        }
    }

    public void clearPlayer(UUID uuid) {
        if (uuid == null) return;
        activePreviewPlayers.remove(uuid);
        timers.remove(uuid);
    }

    public void removeVisualForPlayer(PlayerRef playerRef, UUID uuid) {
    }

    @Override
    public void tick(
            float dt,
            int index,
            @Nonnull ArchetypeChunk<EntityStore> chunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer) {

        PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
        if (playerRef == null) return;

        UUID uuid = playerRef.getUuid();
        if (!activePreviewPlayers.contains(uuid)) return;

        float elapsed = timers.merge(uuid, dt, Float::sum);
        if (elapsed < CHECK_INTERVAL_SECONDS) return;
        timers.put(uuid, 0f);

        TransformComponent transform = chunk.getComponent(index, TransformComponent.getComponentType());
        if (transform == null) return;

        World world = Universe.get().getWorld(playerRef.getWorldUuid());
        if (world == null) return;

        double x = transform.getPosition().x;
        double y = transform.getPosition().y;
        double z = transform.getPosition().z;

        List<MusicZone> zones = plugin.getRepository().zonesForWorld(world.getName());
        MusicZone best = null;
        double bestVol = Double.MAX_VALUE;
        for (MusicZone zone : zones) {
            if (!zone.contains(x, y, z)) continue;
            double v = zone.volume();
            if (v < bestVol) {
                bestVol = v;
                best = zone;
            }
        }

        if (best != null) {
            sendBoxEdges(playerRef, best);
        }
    }

    private static void sendBoxEdges(PlayerRef playerRef, MusicZone zone) {
        double x0 = zone.getMinX(), y0 = zone.getMinY(), z0 = zone.getMinZ();
        double x1 = zone.getMaxX(), y1 = zone.getMaxY(), z1 = zone.getMaxZ();

        sendEdge(playerRef, x0, y0, z0,  x1, y0, z0);
        sendEdge(playerRef, x0, y1, z0,  x1, y1, z0);
        sendEdge(playerRef, x0, y0, z1,  x1, y0, z1);
        sendEdge(playerRef, x0, y1, z1,  x1, y1, z1);

        sendEdge(playerRef, x0, y0, z0,  x0, y1, z0);
        sendEdge(playerRef, x1, y0, z0,  x1, y1, z0);
        sendEdge(playerRef, x0, y0, z1,  x0, y1, z1);
        sendEdge(playerRef, x1, y0, z1,  x1, y1, z1);

        sendEdge(playerRef, x0, y0, z0,  x0, y0, z1);
        sendEdge(playerRef, x1, y0, z0,  x1, y0, z1);
        sendEdge(playerRef, x0, y1, z0,  x0, y1, z1);
        sendEdge(playerRef, x1, y1, z0,  x1, y1, z1);
    }

    private static void sendEdge(PlayerRef playerRef,
                                  double x0, double y0, double z0,
                                  double x1, double y1, double z1) {
        double cx = (x0 + x1) / 2.0;
        double cy = (y0 + y1) / 2.0;
        double cz = (z0 + z1) / 2.0;

        double sx = Math.abs(x1 - x0);
        double sy = Math.abs(y1 - y0);
        double sz = Math.abs(z1 - z0);
        if (sx < 0.001) sx = EDGE_THICKNESS;
        if (sy < 0.001) sy = EDGE_THICKNESS;
        if (sz < 0.001) sz = EDGE_THICKNESS;

        Matrix4d matrix = new Matrix4d();
        matrix.translate(cx, cy, cz);
        matrix.scale(sx, sy, sz);

        float[] mat = matrix.get(new float[16]);

        DisplayDebug pkt = new DisplayDebug(DebugShape.Cube, mat, COLOR, LINE_TTL, FLAGS, null, OPACITY);
        playerRef.getPacketHandler().write((ToClientPacket) pkt);
    }

    @Override
    @Nonnull
    public Set<Dependency<EntityStore>> getDependencies() {
        return Set.of();
    }

    @Override
    @Nonnull
    public Query<EntityStore> getQuery() {
        return query;
    }
}
