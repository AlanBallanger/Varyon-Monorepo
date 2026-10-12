package com.varyon.elite;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.elite.component.EliteComponent;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Applies / removes an elite's <b>class passive</b> as an infinite {@link EntityEffect}
 * ({@link MobClass#getPassiveEffectId()}). Each class has exactly one passive effect (or none,
 * for the Berserker whose enrage is handled in code). The applied effect index is recorded on
 * {@link EliteComponent} so de-elite can remove exactly it.
 */
public final class EliteEffects {

    private static final com.hypixel.hytale.logger.HytaleLogger LOGGER =
            com.hypixel.hytale.logger.HytaleLogger.forEnclosingClass();

    private EliteEffects() {}

    public static void apply(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                             @Nonnull EliteComponent elite) {
        String effectId = elite.getMobClass().getPassiveEffectId();
        if (effectId == null || effectId.isBlank()) {
            elite.setEffectIndices(List.of());
            return;
        }
        EffectControllerComponent ec = store.getComponent(ref, EffectControllerComponent.getComponentType());
        if (ec == null) {
            return;
        }
        List<Integer> applied = new ArrayList<>(1);
        try {
            int idx = EntityEffect.getAssetMap().getIndex(effectId);
            if (idx >= 0) {
                EntityEffect effect = EntityEffect.getAssetMap().getAsset(idx);
                if (effect != null && ec.addInfiniteEffect(ref, idx, effect, store)) {
                    applied.add(idx);
                }
            }
        } catch (Exception e) {
            LOGGER.at(Level.FINE).log("elite passive apply failed (" + effectId + "): " + e.getMessage());
        }
        elite.setEffectIndices(applied);
    }

    public static void strip(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                             @Nonnull List<Integer> effectIndices) {
        if (effectIndices.isEmpty()) {
            return;
        }
        EffectControllerComponent ec = store.getComponent(ref, EffectControllerComponent.getComponentType());
        if (ec == null) {
            return;
        }
        for (int idx : effectIndices) {
            try {
                ec.removeEffect(ref, idx, store);
            } catch (Exception ignored) {
            }
        }
    }
}
