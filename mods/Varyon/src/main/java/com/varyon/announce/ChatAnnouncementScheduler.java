package com.varyon.announce;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.VaryonPlugin;
import com.varyon.config.ChatAnnouncementsConfig;
import com.varyon.util.VaryonWorldAccess;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public final class ChatAnnouncementScheduler {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static volatile ChatAnnouncementScheduler instance;

    private ScheduledFuture<?> task;
    private final AtomicInteger nextIndex = new AtomicInteger(0);

    private ChatAnnouncementScheduler() {}

    public static void init() {
        if (instance == null) {
            synchronized (ChatAnnouncementScheduler.class) {
                if (instance == null) {
                    instance = new ChatAnnouncementScheduler();
                }
            }
        }
        instance.restart();
    }

    public static void shutdown() {
        ChatAnnouncementScheduler sch = instance;
        if (sch != null) {
            sch.cancelTask();
        }
    }

    public static void onConfigReloaded() {
        ChatAnnouncementScheduler sch = instance;
        if (sch != null) {
            sch.restart();
        }
    }

    private void cancelTask() {
        if (task != null) {
            task.cancel(false);
            task = null;
        }
    }

    private void restart() {
        cancelTask();
        nextIndex.set(0);
        if (VaryonPlugin.getStaticConfigManager() == null) {
            return;
        }
        ChatAnnouncementsConfig cfg = VaryonPlugin.getStaticConfigManager().getChatAnnouncementsConfig();
        if (!cfg.isEnabled()) {
            LOGGER.at(Level.INFO).log("Chat announcements disabled");
            return;
        }
        List<String> messages = cfg.getMessages();
        if (messages.isEmpty()) {
            LOGGER.at(Level.INFO).log("Chat announcements: no messages configured");
            return;
        }
        long periodMs = Math.max(1L, (long) cfg.getIntervalMinutes()) * 60_000L;
        task = HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(
                this::tick,
                periodMs,
                periodMs,
                TimeUnit.MILLISECONDS);
        LOGGER.at(Level.INFO).log("Chat announcements started: interval " + cfg.getIntervalMinutes() + " min, " + messages.size() + " message(s)");
    }

    private void tick() {
        try {
            if (VaryonPlugin.getStaticConfigManager() == null) {
                return;
            }
            ChatAnnouncementsConfig cfg = VaryonPlugin.getStaticConfigManager().getChatAnnouncementsConfig();
            if (!cfg.isEnabled()) {
                return;
            }
            List<String> messages = cfg.getMessages();
            if (messages.isEmpty()) {
                return;
            }
            int size = messages.size();
            int idx = Math.floorMod(nextIndex.getAndIncrement(), size);
            String text = messages.get(idx);
            broadcast(text);
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Chat announcement tick failed: " + e.getMessage());
        }
    }

    private void broadcast(@Nonnull String text) {
        Message msg = Message.raw(text).color(new Color(255, 215, 0));
        for (PlayerRef playerRef : Universe.get().getPlayers()) {
            if (playerRef == null || !playerRef.getReference().isValid()) {
                continue;
            }
            try {
                Ref ref = playerRef.getReference();
                Store store = ref.getStore();
                Player player = (Player) store.getComponent(ref, Player.getComponentType());
                if (player == null) {
                    continue;
                }
                World world = ((EntityStore) store.getExternalData()).getWorld();
                if (world == null || !VaryonWorldAccess.isVaryonEnabledWorld(world)) {
                    continue;
                }
                playerRef.sendMessage(msg);
            } catch (Exception e) {
                LOGGER.at(Level.FINE).log("Skip announcement for player: " + e.getMessage());
            }
        }
    }
}
