package com.varyon.comet.audio;

import com.hypixel.hytale.builtin.ambience.AmbiencePlugin;
import com.hypixel.hytale.builtin.ambience.components.AmbienceTracker;
import com.hypixel.hytale.builtin.ambience.resources.AmbienceResource;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.ToClientPacket;
import com.hypixel.hytale.protocol.packets.world.UpdateEnvironmentMusic;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.logging.Logger;

public final class CometAmbienceSync {

    private static final Logger LOGGER = Logger.getLogger(CometAmbienceSync.class.getName());

    private CometAmbienceSync() {
    }

    public static int pushForcedMusicToAllPlayers(Store<EntityStore> store, String ambienceIdForLog) {
        if (store == null) {
            return 0;
        }
        try {
            AmbiencePlugin plugin = AmbiencePlugin.get();
            if (plugin == null) {
                LOGGER.warning("[CometAmbience] push skipped: AmbiencePlugin null");
                return 0;
            }
            AmbienceResource res = store.getResource(AmbienceResource.getResourceType());
            int desired = res.getForcedMusicIndex();
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
                AmbienceTracker tracker = store.getComponent(pref, AmbienceTracker.getComponentType());
                if (tracker != null) {
                    tracker.setForcedMusicIndex(desired);
                    UpdateEnvironmentMusic pkt = tracker.getMusicPacket();
                    pkt.environmentIndex = desired;
                    pr.getPacketHandler().write((ToClientPacket) pkt);
                    viaTracker++;
                    sent++;
                } else {
                    UpdateEnvironmentMusic pkt = new UpdateEnvironmentMusic(desired);
                    pr.getPacketHandler().write((ToClientPacket) pkt);
                    viaDirectPacket++;
                    sent++;
                }
            }
            String label = ambienceIdForLog != null ? ambienceIdForLog : "(cleared)";
            LOGGER.info("[CometAmbience] ambienceId=" + label + " index=" + desired + " packetsSent=" + sent
                    + " playerRefs=" + playerRefs + " skippedRef=" + skippedBadRef + " viaTracker=" + viaTracker
                    + " viaDirectPacket=" + viaDirectPacket);
            return sent;
        } catch (Throwable t) {
            LOGGER.warning("pushForcedMusicToAllPlayers failed: " + t.getMessage());
            return 0;
        }
    }
}
