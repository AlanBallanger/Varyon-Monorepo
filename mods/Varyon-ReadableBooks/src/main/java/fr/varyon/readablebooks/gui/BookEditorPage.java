package fr.varyon.readablebooks.gui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.readablebooks.BookManager;
import fr.varyon.readablebooks.data.BookEntry;

import java.awt.Color;
import javax.annotation.Nonnull;

public class BookEditorPage extends InteractiveCustomUIPage<BookEditorPage.EventDataClass> {

    private static final Color COLOR_OK = new Color(85, 255, 85);
    private static final Color COLOR_INFO = new Color(170, 170, 170);

    private final PlayerRef playerRef;
    private final String dimension;
    private final int x;
    private final int y;
    private final int z;

    private String currentTitle;
    private String currentText;

    public BookEditorPage(
        @Nonnull PlayerRef playerRef,
        @Nonnull String dimension,
        int x, int y, int z,
        @Nonnull String initialTitle,
        @Nonnull String initialText
    ) {
        super(playerRef, CustomPageLifetime.CanDismiss, EventDataClass.CODEC);
        this.playerRef = playerRef;
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.currentTitle = initialTitle;
        this.currentText = initialText;
    }

    @Override
    public void build(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull UICommandBuilder commandBuilder,
        @Nonnull UIEventBuilder eventBuilder,
        @Nonnull Store<EntityStore> store
    ) {
        commandBuilder.append("Pages/ReadableBookEditor.ui");

        commandBuilder.set("#PositionLabel.Text",
            this.dimension + "  ·  " + this.x + ", " + this.y + ", " + this.z);
        commandBuilder.set("#TitleInput.Value", this.currentTitle);
        commandBuilder.set("#TextInput.Value", this.currentText);

        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating, "#SaveButton", snapshot("save"));
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating, "#PreviewButton", snapshot("preview"));
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating, "#ClearButton", snapshot("clear"));
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating, "#CloseButton", EventData.of("Action", "close"));
    }

    /**
     * Pulls the live TextField values into the event alongside the action, so a click reads
     * whatever the admin has typed without needing a ValueChanged round-trip per keystroke.
     */
    private static EventData snapshot(String action) {
        EventData event = new EventData().append("Action", action);
        event.append("@Title", "#TitleInput.Value");
        event.append("@Text", "#TextInput.Value");
        return event;
    }

    @Override
    public void handleDataEvent(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull EventDataClass data
    ) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null || data.action == null) {
            return;
        }

        if ("close".equals(data.action)) {
            player.getPageManager().setPage(ref, store, Page.None);
            return;
        }

        String title = data.title == null ? "" : data.title.trim();
        String text = data.text == null ? "" : data.text;

        switch (data.action) {
            case "save" -> {
                BookManager.getInstance().saveBook(
                    this.dimension, this.x, this.y, this.z,
                    title, text, this.playerRef.getUsername()
                );
                this.currentTitle = title;
                this.currentText = text;
                this.playerRef.sendMessage(
                    Message.raw("Livre enregistré.").color(COLOR_OK));
                player.getPageManager().setPage(ref, store, Page.None);
            }
            case "preview" -> {
                this.currentTitle = title;
                this.currentText = text;
                player.getPageManager().openCustomPage(
                    ref, store, new BookReaderPage(this.playerRef, title, text));
            }
            case "clear" -> {
                this.currentTitle = "";
                this.currentText = "";
                BookManager.getInstance().removeBook(this.dimension, this.x, this.y, this.z);
                this.playerRef.sendMessage(
                    Message.raw("Contenu du livre effacé.").color(COLOR_INFO));
                player.getPageManager().setPage(ref, store, Page.None);
            }
            default -> {
            }
        }
    }

    public static class EventDataClass {
        public static final BuilderCodec<EventDataClass> CODEC =
            BuilderCodec.builder(EventDataClass.class, EventDataClass::new)
                .addField(
                    new KeyedCodec<>("Action", Codec.STRING),
                    (entry, s) -> entry.action = s, entry -> entry.action)
                .addField(
                    new KeyedCodec<>("@Title", Codec.STRING),
                    (entry, s) -> entry.title = s, entry -> entry.title)
                .addField(
                    new KeyedCodec<>("@Text", Codec.STRING),
                    (entry, s) -> entry.text = s, entry -> entry.text)
                .build();

        public String action;
        public String title;
        public String text;
    }
}
