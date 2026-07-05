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

    public TravelCamBoostHintHud(@Nonnull PlayerRef playerRef) {
        super(playerRef, HUD_KEY);
    }

    @Override
    protected void build(@Nonnull UICommandBuilder builder) {
        builder.append("HUD/TravelCamBoostHint.ui");
        builder.set("#TravelCamBoostHintLabel.TextSpans", Message.raw("Appuyez sur Z pour accelerer"));
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
        TravelCamBoostHintHud hud = INSTANCES.computeIfAbsent(uuid, id -> {
            TravelCamBoostHintHud created = new TravelCamBoostHintHud(playerRef);
            hudManager.addCustomHud(playerRef, created);
            return created;
        });

        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set("#TravelCamBoostHintLabel.Visible", true);
        hud.update(false, cmd);
    }

    public static void hide(@Nullable Player player, @Nonnull PlayerRef playerRef) {
        UUID uuid = playerRef.getUuid();
        TravelCamBoostHintHud hud = INSTANCES.get(uuid);
        if (hud == null) {
            return;
        }
        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set("#TravelCamBoostHintLabel.Visible", false);
        hud.update(false, cmd);
    }

    public static void cleanup(@Nonnull UUID uuid) {
        INSTANCES.remove(uuid);
    }
}
