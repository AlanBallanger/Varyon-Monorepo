package fr.varyon.vrpg.classes.ability;

import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ClassSkillKeyFilter implements PlayerPacketFilter {

    @Override
    public boolean test(@Nonnull PlayerRef playerRef, @Nonnull Packet packet) {
        if (!(packet instanceof SyncInteractionChains sync)) return false;
        if (sync.updates == null || sync.updates.length == 0) return false;

        for (SyncInteractionChain chain : sync.updates) {
            InteractionType type = toAbilityType(chain);
            if (type == null) continue;
            ClassSkillSlots.tryCastAbility(playerRef, type);
        }
        return false;
    }

    @Nullable
    private static InteractionType toAbilityType(@Nullable SyncInteractionChain chain) {
        if (chain == null || chain.data == null || !chain.initial) return null;
        return switch (chain.interactionType) {
            case Ability1, Ability2, Ability3 -> chain.interactionType;
            default -> null;
        };
    }
}
