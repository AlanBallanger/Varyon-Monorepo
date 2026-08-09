package fr.varyon.readablebooks.gui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class BookReaderPage extends InteractiveCustomUIPage<BookReaderPage.EventDataClass> {

    private final String title;
    private final String text;

    public BookReaderPage(@Nonnull PlayerRef playerRef, @Nonnull String title, @Nonnull String text) {
        super(playerRef, CustomPageLifetime.CanDismiss, EventDataClass.CODEC);
        this.title = title;
        this.text = text;
    }

    @Override
    public void build(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull UICommandBuilder commandBuilder,
        @Nonnull UIEventBuilder eventBuilder,
        @Nonnull Store<EntityStore> store
    ) {
        commandBuilder.append("Pages/ReadableBookReader.ui");

        String shownTitle = this.title.isBlank() ? "Livre" : this.title;
        String shownText = this.text.isBlank()
            ? "Les pages sont vierges."
            : this.text;

        commandBuilder.set("#BookTitle.Text", shownTitle);
        commandBuilder.set("#BookText.Text", shownText);

        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating, "#CloseButton", EventData.of("Action", "close"));
    }

    @Override
    public void handleDataEvent(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull EventDataClass data
    ) {
        if (!"close".equals(data.action)) {
            return;
        }

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        player.getPageManager().setPage(ref, store, Page.None);
    }

    public static class EventDataClass {
        public static final BuilderCodec<EventDataClass> CODEC =
            BuilderCodec.builder(EventDataClass.class, EventDataClass::new)
                .addField(
                    new KeyedCodec<>("Action", Codec.STRING),
                    (entry, s) -> entry.action = s, entry -> entry.action)
                .build();
        public String action;
    }
}
