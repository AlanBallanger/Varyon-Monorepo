package com.varyon.essence;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.varyon.VaryonPlugin;
import com.varyon.config.ConfigManager;
import com.varyon.config.DifficultyZone;
import com.varyon.config.EssenceRewardsConfig;
import com.varyon.safezone.SafeZoneManager;
import com.varyon.util.MiningOreBlockIds;
import com.varyon.util.ZoneCalculator;
import com.varyon.system.PlacedOreTracker;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.logging.Level;

public class EssenceMiningSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Nonnull
    private final ComponentType<EntityStore, PlayerRef> playerRefComponentType = PlayerRef.getComponentType();

    private final EssenceManager        essenceManager;
    private final ConfigManager         configManager;
    private final EssenceRewardsConfig  rewardsConfig;
    private final PlacedOreTracker      placedOreTracker;

    public EssenceMiningSystem(@Nonnull EssenceManager essenceManager, @Nonnull ConfigManager configManager,
                               @Nonnull EssenceRewardsConfig rewardsConfig,
                               @Nonnull PlacedOreTracker placedOreTracker) {
        super(BreakBlockEvent.class);
        this.essenceManager  = essenceManager;
        this.configManager   = configManager;
        this.rewardsConfig   = rewardsConfig;
        this.placedOreTracker = placedOreTracker;
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                      @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer,
                      @Nonnull BreakBlockEvent event) {

        try {
            PlayerRef playerRef = archetypeChunk.getComponent(index, playerRefComponentType);
            if (playerRef == null) {
                return;
            }

            String world = resolveWorld(store);
            if (!configManager.getZoneConfig().isWorldEnabled(world)) return;

            String blockId = event.getBlockType().getId().toLowerCase();
            if (MiningOreBlockIds.isExcludedFromVaryonOreRewards(blockId)) {
                return;
            }

            double baseReward = rewardsConfig.getOreReward(blockId);
            if (baseReward <= 0) {
                return;
            }

            if (event.getTargetBlock() != null) {
                boolean placed = placedOreTracker.isPlayerPlaced(world, event.getTargetBlock());
                LOGGER.at(Level.INFO).log("AntiExploit check: world=" + world + " pos=" + event.getTargetBlock().x + "," + event.getTargetBlock().y + "," + event.getTargetBlock().z + " playerPlaced=" + placed);
                if (placed) {
                    return;
                }
            }

            UUID playerUuid = playerRef.getUuid();

            Ref<EntityStore> minerRef = archetypeChunk.getReferenceTo(index);
            DifficultyZone zone = ZoneCalculator.getCurrentZone(store, minerRef, world, configManager.getZoneConfig());
            double zoneMultiplier = zone != null ? zone.getEssenceMultiplier() : 1.0;
            double lootMultiplier = zone != null ? zone.getLootMultiplier() : 1.0;

            double pvpMultiplier = 1.0;
            SafeZoneManager szm = VaryonPlugin.getStaticSafeZoneManager();
            if (szm != null) {
                TransformComponent transform = store.getComponent(minerRef, TransformComponent.getComponentType());
                if (transform != null && !szm.isInSafeZone(transform.getPosition().x, transform.getPosition().z)) {
                    pvpMultiplier = rewardsConfig.getPvpEssenceMultiplier();
                }
            }

            double essenceGained = baseReward * zoneMultiplier * lootMultiplier * pvpMultiplier;
            if (essenceGained <= 0) return;

            double current = essenceManager.getEssence(playerUuid);
            int cap = configManager.getZonePermissionsConfig().getEffectiveCap(playerRef, current);
            essenceManager.addEssenceCapped(playerUuid, playerUuid.toString(), essenceGained, cap);

            LOGGER.at(Level.INFO).log("Mine: block=" + blockId + " +" + String.format("%.2f", essenceGained) + " faction points (base=" + baseReward + " zone=" + zoneMultiplier + " loot=" + String.format("%.2f", lootMultiplier) + " pvp=" + pvpMultiplier + ")");
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error in EssenceMiningSystem: " + e.getMessage());
        }
    }

    private String resolveWorld(@Nonnull Store<EntityStore> store) {
        try {
            if (store.getExternalData() != null && ((EntityStore) store.getExternalData()).getWorld() != null) {
                return ((EntityStore) store.getExternalData()).getWorld().getName();
            }
        } catch (Exception ignored) {}
        return "world";
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return playerRefComponentType;
    }
}
