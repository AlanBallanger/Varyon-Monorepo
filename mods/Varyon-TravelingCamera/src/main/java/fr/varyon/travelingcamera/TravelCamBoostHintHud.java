package fr.varyon.travelingcamera;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.entity.entities.player.hud.HudManager;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TravelCamBoostHintHud extends CustomUIHud {

    public static final String HUD_KEY = "travelcam_boost_hint_hud";

    private static final ConcurrentHashMap<UUID, TravelCamBoostHintHud> INSTANCES = new ConcurrentHashMap<>();

    private boolean visible;

    public TravelCamBoostHintHud(@Nonnull PlayerRef playerRef) {
        super(playerRef, HUD_KEY);
    }

    @Override
    protected void build(@Nonnull UICommandBuilder builder) {
        builder.append("HUD/TravelCamBoostHint.ui");
        builder.set("#TravelCamBoostHintLabel.Text", "Appuyez sur Z pour accelerer");
        builder.set("#TravelCamBoostHintBox.Visible", visible);
    }

    public static void show(@Nullable Player player, @Nonnull PlayerRef playerRef) {
        if (player == null) {
            return;
        }
        HudManager hudManager = player.getHudManager();
        if (hudManager == null) {
            return;
        }
        UUID uuid = playerRef.getUuid();
        TravelCamBoostHintHud existing = INSTANCES.get(uuid);
        if (existing != null) {
            existing.visible = true;
            UICommandBuilder cmd = new UICommandBuilder();
            cmd.set("#TravelCamBoostHintBox.Visible", true);
            existing.update(false, cmd);
            return;
        }
        TravelCamBoostHintHud created = new TravelCamBoostHintHud(playerRef);
        created.visible = true;
        INSTANCES.put(uuid, created);
        hudManager.addCustomHud(playerRef, created);
    }

    public static void hide(@Nullable Player player, @Nonnull PlayerRef playerRef) {
        UUID uuid = playerRef.getUuid();
        TravelCamBoostHintHud hud = INSTANCES.get(uuid);
        if (hud == null) {
            return;
        }
        hud.visible = false;
        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set("#TravelCamBoostHintBox.Visible", false);
        hud.update(false, cmd);
    }

    public static void cleanup(@Nonnull UUID uuid) {
        INSTANCES.remove(uuid);
    }
}
