package com.varyon.points;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.event.KillFeedEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.varyon.VaryonPlugin;
import com.varyon.component.MobScalingComponent;
import com.varyon.config.ConfigManager;
import com.varyon.config.DifficultyZone;
import com.varyon.config.PointsRewardsConfig;
import com.varyon.config.VaryonDebug;
import com.varyon.safezone.SafeZoneManager;
import com.varyon.util.ZoneCalculator;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.logging.Level;

public class PointsKillSystem extends EntityEventSystem<EntityStore, KillFeedEvent.KillerMessage> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Nonnull
    private final ComponentType<EntityStore, PlayerRef> playerRefComponentType = PlayerRef.getComponentType();

    private final PointsManager         pointsManager;
    private final ConfigManager         configManager;
    private final PointsRewardsConfig   rewardsConfig;

    public PointsKillSystem(@Nonnull PointsManager pointsManager, @Nonnull ConfigManager configManager,
                             @Nonnull PointsRewardsConfig rewardsConfig) {
        super(KillFeedEvent.KillerMessage.class);
        this.pointsManager   = pointsManager;
        this.configManager   = configManager;
        this.rewardsConfig   = rewardsConfig;
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                      @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer,
                      @Nonnull KillFeedEvent.KillerMessage event) {

        try {
            PlayerRef playerRef = archetypeChunk.getComponent(index, playerRefComponentType);
            if (playerRef == null) {
                return;
            }

            UUID playerUuid = playerRef.getUuid();
            String worldName = "";
            try {
                if (store.getExternalData() instanceof EntityStore es && es.getWorld() != null) {
                    worldName = es.getWorld().getName();
                }
            } catch (Exception ignored) {}
            if (!configManager.getZoneConfig().isWorldEnabled(worldName)) {
                return;
            }

            Ref<EntityStore> victimRef = event.getTargetRef();

            String mobId = "unknown";
            if (victimRef != null && victimRef.isValid()) {
                mobId = resolveMobName(victimRef, store, commandBuffer);
            }
            if (VaryonDebug.isPoints()) {
                LOGGER.at(Level.INFO).log("[Debug points] " + playerRef.getUsername() + " a tué (role=" + mobId + ")");
            }

            double baseReward = rewardsConfig.getMobReward(mobId);
            if (baseReward <= 0) {
                if (VaryonDebug.isPoints()) {
                    LOGGER.at(Level.INFO).log("[Debug points] " + playerRef.getUsername() + " a tué " + mobId + " — aucune récompense configurée");
                }
                return;
            }

            DifficultyZone zone = ZoneCalculator.getCurrentZone(store, archetypeChunk.getReferenceTo(index), configManager.getZoneConfig());
            double zoneMultiplier = zone != null ? zone.getPointsMultiplier() : 1.0;

            double lootMultiplier = 1.0;
            if (victimRef != null && victimRef.isValid()) {
                MobScalingComponent scaling = store.getComponent(victimRef, MobScalingComponent.getComponentType());
                if (scaling != null) {
                    lootMultiplier = scaling.getLootMultiplier();
                    zoneMultiplier = scaling.getPointsMultiplier();
                }
            }

            Ref<EntityStore> killerRef = archetypeChunk.getReferenceTo(index);

            double pvpMultiplier = 1.0;
            SafeZoneManager szm = VaryonPlugin.getStaticSafeZoneManager();
            if (szm != null) {
                TransformComponent transform = store.getComponent(killerRef, TransformComponent.getComponentType());
                if (transform != null && !szm.isInSafeZone(transform.getPosition().x, transform.getPosition().z)) {
                    pvpMultiplier = rewardsConfig.getPvpPointsMultiplier();
                }
            }

            double pointsGained = baseReward * zoneMultiplier * lootMultiplier * pvpMultiplier;
            if (pointsGained <= 0) return;
            double current = pointsManager.getPoints(playerUuid);
            int cap = configManager.getZonePermissionsConfig().getEffectiveCap(playerRef, current);
            double added = pointsManager.addPointsCapped(playerUuid, playerUuid.toString(), pointsGained, cap);
            if (VaryonDebug.isPoints()) {
                LOGGER.at(Level.INFO).log(String.format(
                    "[Debug points] %s mob=%s base=%.2f zone=x%.2f loot=x%.2f pvp=x%.2f => calculé=%.2f | ajouté=%.2f (cap=%d, avant=%.1f)",
                    playerRef.getUsername(), mobId, baseReward, zoneMultiplier, lootMultiplier, pvpMultiplier,
                    pointsGained, added, cap, current));
            }
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error in PointsKillSystem: " + e.getMessage());
        }
    }

    @Nonnull
    private String resolveMobName(@Nonnull Ref<EntityStore> ref,
                                  @Nonnull Store<EntityStore> store,
                                  @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        NPCEntity npc = null;
        try { npc = store.getComponent(ref, NPCEntity.getComponentType()); } catch (Exception ignored) {}
        if (npc == null) {
            try { npc = commandBuffer.getComponent(ref, NPCEntity.getComponentType()); } catch (Exception ignored) {}
        }
        if (npc == null) {
            return "unknown";
        }

        String name = null;
        try { name = npc.getRoleName(); } catch (Exception ignored) {}
        if (name == null || name.isEmpty()) {
            try { name = npc.getNPCTypeId(); } catch (Exception ignored) {}
        }
        return name != null && !name.isEmpty() ? name.toLowerCase() : "unknown";
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return playerRefComponentType;
    }
}
