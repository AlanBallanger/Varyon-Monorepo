package fr.varyon.vrpg.ui;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.ui.ItemGridSlot;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executors;

public final class XpNotifHud extends CustomUIHud {

    public static final String HUD_KEY = "vrpg_xp_notif_hud";

    private static final int MAX_SLOTS = 4;
    private static final long SLOT_DURATION_MS = 4000L;

    private static final ConcurrentHashMap<UUID, XpNotifHud> INSTANCES = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService SCHEDULER =
        java.util.concurrent.Executors.newSingleThreadScheduledExecutor(
            r -> { Thread t = new Thread(r, "xp-notif-hud"); t.setDaemon(true); return t; });

    private final long[] slotExpiresAt = new long[MAX_SLOTS];

    public XpNotifHud(@Nonnull PlayerRef playerRef) {
        super(playerRef, HUD_KEY);
    }

    @Nonnull
    public static XpNotifHud getOrCreate(@Nonnull Player player, @Nonnull PlayerRef playerRef) {
        UUID uuid = playerRef.getUuid();
        XpNotifHud existing = INSTANCES.get(uuid);
        if (existing != null) return existing;
        XpNotifHud hud = new XpNotifHud(playerRef);
        XpNotifHud race = INSTANCES.putIfAbsent(uuid, hud);
        if (race != null) return race;
        player.getHudManager().addCustomHud(playerRef, hud);
        return hud;
    }

    @Nullable
    public static XpNotifHud get(@Nonnull UUID uuid) {
        return INSTANCES.get(uuid);
    }

    public static void cleanup(@Nonnull UUID uuid) {
        INSTANCES.remove(uuid);
    }

    @Override
    protected void build(@Nonnull UICommandBuilder builder) {
        builder.append("Hud/VRpgXpNotif.ui");
    }

    public void showBurst(@Nonnull String xpText, @Nonnull String iconItemId) {
        int slot = findFreeSlot();
        long expiresAt = System.currentTimeMillis() + SLOT_DURATION_MS;
        slotExpiresAt[slot] = expiresAt;

        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set("#VRpgXp" + slot + ".Visible", true);
        cmd.set("#VRpgXpAmount" + slot + ".Text", xpText);
        cmd.set("#VRpgXpIcon" + slot + ".Slots", List.of(new ItemGridSlot(new ItemStack(iconItemId, 1))));
        this.update(false, cmd);

        final int finalSlot = slot;
        final long finalExpires = expiresAt;
        SCHEDULER.schedule(() -> {
            if (slotExpiresAt[finalSlot] == finalExpires) {
                clearSlot(finalSlot);
            }
        }, SLOT_DURATION_MS, TimeUnit.MILLISECONDS);
    }

    private void clearSlot(int slot) {
        slotExpiresAt[slot] = 0L;
        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set("#VRpgXp" + slot + ".Visible", false);
        this.update(false, cmd);
    }

    private int findFreeSlot() {
        long now = System.currentTimeMillis();
        long oldestTime = Long.MAX_VALUE;
        int oldestSlot = 0;
        for (int i = 0; i < MAX_SLOTS; i++) {
            if (slotExpiresAt[i] <= now) return i;
            if (slotExpiresAt[i] < oldestTime) {
                oldestTime = slotExpiresAt[i];
                oldestSlot = i;
            }
        }
        return oldestSlot;
    }
}
