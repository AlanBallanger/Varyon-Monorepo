package com.varyon.progression.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.effect.ActiveEntityEffect;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.progression.config.ArmorRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ArmorProgressionSystem extends DamageEventSystem {

    private int getRequiredLevel(String regionName) {
        if (regionName.startsWith("Zone2_")) return 1;
        if (regionName.startsWith("Zone3_")) return 2;
        if (regionName.startsWith("Zone4_")) return 3;
        return 0;
    }

    @Override
    public void handle(int i, @NotNull ArchetypeChunk<EntityStore> archetypeChunk, @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> commandBuffer, @NotNull Damage damage) {
        var source = damage.getSource();
        var ref = archetypeChunk.getReferenceTo(i);
        var player = store.getComponent(ref, Player.getComponentType());
        assert player != null;

        if (source == Damage.NULL_SOURCE || source instanceof Damage.EnvironmentSource) {
            return;
        } else if (source instanceof ActiveEntityEffect) {
            return;
        } else if (source instanceof Damage.CommandSource) {
            return;
        }

        if (source instanceof Damage.EntitySource entitySource) {
            if (store.getComponent(entitySource.getRef(), Player.getComponentType()) != null) {
                return;
            }
        }

        var zone = player.getWorldMapTracker().getCurrentZone();
        if (zone == null) {
            return;
        }

        var armor = player.getInventory().getArmor();
        var tierHelmet = ArmorRegistry.getArmorLevel(armor.getItemStack((short) 0));
        var tierChest  = ArmorRegistry.getArmorLevel(armor.getItemStack((short) 1));
        var tierGloves = ArmorRegistry.getArmorLevel(armor.getItemStack((short) 2));
        var tierLegs   = ArmorRegistry.getArmorLevel(armor.getItemStack((short) 3));

        int desiredTier = getRequiredLevel(zone.regionName());
        float multiplier = 1f;

        if (tierHelmet < desiredTier) multiplier += 0.30f;
        if (tierChest  < desiredTier) multiplier += 0.50f;
        if (tierGloves < desiredTier) multiplier += 0.20f;
        if (tierLegs   < desiredTier) multiplier += 0.50f;

        damage.setAmount(damage.getAmount() * multiplier);
    }

    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getGatherDamageGroup();
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Player.getComponentType();
    }
}
