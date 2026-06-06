package fr.varyon.vrpg.ui;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import fr.varyon.vrpg.ui.profession.RpgProfessionUiState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RpgUiAdmin {

    private RpgUiAdmin() {}

    public static boolean isAdmin(@Nonnull PlayerRef playerRef) {
        try {
            List<PlayerRef> players = new ArrayList<>(Universe.get().getPlayers());
            for (PlayerRef pr : players) {
                if (pr.getUuid().equals(playerRef.getUuid())) {
                    Player p = pr.getReference().getStore().getComponent(
                        pr.getReference(), Player.getComponentType());
                    return p != null && p.getGameMode() == GameMode.Creative;
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    @Nonnull
    public static List<PlayerRef> getOnlinePlayers() {
        try {
            return new ArrayList<>(Universe.get().getPlayers());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Nullable
    public static PlayerRef adminTargetRef(@Nonnull RpgProfessionUiState state) {
        List<PlayerRef> players = getOnlinePlayers();
        if (players.isEmpty()) return null;
        state.adminPlayerIndex = Math.max(0, Math.min(state.adminPlayerIndex, players.size() - 1));
        return players.get(state.adminPlayerIndex);
    }
}
