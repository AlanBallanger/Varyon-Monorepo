package com.varyon.rtpv;

import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;

/**
 * Listens for the confirm key (A / Ability1 slot) so a player can re-roll their
 * RTP position while the {@link RtpvConfirmHud} confirm window is active.
 */
public final class RtpvKeyFilter implements PlayerPacketFilter {

    @Override
    public boolean test(@Nonnull PlayerRef playerRef, @Nonnull Packet packet) {
        if (!(packet instanceof SyncInteractionChains sync)) return false;
        if (sync.updates == null || sync.updates.length == 0) return false;

        for (SyncInteractionChain chain : sync.updates) {
            if (chain == null || !chain.initial) continue;
            if (chain.interactionType != InteractionType.Ability1) continue;

            RtpvConfirmManager mgr = RtpvConfirmManager.getInstance();
            if (mgr == null) continue;
            RtpvConfirmManager.PendingConfirm pending = mgr.getPendingConfirm(playerRef.getUuid());
            if (pending == null || pending.isExpired()) continue;

            RtpvRetryService.handleConfirmKeyPress(playerRef);
        }
        return false;
    }
}
