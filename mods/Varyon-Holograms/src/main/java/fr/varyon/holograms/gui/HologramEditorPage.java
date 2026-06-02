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
import fr.varyon.holograms.hologram.HologramLineFormat;
import fr.varyon.holograms.hologram.HologramGroups;
import fr.varyon.holograms.hologram.HologramNames;
import org.joml.Vector3d;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HologramEditorPage extends InteractiveCustomUIPage<HologramEditorEventData> {

    private static final AnimFamily[] ANIM_FAMILIES = {
        new AnimFamily("float", "float", new AnimVariant[] {
            variant("float", "NORMAL"), variant("float_slow", "LENT"), variant("float_fast", "RAPIDE") }),
        new AnimFamily("spin", "spin", new AnimVariant[] {
            variant("spin", "NORMAL"), variant("spin_slow", "LENT"), variant("spin_fast", "RAPIDE") }),
        new AnimFamily("wobble", "wobble", new AnimVariant[] {
            variant("wobble", "NORMAL"), variant("wobble_slow", "LENT"), variant("wobble_fast", "RAPIDE") }),
        new AnimFamily("orbit", "orbit", new AnimVariant[] {
            variant("orbit", "NORMAL"), variant("orbit_small", "PETIT"), variant("orbit_fast", "RAPIDE") }),
        new AnimFamily("bounce", "bounce", new AnimVariant[] {
            variant("bounce", "NORMAL"), variant("bounce_small", "PETIT") }),
        new AnimFamily("sway", "sway", new AnimVariant[] {
            variant("sway", "NORMAL"), variant("sway_big", "GRAND") }),
        new AnimFamily("shake", "shake", new AnimVariant[] {
            variant("shake", "NORMAL"), variant("shake_big", "FORT") }),
        new AnimFamily("pulse", "pulse", new AnimVariant[] {
            variant("pulse", "NORMAL"), variant("pulse_big", "GRAND") }),
        new AnimFamily("flip", "flip", new AnimVariant[] {
            variant("flip", "NORMAL"), variant("flip_full", "COMPLET") }),
        new AnimFamily("tumble", "tumble", new AnimVariant[] { variant("tumble", "tumble") }),
        new AnimFamily("heartbeat", "heartbeat", new AnimVariant[] { variant("heartbeat", "heartbeat") }),
        new AnimFamily("breathe", "breathe", new AnimVariant[] { variant("breathe", "breathe") }),
        new AnimFamily("wave", "wave", new AnimVariant[] { variant("wave", "wave") }),
    };

    private static final int IMAGES_PER_ROW = 5;
    private static final int ANIMS_PER_ROW = 8;

    private final VaryonHologramsPlugin plugin;
    private String hologramName;
    private int editingLineIndex = -1;
    private HologramLineFormat.EditState editState = new HologramLineFormat.EditState();
    @javax.annotation.Nullable private String holoAnimPickerFamily;

    public HologramEditorPage(@Nonnull PlayerRef playerRef, @Nonnull VaryonHologramsPlugin plugin,
                               @Nonnull Hologram hologram) {
        super(playerRef, CustomPageLifetime.CanDismiss, HologramEditorEventData.CODEC);
        this.plugin = plugin;
        this.hologramName = hologram.getName();
        syncHoloAnimPickerFamily(hologram.getAnimation());
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder evt, @Nonnull Store<EntityStore> store) {
        Hologram hologram = plugin.getHologramManager().getHologram(hologramName);
        if (hologram == null) { closePage(ref, store); return; }

        cmd.append("Pages/HologramEditorPage.ui");
        cmd.set("#HologramNameInput.Value", hologram.getName());
        cmd.set("#HologramGroupInput.Value", safeStr(hologram.getGroup()));

        Vector3d pos = hologram.getPosition();
        cmd.set("#XInput.Value", String.format(Locale.US, "%.1f", pos.x));
        cmd.set("#YInput.Value", String.format(Locale.US, "%.1f", pos.y));
        cmd.set("#ZInput.Value", String.format(Locale.US, "%.1f", pos.z));

        buildHologramAnimSection(hologram, cmd, evt);
        buildLinesList(hologram, cmd, evt);
        buildLineEditor(cmd, evt);
        bindEditorEvents(evt);
        bindPageEvents(evt);
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
            case "setMode" -> {
                if (!canEdit) { deny(playerRef, ref, store); return; }
                applyMode(parseMode(data.getMode()));
                refreshUI(ref, store);
            }
            case "toggleBillboard" -> {
                if (!canEdit) { deny(playerRef, ref, store); return; }
                editState.billboard = !editState.billboard;
                refreshUI(ref, store);
            }
            case "setSingleSided" -> {
                if (!canEdit) { deny(playerRef, ref, store); return; }
                editState.doubleSided = false;
                refreshUI(ref, store);
            }
            case "setDoubleSided" -> {
                if (!canEdit) { deny(playerRef, ref, store); return; }
                editState.doubleSided = true;
                refreshUI(ref, store);
            }
            case "selectAnim" -> {
                if (!canEdit) { deny(playerRef, ref, store); return; }
                String anim = data.getAnim();
                if (anim == null || anim.isBlank()) return;
                if ("none".equalsIgnoreCase(anim)) {
                    holoAnimPickerFamily = null;
                    plugin.getHologramManager().setHologramAnimation(hologramName, null);
                } else if ("back".equalsIgnoreCase(anim)) {
                    holoAnimPickerFamily = null;
                } else if (anim.startsWith("family:")) {
                    String familyId = anim.substring("family:".length());
                    AnimFamily family = findFamily(familyId);
                    if (family == null) return;
                    if (family.hasMultipleVariants()) {
                        holoAnimPickerFamily = familyId;
                    } else {
                        holoAnimPickerFamily = null;
                        plugin.getHologramManager().setHologramAnimation(hologramName, family.variants[0].id);
                    }
                } else {
                    plugin.getHologramManager().setHologramAnimation(hologramName, anim);
                    syncHoloAnimPickerFamily(anim);
                }
                refreshUI(ref, store);
            }
            case "selectImage" -> {
                if (!canEdit) { deny(playerRef, ref, store); return; }
                if (data.getImage() != null && !data.getImage().isBlank()) {
                    editState.imageName = data.getImage().trim();
                }
                refreshUI(ref, store);
            }
            case "scaleUp" -> {
                if (!canEdit) { deny(playerRef, ref, store); return; }
                editState.scale = clampScale(editState.scale + 0.25f);
                refreshUI(ref, store);
            }
            case "scaleDown" -> {
                if (!canEdit) { deny(playerRef, ref, store); return; }
                editState.scale = clampScale(editState.scale - 0.25f);
                refreshUI(ref, store);
            }
            case "itemScaleUp" -> {
                if (!canEdit) { deny(playerRef, ref, store); return; }
                editState.scale = clampScale(editState.scale + 0.25f);
                refreshUI(ref, store);
            }
            case "itemScaleDown" -> {
                if (!canEdit) { deny(playerRef, ref, store); return; }
                editState.scale = clampScale(editState.scale - 0.25f);
                refreshUI(ref, store);
            }
            case "addLine" -> {
                if (!canEdit) { deny(playerRef, ref, store); return; }
                mergeEditorFields(data);
                String line = HologramLineFormat.format(editState);
                if (!line.isBlank()) {
                    hologram.addLine(line);
                    plugin.getHologramManager().updateHologram(hologram);
                    resetEditor();
                }
                refreshUI(ref, store);
            }
            case "updateLine" -> {
                if (!canEdit) { deny(playerRef, ref, store); return; }
                mergeEditorFields(data);
                if (editingLineIndex >= 0 && editingLineIndex < hologram.getLineCount()) {
                    hologram.setLine(editingLineIndex, HologramLineFormat.format(editState));
                    plugin.getHologramManager().updateHologram(hologram);
                    resetEditor();
                }
                refreshUI(ref, store);
            }
            case "removeLine" -> {
                if (!canDelete) { deny(playerRef, ref, store); return; }
                int idx = data.getLineIndexInt();
                if (idx >= 0 && idx < hologram.getLineCount()) {
                    hologram.removeLine(idx);
                    plugin.getHologramManager().updateHologram(hologram);
                    resetEditor();
                }
                refreshUI(ref, store);
            }
            case "editLine" -> {
                int idx = data.getLineIndexInt();
                if (idx >= 0 && idx < hologram.getLineCount()) {
                    editingLineIndex = idx;
                    editState = HologramLineFormat.parse(hologram.getLines().get(idx));
                }
                refreshUI(ref, store);
            }
            case "cancelEdit" -> {
                resetEditor();
                refreshUI(ref, store);
            }
            case "rename" -> {
                if (!canEdit) { deny(playerRef, ref, store); return; }
                String newName = data.getHologramNameInput();
                if (newName == null || newName.isBlank()) {
                    playerRef.sendMessage(Message.raw("Nom vide.").color("#FF5555"));
                    refreshUI(ref, store);
                    return;
                }
                try {
                    plugin.getHologramManager().renameHologram(hologramName, newName);
                    hologramName = HologramNames.requireValid(newName);
                    playerRef.sendMessage(Message.raw("Renommé en '").color("#55FF55")
                        .insert(Message.raw(hologramName).color("#FFFF55"))
                        .insert(Message.raw("'.").color("#55FF55")));
                } catch (IllegalArgumentException e) {
                    playerRef.sendMessage(Message.raw(e.getMessage()).color("#FF5555"));
                }
                refreshUI(ref, store);
            }
            case "setGroup" -> {
                if (!canEdit) { deny(playerRef, ref, store); return; }
                try {
                    plugin.getHologramManager().setHologramGroup(hologramName, data.getHologramGroupInput());
                    Hologram updated = plugin.getHologramManager().getHologram(hologramName);
                    String label = updated != null
                        ? HologramGroups.display(updated.getGroup())
                        : HologramGroups.display(null);
                    playerRef.sendMessage(Message.raw("Groupe: ").color("#55FF55")
                        .insert(Message.raw(label).color("#FFFF55")));
                } catch (IllegalArgumentException e) {
                    playerRef.sendMessage(Message.raw(e.getMessage()).color("#FF5555"));
                }
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
            case "backToList" -> {
                player.getPageManager().openCustomPage(ref, store,
                    new HologramListPage(playerRef, plugin));
            }
            case "posXUp"   -> nudge(hologram, canMove, playerRef, ref, store,  0.5,  0,    0);
            case "posXDown" -> nudge(hologram, canMove, playerRef, ref, store, -0.5,  0,    0);
            case "posYUp"   -> nudge(hologram, canMove, playerRef, ref, store,  0,    0.5,  0);
            case "posYDown" -> nudge(hologram, canMove, playerRef, ref, store,  0,   -0.5,  0);
            case "posZUp"   -> nudge(hologram, canMove, playerRef, ref, store,  0,    0,    0.5);
            case "posZDown" -> nudge(hologram, canMove, playerRef, ref, store,  0,    0,   -0.5);
        }
    }

    @Override
    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {}

    private void bindPageEvents(@Nonnull UIEventBuilder evt) {
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#RenameButton",
            new EventData().append("Action", "rename").append("@HologramName", "#HologramNameInput.Value"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#SetGroupButton",
            new EventData().append("Action", "setGroup").append("@HologramGroup", "#HologramGroupInput.Value"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BackToListButton",
            EventData.of("Action", "backToList"));
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
    }

    private void bindEditorEvents(@Nonnull UIEventBuilder evt) {
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#ModeTextButton",
            EventData.of("Action", "setMode").append("Mode", "text"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#ModeImageButton",
            EventData.of("Action", "setMode").append("Mode", "image"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#ModeItemButton",
            EventData.of("Action", "setMode").append("Mode", "item"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BillboardButton",
            EventData.of("Action", "toggleBillboard"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#SingleSidedButton",
            EventData.of("Action", "setSingleSided"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#DoubleSidedButton",
            EventData.of("Action", "setDoubleSided"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#ScaleUpButton",
            EventData.of("Action", "scaleUp"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#ScaleDownButton",
            EventData.of("Action", "scaleDown"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#ItemScaleUpButton",
            EventData.of("Action", "itemScaleUp"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#ItemScaleDownButton",
            EventData.of("Action", "itemScaleDown"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#AddLineButton",
            new EventData().append("Action", "addLine")
                .append("@TextContent", "#TextContentInput.Value")
                .append("@ItemId", "#ItemIdInput.Value")
                .append("@Scale", "#ScaleInput.Value")
                .append("@ItemScale", "#ItemScaleInput.Value"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#UpdateLineButton",
            new EventData().append("Action", "updateLine")
                .append("@TextContent", "#TextContentInput.Value")
                .append("@ItemId", "#ItemIdInput.Value")
                .append("@Scale", "#ScaleInput.Value")
                .append("@ItemScale", "#ItemScaleInput.Value"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CancelEditButton",
            EventData.of("Action", "cancelEdit"));
    }

    private void buildLineEditor(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt) {
        boolean editing = editingLineIndex >= 0;
        cmd.set("#EditModeLabel.Text", editing
            ? "Edition ligne " + (editingLineIndex + 1)
            : "Nouvelle ligne");
        cmd.set("#AddLineButton.Visible", !editing);
        cmd.set("#UpdateLineButton.Visible", editing);
        cmd.set("#CancelEditButton.Visible", editing);

        cmd.set("#TextContentInput.Value", safeStr(editState.text));
        cmd.set("#ItemIdInput.Value", safeStr(editState.itemId));
        cmd.set("#ScaleInput.Value", formatScale(editState.scale));
        cmd.set("#ItemScaleInput.Value", formatScale(editState.scale));

        if (editState.mode == HologramLineFormat.Mode.IMAGE) {
            buildImagePickList(cmd, evt);
        } else {
            cmd.clear("#ImagePickList");
        }

        cmd.set("#TextModeSection.Visible", editState.mode == HologramLineFormat.Mode.TEXT);
        cmd.set("#ImageModeSection.Visible", editState.mode == HologramLineFormat.Mode.IMAGE);
        cmd.set("#ItemModeSection.Visible", editState.mode == HologramLineFormat.Mode.ITEM);

        if (editState.mode == HologramLineFormat.Mode.IMAGE) {
            highlightImageOption("#BillboardButton", editState.billboard, cmd);
            highlightImageOption("#SingleSidedButton", !editState.doubleSided, cmd);
            highlightImageOption("#DoubleSidedButton", editState.doubleSided, cmd);
        }
    }

    private static void highlightImageOption(@Nonnull String selector, boolean active, @Nonnull UICommandBuilder cmd) {
        cmd.set(selector + ".Background", active ? "#3a6a9e" : "#2a3544");
    }

    private void buildImagePickList(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt) {
        cmd.clear("#ImagePickList");
        Set<String> images = plugin.getHologramManager().getImageManager().getAvailableImages();
        List<String> sorted = new ArrayList<>(images);
        sorted.sort(Comparator.naturalOrder());
        if (sorted.isEmpty()) {
            cmd.appendInline("#ImagePickList",
                "Label { Text: \"Aucune image dans images/\"; Style: (TextColor: #96a9be, FontSize: 11); }");
            return;
        }
        String current = safeStr(editState.imageName);
        if (!current.isBlank() && sorted.stream().noneMatch(n -> n.equalsIgnoreCase(current))) {
            sorted.add(0, current);
        }
        int rowIdx = -1;
        int col = 0;
        for (int i = 0; i < sorted.size(); i++) {
            if (col == 0) {
                rowIdx++;
                cmd.appendInline("#ImagePickList", "Group { LayoutMode: Left; Anchor: (Bottom: 2); }");
            }
            String name = sorted.get(i);
            String rowSel = "#ImagePickList[" + rowIdx + "]";
            cmd.append(rowSel, "Pages/HologramImagePickButton.ui");
            String sel = rowSel + "[" + col + "]";
            boolean selected = name.equalsIgnoreCase(current);
            cmd.set(sel + " #ImagePickLabel.Text", name);
            if (selected) {
                cmd.set(sel + " #ImagePickButton.Background", "#3a6a9e");
            }
            evt.addEventBinding(CustomUIEventBindingType.Activating, sel + " #ImagePickButton",
                EventData.of("Action", "selectImage").append("Image", name), false);
            col++;
            if (col >= IMAGES_PER_ROW) col = 0;
        }
    }

    private void buildHologramAnimSection(@Nonnull Hologram hologram, @Nonnull UICommandBuilder cmd,
                                           @Nonnull UIEventBuilder evt) {
        AnimFamily openFamily = holoAnimPickerFamily != null ? findFamily(holoAnimPickerFamily) : null;
        cmd.set("#AnimSectionLabel.Text", openFamily != null
            ? "Animation : " + openFamily.label
            : "Animation");
        buildAnimList(cmd, evt, hologram.getAnimation());
    }

    private void buildAnimList(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt,
                                  @javax.annotation.Nullable String currentAnim) {
        cmd.clear("#AnimList");
        List<String[]> entries = new ArrayList<>();

        if (holoAnimPickerFamily != null) {
            AnimFamily family = findFamily(holoAnimPickerFamily);
            if (family != null) {
                entries.add(new String[] { "back", "< RETOUR" });
                entries.add(new String[] { "none", "AUCUNE" });
                for (AnimVariant variant : family.variants) {
                    entries.add(new String[] { variant.id, variant.label });
                }
            }
        } else {
            entries.add(new String[] { "none", "AUCUNE" });
            for (AnimFamily family : ANIM_FAMILIES) {
                entries.add(new String[] { "family:" + family.id, family.label });
            }
        }

        appendAnimButtons(cmd, evt, entries, currentAnim);
    }

    private void appendAnimButtons(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt,
                                    @Nonnull List<String[]> entries,
                                    @javax.annotation.Nullable String currentAnim) {
        int rowIdx = -1;
        int col = 0;
        String currentFamily = resolveFamilyId(currentAnim);

        for (String[] entry : entries) {
            if (col == 0) {
                rowIdx++;
                cmd.appendInline("#AnimList", "Group { LayoutMode: Left; Anchor: (Bottom: 2); }");
            }
            String rowSel = "#AnimList[" + rowIdx + "]";
            cmd.append(rowSel, "Pages/HologramAnimButton.ui");
            String sel = rowSel + "[" + col + "]";
            String animId = entry[0];
            String label = entry[1];
            boolean selected = isAnimEntrySelected(animId, currentAnim, currentFamily);
            cmd.set(sel + " #AnimLabel.Text", selected ? "> " + label : label);
            evt.addEventBinding(CustomUIEventBindingType.Activating, sel + " #AnimButton",
                EventData.of("Action", "selectAnim").append("Anim", animId), false);
            col++;
            if (col >= ANIMS_PER_ROW) col = 0;
        }
    }

    private static boolean isAnimEntrySelected(@Nonnull String animId, @javax.annotation.Nullable String currentAnim,
                                                @javax.annotation.Nullable String currentFamily) {
        if ("none".equalsIgnoreCase(animId)) {
            return currentAnim == null || currentAnim.isBlank();
        }
        if ("back".equalsIgnoreCase(animId)) {
            return false;
        }
        if (animId.startsWith("family:")) {
            return animId.substring("family:".length()).equalsIgnoreCase(currentFamily);
        }
        return animId.equalsIgnoreCase(currentAnim);
    }

    private void syncHoloAnimPickerFamily(@javax.annotation.Nullable String currentAnim) {
        if (currentAnim == null || currentAnim.isBlank()) {
            holoAnimPickerFamily = null;
            return;
        }
        String familyId = resolveFamilyId(currentAnim);
        AnimFamily family = familyId != null ? findFamily(familyId) : null;
        holoAnimPickerFamily = family != null && family.hasMultipleVariants() ? familyId : null;
    }

    @javax.annotation.Nullable
    private static String resolveFamilyId(@javax.annotation.Nullable String animId) {
        if (animId == null || animId.isBlank()) return null;
        for (AnimFamily family : ANIM_FAMILIES) {
            for (AnimVariant variant : family.variants) {
                if (variant.id.equalsIgnoreCase(animId)) return family.id;
            }
        }
        return null;
    }

    @javax.annotation.Nullable
    private static AnimFamily findFamily(@Nonnull String familyId) {
        for (AnimFamily family : ANIM_FAMILIES) {
            if (family.id.equalsIgnoreCase(familyId)) return family;
        }
        return null;
    }

    private static AnimVariant variant(@Nonnull String id, @Nonnull String label) {
        return new AnimVariant(id, label);
    }

    private record AnimVariant(@Nonnull String id, @Nonnull String label) {}

    private record AnimFamily(@Nonnull String id, @Nonnull String label, @Nonnull AnimVariant[] variants) {
        boolean hasMultipleVariants() {
            return variants.length > 1;
        }
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
            String summary = escape(summarizeLine(lines.get(i)));
            cmd.set(selector + " #LineText.Text", i == editingLineIndex ? "> " + summary : summary);
            cmd.set(selector + " #DeleteLabel.Text", "X");
            evt.addEventBinding(CustomUIEventBindingType.Activating, selector + " #Button",
                EventData.of("Action", "editLine").append("LineIndex", String.valueOf(i)), false);
            evt.addEventBinding(CustomUIEventBindingType.Activating, selector + " #LineDeleteButton",
                EventData.of("Action", "removeLine").append("LineIndex", String.valueOf(i)), false);
        }
    }

    private void refreshUI(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        Hologram hologram = plugin.getHologramManager().getHologram(hologramName);
        if (hologram == null) { closePage(ref, store); return; }

        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder evt = new UIEventBuilder();
        Vector3d pos = hologram.getPosition();
        cmd.set("#XInput.Value", String.format(Locale.US, "%.1f", pos.x));
        cmd.set("#YInput.Value", String.format(Locale.US, "%.1f", pos.y));
        cmd.set("#ZInput.Value", String.format(Locale.US, "%.1f", pos.z));
        cmd.set("#HologramNameInput.Value", hologram.getName());
        cmd.set("#HologramGroupInput.Value", safeStr(hologram.getGroup()));
        buildHologramAnimSection(hologram, cmd, evt);
        buildLinesList(hologram, cmd, evt);
        buildLineEditor(cmd, evt);
        bindEditorEvents(evt);
        sendUpdate(cmd, evt, false);
    }

    private void mergeEditorFields(@Nonnull HologramEditorEventData data) {
        switch (editState.mode) {
            case TEXT -> {
                if (data.getTextContent() != null) editState.text = data.getTextContent();
            }
            case IMAGE -> {
                editState.scale = clampScale(data.getScale(editState.scale));
            }
            case ITEM -> {
                if (data.getItemId() != null && !data.getItemId().isBlank()) {
                    editState.itemId = data.getItemId().trim();
                }
                editState.scale = clampScale(data.getItemScale(editState.scale));
            }
        }
    }

    private void applyMode(@Nonnull HologramLineFormat.Mode mode) {
        editState.mode = mode;
        if (mode == HologramLineFormat.Mode.IMAGE && safeStr(editState.imageName).isBlank()) {
            Set<String> images = plugin.getHologramManager().getImageManager().getAvailableImages();
            if (!images.isEmpty()) {
                editState.imageName = images.iterator().next();
            }
        }
    }

    private void resetEditor() {
        editingLineIndex = -1;
        editState = new HologramLineFormat.EditState();
    }

    private void nudge(@Nonnull Hologram hologram, boolean canMove, @Nonnull PlayerRef playerRef,
                       @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                       double dx, double dy, double dz) {
        if (!canMove) { deny(playerRef, ref, store); return; }
        Vector3d p = hologram.getPosition();
        plugin.getHologramManager().moveHologram(hologram, new Vector3d(p.x + dx, p.y + dy, p.z + dz));
        refreshUI(ref, store);
    }

    private void deny(@Nonnull PlayerRef playerRef, @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        playerRef.sendMessage(Message.raw("Permission refusée.").color("#FF5555"));
        refreshUI(ref, store);
    }

    private void closePage(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) player.getPageManager().setPage(ref, store, Page.None);
    }

    @Nonnull
    private static HologramLineFormat.Mode parseMode(@javax.annotation.Nullable String mode) {
        if (mode == null) return HologramLineFormat.Mode.TEXT;
        return switch (mode.toLowerCase(Locale.ROOT)) {
            case "image" -> HologramLineFormat.Mode.IMAGE;
            case "item" -> HologramLineFormat.Mode.ITEM;
            default -> HologramLineFormat.Mode.TEXT;
        };
    }

    private static float clampScale(float scale) {
        return Math.max(0.1f, Math.min(100f, scale));
    }

    @Nonnull
    private static String formatScale(float scale) {
        if (scale == (long) scale) return String.valueOf((long) scale);
        return String.format(Locale.US, "%.2f", scale);
    }

    @Nonnull
    private static String safeStr(@javax.annotation.Nullable String value) {
        return value != null ? value : "";
    }

    @Nonnull
    private static String summarizeLine(@Nonnull String line) {
        HologramLineFormat.EditState state = HologramLineFormat.parse(line);
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(HologramLineFormat.modeLabel(state.mode)).append("] ");
        switch (state.mode) {
            case IMAGE -> {
                sb.append(safeStr(state.imageName).isBlank() ? "?" : state.imageName);
                if (state.scale != 1f) sb.append(" x").append(formatScale(state.scale));
                if (state.billboard) sb.append(" BB");
                if (state.doubleSided) sb.append(" 2F");
            }
            case ITEM -> {
                sb.append(safeStr(state.itemId).isBlank() ? "?" : state.itemId);
                if (state.scale != 1f) sb.append(" x").append(formatScale(state.scale));
            }
            default -> sb.append(state.text != null ? state.text : "");
        }
        return sb.toString();
    }

    private static String escape(@Nonnull String text) {
        return text.replace(";", ",").replace("{", "(").replace("}", ")").replace("\"", "'");
    }
}
