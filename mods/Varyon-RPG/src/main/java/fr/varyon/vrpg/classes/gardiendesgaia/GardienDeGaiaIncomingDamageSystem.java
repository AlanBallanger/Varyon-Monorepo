package fr.varyon.vrpg.classes.gardiendesgaia;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import fr.varyon.vrpg.classes.ClassAccount;
import fr.varyon.vrpg.classes.ClassManager;
import fr.varyon.vrpg.classes.PlayerClass;
import fr.varyon.vrpg.classes.PlayerSpecialization;

import javax.annotation.Nonnull;
import java.util.UUID;

public final class GardienDeGaiaIncomingDamageSystem extends DamageEventSystem {

    private final ClassManager classManager;
    private final GardienDeGaiaState state;

    public GardienDeGaiaIncomingDamageSystem(@Nonnull ClassManager classManager,
                                              @Nonnull GardienDeGaiaState state) {
        this.classManager = classManager;
        this.state        = state;
    }

    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Player.getComponentType();
    }

    @Override
    public void handle(int index,
                       @Nonnull ArchetypeChunk<EntityStore> chunk,
                       @Nonnull Store<EntityStore> store,
                       @Nonnull CommandBuffer<EntityStore> commandBuffer,
                       @Nonnull Damage damage) {
        try {
            if (damage.isCancelled() || damage.getAmount() <= 0f) return;

            PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
            if (playerRef == null) return;

            UUID uuid = playerRef.getUuid();
            ClassAccount acc = classManager.getOrLoad(uuid);
            if (acc.getActiveClass() != PlayerClass.MAGE) return;
            if (acc.getActiveSpec(PlayerClass.MAGE) != PlayerSpecialization.GARDIEN_DE_GAIA) return;

            // Écorce Protectrice : réduit les dégâts reçus
            float reduction = state.getEcorceReduction(uuid);
            if (reduction > 0f) {
                damage.setAmount(damage.getAmount() * (1f - reduction));
            }

            // Marque de Renaissance : absorbe les dégâts fatals
            if (damage.getAmount() >= getPlayerHp(chunk, index, store)) {
                if (state.consumeMarque(uuid)) {
                    damage.setAmount(0f);
                    // Restaure le joueur à 30% PV
                    try {
                        EntityStatMap sm = store.getComponent(chunk.getReferenceTo(index), EntityStatMap.getComponentType());
                        if (sm != null) {
                            int hIdx = DefaultEntityStatTypes.getHealth();
                            var hp = sm.get(hIdx);
                            if (hp != null) sm.setStatValue(hIdx, hp.getMax() * 0.30f);
                        }
                    } catch (Exception ignored2) {}
                }
            }

        } catch (Exception ignored) {}
    }

    private float getPlayerHp(ArchetypeChunk<EntityStore> chunk, int index, Store<EntityStore> store) {
        try {
            EntityStatMap sm = store.getComponent(chunk.getReferenceTo(index), EntityStatMap.getComponentType());
            if (sm == null) return Float.MAX_VALUE;
            int hIdx = DefaultEntityStatTypes.getHealth();
            var hp = sm.get(hIdx);
            return hp != null ? hp.get() : Float.MAX_VALUE;
        } catch (Exception e) {
            return Float.MAX_VALUE;
        }
    }
}
