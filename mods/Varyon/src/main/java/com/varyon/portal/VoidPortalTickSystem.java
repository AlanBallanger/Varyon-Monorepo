package com.varyon.portal;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.VaryonPlugin;
import com.varyon.config.ZoneConfig;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VoidPortalTickSystem extends EntityTickingSystem<EntityStore> {

    private static final String VOID_PORTAL_BLOCK_ID = "Varyon_Portal_Void";
    private static final long OPEN_COOLDOWN_MS = 2000;
    private static final int CHECK_INTERVAL_TICKS = 5;

    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private int tickCounter = 0;

    @Override
    public void tick(float deltaTime, int index, @NonNullDecl ArchetypeChunk<EntityStore> archetypeChunk,
                     @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> commandBuffer) {

        if (index == 0) tickCounter++;
        if (tickCounter % CHECK_INTERVAL_TICKS != 0) return;

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        Player player = store.getComponent(ref, Player.getComponentType());
        if (playerRef == null || player == null) return;

        double px = playerRef.getTransform().getPosition().x;
        double py = playerRef.getTransform().getPosition().y;
        double pz = playerRef.getTransform().getPosition().z;

        int bx = (int) Math.floor(px);
        int by = (int) Math.floor(py);
        int bz = (int) Math.floor(pz);

        World world;
        try {
            world = ((EntityStore) store.getExternalData()).getWorld();
        } catch (Exception e) {
            return;
        }

        if (!isVoidPortalBlock(world, bx, by, bz) && !isVoidPortalBlock(world, bx, by + 1, bz)) {
            return;
        }

        UUID playerId = playerRef.getUuid();
        long now = System.currentTimeMillis();
        Long lastOpen = cooldowns.get(playerId);
        if (lastOpen != null && now - lastOpen < OPEN_COOLDOWN_MS) {
            return;
        }
        cooldowns.put(playerId, now);

        ZoneConfig zoneConfig = VaryonPlugin.getStaticConfigManager() != null ? VaryonPlugin.getStaticConfigManager().getZoneConfig() : null;
        if (zoneConfig != null && !zoneConfig.isWorldEnabled(world.getName())) {
            return;
        }

        try {
            VoidPortalUIPage page = new VoidPortalUIPage(playerRef);
            player.getPageManager().openCustomPage(ref, store, page);
        } catch (Exception ignored) {}
    }

    private boolean isVoidPortalBlock(World world, int x, int y, int z) {
        try {
            long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
            WorldChunk chunk = world.getChunk(chunkIndex);
            if (chunk == null) return false;
            BlockType blockType = chunk.getBlockType(x, y, z);
            return blockType != null && blockType.getId() != null
                && blockType.getId().contains(VOID_PORTAL_BLOCK_ID);
        } catch (Exception e) {
            return false;
        }
    }

    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }
}
