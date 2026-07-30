package com.faiizer.craftrestrict.utils;

import com.faiizer.craftrestrict.Main;
import com.faiizer.craftrestrict.config.CraftRestrictConfig;
import com.faiizer.craftrestrict.config.RestrictionRule;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import java.util.UUID;

public class PermissionsUtils {

    public static boolean shouldRestrict(PlayerRef playerRef, String id, String type) {
        UUID uuid = playerRef.getUuid();
        PermissionsModule permissionsModule = PermissionsModule.get();
        if (permissionsModule.hasPermission(uuid, "*")
                || permissionsModule.hasPermission(uuid, "craftrestrict.*")
                || permissionsModule.hasPermission(uuid, "craftrestrict." + type + ".*")) {
            return false;
        }

        String worldName = resolveWorldName(playerRef);
        for (RestrictionRule rule : Main.getConfig().getRestrictionRules().values()) {
            if (!rule.matchesId(id) || !rule.matchesWorld(worldName)) {
                continue;
            }
            if (!rule.hasPermission()) {
                return true;
            }
            if (!permissionsModule.hasPermission(uuid, rule.getPermission())) {
                return true;
            }
        }

        String permNode = "craftrestrict." + type + "." + id;
        boolean hasSpecificPerm = permissionsModule.hasPermission(uuid, permNode);
        boolean isAllowMode = Main.getConfig().getRestrictionMode().equalsIgnoreCase("allow");
        return isAllowMode != hasSpecificPerm;
    }

    private static String resolveWorldName(PlayerRef playerRef) {
        try {
            UUID worldUuid = playerRef.getWorldUuid();
            if (worldUuid == null) {
                return null;
            }
            World world = Universe.get().getWorld(worldUuid);
            return world != null ? world.getName() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public static void sendRestrictionNotifications(PlayerRef playerRef, String type) {
        boolean isBench = type.equalsIgnoreCase("bench");
        CraftRestrictConfig config = Main.getConfig();
        boolean shouldSendMessage = isBench ? config.isSendBenchDenyMessage() : config.isSendRecipeDenyMessage();
        String messageContent = isBench ? config.getBenchDenyMessage() : config.getRecipeDenyMessage();
        if (shouldSendMessage && messageContent != null) {
            playerRef.sendMessage(Message.raw(messageContent));
        }
        boolean shouldPlaySound = isBench ? config.isSendBenchDenySound() : config.isSendRecipeDenySound();
        String soundId = isBench ? config.getBenchDenySound() : config.getRecipeDenySound();
        if (shouldPlaySound) {
            int soundIndex;
            try {
                soundIndex = SoundEvent.getAssetMap().getIndex(soundId);
            } catch (Exception e) {
                soundIndex = SoundEvent.getAssetMap().getIndex("SFX_Antelope_Alerted");
            }
            SoundUtil.playSoundEvent2dToPlayer(playerRef, soundIndex, SoundCategory.UI);
        }
    }
}
