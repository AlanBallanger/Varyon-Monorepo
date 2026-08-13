package com.varyon.dummy.systems;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemGroupDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EcsEvent;
import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import com.varyon.dummy.DummyComponent;

import org.joml.Vector3i;

import javax.annotation.Nonnull;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

public class DestroyDummy extends DamageEventSystem {

    private static final Duration HIT_RESET_TIME = Duration.ofSeconds(10L);
    private static final int NUMBER_OF_HITS = 3;

    @Nonnull
    private static final Query<EntityStore> QUERY = Archetype.of(
            DummyComponent.getComponentType(),
            TransformComponent.getComponentType()
    );

    @Nonnull
    private static final Set<Dependency<EntityStore>> DEPENDENCIES = Set.of(
            new SystemGroupDependency(Order.AFTER, DamageModule.get().getGatherDamageGroup()),
            new SystemGroupDependency(Order.AFTER, DamageModule.get().getFilterDamageGroup()),
            new SystemGroupDependency(Order.BEFORE, DamageModule.get().getInspectDamageGroup())
    );

    @Override
    public Query<EntityStore> getQuery() {
        return QUERY;
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return DEPENDENCIES;
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store_, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull Damage damage) {
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        DummyComponent dummyComponent = archetypeChunk.getComponent(index, DummyComponent.getComponentType());
        TransformComponent transform = archetypeChunk.getComponent(index, TransformComponent.getComponentType());
        assert dummyComponent != null;
        assert transform != null;

        Instant currentTime = commandBuffer.getResource(TimeResource.getResourceType()).getNow();
        if (dummyComponent.getLastHit() != null && currentTime.isAfter(dummyComponent.getLastHit().plus(HIT_RESET_TIME))) {
            dummyComponent.setLastHit(null);
            dummyComponent.setNumberOfHits(0);
        }

        if (damage.getAmount() <= 0.0f) {
            return;
        }

        Damage.Source source = damage.getSource();
        if (!(source instanceof Damage.EntitySource entitySource)) {
            return;
        }

        Ref<EntityStore> sourceRef = entitySource.getRef();
        Player player = sourceRef.isValid() ? commandBuffer.getComponent(sourceRef, Player.getComponentType()) : null;
        if (player == null) {
            return;
        }

        boolean shouldDropItem = player.getGameMode() != GameMode.Creative;
        InventoryComponent.Hotbar hotbar = commandBuffer.getComponent(sourceRef, InventoryComponent.Hotbar.getComponentType());
        assert hotbar != null;
        ItemStack activeItem = hotbar.getActiveItem();
        if (activeItem != null) {
            dummyComponent.setLastHit(null);
            dummyComponent.setNumberOfHits(0);
            return;
        }

        dummyComponent.setNumberOfHits(dummyComponent.getNumberOfHits() + 1);
        dummyComponent.setLastHit(currentTime);

        if (dummyComponent.getNumberOfHits() >= NUMBER_OF_HITS) {
            commandBuffer.run(store -> {
                BlockType blockType = BlockType.getAssetMap().getAsset("Tinkering_Target_Dummy");
                if (blockType == null) {
                    return;
                }

                Vector3i blockPos = new Vector3i(transform.getPosition(), org.joml.RoundingMode.FLOOR);
                BreakBlockEvent event = new BreakBlockEvent(activeItem, blockPos, blockType);
                store.invoke(sourceRef, (EcsEvent) event);
                if (event.isCancelled()) {
                    return;
                }

                if (shouldDropItem && dummyComponent.getSourceItem() != null) {
                    Rotation3f rotation = transform.getRotation();
                    Holder<EntityStore> drop = ItemComponent.generateItemDrop(store, new ItemStack(dummyComponent.getSourceItem()), transform.getPosition(), rotation, 0.0f, 1.0f, 0.0f);
                    if (drop != null) {
                        store.addEntity(drop, AddReason.SPAWN);
                    }
                }

                if (ref.isValid()) {
                    store.removeEntity(ref, RemoveReason.REMOVE);
                }
            });
        }
    }
}
