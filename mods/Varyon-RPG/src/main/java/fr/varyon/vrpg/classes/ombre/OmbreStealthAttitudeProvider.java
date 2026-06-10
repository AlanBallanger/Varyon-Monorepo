package fr.varyon.vrpg.classes.ombre;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.asset.type.attitude.Attitude;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.blackboard.view.attitude.IAttitudeProvider;
import com.hypixel.hytale.server.npc.role.Role;

import javax.annotation.Nonnull;

public final class OmbreStealthAttitudeProvider implements IAttitudeProvider {

    private static final com.hypixel.hytale.logger.HytaleLogger LOG =
        com.hypixel.hytale.logger.HytaleLogger.forEnclosingClass();

    private final OmbreState ombreState;

    public OmbreStealthAttitudeProvider(@Nonnull OmbreState ombreState) {
        this.ombreState = ombreState;
    }

    @Override
    public Attitude getAttitude(@Nonnull Ref<EntityStore> npcRef,
                                @Nonnull Role role,
                                @Nonnull Ref<EntityStore> targetRef,
                                @Nonnull ComponentAccessor<EntityStore> accessor) {
        try {
            PlayerRef playerRef = accessor.getComponent(targetRef, PlayerRef.getComponentType());
            if (playerRef == null) return null;
            boolean inStealth = ombreState.isInStealth(playerRef.getUuid());
            LOG.atInfo().log("[StealthProvider] player=" + playerRef.getUuid() + " inStealth=" + inStealth);
            if (inStealth) {
                return Attitude.IGNORE;
            }
        } catch (Exception e) {
            LOG.atWarning().log("[StealthProvider] exception: " + e.getMessage());
        }
        return null;
    }
}
