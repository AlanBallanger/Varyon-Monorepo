package fr.varyon.quiver;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.ProjectileComponent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;

public final class QuiverDamageBonusSystem extends DamageEventSystem {
    private static final String QUIVER_UTILITY = "Utility_Leather_Quiver";
    private static final String QUIVER_LIGHT = "Light_Leather_Quiver";
    private static final String QUIVER_MEDIUM = "Medium_Leather_Quiver";
    private static final String QUIVER_HEAVY = "Heavy_Leather_Quiver";

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
    }

    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        HashSet<Dependency<EntityStore>> deps = new HashSet<>();
        deps.add(new SystemDependency(Order.AFTER, DamageSystems.ArmorDamageReduction.class));
        deps.add(new SystemDependency(Order.AFTER, DamageSystems.WieldingDamageReduction.class));
        deps.add(new SystemDependency(Order.BEFORE, DamageSystems.ApplyDamage.class));
        deps.add(new SystemDependency(Order.BEFORE, DamageSystems.EntityUIEvents.class));
        return deps;
    }

    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull Damage event) {
        if (event.getCause() != DamageCause.PROJECTILE) {
            return;
        }
        Damage.Source source = event.getSource();
        if (!(source instanceof Damage.EntitySource entitySource)) {
            return;
        }
        Ref sourceRef = entitySource.getRef();
        Player attacker = (Player) store.getComponent(sourceRef, Player.getComponentType());
        if (attacker == null) {
            return;
        }
        InventoryComponent.Utility utilityComponent = (InventoryComponent.Utility) store.getComponent(sourceRef, InventoryComponent.Utility.getComponentType());
        ItemContainer utilityContainer = utilityComponent != null ? utilityComponent.getInventory() : null;
        UtilityQuiver utilityQuiver = this.findUtilityQuiver(utilityContainer);
        float offhandBonus = utilityQuiver.bonus();
        if (offhandBonus <= 0.0f) {
            return;
        }
        ProjectileInfo projectileInfo = this.getProjectileInfo(store, source, (Ref<EntityStore>) sourceRef);
        if (!projectileInfo.isBowShot()) {
            return;
        }
        float before = event.getAmount();
        event.setAmount(before * (1.0f + offhandBonus));
    }

    private ProjectileInfo getProjectileInfo(Store<EntityStore> store, Damage.Source source, Ref<EntityStore> sourceRef) {
        String projectileAssetName = null;
        boolean arrowProjectile = false;
        if (source instanceof Damage.ProjectileSource projectileSource) {
            Ref projectileRef = projectileSource.getProjectile();
            if (projectileRef == null) {
                return new ProjectileInfo(null, null, false, false);
            }
            ProjectileComponent projectile = (ProjectileComponent) store.getComponent(projectileRef, ProjectileComponent.getComponentType());
            if (projectile == null) {
                return new ProjectileInfo(null, null, false, false);
            }
            projectileAssetName = projectile.getProjectileAssetName();
            if (projectileAssetName != null) {
                arrowProjectile = projectileAssetName.toLowerCase().contains("arrow");
            }
        }
        InventoryComponent.Hotbar hotbar = (InventoryComponent.Hotbar) store.getComponent(sourceRef, InventoryComponent.Hotbar.getComponentType());
        ItemStack inHand = hotbar != null ? hotbar.getActiveItem() : null;
        String inHandId = ItemStack.isEmpty(inHand) ? null : inHand.getItemId();
        boolean bowInHand = inHandId != null && inHandId.toLowerCase().contains("bow");
        return new ProjectileInfo(inHandId, projectileAssetName, arrowProjectile, arrowProjectile || bowInHand);
    }

    private float getQuiverBonus(String id) {
        if (id == null) {
            return 0.0f;
        }
        return switch (id) {
            case QUIVER_HEAVY -> 0.15f;
            case QUIVER_MEDIUM -> 0.1f;
            case QUIVER_LIGHT, QUIVER_UTILITY -> 0.05f;
            default -> 0.0f;
        };
    }

    private UtilityQuiver findUtilityQuiver(ItemContainer utilityContainer) {
        if (utilityContainer == null) {
            return new UtilityQuiver(null, 0.0f);
        }
        float bestBonus = 0.0f;
        String bestId = null;
        short capacity = utilityContainer.getCapacity();
        for (short slot = 0; slot < capacity; slot++) {
            ItemStack stack = utilityContainer.getItemStack(slot);
            if (ItemStack.isEmpty(stack)) {
                continue;
            }
            String itemId = stack.getItemId();
            float bonus = this.getQuiverBonus(itemId);
            if (bonus > bestBonus) {
                bestBonus = bonus;
                bestId = itemId;
            }
        }
        return new UtilityQuiver(bestId, bestBonus);
    }

    private record UtilityQuiver(String itemId, float bonus) {
    }

    private record ProjectileInfo(String inHandItemId, String projectileAssetName, boolean isArrowProjectile, boolean isBowShot) {
    }
}
