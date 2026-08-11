package com.varyon.bossarena.compat;

import com.varyon.component.MobScalingComponent;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Hard references to Varyon types. Loaded only when {@link VaryonMobScale} confirms Varyon is present.
 */
final class VaryonMobScaleBridge {

    private VaryonMobScaleBridge() {}

    static VaryonMobScale.Scale read(Store<EntityStore> store, Ref<EntityStore> entityRef) {
        if (MobScalingComponent.getComponentType() == null) {
            return VaryonMobScale.Scale.NONE;
        }
        MobScalingComponent scaling = store.getComponent(entityRef, MobScalingComponent.getComponentType());
        if (scaling == null) {
            return VaryonMobScale.Scale.NONE;
        }
        return new VaryonMobScale.Scale(
                scaling.getHealthMultiplier(),
                scaling.getDamageMultiplier(),
                scaling.getMobLevel()
        );
    }

    /**
     * Keeps level / loot / essence for nameplates and Varyon side systems, but zeroes combat
     * multipliers so BossArena (which already absorbed them) is the sole combat scaler.
     */
    static void neutralizeCombatMultipliers(Store<EntityStore> store, Ref<EntityStore> entityRef) {
        if (MobScalingComponent.getComponentType() == null) {
            return;
        }
        MobScalingComponent scaling = store.getComponent(entityRef, MobScalingComponent.getComponentType());
        if (scaling == null) {
            return;
        }
        if (Math.abs(scaling.getHealthMultiplier() - 1.0f) < 0.0001f
                && Math.abs(scaling.getDamageMultiplier() - 1.0f) < 0.0001f) {
            return;
        }
        MobScalingComponent neutralized = new MobScalingComponent(
                scaling.getMobLevel(),
                1.0f,
                1.0f,
                scaling.getLootMultiplier(),
                scaling.getPointsMultiplier()
        );
        store.putComponent(entityRef, MobScalingComponent.getComponentType(), neutralized);
    }

    /**
     * Zeroes Varyon's loot and essence multipliers. Arena wave mobs must drop nothing at all, and
     * their death interaction alone does not stop Varyon-side drops.
     */
    static void zeroLootAndEssence(Store<EntityStore> store, Ref<EntityStore> entityRef) {
        if (MobScalingComponent.getComponentType() == null) {
            return;
        }
        MobScalingComponent scaling = store.getComponent(entityRef, MobScalingComponent.getComponentType());
        if (scaling == null) {
            return;
        }
        if (scaling.getLootMultiplier() == 0.0f && scaling.getPointsMultiplier() == 0.0f) {
            return;
        }
        MobScalingComponent stripped = new MobScalingComponent(
                scaling.getMobLevel(),
                scaling.getHealthMultiplier(),
                scaling.getDamageMultiplier(),
                0.0f,
                0.0f
        );
        store.putComponent(entityRef, MobScalingComponent.getComponentType(), stripped);
    }
}
