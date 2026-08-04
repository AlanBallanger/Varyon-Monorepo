package com.varyon.easyhunger.events;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.server.core.auth.PlayerAuthentication;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.io.adapter.PacketWatcher;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.easyhunger.EasyHungerUtils;
import com.varyon.easyhunger.components.HungerComponent;
import com.varyon.easyhunger.ui.EasyHungerHud;
import com.varyon.easyhunger.ui.EasyWaterHud;

import java.util.UUID;

public class GameModeChangeListener implements PacketWatcher {
    static private final int SET_GAMEMODE_PACKET_ID = 101;

    @Override
    public void accept(PacketHandler packetHandler, Packet packet) {
        if (packet.getId() != SET_GAMEMODE_PACKET_ID) return;

        PlayerAuthentication playerAuthentication = packetHandler.getAuth();
        if (playerAuthentication == null) return;


        UUID userUuid = playerAuthentication.getUuid();
        PlayerRef playerRef = Universe.get().getPlayer(userUuid);
        if (playerRef == null) return;

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null) return;

        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();

        world.execute(() -> {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) return;
            GameMode gameMode = player.getGameMode();
            EasyHungerHud.updatePlayerGameMode(playerRef, gameMode);
            EasyWaterHud.updatePlayerGameMode(playerRef, gameMode); // Update Thirst GUI too
            if (gameMode == GameMode.Creative) {
                EasyHungerUtils.removeHungerRelatedEffectsFromEntity(ref, store);
                EasyHungerUtils.setPlayerHungerLevel(ref, store, com.varyon.easyhunger.EasyHunger.get().getConfig().getMaxHunger());
                
                // Auto-Fill Thirst
                com.varyon.easyhunger.components.ThirstComponent thirst = store.getComponent(ref, com.varyon.easyhunger.components.ThirstComponent.getComponentType());
                if (thirst != null) {
                   float max = com.varyon.easyhunger.EasyHunger.get().getConfig().getMaxThirst();
                   thirst.setThirstLevel(max);
                   EasyWaterHud.updatePlayerThirstLevel(playerRef, max);
                }
            }
        });
    }
}
