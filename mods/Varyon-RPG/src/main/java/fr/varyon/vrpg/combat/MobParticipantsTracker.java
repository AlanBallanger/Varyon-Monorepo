package fr.varyon.vrpg.combat;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MobParticipantsTracker {

    public record Entry(UUID playerUuid, Ref<EntityStore> playerRef) {}

    private final ConcurrentHashMap<Integer, ConcurrentHashMap<UUID, Entry>> participants
        = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, UUID> handledByPrediction = new ConcurrentHashMap<>();

    public final class AttackerRecorder extends DamageEventSystem {

        @Override
        public SystemGroup<EntityStore> getGroup() {
            return DamageModule.get().getFilterDamageGroup();
        }

        @Override
        @Nonnull
        public Query<EntityStore> getQuery() {
            return NPCEntity.getComponentType();
        }

        @Override
        public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> chunk,
                           @Nonnull Store<EntityStore> store,
                           @Nonnull CommandBuffer<EntityStore> commandBuffer,
                           @Nonnull Damage damage) {
            if (damage.isCancelled() || damage.getAmount() <= 0f) return;
            Damage.Source source = damage.getSource();
            if (!(source instanceof Damage.EntitySource entitySource)) return;

            Ref<EntityStore> attackerRef = entitySource.getRef();
            if (attackerRef == null || !attackerRef.isValid()) return;

            Player player = store.getComponent(attackerRef, Player.getComponentType());
            if (player == null) player = commandBuffer.getComponent(attackerRef, Player.getComponentType());
            if (player == null) return;

            PlayerRef playerRef = store.getComponent(attackerRef, PlayerRef.getComponentType());
            if (playerRef == null) playerRef = commandBuffer.getComponent(attackerRef, PlayerRef.getComponentType());
            if (playerRef == null) return;

            Ref<EntityStore> victimRef = chunk.getReferenceTo(index);
            UUID uuid = playerRef.getUuid();
            participants
                .computeIfAbsent(System.identityHashCode(victimRef), k -> new ConcurrentHashMap<>())
                .put(uuid, new Entry(uuid, attackerRef));
        }
    }

    public void markHandledByPrediction(@Nonnull Ref<EntityStore> victimRef, @Nonnull UUID playerId) {
        handledByPrediction.put(System.identityHashCode(victimRef), playerId);
    }

    @Nullable
    public ConcurrentHashMap<UUID, Entry> removeParticipants(@Nonnull Ref<EntityStore> victimRef) {
        return participants.remove(System.identityHashCode(victimRef));
    }

    @Nullable
    public UUID removeHandledByPrediction(@Nonnull Ref<EntityStore> victimRef) {
        return handledByPrediction.remove(System.identityHashCode(victimRef));
    }

    public AttackerRecorder createRecorderSystem() {
        return new AttackerRecorder();
    }
}
