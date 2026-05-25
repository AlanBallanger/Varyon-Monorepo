package com.varyon.varyonui.input;

import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interface_.ChatMessage;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketFilter;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.varyon.varyonui.config.AccueilShortcutConfig;

import javax.annotation.Nonnull;
import java.util.Locale;
import java.util.UUID;

public final class VaryonAccueilOKeyFilter implements PlayerPacketFilter {

    @Override
    public boolean test(@Nonnull PlayerRef playerRef, @Nonnull Packet packet) {
        UUID uuid = playerRef.getUuid();
        if (uuid == null || AccueilShortcutConfig.getInstance().getMode(uuid) != AccueilShortcutConfig.Mode.O) {
            return false;
        }
        if (packet instanceof ChatMessage cm) {
            String text = extractChatText(cm);
            if (!isGmOpenKeyShortcut(text)) {
                return false;
            }
            VaryonAccueilKeyHelper.runAccueilOpenKeyDebounced(playerRef);
            return true;
        }
        if (!(packet instanceof SyncInteractionChains sync)) {
            return false;
        }
        if (sync.updates == null || sync.updates.length == 0) {
            return false;
        }
        for (SyncInteractionChain chain : sync.updates) {
            if (chain == null) {
                continue;
            }
            if (chain.interactionType == InteractionType.GameModeSwap) {
                VaryonAccueilKeyHelper.runAccueilOpenKeyDebounced(playerRef);
                return true;
            }
        }
        return false;
    }

    private static String extractChatText(ChatMessage cm) {
        return cm.message;
    }

    private static boolean isGmOpenKeyShortcut(String raw) {
        if (raw == null) {
            return false;
        }
        String s = raw.trim();
        if (s.startsWith("/")) {
            s = s.substring(1).trim();
        }
        String lower = s.toLowerCase(Locale.ROOT);
        return "gm a".equals(lower) || "gm c".equals(lower);
    }
}
