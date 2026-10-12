package com.varyon.elite;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.varyon.component.MobScalingComponent;
import com.varyon.config.ConfigManager;
import com.varyon.config.DifficultyZone;
import com.varyon.config.VaryonDebug;
import com.varyon.elite.ability.EliteAbility;
import com.varyon.elite.component.EliteAbilityStateComponent;
import com.varyon.elite.component.EliteComponent;
import com.varyon.safezone.SafeZoneManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Promotes eligible NPCs to elites at spawn. Prefers the level/zone assigned by
 * {@link com.varyon.system.MobScalingRefSystem} via {@link MobScalingComponent}, but falls
 * back to a direct zone lookup so promotion does not depend on RefSystem ordering.
 * See {@code docs/ELITE_MOBS_DESIGN.md} section 3.
 *
 * Enable {@code /varyon debug scaling} to log why a given spawn was or wasn't promoted.
 */
public class EliteSpawnRefSystem extends RefSystem<EntityStore> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final ConfigManager configManager;
    private final EliteConfig eliteConfig;
    @Nullable private final SafeZoneManager safeZoneManager;

    public EliteSpawnRefSystem(@Nonnull ConfigManager configManager,
                               @Nonnull EliteConfig eliteConfig,
                               @Nullable SafeZoneManager safeZoneManager) {
        this.configManager = configManager;
        this.eliteConfig = eliteConfig;
        this.safeZoneManager = safeZoneManager;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return Query.and(NPCEntity.getComponentType());
    }

    @Override
    public void onEntityAdded(@Nonnull Ref<EntityStore> ref, @Nonnull AddReason reason,
                              @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        if (!eliteConfig.isEnabled() || reason != AddReason.SPAWN) {
            return;
        }

        NPCEntity npc = store.getComponent(ref, NPCEntity.getComponentType());
        if (npc == null || store.getComponent(ref, EliteComponent.getComponentType()) != null) {
            return;
        }

        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null) {
            return;
        }
        org.joml.Vector3d pos = transform.getPosition();
        String worldName = EliteEntities.worldName(store);

        // Resolve the difficulty zone. Prefer Varyon's MobScalingComponent (mobLevel -> zone),
        // but fall back to computing it directly so elite spawns don't depend on RefSystem
        // ordering or on the scaling system having run first.
        int zoneId = resolveZoneId(store, ref, pos, worldName);
        if (zoneId < 1) {
            debug(role(npc), "aucune zone (monde %s hors config ou mob hors zone)", worldName);
            return;
        }
        EliteConfig.ZoneEliteDef zoneDef = eliteConfig.getZoneDef(zoneId, worldName);
        if (zoneDef == null || zoneDef.getEliteChance() <= 0) {
            debug(role(npc), "zone %d sans chance d'elite (%s)", zoneId,
                    zoneDef == null ? "pas de def" : "chance 0");
            return;
        }

        if (safeZoneManager != null && safeZoneManager.isInSafeZone(pos.x, pos.z)) {
            return;
        }

        String role = resolveRole(npc);
        if (role == null) {
            return;
        }
        if (!eliteConfig.isEligible(role)) {
            debug(role, "role non eligible (aucun eligible_fragment ou exclu)");
            return;
        }
        // An optional mob rule narrows the rolled gear (weapon family, armour material...).
        EliteConfig.MobRuleDef rule = eliteConfig.matchRule(role);

        long seed = EliteRoller.seedFor(role, zoneId, pos.x, pos.y, pos.z);
        if (!EliteRoller.rollIsElite(seed, zoneDef)) {
            debug(role, "roll rate (zone %d, chance %.0f%%)", zoneId, zoneDef.getEliteChance() * 100);
            return;
        }

        int tier = EliteRoller.rollTier(seed, zoneDef);
        MobClass mobClass = EliteRoller.rollClass(seed, tier, eliteConfig.getClassWeights(worldName));
        List<EliteAbility> abilities = EliteRoller.rollAbilities(seed, mobClass, tier);

        EliteConfig.NamedElite named = eliteConfig.getNamedElite(seed);
        String namedId = null;
        double statBonus = 0.0;
        if (named != null && named.getMobClass() == mobClass && named.getTier() == tier) {
            namedId = named.getDisplayName();
            statBonus = named.getStatBonus();
        }

        promote(ref, store, commandBuffer, npc, mobClass, tier, seed, abilities, namedId, statBonus,
                eliteConfig, rule);

        if (VaryonDebug.isScaling()) {
            LOGGER.at(Level.INFO).log(String.format(
                    "[Elite] %s promu %s T%d (zone %d, seed %d, %d capacites%s)",
                    role, mobClass, tier, zoneId, seed, abilities.size(),
                    namedId != null ? ", nomme " + namedId : ""));
        }
    }

    /** MobScalingComponent's level if present, otherwise a direct zone lookup. 0 = no zone. */
    private int resolveZoneId(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref,
                              @Nonnull org.joml.Vector3d pos, @Nullable String worldName) {
        MobScalingComponent scaling = store.getComponent(ref, MobScalingComponent.getComponentType());
        if (scaling != null) {
            return zoneIdFromLevel(scaling.getMobLevel());
        }
        if (worldName == null || !configManager.getZoneConfig().isWorldEnabled(worldName)) {
            return 0;
        }
        DifficultyZone zone = com.varyon.util.ZoneCalculator.getZoneAtPosition(
                pos.x, pos.z, worldName, configManager.getZoneConfig());
        return zone != null ? Math.max(1, Math.min(EliteConfig.ZONE_COUNT, zone.getZoneId())) : 0;
    }

    private static void debug(@Nullable String role, @Nonnull String fmt, Object... args) {
        if (VaryonDebug.isScaling()) {
            LOGGER.at(Level.INFO).log("[Elite] " + (role != null ? role + " : " : "") + String.format(fmt, args));
        }
    }

    @Nullable
    private static String role(@Nonnull NPCEntity npc) {
        return resolveRole(npc);
    }

    /** Spawn-roll path: components go through the command buffer. */
    public static void promote(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                               @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull NPCEntity npc,
                               @Nonnull MobClass mobClass, int tier, long seed,
                               @Nonnull List<EliteAbility> abilities, @Nullable String namedId, double statBonus,
                               @Nonnull EliteConfig config, @Nullable EliteConfig.MobRuleDef rule) {
        EliteComponent elite = new EliteComponent(mobClass, tier, seed, abilities, namedId, statBonus);
        commandBuffer.addComponent(ref, EliteComponent.getComponentType(), elite);
        commandBuffer.addComponent(ref, EliteAbilityStateComponent.getComponentType(),
                new EliteAbilityStateComponent());
        finishPromotion(ref, store, npc, elite, config, rule);
    }

    /** Command path: components written directly on the store, inside {@code world.execute}. */
    public static void promoteDirect(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                                     @Nonnull NPCEntity npc, @Nonnull MobClass mobClass, int tier, long seed,
                                     @Nonnull List<EliteAbility> abilities, @Nullable String namedId, double statBonus,
                                     @Nonnull EliteConfig config) {
        EliteComponent elite = new EliteComponent(mobClass, tier, seed, abilities, namedId, statBonus);
        store.putComponent(ref, EliteComponent.getComponentType(), elite);
        store.putComponent(ref, EliteAbilityStateComponent.getComponentType(),
                new EliteAbilityStateComponent());
        finishPromotion(ref, store, npc, elite, config, null);
    }

    private static void finishPromotion(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                                        @Nonnull NPCEntity npc, @Nonnull EliteComponent elite,
                                        @Nonnull EliteConfig config, @Nullable EliteConfig.MobRuleDef rule) {
        EliteStats.Rolled rolled = EliteStats.roll(config, elite.getTier(), elite.getSeed());
        elite.setRolledScaling(rolled.damageMultiplier(), rolled.sizeMultiplier());
        applyModelScale(ref, store, rolled.sizeMultiplier());

        String role = resolveRole(npc);
        boolean humanoid = role != null && config.isHumanoid(role);
        EliteEquipment.apply(ref, store, npc, elite.getMobClass(), elite.getTier(), elite.getSeed(),
                elite, rule, humanoid);
        EliteEffects.apply(ref, store, elite);

        // Armoured elites already get the vanilla armour system's own DamageResistance per piece -
        // stacking the elite tier's HP multiplier on top would double-compensate for survivability.
        // Only unarmoured elites (non-humanoids, or a humanoid whose armour assets were missing)
        // get the HP scaling.
        EntityStatMap statMap = store.getComponent(ref, EntityStatMap.getComponentType());
        if (statMap != null && !EliteEquipment.hasArmorPiece(elite.getEquipment())) {
            EliteStats.applyHealthAtSpawn(statMap, rolled.hpMultiplier());
        }
        // Varyon's zone-scaling health modifier may be added after us (RefSystem order is not
        // guaranteed). Keep topping HP to max for a short window so the elite spawns full
        // rather than regenerating up to it.
        elite.armHealToFull(System.currentTimeMillis() + 3000L);
        UUID id = EliteEntities.uuid(store, ref);
        if (id != null) {
            String worldName = EliteEntities.worldName(store);
            EliteRegistry.get().register(id, new EliteRegistry.ActiveElite(
                    id, elite.getMobClass(), elite.getTier(),
                    worldName != null ? worldName : "?", elite.getNamedId()));
        }
    }

    @Override
    public void onEntityRemove(@Nonnull Ref<EntityStore> ref, @Nonnull RemoveReason reason,
                               @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        if (store.getComponent(ref, EliteComponent.getComponentType()) == null) {
            return;
        }
        UUID id = EliteEntities.uuid(store, ref);
        if (id != null) {
            EliteRegistry.get().unregister(id);
        }
    }

    private static int zoneIdFromLevel(int mobLevel) {
        int zone = (int) Math.ceil(mobLevel / 10.0);
        return Math.max(1, Math.min(EliteConfig.ZONE_COUNT, zone));
    }

    /**
     * Rebuilds the NPC's {@code ModelComponent} from its asset with {@code Model.createScaledModel},
     * instead of the {@code EntityScaleComponent} visual-only multiplier. This is the technique
     * QuestLines-Denizens uses for its scaled spawns: the scaled model carries its own bounding box,
     * which {@code ModelSystems$UpdateBoundingBox} picks up on the component change, and the client
     * derives the nameplate height from that box - so unlike {@code EntityScaleComponent} (a pure
     * post-spawn visual multiplier the client's nameplate placement ignores), this actually moves it.
     * Removed again by de-elite via {@link #restoreBaseModel}.
     */
    static void applyModelScale(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, float size) {
        try {
            var modelType = com.hypixel.hytale.server.core.modules.entity.component.ModelComponent.getComponentType();
            var modelComp = store.getComponent(ref, modelType);
            if (modelComp == null) {
                return;
            }
            var model = modelComp.getModel();
            String assetId = model.getModelAssetId();
            if (assetId == null) {
                return;
            }
            var asset = com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset.getAssetMap().getAsset(assetId);
            if (asset == null) {
                return;
            }

            EliteComponent elite = store.getComponent(ref, EliteComponent.getComponentType());
            float baseScale = elite != null && elite.getBaseModelScale() > 0f
                    ? elite.getBaseModelScale() : model.getScale();
            if (elite != null && elite.getBaseModelScale() <= 0f) {
                elite.setBaseModelScale(baseScale);
            }

            float targetScale = baseScale * size;
            if (Math.abs(targetScale - model.getScale()) < 0.01f) {
                return;
            }
            var scaledModel = com.hypixel.hytale.server.core.asset.type.model.config.Model.createScaledModel(asset, targetScale);
            store.putComponent(ref, modelType,
                    new com.hypixel.hytale.server.core.modules.entity.component.ModelComponent(scaledModel));
        } catch (Exception e) {
            LOGGER.at(Level.FINE).log("elite model scale failed: " + e.getMessage());
        }
    }

    @Nullable
    private static String resolveRole(@Nonnull NPCEntity npc) {
        try {
            String typeId = npc.getNPCTypeId();
            if (typeId != null && !typeId.isBlank()) {
                return EliteRoles.canonical(typeId);
            }
        } catch (Exception ignored) {
        }
        try {
            String role = npc.getRoleName();
            if (role != null && !role.isBlank()) {
                return EliteRoles.canonical(role);
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
