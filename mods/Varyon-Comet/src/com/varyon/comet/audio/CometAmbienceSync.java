package com.varyon.comet.audio;

import com.hypixel.hytale.builtin.audio.components.ForcedMusicTracker;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.ToClientPacket;
import com.hypixel.hytale.protocol.packets.world.UpdateForcedMusic;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.comet.CometConfig;

import java.util.logging.Logger;

public final class CometAmbienceSync {

    private static final Logger LOGGER = Logger.getLogger(CometAmbienceSync.class.getName());

    private CometAmbienceSync() {
    }

    public static int pushForcedMusicToAllPlayers(Store<EntityStore> store, int containerIndex, String ambienceIdForLog) {
        if (store == null) {
            return 0;
        }
        try {
            com.hypixel.hytale.server.core.universe.world.World world = ((EntityStore) store.getExternalData()).getWorld();
            if (world == null) {
                LOGGER.warning("[CometAmbience] push skipped: World null");
                return 0;
            }
            int sent = 0;
            int viaTracker = 0;
            int viaDirectPacket = 0;
            int skippedBadRef = 0;
            int playerRefs = 0;
            for (PlayerRef pr : world.getPlayerRefs()) {
                playerRefs++;
                Ref<EntityStore> pref = pr.getReference();
                if (pref == null || !pref.isValid()) {
                    skippedBadRef++;
                    continue;
                }
                ForcedMusicTracker tracker = store.getComponent(pref, ForcedMusicTracker.getComponentType());
                if (tracker != null) {
                    tracker.setCurrentContainerIndex(containerIndex);
                    UpdateForcedMusic pkt = tracker.getMusicPacket();
                    pkt.containerIndex = containerIndex;
                    pr.getPacketHandler().write((ToClientPacket) pkt);
                    viaTracker++;
                    sent++;
                } else {
                    UpdateForcedMusic pkt = new UpdateForcedMusic(containerIndex);
                    pr.getPacketHandler().write((ToClientPacket) pkt);
                    viaDirectPacket++;
                    sent++;
                }
            }
            String label = ambienceIdForLog != null ? ambienceIdForLog : "(cleared)";
            if (CometConfig.getInstance().isDebugLoggingEnabled()) {
                LOGGER.info("[CometAmbience] ambienceId=" + label + " index=" + containerIndex + " packetsSent=" + sent
                        + " playerRefs=" + playerRefs + " skippedRef=" + skippedBadRef + " viaTracker=" + viaTracker
                        + " viaDirectPacket=" + viaDirectPacket);
            }
            return sent;
        } catch (Throwable t) {
            LOGGER.warning("pushForcedMusicToAllPlayers failed: " + t.getMessage());
            return 0;
        }
    }
}
