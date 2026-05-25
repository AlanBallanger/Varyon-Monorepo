package com.varyon.system;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.varyon.component.MobScalingComponent;
import com.varyon.config.ConfigManager;
import com.varyon.config.DifficultyZone;
import com.varyon.util.ZoneCalculator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;

public class MobScalingRefSystem extends RefSystem<EntityStore> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String HEALTH_MODIFIER_KEY = "Varyon_Health";
    private static final Random RANDOM = new Random();

    private final ConfigManager configManager;

    public MobScalingRefSystem(@Nonnull ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(NPCEntity.getComponentType());
    }

    @Override
    public void onEntityAdded(@Nonnull Ref<EntityStore> ref, @Nonnull AddReason reason,
                              @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        NPCEntity npcEntity = store.getComponent(ref, NPCEntity.getComponentType());
        if (npcEntity == null) {
            return;
        }

        if (reason != AddReason.SPAWN) {
            MobScalingComponent existing = store.getComponent(ref, MobScalingComponent.getComponentType());
            if (existing != null) {
                reapplyHealthModifier(ref, store, existing.getHealthMultiplier());
            }
            return;
        }

        Object ext = store.getExternalData();
        if (!(ext instanceof EntityStore entityStore) || entityStore.getWorld() == null) {
            return;
        }
        String worldName = entityStore.getWorld().getName();
        if (worldName == null || worldName.isBlank() || !configManager.getZoneConfig().isWorldEnabled(worldName)) {
            return;
        }

        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            return;
        }

        Vector3d pos = transform.getPosition();
        DifficultyZone zone = ZoneCalculator.getZoneAtPosition(pos.getX(), pos.getZ(), worldName, configManager.getZoneConfig());

        if (zone == null) {
            return;
        }

        applyScaling(ref, store, commandBuffer, zone);
    }

    @Override
    public void onEntityRemove(@Nonnull Ref<EntityStore> ref, @Nonnull RemoveReason reason,
                               @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
    }

    private void applyScaling(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                              @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull DifficultyZone zone) {
        MobScalingComponent existing = store.getComponent(ref, MobScalingComponent.getComponentType());
        if (existing != null) {
            return;
        }

        List<DifficultyZone> zones = configManager.getZoneConfig().getZones();
        int zoneIndex = -1;
        for (int i = 0; i < zones.size(); i++) {
            if (zones.get(i).getZoneId() == zone.getZoneId()) {
                zoneIndex = i;
                break;
            }
        }

        if (zoneIndex < 0) {
            return;
        }

        int minLevel = (zoneIndex + 1) * 10 - 9;
        int maxLevel = (zoneIndex + 1) * 10;
        int mobLevel = RANDOM.nextInt(maxLevel - minLevel + 1) + minLevel;

        double currentHealthMult = zone.getHealthMultiplier();
        double currentDamageMult = zone.getDamageMultiplier();
        double currentLootMult = zone.getLootMultiplier();
        double currentEssenceMult = zone.getEssenceMultiplier();
        double nextHealthMult = currentHealthMult;
        double nextDamageMult = currentDamageMult;
        double nextLootMult = currentLootMult;
        double nextEssenceMult = currentEssenceMult;

        if (zoneIndex + 1 < zones.size()) {
            DifficultyZone nextZone = zones.get(zoneIndex + 1);
            nextHealthMult = nextZone.getHealthMultiplier();
            nextDamageMult = nextZone.getDamageMultiplier();
            nextLootMult = nextZone.getLootMultiplier();
            nextEssenceMult = nextZone.getEssenceMultiplier();
        } else {
            nextHealthMult = currentHealthMult + 0.5;
            nextDamageMult = currentDamageMult + 0.5;
            nextLootMult = currentLootMult + 0.5;
            nextEssenceMult = currentEssenceMult + 0.5;
        }

        double levelProgress = (mobLevel - minLevel) / 10.0;
        float healthMultiplier = (float) (currentHealthMult + (nextHealthMult - currentHealthMult) * levelProgress);
        float damageMultiplier = (float) (currentDamageMult + (nextDamageMult - currentDamageMult) * levelProgress);
        float lootMultiplier = (float) (currentLootMult + (nextLootMult - currentLootMult) * levelProgress);
        float essenceMultiplier = (float) (currentEssenceMult + (nextEssenceMult - currentEssenceMult) * levelProgress);

        commandBuffer.addComponent(ref, MobScalingComponent.getComponentType(),
                new MobScalingComponent(mobLevel, healthMultiplier, damageMultiplier, lootMultiplier, essenceMultiplier));

        if (mobLevel >= 1) {
            ComponentType<EntityStore, com.hypixel.hytale.server.core.entity.nameplate.Nameplate> nameplateType = 
                com.hypixel.hytale.server.core.entity.nameplate.Nameplate.getComponentType();
            String nameplate = "Lvl " + mobLevel;
            commandBuffer.putComponent(ref, nameplateType, new com.hypixel.hytale.server.core.entity.nameplate.Nameplate(nameplate));
        }

        if (healthMultiplier != 1.0f) {
            reapplyHealthModifier(ref, store, healthMultiplier);
            LOGGER.at(Level.FINE).log("Mob level " + mobLevel + " scaled: HP×" +
                String.format("%.2f", healthMultiplier) + " DMG×" + String.format("%.2f", damageMultiplier));
        }
    }

    private void reapplyHealthModifier(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                                       float healthMultiplier) {
        if (healthMultiplier == 1.0f) {
            return;
        }
        EntityStatMap statMap = store.getComponent(ref, EntityStatMap.getComponentType());
        if (statMap == null) {
            return;
        }
        int healthIndex = DefaultEntityStatTypes.getHealth();
        EntityStatValue healthStat = statMap.get(healthIndex);
        if (healthStat == null) {
            return;
        }
        if (statMap.getModifier(healthIndex, HEALTH_MODIFIER_KEY) != null) {
            return;
        }
        StaticModifier healthModifier = new StaticModifier(
                StaticModifier.ModifierTarget.MAX,
                StaticModifier.CalculationType.MULTIPLICATIVE,
                healthMultiplier
        );
        statMap.putModifier(healthIndex, HEALTH_MODIFIER_KEY, healthModifier);
        statMap.setStatValue(healthIndex, healthStat.getMax() * healthMultiplier);
    }
}
