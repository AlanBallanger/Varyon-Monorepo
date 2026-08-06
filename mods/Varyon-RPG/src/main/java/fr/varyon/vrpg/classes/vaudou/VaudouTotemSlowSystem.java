package fr.varyon.vrpg.classes.vaudou;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.OverlapBehavior;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class VaudouTotemSlowSystem extends EntityTickingSystem<EntityStore> {

    private static final String SLOW_EFFECT_ID = "Vrpg_Totem_Entrave_Slow";
    private static final float  SLOW_REFRESH_SEC = 1.5f;
    private static final int    TICK_INTERVAL = 10;

    private int tickCounter = 0;
    @Nullable private EntityEffect slowEffect;

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return NPCEntity.getComponentType();
    }

    @Override
    public void tick(float deltaTime, int index,
                     ArchetypeChunk<EntityStore> chunk,
                     Store<EntityStore> store,
                     CommandBuffer<EntityStore> commandBuffer) {
        if (++tickCounter < TICK_INTERVAL) return;
        tickCounter = 0;
        if (!VaudouTotemHelper.hasActiveSlownessTotem()) return;

        try {
            Ref<EntityStore> ref = chunk.getReferenceTo(index);
            TransformComponent tc = store.getComponent(ref, TransformComponent.getComponentType());
            if (tc == null || !VaudouTotemHelper.isInSlownessTotem(tc.getPosition())) return;

            EntityEffect effect = resolveSlowEffect();
            if (effect == null) return;

            EffectControllerComponent ec =
                store.getComponent(ref, EffectControllerComponent.getComponentType());
            if (ec == null) return;
            ec.addEffect(ref, effect, SLOW_REFRESH_SEC, OverlapBehavior.OVERWRITE, store);
        } catch (Exception ignored) {}
    }

    @Nullable
    private EntityEffect resolveSlowEffect() {
        if (slowEffect != null) return slowEffect;
        try {
            int idx = EntityEffect.getAssetMap().getIndex(SLOW_EFFECT_ID);
            if (idx < 0) return null;
            slowEffect = (EntityEffect) EntityEffect.getAssetMap().getAsset(idx);
        } catch (Exception ignored) {}
        return slowEffect;
    }
}
