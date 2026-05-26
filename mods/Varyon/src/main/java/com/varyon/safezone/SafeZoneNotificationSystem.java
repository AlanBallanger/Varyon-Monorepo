package com.varyon.safezone;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.util.EventTitleUtil;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.config.MessagesConfig;
import com.varyon.config.ZoneConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.Color;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class SafeZoneNotificationSystem extends EntityTickingSystem<EntityStore> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static SafeZoneManager safeZoneManager;
    private final Map<UUID, Boolean> playerInSafeZone = new ConcurrentHashMap<>();
    private SafeZoneConfig config;
    private ZoneConfig zoneConfig;
    private MessagesConfig messagesConfig;

    public SafeZoneNotificationSystem(@Nonnull SafeZoneConfig config, @Nonnull ZoneConfig zoneConfig,
                                      @Nonnull MessagesConfig messagesConfig) {
        this.config = config;
        this.zoneConfig = zoneConfig;
        this.messagesConfig = messagesConfig;
    }

    public static void setSafeZoneManager(@Nonnull SafeZoneManager manager) {
        safeZoneManager = manager;
    }

    public void applyReloadedConfigs(@Nonnull SafeZoneConfig safeZoneConfig,
                                     @Nonnull ZoneConfig zoneConfig,
                                     @Nonnull MessagesConfig messagesConfig) {
        this.config = safeZoneConfig;
        this.zoneConfig = zoneConfig;
        this.messagesConfig = messagesConfig;
    }

    @Override
    public void tick(float deltaTime, int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                     @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        
        if (safeZoneManager == null) {
            return;
        }

        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        Player player = store.getComponent(ref, Player.getComponentType());

        if (playerRef == null || player == null) {
            return;
        }

        String worldName = ((EntityStore)store.getExternalData()).getWorld().getName();
        if (!zoneConfig.isWorldEnabled(worldName)) {
            return;
        }

        double x = playerRef.getTransform().getPosition().x;
        double z = playerRef.getTransform().getPosition().z;
        boolean isInSafeZone = safeZoneManager.isInSafeZone(x, z);
        UUID playerId = playerRef.getUuid();

        Boolean wasInSafeZone = playerInSafeZone.get(playerId);

        if (wasInSafeZone == null || wasInSafeZone != isInSafeZone) {
            playerInSafeZone.put(playerId, isInSafeZone);
            
            if (wasInSafeZone != null) {
                if (isInSafeZone) {
                    showSafeZoneEnterNotification(playerRef);
                    LOGGER.at(Level.FINE).log("Player {} entered safe zone", playerId);
                } else {
                    showPvpZoneEnterNotification(playerRef);
                    LOGGER.at(Level.FINE).log("Player {} left safe zone", playerId);
                }
            }
        }
    }

    private void showSafeZoneEnterNotification(PlayerRef playerRef) {
        Message titleMessage = Message.raw(messagesConfig.getSafeZone().enterSafeTitle).color(Color.GREEN);
        Message topMessage = Message.raw(messagesConfig.getSafeZone().enterSafeSubtitle);
        
        float duration = 2.0f;
        float fadeIn = 0.3f;
        float fadeOut = 0.5f;
        
        EventTitleUtil.showEventTitleToPlayer(playerRef, titleMessage, topMessage, false, null, duration, fadeIn, fadeOut);
    }

    private void showPvpZoneEnterNotification(PlayerRef playerRef) {
        Message titleMessage = Message.raw(messagesConfig.getSafeZone().enterPvpTitle).color(Color.RED);
        Message topMessage = Message.raw(messagesConfig.getSafeZone().enterPvpSubtitle);
        
        float duration = 2.0f;
        float fadeIn = 0.3f;
        float fadeOut = 0.5f;
        
        EventTitleUtil.showEventTitleToPlayer(playerRef, titleMessage, topMessage, false, null, duration, fadeIn, fadeOut);
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }

    public void removePlayer(UUID playerId) {
        playerInSafeZone.remove(playerId);
    }

    public void clearAll() {
        playerInSafeZone.clear();
    }
}
