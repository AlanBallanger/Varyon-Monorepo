package fr.varyon.vrpg.classes.rodeur;

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

public final class RodeurTrapRootSystem extends EntityTickingSystem<EntityStore> {

    private static final String ROOT_EFFECT_ID = "Vrpg_Ombre_Root";
    private static final int    TICK_INTERVAL   = 10;

    private int tickCounter = 0;
    @Nullable private EntityEffect rootEffect;

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
        if (!RodeurTrapHelper.hasActiveTrap()) return;

        try {
            Ref<EntityStore> ref = chunk.getReferenceTo(index);
            TransformComponent tc = store.getComponent(ref, TransformComponent.getComponentType());
            if (tc == null) return;
            RodeurTrapHelper.ActiveTrap trap = RodeurTrapHelper.trapAt(tc.getPosition());
            if (trap == null) return;

            EntityEffect effect = resolveRootEffect();
            if (effect == null) return;

            EffectControllerComponent ec =
                store.getComponent(ref, EffectControllerComponent.getComponentType());
            if (ec == null) return;
            ec.addEffect(ref, effect, trap.rootMs() / 1000f, OverlapBehavior.OVERWRITE, store);

            var world = store.getExternalData().getWorld();
            if (world != null) RodeurTrapHelper.onTriggered(world, trap);
        } catch (Exception ignored) {}
    }

    @Nullable
    private EntityEffect resolveRootEffect() {
        if (rootEffect != null) return rootEffect;
        try {
            int idx = EntityEffect.getAssetMap().getIndex(ROOT_EFFECT_ID);
            if (idx < 0) return null;
            rootEffect = (EntityEffect) EntityEffect.getAssetMap().getAsset(idx);
        } catch (Exception ignored) {}
        return rootEffect;
    }
}
