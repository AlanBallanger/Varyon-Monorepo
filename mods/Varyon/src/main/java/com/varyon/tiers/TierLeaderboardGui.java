package com.varyon.tiers;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.List;

public class TierLeaderboardGui extends InteractiveCustomUIPage<TierLeaderboardGui.EventDataClass> {

    private static final int MAX_ENTRIES = 50;

    private final TierLeaderboardManager manager;

    public TierLeaderboardGui(@Nonnull PlayerRef playerRef, @Nonnull TierLeaderboardManager manager) {
        super(playerRef, CustomPageLifetime.CanDismiss, EventDataClass.CODEC);
        this.manager = manager;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                       @Nonnull UICommandBuilder commandBuilder,
                       @Nonnull UIEventBuilder eventBuilder,
                       @Nonnull Store<EntityStore> store) {
        commandBuilder.append("TierLeaderboardMenu.ui");
        commandBuilder.clear("#TierListContainer");

        manager.getLeaderboard().thenAccept(entries -> {
            UICommandBuilder update = new UICommandBuilder();
            fillEntries(update, entries);
            sendUpdate(update, new UIEventBuilder(), false);
        });
    }

    private void fillEntries(UICommandBuilder cmd, List<TierLeaderboardManager.TierEntry> entries) {
        cmd.clear("#TierListContainer");

        int shown = Math.min(entries.size(), MAX_ENTRIES);
        for (int i = 0; i < shown; i++) {
            TierLeaderboardManager.TierEntry entry = entries.get(i);
            cmd.append("#TierListContainer", "TierLeaderboardEntry.ui");

            String elementId = "#TierListContainer[" + i + "]";
            cmd.set(elementId + " #BgOdd.Visible", i % 2 == 1);
            if (i == 0) {
                cmd.set(elementId + " #Rank.Visible", false);
                cmd.set(elementId + " #RankGold.Text", "1");
                cmd.set(elementId + " #RankGold.Visible", true);
            } else if (i == 1) {
                cmd.set(elementId + " #Rank.Visible", false);
                cmd.set(elementId + " #RankSilver.Text", "2");
                cmd.set(elementId + " #RankSilver.Visible", true);
            } else if (i == 2) {
                cmd.set(elementId + " #Rank.Visible", false);
                cmd.set(elementId + " #RankBronze.Text", "3");
                cmd.set(elementId + " #RankBronze.Visible", true);
            } else {
                cmd.set(elementId + " #Rank.Text", String.valueOf(i + 1));
            }
            cmd.set(elementId + " #PlayerName.Text", entry.name());
            cmd.set(elementId + " #TierLabel.Text", entry.tier() > 0 ? "Tier " + entry.tier() : "Aucun tier");
        }
    }

    public static class EventDataClass {
        public static final BuilderCodec<EventDataClass> CODEC =
            BuilderCodec.<EventDataClass>builder(EventDataClass.class, EventDataClass::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (d, v, e) -> d.action = v, (d, e) -> d.action)
                .add()
                .build();
        private String action;
    }
}
