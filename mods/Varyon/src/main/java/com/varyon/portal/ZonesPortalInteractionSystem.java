package com.varyon.portal;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.InteractionChain;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.InteractionManager;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.VaryonPlugin;
import com.varyon.config.ZoneConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class ZonesPortalInteractionSystem extends EntityEventSystem<EntityStore, UseBlockEvent.Post> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String ZONES_PORTAL_BLOCK_ID = "Varyon_Portal_Zones";
    private static final long OPEN_COOLDOWN_MS = 2000;

    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public ZonesPortalInteractionSystem() {
        super(UseBlockEvent.Post.class);
    }

    @Override
    public void handle(int index,
                       @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull UseBlockEvent.Post event) {

        BlockType blockType = event.getBlockType();
        if (blockType == null || blockType.getId() == null) {
            return;
        }
        if (!blockType.getId().contains(ZONES_PORTAL_BLOCK_ID)) {
            return;
        }

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        if (ref == null) {
            return;
        }

        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRefComp = store.getComponent(ref, PlayerRef.getComponentType());
        if (player == null || playerRefComp == null) {
            return;
        }

        UUID playerId = playerRefComp.getUuid();
        long now = System.currentTimeMillis();
        Long lastOpen = cooldowns.get(playerId);
        if (lastOpen != null && now - lastOpen < OPEN_COOLDOWN_MS) {
            return;
        }
        cooldowns.put(playerId, now);

        cancelInteractionChain(event.getContext());

        String worldName = "";
        try {
            if (store.getExternalData() instanceof EntityStore es && es.getWorld() != null) {
                worldName = es.getWorld().getName();
            }
        } catch (Exception ignored) {}
        ZoneConfig zoneConfig = VaryonPlugin.getStaticConfigManager() != null ? VaryonPlugin.getStaticConfigManager().getZoneConfig() : null;
        if (zoneConfig != null && !zoneConfig.isWorldEnabled(worldName)) {
            return;
        }

        try {
            ZonesPortalUIPage page = new ZonesPortalUIPage(playerRefComp);
            player.getPageManager().openCustomPage(ref, store, page);
            LOGGER.at(Level.FINE).log("Opened ZonesPortalMenu for player " + playerId);
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Failed to open ZonesPortalMenu: " + e.getMessage());
        }
    }

    private void cancelInteractionChain(@Nullable InteractionContext context) {
        if (context == null) return;
        try {
            InteractionChain chain = context.getChain();
            InteractionManager manager = context.getInteractionManager();
            if (chain != null && manager != null) {
                manager.cancelChains(chain);
            }
        } catch (Exception ignored) {
        }
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}