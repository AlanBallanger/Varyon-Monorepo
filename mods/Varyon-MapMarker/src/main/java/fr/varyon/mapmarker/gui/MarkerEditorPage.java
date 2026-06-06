package fr.varyon.mapmarker.gui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.mapmarker.MarkerEntry;
import fr.varyon.mapmarker.VaryonMapMarkerPlugin;
import org.joml.Vector3d;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MarkerEditorPage extends InteractiveCustomUIPage<MarkerEventData> {

    private static final int IMAGES_PER_ROW = 5;

    private final VaryonMapMarkerPlugin plugin;
    private final World world;
    private final String markerId;

    public MarkerEditorPage(@Nonnull PlayerRef playerRef, @Nonnull VaryonMapMarkerPlugin plugin,
                            @Nonnull World world, @Nonnull String markerId) {
        super(playerRef, CustomPageLifetime.CanDismiss, MarkerEventData.CODEC);
        this.plugin = plugin;
        this.world = world;
        this.markerId = markerId;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder evt, @Nonnull Store<EntityStore> store) {
        MarkerEntry entry = plugin.findMarkerById(markerId);
        if (entry == null) { closePage(ref, store); return; }

        cmd.append("Pages/MarkerEditorPage.ui");
        cmd.set("#MarkerNameInput.Value", entry.markerName());
        cmd.set("#MarkerGroupInput.Value", entry.group() != null ? entry.group() : "");
        cmd.set("#PosXLabel.Text", String.format(Locale.US, "%.0f", entry.x()));
        cmd.set("#PosZLabel.Text", String.format(Locale.US, "%.0f", entry.z()));
        buildImagePickList(entry, cmd, evt);
        bindEvents(evt);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                                @Nonnull MarkerEventData data) {
        String action = data.getAction();
        if (action == null) return;

        MarkerEntry entry = plugin.findMarkerById(markerId);
        if (entry == null) { closePage(ref, store); return; }

        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        Player player = store.getComponent(ref, Player.getComponentType());
        if (playerRef == null || player == null) return;

        switch (action) {
            case "rename" -> {
                String newName = data.getMarkerName();
                if (newName == null || newName.isBlank()) {
                    playerRef.sendMessage(Message.raw("Nom vide.").color("#FF5555"));
                    refreshUI(ref, store);
                    return;
                }
                plugin.updateMarkerName(world, markerId, newName.trim());
                playerRef.sendMessage(Message.raw("Marqueur renommé : " + newName.trim()).color("#55FF55"));
                refreshUI(ref, store);
            }
            case "setGroup" -> {
                String newGroup = data.getMarkerGroup();
                plugin.updateMarkerGroup(world, markerId, newGroup);
                refreshUI(ref, store);
            }
            case "selectImage" -> {
                String image = data.getImage();
                if (image == null || image.isBlank()) { refreshUI(ref, store); return; }
                plugin.updateMarkerImage(world, markerId, image.trim());
                refreshUI(ref, store);
            }
            case "moveHere" -> {
                TransformComponent tc = store.getComponent(ref, TransformComponent.getComponentType());
                if (tc == null) { refreshUI(ref, store); return; }
                Vector3d pos = tc.getPosition();
                plugin.moveMarker(world, markerId, (float) pos.x, (float) pos.z);
                playerRef.sendMessage(Message.raw(
                    "Marqueur déplacé à X " + String.format("%.0f", pos.x)
                    + " Z " + String.format("%.0f", pos.z)).color("#55FF55"));
                refreshUI(ref, store);
            }
            case "posXUp"   -> nudge(world, ref, store,  1f, 0f);
            case "posXDown" -> nudge(world, ref, store, -1f, 0f);
            case "posZUp"   -> nudge(world, ref, store,  0f,  1f);
            case "posZDown" -> nudge(world, ref, store,  0f, -1f);
            case "delete" -> {
                plugin.clearMarkerById(world, markerId);
                playerRef.sendMessage(Message.raw("Marqueur supprimé.").color("#FF5555"));
                player.getPageManager().openCustomPage(ref, store,
                    new MarkerListPage(playerRef, plugin, world));
            }
            case "backToList" ->
                player.getPageManager().openCustomPage(ref, store,
                    new MarkerListPage(playerRef, plugin, world));
            case "exit" -> closePage(ref, store);
        }
    }

    @Override
    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {}

    private void buildImagePickList(@Nonnull MarkerEntry entry, @Nonnull UICommandBuilder cmd,
                                    @Nonnull UIEventBuilder evt) {
        cmd.clear("#ImagePickList");
        List<String> images = new ArrayList<>(plugin.listAvailablePngs());
        images.sort(Comparator.naturalOrder());
        if (images.isEmpty()) {
            cmd.appendInline("#ImagePickList",
                "Label { Text: \"Aucune image dans images/\"; Style: (TextColor: #96a9be, FontSize: 11); }");
            return;
        }
        String current = entry.imageName();
        int rowIdx = 0;
        int col = 0;
        for (int i = 0; i < images.size(); i++) {
            if (col == 0) {
                cmd.appendInline("#ImagePickList", "Group { LayoutMode: Left; Anchor: (Bottom: 2); }");
            }
            String name = images.get(i);
            String rowSel = "#ImagePickList[" + rowIdx + "]";
            cmd.append(rowSel, "Pages/MarkerImagePickButton.ui");
            String sel = rowSel + "[" + col + "]";
            boolean selected = name.equalsIgnoreCase(current);
            cmd.set(sel + " #ImagePickLabel.Text", name);
            if (selected) {
                cmd.set(sel + " #ImagePickButton.Background", "#3a6a9e");
            }
            evt.addEventBinding(CustomUIEventBindingType.Activating, sel + " #ImagePickButton",
                EventData.of("Action", "selectImage").append("Image", name), false);
            col++;
            if (col >= IMAGES_PER_ROW) {
                col = 0;
                rowIdx++;
            }
        }
    }

    private void bindEvents(@Nonnull UIEventBuilder evt) {
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#RenameButton",
            new EventData().append("Action", "rename").append("@MarkerName", "#MarkerNameInput.Value"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#SetGroupButton",
            new EventData().append("Action", "setGroup").append("@MarkerGroup", "#MarkerGroupInput.Value"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#MoveHereButton",
            EventData.of("Action", "moveHere"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#DeleteButton",
            EventData.of("Action", "delete"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BackToListButton",
            EventData.of("Action", "backToList"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#ExitButton",
            EventData.of("Action", "exit"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#XUpButton",
            EventData.of("Action", "posXUp"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#XDownButton",
            EventData.of("Action", "posXDown"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#ZUpButton",
            EventData.of("Action", "posZUp"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#ZDownButton",
            EventData.of("Action", "posZDown"));
    }

    private void nudge(@Nonnull World w, @Nonnull Ref<EntityStore> ref,
                       @Nonnull Store<EntityStore> store, float dx, float dz) {
        MarkerEntry entry = plugin.findMarkerById(markerId);
        if (entry == null) return;
        plugin.moveMarker(w, markerId, entry.x() + dx, entry.z() + dz);
        refreshUI(ref, store);
    }

    private void refreshUI(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        MarkerEntry entry = plugin.findMarkerById(markerId);
        if (entry == null) { closePage(ref, store); return; }

        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder evt = new UIEventBuilder();
        cmd.set("#MarkerNameInput.Value", entry.markerName());
        cmd.set("#MarkerGroupInput.Value", entry.group() != null ? entry.group() : "");
        cmd.set("#PosXLabel.Text", String.format(Locale.US, "%.0f", entry.x()));
        cmd.set("#PosZLabel.Text", String.format(Locale.US, "%.0f", entry.z()));
        buildImagePickList(entry, cmd, evt);
        bindEvents(evt);
        sendUpdate(cmd, evt, false);
    }

    private void closePage(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) player.getPageManager().setPage(ref, store, Page.None);
    }
}
