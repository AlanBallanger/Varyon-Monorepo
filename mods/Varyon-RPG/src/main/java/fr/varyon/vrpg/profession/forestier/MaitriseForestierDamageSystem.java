package fr.varyon.vrpg.profession.forestier;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.vrpg.rpg.PlayerAccount;
import fr.varyon.vrpg.rpg.Profession;
import fr.varyon.vrpg.rpg.ProfessionManager;
import fr.varyon.vrpg.world.EnvironmentUtil;

import javax.annotation.Nullable;

public final class MaitriseForestierDamageSystem extends DamageEventSystem {

    private static final float ZONE_REDUCTION = 0.15f;

    private final ProfessionManager professionManager;
    private final ComponentType<EntityStore, PlayerRef> playerRefType = PlayerRef.getComponentType();

    public MaitriseForestierDamageSystem(ProfessionManager professionManager) {
        this.professionManager = professionManager;
    }

    @Override
    public void handle(int index,
                       ArchetypeChunk<EntityStore> chunk,
                       Store<EntityStore> store,
                       CommandBuffer<EntityStore> commandBuffer,
                       Damage damage) {
        try {
            PlayerRef playerRef = chunk.getComponent(index, playerRefType);
            if (playerRef == null) return;

            PlayerAccount acc = professionManager.getAccount(playerRef.getUuid());
            if (acc == null || !acc.isActive(Profession.FORESTIER)) return;
            if (acc.getTalentRank(Profession.FORESTIER, "bonus_1") <= 0) return;

            Ref<EntityStore> ref = chunk.getReferenceTo(index);
            TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
            String envId = transform != null ? EnvironmentUtil.getEnvironmentId(ref, transform, store) : null;

            if (EnvironmentUtil.isZone1(envId) || EnvironmentUtil.isZone2(envId)) {
                damage.setAmount(damage.getAmount() * (1.0f - ZONE_REDUCTION));
            }
        } catch (Exception ignored) {}
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return playerRefType;
    }
}
