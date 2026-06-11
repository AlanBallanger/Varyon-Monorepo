package fr.varyon.vrpg.classes.berserker;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.vrpg.classes.ClassAccount;
import fr.varyon.vrpg.classes.ClassManager;
import fr.varyon.vrpg.classes.PlayerClass;
import fr.varyon.vrpg.classes.PlayerSpecialization;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BerserkerTickSystem extends EntityTickingSystem<EntityStore> {

    private static final int CHECK_INTERVAL = 2;

    private final ClassManager classManager;
    private final BerserkerState berserkerState;
    private final BerserkerCombatTracker combatTracker;
    private final ComponentType<EntityStore, PlayerRef> playerRefType = PlayerRef.getComponentType();
    private final Map<UUID, Integer> tickCounters = new ConcurrentHashMap<>();
    private final Map<UUID, Float>   appliedSpeedBoost = new ConcurrentHashMap<>();

    public BerserkerTickSystem(@Nonnull ClassManager classManager,
                                @Nonnull BerserkerState berserkerState,
                                @Nonnull BerserkerCombatTracker combatTracker) {
        this.classManager   = classManager;
        this.berserkerState = berserkerState;
        this.combatTracker  = combatTracker;
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return playerRefType;
    }

    @Override
    public void tick(float deltaTime, int index,
                     ArchetypeChunk<EntityStore> chunk,
                     Store<EntityStore> store,
                     CommandBuffer<EntityStore> commandBuffer) {
        PlayerRef playerRef = chunk.getComponent(index, playerRefType);
        if (playerRef == null) return;
        UUID uuid = playerRef.getUuid();

        int tc = tickCounters.merge(uuid, 1, Integer::sum);
        if (tc % CHECK_INTERVAL != 0) return;

        ClassAccount acc = classManager.getAccount(uuid);
        if (acc == null) return;
        if (acc.getActiveClass() != PlayerClass.BARBARE) return;
        if (acc.getActiveSpec(PlayerClass.BARBARE) != PlayerSpecialization.BERSERKER) return;

        boolean inCombat = combatTracker.isInCombat(uuid);

        // --- Ferveur Guerrière : tick un stack par seconde en combat ---
        int ferveurRank = acc.getTalentRank(PlayerClass.BARBARE, BerserkerPassifs.FERVEUR_NODE);
        if (ferveurRank > 0) {
            if (inCombat) {
                berserkerState.tickFerveur(uuid, BerserkerPassifs.FERVEUR_MAX_STACKS);
            } else {
                berserkerState.resetFerveur(uuid);
            }
        }

        // --- Frénésie : vitesse de déplacement ---
        int frenesieRank = acc.getTalentRank(PlayerClass.BARBARE, BerserkerPassifs.FRENESIE_NODE);
        float targetSpeedBoost = 0f;
        if (frenesieRank > 0) {
            int stacks = berserkerState.getFrenesieStacks(uuid);
            if (stacks > 0) {
                targetSpeedBoost = stacks * BerserkerPassifs.frenesieSpdBonusPerStack(frenesieRank);
            }
        }

        float applied = appliedSpeedBoost.getOrDefault(uuid, 0f);
        if (Math.abs(targetSpeedBoost - applied) > 0.001f) {
            try {
                com.hypixel.hytale.component.Ref<EntityStore> ref = chunk.getReferenceTo(index);
                MovementManager mm = store.getComponent(ref, MovementManager.getComponentType());
                if (mm != null) {
                    if (targetSpeedBoost > 0f) {
                        float target = mm.getDefaultSettings().baseSpeed * (1f + targetSpeedBoost);
                        mm.getSettings().baseSpeed = target;
                    } else {
                        mm.resetDefaultsAndUpdate(ref, store);
                    }
                    mm.update(playerRef.getPacketHandler());
                    appliedSpeedBoost.put(uuid, targetSpeedBoost);
                }
            } catch (Exception ignored) {}
        }
    }

    public void removePlayer(@Nonnull UUID uuid) {
        tickCounters.remove(uuid);
        appliedSpeedBoost.remove(uuid);
        berserkerState.resetFerveur(uuid);
    }
}
