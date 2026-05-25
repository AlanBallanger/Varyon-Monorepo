package com.varyon.essence;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.EntityUtils;
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
import com.varyon.config.EssenceRewardsConfig;
import com.varyon.safezone.SafeZoneManager;
import com.varyon.util.ZoneCalculator;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class EssenceKillSystem extends EntityEventSystem<EntityStore, KillFeedEvent.KillerMessage> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Set<String> UNSAFE_METHODS = Set.of(
        "remove", "delete", "destroy", "kill", "unload", "clear", "close",
        "unloadfromworld", "clearreference", "markneedssave", "invalidateequipmentnetwork"
    );

    @Nonnull
    private final ComponentType<EntityStore, PlayerRef> playerRefComponentType = PlayerRef.getComponentType();

    private final EssenceManager        essenceManager;
    private final ConfigManager         configManager;
    private final EssenceRewardsConfig  rewardsConfig;

    private volatile Method cachedNameMethod;
    private volatile String cachedNameSource;

    public EssenceKillSystem(@Nonnull EssenceManager essenceManager, @Nonnull ConfigManager configManager,
                             @Nonnull EssenceRewardsConfig rewardsConfig) {
        super(KillFeedEvent.KillerMessage.class);
        this.essenceManager  = essenceManager;
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

            double baseReward = rewardsConfig.getMobReward(mobId);
            if (baseReward <= 0) {
                return;
            }

            DifficultyZone zone = ZoneCalculator.getCurrentZone(store, archetypeChunk.getReferenceTo(index), configManager.getZoneConfig());
            double zoneMultiplier = zone != null ? zone.getEssenceMultiplier() : 1.0;

            double lootMultiplier = 1.0;
            if (victimRef != null && victimRef.isValid()) {
                MobScalingComponent scaling = store.getComponent(victimRef, MobScalingComponent.getComponentType());
                if (scaling != null) {
                    lootMultiplier = scaling.getLootMultiplier();
                    zoneMultiplier = scaling.getEssenceMultiplier();
                }
            }

            Ref<EntityStore> killerRef = archetypeChunk.getReferenceTo(index);

            double pvpMultiplier = 1.0;
            SafeZoneManager szm = VaryonPlugin.getStaticSafeZoneManager();
            if (szm != null) {
                TransformComponent transform = store.getComponent(killerRef, TransformComponent.getComponentType());
                if (transform != null && !szm.isInSafeZone(transform.getPosition().getX(), transform.getPosition().getZ())) {
                    pvpMultiplier = rewardsConfig.getPvpEssenceMultiplier();
                }
            }

            double essenceGained = baseReward * zoneMultiplier * lootMultiplier * pvpMultiplier;
            if (essenceGained <= 0) return;
            Player player = null;
            try { player = (Player) store.getComponent(killerRef, Player.getComponentType()); } catch (Exception ignored) {}
            if (player != null) {
                double current = essenceManager.getEssence(playerUuid);
                int cap = configManager.getZonePermissionsConfig().getEffectiveCap(player, current);
                essenceManager.addEssenceCapped(playerUuid, playerUuid.toString(), essenceGained, cap);
            } else {
                essenceManager.addEssence(playerUuid, playerUuid.toString(), essenceGained);
            }

            LOGGER.at(Level.INFO).log("Kill: mob=" + mobId + " +" + String.format("%.2f", essenceGained) + " faction points (base=" + baseReward + " loot=" + String.format("%.2f", lootMultiplier) + " zone=" + String.format("%.2f", zoneMultiplier) + " pvp=" + pvpMultiplier + ")");
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Error in EssenceKillSystem: " + e.getMessage());
        }
    }

    @Nonnull
    private String resolveMobName(@Nonnull Ref<EntityStore> ref,
                                  @Nonnull Store<EntityStore> store,
                                  @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        if (cachedNameMethod != null) {
            return resolveWithCachedMethod(ref, store, commandBuffer);
        }

        try {
            Entity entity = null;
            try { entity = EntityUtils.getEntity(ref, commandBuffer); } catch (Exception ignored) {}
            if (entity == null) {
                try { entity = EntityUtils.getEntity(ref, store); } catch (Exception ignored) {}
            }

            if (entity != null) {
                String result = scanStringMethods(entity, "Entity");
                if (result != null) return result;
            }

            NPCEntity npc = null;
            try { npc = store.getComponent(ref, NPCEntity.getComponentType()); } catch (Exception ignored) {}
            if (npc == null) {
                try { npc = commandBuffer.getComponent(ref, NPCEntity.getComponentType()); } catch (Exception ignored) {}
            }

            if (npc != null) {
                String result = scanStringMethods(npc, "NPCEntity");
                if (result != null) return result;
            }
        } catch (Exception e) {
            LOGGER.at(Level.FINE).log("resolveMobName error: " + e.getMessage());
        }
        return "unknown";
    }

    private String scanStringMethods(@Nonnull Object obj, @Nonnull String source) {
        for (Method m : obj.getClass().getMethods()) {
            if (m.getDeclaringClass() == Object.class) continue;
            if (m.getParameterCount() != 0) continue;
            if (m.getReturnType() != String.class) continue;
            if (UNSAFE_METHODS.contains(m.getName().toLowerCase())) continue;

            try {
                String val = (String) m.invoke(obj);
                if (val != null && !val.isEmpty() && val.length() < 100) {
                    String lower = val.toLowerCase();
                    if (rewardsConfig.getMobReward(lower) > 0) {
                        cachedNameMethod = m;
                        cachedNameSource = source;
                        LOGGER.at(Level.INFO).log("Mob name resolved via " + source + "." + m.getName() + "() = " + val);
                        return lower;
                    }
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    @Nonnull
    private String resolveWithCachedMethod(@Nonnull Ref<EntityStore> ref,
                                           @Nonnull Store<EntityStore> store,
                                           @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        try {
            Object target = null;
            if ("Entity".equals(cachedNameSource)) {
                try { target = EntityUtils.getEntity(ref, commandBuffer); } catch (Exception ignored) {}
                if (target == null) {
                    try { target = EntityUtils.getEntity(ref, store); } catch (Exception ignored) {}
                }
            } else {
                try { target = store.getComponent(ref, NPCEntity.getComponentType()); } catch (Exception ignored) {}
                if (target == null) {
                    try { target = commandBuffer.getComponent(ref, NPCEntity.getComponentType()); } catch (Exception ignored) {}
                }
            }

            if (target != null) {
                String val = (String) cachedNameMethod.invoke(target);
                if (val != null && !val.isEmpty()) {
                    return val.toLowerCase();
                }
            }
        } catch (Exception e) {
            cachedNameMethod = null;
            cachedNameSource = null;
        }
        return "unknown";
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return playerRefComponentType;
    }
}
