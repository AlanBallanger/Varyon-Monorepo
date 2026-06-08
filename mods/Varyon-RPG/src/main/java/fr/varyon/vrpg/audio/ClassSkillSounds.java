package fr.varyon.vrpg.audio;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.protocol.ToClientPacket;
import com.hypixel.hytale.protocol.packets.world.PlaySoundEvent2D;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ClassSkillSounds {

    private static final HytaleLogger LOG = HytaleLogger.forEnclosingClass();

    public static final String COUP_ESTOC_SOUND     = "SFX_Sword_T2_Lunge_Local";
    public static final String ASSAUT_ECLAIR_SOUND  = "SFX_Daggers_T1_Pounce";
    public static final String ASSAUT_ECLAIR_IMPACT = "SFX_Sword_T2_Swing";

    private ClassSkillSounds() {}

    public static void playSkillSound(@Nonnull String soundId,
                                      @Nonnull PlayerRef playerRef,
                                      @Nonnull Vector3d position,
                                      @Nullable CommandBuffer<EntityStore> commandBuffer) {
        try {
            int idx = SoundEvent.getAssetMap().getIndex(soundId);
            if (idx <= 0) return;
            playerRef.getPacketHandler().writeNoCache(
                (ToClientPacket) new PlaySoundEvent2D(idx, SoundCategory.SFX, 1.0f, 1.0f));
            if (commandBuffer != null) {
                SoundUtil.playSoundEvent3d(idx, SoundCategory.SFX, position.x, position.y, position.z, commandBuffer);
            }
        } catch (Exception e) {
            LOG.atWarning().log("[ClassSkillSounds] son ERREUR id=" + soundId + " : " + e.getMessage());
        }
    }
}
