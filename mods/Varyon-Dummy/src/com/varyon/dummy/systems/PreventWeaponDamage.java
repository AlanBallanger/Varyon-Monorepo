package com.varyon.dummy.systems;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemGroupDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.ItemUtils;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import com.varyon.dummy.DummyComponent;

import javax.annotation.Nonnull;

import java.util.Set;

public class PreventWeaponDamage extends DamageEventSystem {

    @Nonnull
    private static final Query<EntityStore> QUERY = Archetype.of(
            DummyComponent.getComponentType(),
            TransformComponent.getComponentType()
    );

    @Nonnull
    private static final Set<Dependency<EntityStore>> DEPENDENCIES = Set.of(
            new SystemGroupDependency(Order.AFTER, DamageModule.get().getGatherDamageGroup()),
            new SystemGroupDependency(Order.AFTER, DamageModule.get().getFilterDamageGroup()),
            new SystemGroupDependency(Order.AFTER, DamageModule.get().getInspectDamageGroup())
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
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull Damage damage) {
        Damage.Source source = damage.getSource();
        if (!(source instanceof Damage.EntitySource entitySource)) {
            return;
        }

        Ref<EntityStore> sourceRef = entitySource.getRef();
        Player player = sourceRef.isValid() ? commandBuffer.getComponent(sourceRef, Player.getComponentType()) : null;
        if (player == null) {
            return;
        }

        InventoryComponent.Hotbar hotbar = commandBuffer.getComponent(sourceRef, InventoryComponent.Hotbar.getComponentType());
        assert hotbar != null;
        ItemStack activeItem = hotbar.getActiveItem();
        if (activeItem == null) {
            return;
        }

        DamageCause cause = damage.getCause();
        if (cause == null || !cause.isDurabilityLoss()) {
            return;
        }

        Item item = activeItem.getItem();
        if (item.getWeapon() == null || !ItemUtils.canDecreaseItemStackDurability(sourceRef, commandBuffer)) {
            return;
        }

        ItemUtils.updateItemStackDurability(sourceRef, activeItem, hotbar.getInventory(), hotbar.getActiveSlot(), item.getDurabilityLossOnHit(), commandBuffer);
    }
}
