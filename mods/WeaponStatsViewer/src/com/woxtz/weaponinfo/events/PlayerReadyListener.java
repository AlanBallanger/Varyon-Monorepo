/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.hypixel.hytale.component.Ref
 *  com.hypixel.hytale.component.Store
 *  com.hypixel.hytale.server.core.Message
 *  com.hypixel.hytale.server.core.entity.entities.Player
 *  com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent
 *  com.hypixel.hytale.server.core.universe.PlayerRef
 */
package com.woxtz.weaponinfo.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.woxtz.weaponinfo.PluginConfig;
import com.woxtz.weaponinfo.util.GradientParser;

public class PlayerReadyListener {
    public static void onPlayerReady(PlayerReadyEvent event) {
        Player player = event.getPlayer();
        Ref ref = player.getReference();
        if (ref != null && ref.isValid()) {
            Store store = ref.getStore();
            PlayerRef playerRef = (PlayerRef)store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRef != null) {
                System.out.println("[WeaponInfo] PlayerReady event fired for: " + playerRef.getUsername());
                if (PluginConfig.getInstance().isShowWelcomeMessage() && (PluginConfig.getInstance().isPublicCommand() || playerRef.hasPermission("server.op"))) {
                    String language = playerRef.getLanguage();
                    System.out.println("[WeaponInfo] Player language: " + language);
                    if (language.startsWith("es")) {
                        playerRef.sendMessage(Message.raw((String)"\u00bb Weapon Stats Viewer activado \u00ab").color("#FFD700").bold(true));
                        playerRef.sendMessage(GradientParser.parse("Usa <gradient:#FFD700:#FF1493>/weapons</gradient> para ver todas las armas"));
                    } else {
                        playerRef.sendMessage(Message.raw((String)"\u00bb Weapon Stats Viewer enabled \u00ab").color("#FFD700").bold(true));
                        playerRef.sendMessage(GradientParser.parse("Use <gradient:#FFD700:#FF1493>/weapons</gradient> to browse all weapons"));
                    }
                    System.out.println("[WeaponInfo] Welcome message sent to: " + playerRef.getUsername());
                }
            } else {
                System.out.println("[WeaponInfo] PlayerRef is null for player ref index: " + ref.getIndex());
            }
        } else {
            System.out.println("[WeaponInfo] Player reference is null or invalid");
        }
    }
}

