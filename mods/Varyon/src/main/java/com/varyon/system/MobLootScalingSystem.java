package com.varyon.system;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.asset.type.gameplay.DeathConfig;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeferredCorpseRemoval;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.item.ItemModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.varyon.component.MobScalingComponent;
import com.varyon.component.NoLootComponent;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Spawns the extra loot sets for a scaled NPC based on its loot multiplier.
 *
 * Mirrors the vanilla {@code NPCDamageSystems$DropDeathItems} ticking system: it waits
 * for the same condition (the role drops instantly, or {@link DeferredCorpseRemoval#shouldRemove()}
 * is true) before spawning, so the bonus drops appear on the same tick as the vanilla loot
 * instead of instantly at the moment of death.
 */
public class MobLootScalingSystem extends EntityTickingSystem<EntityStore> {

    private Query<EntityStore> query;

    @Override
    @Nonnull
    public Query<EntityStore> getQuery() {
        if (query == null) {
            query = Query.and(
                MobScalingComponent.getComponentType(),
                DeathComponent.getComponentType(),
                NPCEntity.getComponentType()
            );
        }
        return query;
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
                     @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {

        Player player = chunk.getComponent(index, Player.getComponentType());
        if (player != null) {
            return;
        }

        MobScalingComponent scaling = chunk.getComponent(index, MobScalingComponent.getComponentType());
        if (scaling == null || scaling.isBonusLootDropped()) {
            return;
        }

        float lootMultiplier = scaling.getLootMultiplier();
        if (lootMultiplier <= 1.0f) {
            scaling.setBonusLootDropped();
            return;
        }

        DeathComponent death = chunk.getComponent(index, DeathComponent.getComponentType());
        if (death == null || death.getItemsLossMode() != DeathConfig.ItemsLossMode.ALL) {
            scaling.setBonusLootDropped();
            return;
        }

        if (chunk.getComponent(index, NoLootComponent.getComponentType()) != null) {
            scaling.setBonusLootDropped();
            return;
        }

        NPCEntity npc = chunk.getComponent(index, NPCEntity.getComponentType());
        if (npc == null) {
            return;
        }
        Role role = npc.getRole();
        if (role == null) {
            return;
        }

        String dropListId = role.getDropListId();
        ItemModule itemModule = ItemModule.get();
        if (dropListId == null || !itemModule.isEnabled()) {
            scaling.setBonusLootDropped();
            return;
        }

        // Same gate as vanilla DropDeathItems: wait for the corpse to be ready to disappear
        // (unless the role is configured to drop instantly).
        boolean instant = role.isDropDeathItemsInstantly();
        DeferredCorpseRemoval corpse = chunk.getComponent(index, DeferredCorpseRemoval.getComponentType());
        if (!instant && (corpse == null || !corpse.shouldRemove())) {
            return;
        }

        TransformComponent transform = store.getComponent(chunk.getReferenceTo(index), TransformComponent.getComponentType());
        if (transform == null) {
            scaling.setBonusLootDropped();
            return;
        }
        HeadRotation headRotation = store.getComponent(chunk.getReferenceTo(index), HeadRotation.getComponentType());
        com.hypixel.hytale.math.vector.Rotation3fc rot = headRotation != null
            ? headRotation.getRotation()
            : com.hypixel.hytale.math.vector.Rotation3f.ZERO;

        org.joml.Vector3d dropPosition = new org.joml.Vector3d(transform.getPosition()).add(0.0, 1.0, 0.0);

        int extraDropSets = (int) Math.floor(lootMultiplier) - 1;
        float fractionalPart = lootMultiplier - (float) Math.floor(lootMultiplier);
        if (fractionalPart > 0 && Math.random() < fractionalPart) {
            extraDropSets++;
        }

        for (int i = 0; i < extraDropSets; i++) {
            List<ItemStack> randomItems = itemModule.getRandomItemDrops(dropListId);
            if (randomItems.isEmpty()) {
                continue;
            }
            org.joml.Vector3d offsetPosition = new org.joml.Vector3d(
                dropPosition.x + (Math.random() - 0.5) * 0.5,
                dropPosition.y + 0.1 * i,
                dropPosition.z + (Math.random() - 0.5) * 0.5
            );
            Holder<EntityStore>[] drops = ItemComponent.generateItemDrops(
                store, new ObjectArrayList<>(randomItems), offsetPosition, rot);
            commandBuffer.addEntities(drops, AddReason.SPAWN);
        }

        scaling.setBonusLootDropped();
    }
}
