package com.varyon.system;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.varyon.component.MobScalingComponent;
import com.varyon.config.ConfigManager;
import com.varyon.config.DifficultyZone;
import com.varyon.util.ZoneCalculator;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class MobFragmentDropSystem {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /**
     * Last player entity ref that damaged this victim (same idea as Ecotale {@code MobLastAttackerSystem}).
     */
    private final ConcurrentHashMap<Integer, Ref<EntityStore>> lastPlayerAttackerByVictim = new ConcurrentHashMap<>();

    /**
     * Set when a player deals damage to an NPC; DropOnDeath requires this so mob-on-mob
     * deaths (or other non-player kills) never grant fragments.
     */
    private final ConcurrentHashMap<Integer, Boolean> victimDamagedByPlayer = new ConcurrentHashMap<>();

    private final ConfigManager configManager;

    public MobFragmentDropSystem(@Nonnull ConfigManager configManager) {
        this.configManager = configManager;
    }

    public final class PlayerDamageTagger extends DamageEventSystem {

        public PlayerDamageTagger() {}

        @Override
        public SystemGroup<EntityStore> getGroup() {
            return DamageModule.get().getFilterDamageGroup();
        }

        @Override
        @Nonnull
        public Query<EntityStore> getQuery() {
            return NPCEntity.getComponentType();
        }

        @Override
        public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
                           @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer,
                           @Nonnull Damage damage) {
            try {
                if (damage.isCancelled() || damage.getAmount() <= 0.0f) {
                    return;
                }
                Damage.Source source = damage.getSource();
                if (!(source instanceof Damage.EntitySource entitySource)) {
                    return;
                }
                Ref<EntityStore> attackerRef = entitySource.getRef();
                if (attackerRef == null || !attackerRef.isValid()) {
                    return;
                }
                Player player = store.getComponent(attackerRef, Player.getComponentType());
                if (player == null) {
                    player = commandBuffer.getComponent(attackerRef, Player.getComponentType());
                }
                if (player == null) {
                    return;
                }
                Ref<EntityStore> victimRef = archetypeChunk.getReferenceTo(index);
                int victimId = System.identityHashCode(victimRef);
                victimDamagedByPlayer.put(victimId, Boolean.TRUE);
                lastPlayerAttackerByVictim.put(victimId, attackerRef);
            } catch (Exception e) {
                LOGGER.at(Level.WARNING).log("PlayerDamageTagger error: " + e.getMessage());
            }
        }
    }

    // -------------------------------------------------------------------------
    // Death ” resolve contributing player like Ecotale mob coins (killer else last attacker).
    // -------------------------------------------------------------------------
    public final class DropOnDeath extends DeathSystems.OnDeathSystem {

        @Override
        public Query<EntityStore> getQuery() {
            return NPCEntity.getComponentType();
        }

        @Override
        @SuppressWarnings({"rawtypes", "unchecked"})
        public void onComponentAdded(
                @Nonnull Ref ref,
                @Nonnull DeathComponent death,
                @Nonnull Store store,
                @Nonnull CommandBuffer commandBuffer) {
            try {
                if (store.getComponent(ref, com.varyon.component.NoLootComponent.getComponentType()) != null) {
                    return;
                }

                int victimId = System.identityHashCode(ref);

                String worldName = ((EntityStore) store.getExternalData()).getWorld().getName();
                if (!configManager.getZoneConfig().isWorldEnabled(worldName)) {
                    lastPlayerAttackerByVictim.remove(victimId);
                    victimDamagedByPlayer.remove(victimId);
                    return;
                }

                Ref<EntityStore> lastAttackerRef = lastPlayerAttackerByVictim.remove(victimId);
                Boolean playerDamaged = victimDamagedByPlayer.remove(victimId);
                if (!Boolean.TRUE.equals(playerDamaged)) {
                    return;
                }

                Player creditPlayer = resolveContributingPlayer(store, death, lastAttackerRef, commandBuffer);
                if (creditPlayer == null) {
                    return;
                }

                PlayerRef creditPlayerRef = Universe.get().getPlayer(creditPlayer.getUuid());
                int zoneId = resolveZoneIdFromPosition(store, ref, worldName);
                int maxUnlocked = creditPlayerRef != null
                    ? configManager.getZonePermissionsConfig().getMaxAccessibleZone(creditPlayerRef)
                    : 1;
                int lootZone = Math.min(zoneId, maxUnlocked);

                if (lootZone < 1) {
                    return;
                }

                NPCEntity npc = (NPCEntity) store.getComponent(ref, NPCEntity.getComponentType());
                if (npc == null) return;

                String roleName = npc.getRoleName();
                if (roleName == null || roleName.isBlank()) return;

                int fragments = configManager.getMobFragmentsConfig().getFragments(roleName.toLowerCase(Locale.ROOT));
                if (fragments <= 0) return;

                String itemId = configManager.getZoneLootConfig().getItemForZone(lootZone);
                if (itemId == null || itemId.isBlank()) return;

                TransformComponent transform = (TransformComponent) store.getComponent(ref, TransformComponent.getComponentType());
                if (transform == null) return;

                org.joml.Vector3d pos = new org.joml.Vector3d(transform.getPosition()).add(0.0, 1.0, 0.0);
                HeadRotation headRotation = (HeadRotation) store.getComponent(ref, HeadRotation.getComponentType());
                com.hypixel.hytale.math.vector.Rotation3fc rot = headRotation != null ? headRotation.getRotation() : com.hypixel.hytale.math.vector.Rotation3f.ZERO;

                Holder[] itemEntities = ItemComponent.generateItemDrops(store, List.of(new ItemStack(itemId, fragments)), pos, rot);
                commandBuffer.addEntities(itemEntities, AddReason.SPAWN);

            } catch (Exception e) {
                LOGGER.at(Level.WARNING).log("DropOnDeath error: " + e.getMessage());
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Nullable
    private Player resolveContributingPlayer(@Nonnull Store store, @Nonnull DeathComponent death,
                                             @Nullable Ref<EntityStore> lastAttackerRef,
                                             @Nonnull CommandBuffer commandBuffer) {
        Damage deathInfo = death.getDeathInfo();
        if (deathInfo != null) {
            Damage.Source source = deathInfo.getSource();
            if (source instanceof Damage.EntitySource entitySource) {
                Ref<EntityStore> killerRef = entitySource.getRef();
                if (killerRef != null && killerRef.isValid()) {
                    Player p = (Player) store.getComponent(killerRef, Player.getComponentType());
                    if (p == null) {
                        p = (Player) commandBuffer.getComponent(killerRef, Player.getComponentType());
                    }
                    if (p != null) {
                        return p;
                    }
                }
            }
        }
        if (lastAttackerRef != null && lastAttackerRef.isValid()) {
            Player p = (Player) store.getComponent(lastAttackerRef, Player.getComponentType());
            if (p == null) {
                p = (Player) commandBuffer.getComponent(lastAttackerRef, Player.getComponentType());
            }
            if (p != null) {
                return p;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private int resolveZoneIdFromPosition(@Nonnull Store store, @Nonnull Ref ref, @Nonnull String worldName) {
        if (worldName != null && !worldName.isBlank()) {
            Integer instanceZoneId = configManager.getZoneConfig().getZoneIdForInstanceWorld(worldName);
            if (instanceZoneId != null) return instanceZoneId;
        }
        TransformComponent transform = (TransformComponent) store.getComponent(ref, TransformComponent.getComponentType());
        if (transform != null) {
            DifficultyZone zone = ZoneCalculator.getZoneAtPosition(
                    transform.getPosition().x,
                    transform.getPosition().z,
                    worldName,
                    configManager.getZoneConfig());
            if (zone != null) return zone.getZoneId();
        }
        MobScalingComponent scaling = (MobScalingComponent) store.getComponent(ref, MobScalingComponent.getComponentType());
        return scaling != null ? Math.max(1, (int) Math.ceil(scaling.getMobLevel() / 10.0)) : 1;
    }

    public DropOnDeath createDropSystem() {
        return new DropOnDeath();
    }

    public PlayerDamageTagger createPlayerDamageTagger() {
        return new PlayerDamageTagger();
    }
}
