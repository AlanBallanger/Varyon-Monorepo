package fr.varyon.vrpg.profession.chasseur;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.vrpg.rpg.PlayerAccount;
import fr.varyon.vrpg.rpg.Profession;
import fr.varyon.vrpg.rpg.ProfessionManager;
import fr.varyon.vrpg.world.EnvironmentUtil;

import javax.annotation.Nullable;

public final class MaitriseChasseurDamageSystem extends DamageEventSystem {

    private static final float NIGHT_REDUCTION = 0.15f;

    private final ProfessionManager professionManager;
    private final ComponentType<EntityStore, PlayerRef> playerRefType = PlayerRef.getComponentType();

    public MaitriseChasseurDamageSystem(ProfessionManager professionManager) {
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
            if (fr.varyon.vrpg.rpg.CreativeGate.isCreative(playerRef)) return;

            PlayerAccount acc = professionManager.getAccount(playerRef.getUuid());
            if (acc == null || !acc.isActive(Profession.CHASSEUR)) return;
            if (acc.getTalentRank(Profession.CHASSEUR, "bonus_0") <= 0) return;

            if (EnvironmentUtil.isNight(store)) {
                damage.setAmount(damage.getAmount() * (1.0f - NIGHT_REDUCTION));
            }
        } catch (Exception ignored) {}
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return playerRefType;
    }
}
