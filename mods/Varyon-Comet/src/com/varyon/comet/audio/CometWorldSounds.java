package com.varyon.comet.audio;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.Position;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.protocol.ToClientPacket;
import com.hypixel.hytale.protocol.packets.world.PlaySoundEvent3D;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.PlayerUtil;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.comet.CometConfig;

import java.util.logging.Logger;
import javax.annotation.Nullable;

public final class CometWorldSounds {

    private static final Logger LOGGER = Logger.getLogger(CometWorldSounds.class.getName());

    private CometWorldSounds() {
    }

    public static void playCometLand(Store<EntityStore> store, Vector3i blockPos) {
        CometConfig cfg = CometConfig.getInstance();
        String id = cfg != null ? cfg.cometLandSoundEventId : null;
        play3d(store, blockPos, id);
    }

    public static void playCometDestroy(Store<EntityStore> store, Vector3i blockPos) {
        CometConfig cfg = CometConfig.getInstance();
        String id = cfg != null ? cfg.cometDestroySoundEventId : null;
        play3d(store, blockPos, id);
    }

    public static void playCometFallingNotify(@Nullable PlayerRef playerRef) {
        if (playerRef == null) {
            return;
        }
        CometConfig cfg = CometConfig.getInstance();
        String id = cfg != null ? cfg.cometFallingNotifySoundEventId : null;
        if (id == null || id.isBlank()) {
            return;
        }
        try {
            int idx = SoundEvent.getAssetMap().getIndex(id.trim());
            if (idx < 0) {
                LOGGER.warning("SoundEvent id '" + id + "' not found (comet falling notify).");
                return;
            }
            SoundUtil.playSoundEvent2dToPlayer(playerRef, idx, SoundCategory.UI, 0.5f, 1.0f);
        } catch (Throwable t) {
            LOGGER.fine("Comet falling notify sound skipped: " + t.getMessage());
        }
    }

    public static void playCometDestroy(World world, Vector3i blockPos) {
        if (world == null || blockPos == null) {
            return;
        }
        try {
            Store<EntityStore> store = world.getEntityStore().getStore();
            playCometDestroy(store, blockPos);
        } catch (Throwable t) {
            LOGGER.fine("Comet destroy sound skipped: " + t.getMessage());
        }
    }

    private static void play3d(Store<EntityStore> store, Vector3i blockPos, String soundEventId) {
        if (store == null || blockPos == null || soundEventId == null || soundEventId.isBlank()) {
            return;
        }
        try {
            int idx = SoundEvent.getAssetMap().getIndex(soundEventId.trim());
            if (idx < 0) {
                LOGGER.warning("SoundEvent id '" + soundEventId + "' not found in assets.");
                return;
            }
            double x = blockPos.x + 0.5;
            double y = blockPos.y + 0.5;
            double z = blockPos.z + 0.5;
            PlaySoundEvent3D packet = new PlaySoundEvent3D(idx, SoundCategory.SFX, new Position(x, y, z), 1.0f, 1.0f);
            PlayerUtil.broadcastPacketToPlayers(store, (ToClientPacket) packet);
        } catch (Throwable t) {
            LOGGER.warning("World sound failed: " + t.getMessage());
        }
    }
}
