package fr.varyon.vrpg.profession.mineur;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.vrpg.rpg.PlayerAccount;
import fr.varyon.vrpg.rpg.Profession;
import fr.varyon.vrpg.rpg.ProfessionManager;

import javax.annotation.Nullable;

public final class MaitriseMineurDamageSystem extends DamageEventSystem {

    private static final float FIRE_LAVA_REDUCTION = 0.30f;
    private static final float UNDERGROUND_REDUCTION = 0.15f;
    private static final int UNDERGROUND_THRESHOLD = 100;

    private final ProfessionManager professionManager;
    private final ComponentType<EntityStore, PlayerRef> playerRefType = PlayerRef.getComponentType();

    public MaitriseMineurDamageSystem(ProfessionManager professionManager) {
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
            if (acc == null || !acc.isActive(Profession.MINEUR)) return;

            DamageCause cause = damage.getCause();
            String causeId = cause != null ? cause.getId() : null;

            boolean isFireOrLava = "Fire".equals(causeId) || "Lava".equals(causeId)
                || "FireTick".equals(causeId) || "OnFire".equals(causeId);

            if (isFireOrLava && acc.getTalentRank(Profession.MINEUR, "bonus_0") > 0) {
                damage.setAmount(damage.getAmount() * (1.0f - FIRE_LAVA_REDUCTION));
                return;
            }

            if (acc.getTalentRank(Profession.MINEUR, "bonus_1") > 0) {
                Ref<EntityStore> ref = chunk.getReferenceTo(index);
                TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
                if (transform != null && transform.getPosition().y < UNDERGROUND_THRESHOLD) {
                    damage.setAmount(damage.getAmount() * (1.0f - UNDERGROUND_REDUCTION));
                }
            }
        } catch (Exception ignored) {}
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return playerRefType;
    }
}
