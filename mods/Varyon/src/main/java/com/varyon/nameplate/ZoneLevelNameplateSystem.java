package com.varyon.nameplate;

import com.frotty27.nameplatebuilder.api.NameplateData;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.varyon.component.MobScalingComponent;
import com.varyon.config.ConfigManager;
import com.varyon.util.ZoneCalculator;

import javax.annotation.Nonnull;
import java.util.logging.Level;

/**
 * Tick system that pushes the Varyon monster tier (1–10) to NameplateBuilder.
 *
 * Resolution order:
 *   1. Prefer MobScalingComponent mob level when present.
 *   2. Otherwise derive a tier from the NPC zone (see {@link ZoneCalculator}).
 *
 * Key fragments = tier/mineral weights from reference TOMLs (same folder as {@code config.toml}). Faction points use reference × 1.5 (see {@link com.varyon.config.PointsRewardsConfig}).
 *
 * Segment : "monster_level"
 *   Variant 0 (default) : "Nv.5"
 *   Variant 1            : "5"
 */
public class ZoneLevelNameplateSystem extends EntityTickingSystem<EntityStore> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final ComponentType<EntityStore, NPCEntity>     npcType;
    private final ComponentType<EntityStore, NameplateData> nameplateDataType;
    private final ConfigManager                             configManager;

    public ZoneLevelNameplateSystem(
            @Nonnull ComponentType<EntityStore, NameplateData> nameplateDataType,
            @Nonnull ConfigManager configManager) {
        this.npcType           = NPCEntity.getComponentType();
        this.nameplateDataType = nameplateDataType;
        this.configManager     = configManager;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return npcType;
    }

    @Override
    public void tick(float dt, int index,
                     @Nonnull ArchetypeChunk<EntityStore> chunk,
                     @Nonnull Store<EntityStore> store,
                     @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        try {
            NPCEntity npc = chunk.getComponent(index, npcType);
            if (npc == null) return;

            Ref<EntityStore> ref = chunk.getReferenceTo(index);

            String worldName = store.getExternalData().getWorld().getName();
            if (!configManager.getZoneConfig().isWorldEnabled(worldName)) {
                NameplateData existing = store.getComponent(ref, nameplateDataType);
                if (existing != null) {
                    existing.setText("monster_level", "");
                    existing.setText("monster_level.1", "");
                }
                return;
            }

            // --- 1. Try level from MobScalingComponent (assigned at spawn) ---
            int tier = -1;
            MobScalingComponent scaling = store.getComponent(ref, MobScalingComponent.getComponentType());
            if (scaling != null) {
                tier = scaling.getMobLevel();
            }

            if (tier < 0) {
                ZoneCalculator.ZoneWithIndex zoneWithIndex =
                        ZoneCalculator.getCurrentZoneIndexed(store, ref, worldName, configManager.getZoneConfig());
                if (zoneWithIndex.zone() != null) {
                    int zoneIndex = zoneWithIndex.index();
                    tier = zoneIndex >= 0 ? (zoneIndex + 1) * 10 - 4 : 1;
                } else {
                    tier = 1;
                }
            }

            // --- 3. Push to NameplateData ---
            NameplateData existing = store.getComponent(ref, nameplateDataType);
            if (existing == null) {
                NameplateData data = new NameplateData();
                applyTier(data, tier);
                commandBuffer.putComponent(ref, nameplateDataType, data);
            } else {
                applyTier(existing, tier);
            }
        } catch (Exception e) {
            LOGGER.at(Level.FINE).log("ZoneLevelNameplateSystem tick error: " + e.getMessage());
        }
    }

    private void applyTier(@Nonnull NameplateData data, int tier) {
        data.setText("monster_level",   "Nv." + tier);
        data.setText("monster_level.1", String.valueOf(tier));
    }
}
