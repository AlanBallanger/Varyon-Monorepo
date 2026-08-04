package fr.varyon.vrpg.rpg;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;

import javax.annotation.Nullable;
import java.util.UUID;

public final class CreativeGate {

    private CreativeGate() {}

    public static boolean isCreative(@Nullable Player player) {
        return player != null && player.getGameMode() == GameMode.Creative;
    }

    public static boolean isCreative(@Nullable PlayerRef playerRef) {
        if (playerRef == null) return false;
        try {
            return isCreative(playerRef.getComponent(Player.getComponentType()));
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isCreative(@Nullable UUID uuid) {
        if (uuid == null) return false;
        try {
            for (PlayerRef pr : Universe.get().getPlayers()) {
                if (uuid.equals(pr.getUuid())) return isCreative(pr);
            }
        } catch (Exception ignored) {}
        return false;
    }
}
