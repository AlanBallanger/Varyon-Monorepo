package com.varyon.hud;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class RtpvConfirmHud extends CustomUIHud {

    public static final String HUD_KEY = "varyon_rtpv_confirm_hud";
    public static final long DURATION_MS = 15_000L;

    private static final ConcurrentHashMap<UUID, RtpvConfirmHud> INSTANCES = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService SCHEDULER =
        java.util.concurrent.Executors.newSingleThreadScheduledExecutor(
            r -> { Thread t = new Thread(r, "rtpv-confirm-hud"); t.setDaemon(true); return t; });

    private volatile long expiresAt;

    public RtpvConfirmHud(@Nonnull PlayerRef playerRef) {
        super(playerRef, HUD_KEY);
    }

    @Nonnull
    public static RtpvConfirmHud getOrCreate(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        UUID uuid = playerRef.getUuid();
        RtpvConfirmHud existing = INSTANCES.get(uuid);
        if (existing != null) return existing;
        RtpvConfirmHud hud = new RtpvConfirmHud(playerRef);
        RtpvConfirmHud race = INSTANCES.putIfAbsent(uuid, hud);
        if (race != null) return race;
        player.getHudManager().addCustomHud(playerRef, hud);
        return hud;
    }

    @Nullable
    public static RtpvConfirmHud get(@Nonnull UUID uuid) {
        return INSTANCES.get(uuid);
    }

    public static void cleanup(@Nonnull UUID uuid) {
        INSTANCES.remove(uuid);
    }

    @Override
    protected void build(@Nonnull UICommandBuilder builder) {
        builder.append("HUD/RtpvConfirmHud.ui");
    }

    public void show(@Nonnull String title, @Nonnull String subtitle) {
        long myExpiry = System.currentTimeMillis() + DURATION_MS;
        expiresAt = myExpiry;

        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set("#RtpvConfirmHud.Visible", true);
        cmd.set("#RtpvConfirmTitle.Text", title);
        cmd.set("#RtpvConfirmSubtitle.Text", subtitle);
        this.update(false, cmd);

        SCHEDULER.schedule(() -> {
            if (expiresAt == myExpiry) {
                hide();
            }
        }, DURATION_MS, TimeUnit.MILLISECONDS);
    }

    public void hide() {
        expiresAt = 0L;
        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set("#RtpvConfirmHud.Visible", false);
        this.update(false, cmd);
    }
}
