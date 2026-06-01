package fr.varyon.holograms.gui;

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
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.holograms.VaryonHologramsPlugin;
import fr.varyon.holograms.hologram.Hologram;
import org.joml.Vector3d;
import javax.annotation.Nonnull;
import java.util.List;

public class HologramEditorPage extends InteractiveCustomUIPage<HologramEditorEventData> {

    private final VaryonHologramsPlugin plugin;
    private final String hologramName;
    private int editingLineIndex = -1;

    public HologramEditorPage(@Nonnull PlayerRef playerRef, @Nonnull VaryonHologramsPlugin plugin,
                               @Nonnull Hologram hologram) {
        super(playerRef, CustomPageLifetime.CanDismiss, HologramEditorEventData.CODEC);
        this.plugin = plugin;
        this.hologramName = hologram.getName();
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder evt, @Nonnull Store<EntityStore> store) {
        Hologram hologram = plugin.getHologramManager().getHologram(hologramName);
        if (hologram == null) { closePage(ref, store); return; }

        cmd.append("Pages/HologramEditorPage.ui");
        cmd.set("#HologramName.Text", hologram.getName());

        Vector3d pos = hologram.getPosition();
        cmd.set("#XInput.Value", String.format("%.1f", pos.x));
        cmd.set("#YInput.Value", String.format("%.1f", pos.y));
        cmd.set("#ZInput.Value", String.format("%.1f", pos.z));

        buildLinesList(hologram, cmd, evt);

        evt.addEventBinding(CustomUIEventBindingType.Activating, "#AddLineButton",
            new EventData().append("Action", "addLine").append("@NewText", "#NewLineInput.Value"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#UpdateLineButton",
            new EventData().append("Action", "updateLine").append("@NewText", "#NewLineInput.Value"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CancelEditButton",
            EventData.of("Action", "cancelEdit"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#MoveHereButton",
            EventData.of("Action", "moveHere"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#DeleteButton",
            EventData.of("Action", "delete"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#ExitButton",
            EventData.of("Action", "exit"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#XUpButton",
            EventData.of("Action", "posXUp"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#XDownButton",
            EventData.of("Action", "posXDown"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#YUpButton",
            EventData.of("Action", "posYUp"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#YDownButton",
            EventData.of("Action", "posYDown"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#ZUpButton",
            EventData.of("Action", "posZUp"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#ZDownButton",
            EventData.of("Action", "posZDown"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#SetPositionButton",
            new EventData().append("Action", "setPosition")
                .append("@X", "#XInput.Value")
                .append("@Y", "#YInput.Value")
                .append("@Z", "#ZInput.Value"));
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                                 @Nonnull HologramEditorEventData data) {
        String action = data.getAction();
        if (action == null) return;

        Hologram hologram = plugin.getHologramManager().getHologram(hologramName);
        if (hologram == null) { closePage(ref, store); return; }

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (playerRef == null) return;

        boolean canEdit = playerRef.hasPermission("*") || playerRef.hasPermission("varyon.holograms.edit");
        boolean canMove = playerRef.hasPermission("*") || playerRef.hasPermission("varyon.holograms.move");
        boolean canDelete = playerRef.hasPermission("*") || playerRef.hasPermission("varyon.holograms.delete");

        switch (action) {
            case "addLine" -> {
                if (!canEdit) { deny(playerRef, ref, store); return; }
                String text = data.getNewText();
                if (text != null && !text.isBlank()) {
                    hologram.addLine(text.trim());
                    plugin.getHologramManager().updateHologram(hologram);
                    editingLineIndex = -1;
                }
                refreshUI(ref, store);
            }
            case "updateLine" -> {
                if (!canEdit) { deny(playerRef, ref, store); return; }
                String text = data.getNewText();
                if (editingLineIndex >= 0 && editingLineIndex < hologram.getLineCount()
                        && text != null && !text.isBlank()) {
                    hologram.setLine(editingLineIndex, text.trim());
                    plugin.getHologramManager().updateHologram(hologram);
                    editingLineIndex = -1;
                }
                refreshUI(ref, store);
            }
            case "removeLine" -> {
                if (!canDelete) { deny(playerRef, ref, store); return; }
                int idx = data.getLineIndexInt();
                if (idx >= 0 && idx < hologram.getLineCount()) {
                    hologram.removeLine(idx);
                    plugin.getHologramManager().updateHologram(hologram);
                    editingLineIndex = -1;
                }
                refreshUI(ref, store);
            }
            case "editLine" -> {
                int idx = data.getLineIndexInt();
                if (idx >= 0 && idx < hologram.getLineCount()) {
                    editingLineIndex = idx;
                    enterEditMode(ref, store, idx, hologram.getLines().get(idx));
                }
            }
            case "cancelEdit" -> {
                editingLineIndex = -1;
                refreshUI(ref, store);
            }
            case "moveHere" -> {
                if (!canMove) { deny(playerRef, ref, store); return; }
                TransformComponent tc = store.getComponent(ref, TransformComponent.getComponentType());
                if (tc != null) {
                    Vector3d pos = tc.getPosition();
                    plugin.getHologramManager().moveHologram(hologram, new Vector3d(pos.x, pos.y + 1.5, pos.z));
                }
                refreshUI(ref, store);
            }
            case "delete" -> {
                if (!canDelete) { deny(playerRef, ref, store); return; }
                plugin.getHologramManager().deleteHologram(hologramName);
                closePage(ref, store);
            }
            case "exit" -> closePage(ref, store);
            case "posXUp"   -> nudge(hologram, canMove, playerRef, ref, store,  0.5,  0,    0);
            case "posXDown" -> nudge(hologram, canMove, playerRef, ref, store, -0.5,  0,    0);
            case "posYUp"   -> nudge(hologram, canMove, playerRef, ref, store,  0,    0.5,  0);
            case "posYDown" -> nudge(hologram, canMove, playerRef, ref, store,  0,   -0.5,  0);
            case "posZUp"   -> nudge(hologram, canMove, playerRef, ref, store,  0,    0,    0.5);
            case "posZDown" -> nudge(hologram, canMove, playerRef, ref, store,  0,    0,   -0.5);
            case "setPosition" -> {
                if (!canMove) { deny(playerRef, ref, store); return; }
                Vector3d cur = hologram.getPosition();
                plugin.getHologramManager().moveHologram(hologram, new Vector3d(
                    data.getPosX(cur.x), data.getPosY(cur.y), data.getPosZ(cur.z)));
                refreshUI(ref, store);
            }
        }
    }

    @Override
    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {}

    private void nudge(@Nonnull Hologram hologram, boolean canMove, @Nonnull PlayerRef playerRef,
                       @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                       double dx, double dy, double dz) {
        if (!canMove) { deny(playerRef, ref, store); return; }
        Vector3d p = hologram.getPosition();
        plugin.getHologramManager().moveHologram(hologram, new Vector3d(p.x + dx, p.y + dy, p.z + dz));
        refreshUI(ref, store);
    }

    private void buildLinesList(@Nonnull Hologram hologram, @Nonnull UICommandBuilder cmd,
                                 @Nonnull UIEventBuilder evt) {
        cmd.clear("#LinesList");
        List<String> lines = hologram.getLines();
        if (lines.isEmpty()) {
            cmd.appendInline("#LinesList",
                "Label { Text: \"Aucune ligne.\"; Style: (TextColor: #96a9be, HorizontalAlignment: Center); }");
            return;
        }
        for (int i = 0; i < lines.size(); i++) {
            String selector = "#LinesList[" + i + "]";
            cmd.append("#LinesList", "Pages/HologramLineItem.ui");
            cmd.set(selector + " #LineNumber.Text", (i + 1) + ".");
            cmd.set(selector + " #LineText.Text", escape(lines.get(i)));
            cmd.set(selector + " #DeleteLabel.Text", "X");
            evt.addEventBinding(CustomUIEventBindingType.Activating, selector + " #Button",
                EventData.of("Action", "editLine").append("LineIndex", String.valueOf(i)), false);
            evt.addEventBinding(CustomUIEventBindingType.Activating, selector + " #LineDeleteButton",
                EventData.of("Action", "removeLine").append("LineIndex", String.valueOf(i)), false);
        }
    }

    private void enterEditMode(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                                int lineIndex, @Nonnull String lineText) {
        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set("#NewLineInput.Value", lineText);
        cmd.set("#EditModeLabel.Text", "Ligne " + (lineIndex + 1) + " :");
        cmd.set("#EditModeLabel.Visible", true);
        cmd.set("#AddLineButton.Visible", false);
        cmd.set("#UpdateLineButton.Visible", true);
        cmd.set("#CancelEditButton.Visible", true);
        sendUpdate(cmd, null, false);
    }

    private void refreshUI(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        Hologram hologram = plugin.getHologramManager().getHologram(hologramName);
        if (hologram == null) { closePage(ref, store); return; }

        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder evt = new UIEventBuilder();
        Vector3d pos = hologram.getPosition();
        cmd.set("#XInput.Value", String.format("%.1f", pos.x));
        cmd.set("#YInput.Value", String.format("%.1f", pos.y));
        cmd.set("#ZInput.Value", String.format("%.1f", pos.z));
        cmd.set("#NewLineInput.Value", "");
        cmd.set("#EditModeLabel.Visible", false);
        cmd.set("#AddLineButton.Visible", true);
        cmd.set("#UpdateLineButton.Visible", false);
        cmd.set("#CancelEditButton.Visible", false);
        buildLinesList(hologram, cmd, evt);
        sendUpdate(cmd, evt, false);
    }

    private void deny(@Nonnull PlayerRef playerRef, @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        playerRef.sendMessage(Message.raw("Permission refusée.").color("#FF5555"));
        refreshUI(ref, store);
    }

    private void closePage(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) player.getPageManager().setPage(ref, store, Page.None);
    }

    private static String escape(@Nonnull String text) {
        return text.replace(";", ",").replace("{", "(").replace("}", ")").replace("\"", "'");
    }
}
