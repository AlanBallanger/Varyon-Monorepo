package fr.varyon.vrpg.classes.ability;

import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import fr.varyon.vrpg.classes.ClassAccount;
import fr.varyon.vrpg.classes.ClassManager;
import fr.varyon.vrpg.classes.PlayerClass;
import fr.varyon.vrpg.classes.PlayerSpecialization;
import fr.varyon.vrpg.classes.rempart.RempartState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ClassSkillKeyFilter implements PlayerPacketFilter {

    private final ClassManager classManager;
    private final RempartState rempartState;

    public ClassSkillKeyFilter(@Nonnull ClassManager classManager, @Nonnull RempartState rempartState) {
        this.classManager = classManager;
        this.rempartState = rempartState;
    }

    @Override
    public boolean test(@Nonnull PlayerRef playerRef, @Nonnull Packet packet) {
        if (!(packet instanceof SyncInteractionChains sync)) return false;
        if (sync.updates == null || sync.updates.length == 0) return false;

        boolean blockPacket = false;
        for (SyncInteractionChain chain : sync.updates) {
            if (chain == null || !chain.initial) continue;

            if (chain.interactionType == InteractionType.Secondary) {
                onSecondary(playerRef);
                continue;
            }

            InteractionType type = toAbilityType(chain);
            if (type == null) continue;
            if (isArbaletrier(playerRef)) blockPacket = true;
            ClassSkillSlots.tryCastAbility(playerRef, type);
        }
        return blockPacket;
    }

    private boolean isArbaletrier(@Nonnull PlayerRef playerRef) {
        try {
            java.util.UUID uuid = playerRef.getUuid();
            if (uuid == null) return false;
            ClassAccount acc = classManager.getAccount(uuid);
            if (acc == null) return false;
            return acc.getActiveClass() == PlayerClass.TIREUR
                && acc.getActiveSpec(PlayerClass.TIREUR) == PlayerSpecialization.ARBALETRIER;
        } catch (Exception e) { return false; }
    }

    private void onSecondary(@Nonnull PlayerRef playerRef) {
        try {
            java.util.UUID uuid = playerRef.getUuid();
            if (uuid == null) return;
            ClassAccount acc = classManager.getAccount(uuid);
            if (acc == null) return;
            if (acc.getActiveClass() != PlayerClass.GUERRIER) return;
            if (acc.getActiveSpec(PlayerClass.GUERRIER) != PlayerSpecialization.REMPART) return;
            rempartState.markShieldRaised(uuid);
        } catch (Exception ignored) {}
    }

    @Nullable
    private static InteractionType toAbilityType(@Nullable SyncInteractionChain chain) {
        if (chain == null || chain.data == null) return null;
        return switch (chain.interactionType) {
            case Ability1, Ability2, Ability3 -> chain.interactionType;
            default -> null;
        };
    }
}
