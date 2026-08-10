package com.varyon.death;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.VaryonPlugin;
import com.varyon.config.DeathConfig;
import com.varyon.points.PointsManager;

import javax.annotation.Nonnull;
import java.util.logging.Level;

public class DeathDetectionSystem extends DeathSystems.OnDeathSystem {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final DeathPointManager deathPointManager;

    public DeathDetectionSystem(@Nonnull DeathPointManager deathPointManager) {
        this.deathPointManager = deathPointManager;
    }
    
    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(Player.getComponentType());
    }
    
    @Override
    public void onComponentAdded(@Nonnull Ref ref, @Nonnull DeathComponent component,
                                @Nonnull Store store, @Nonnull CommandBuffer commandBuffer) {
        
        PlayerRef playerRef = (PlayerRef) store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }
        
        Player player = (Player) store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        
        String worldName = "world";
        try {
            if (store.getExternalData() != null && ((EntityStore)store.getExternalData()).getWorld() != null) {
                worldName = ((EntityStore)store.getExternalData()).getWorld().getName();
            } else if (player.getWorld() != null) {
                worldName = player.getWorld().getName();
            }
        } catch (Exception e) {
            if (player.getWorld() != null) {
                worldName = player.getWorld().getName();
            }
        }
        
        Transform transform = playerRef.getTransform();
        double x = transform.getPosition().x;
        double y = transform.getPosition().y;
        double z = transform.getPosition().z;
        
        deathPointManager.recordDeathPoint(playerRef.getUuid(), worldName, x, y, z);

        if (VaryonPlugin.getStaticConfigManager() != null && VaryonPlugin.getStaticConfigManager().getZoneConfig().isWorldEnabled(worldName)) {
            PointsManager pointsManager = VaryonPlugin.getStaticPointsManager();
            DeathConfig deathConfig = VaryonPlugin.getStaticConfigManager().getDeathConfig();
            if (pointsManager != null && deathConfig.getPointsLossPercent() > 0) {
                double current = pointsManager.getPoints(playerRef.getUuid());
                double loss = current * (deathConfig.getPointsLossPercent() / 100.0);
                if (loss > 0) {
                    pointsManager.addPoints(playerRef.getUuid(), playerRef.getUsername(), -loss);
                    LOGGER.at(Level.INFO).log("Death: player " + playerRef.getUuid() + " lost " + String.format("%.1f", loss) + " faction points (" + (int) deathConfig.getPointsLossPercent() + "%)");
                }
            }
        }

        LOGGER.at(Level.INFO).log("Death detected for player " + playerRef.getUuid() + " at " +
            worldName + ":" + (int)x + "," + (int)y + "," + (int)z);
    }
}
