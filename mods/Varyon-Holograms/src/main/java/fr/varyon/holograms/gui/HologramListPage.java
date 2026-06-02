package fr.varyon.holograms.gui;

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
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.holograms.VaryonHologramsPlugin;
import fr.varyon.holograms.hologram.Hologram;
import fr.varyon.holograms.hologram.HologramGroups;
import fr.varyon.holograms.hologram.HologramNames;
import org.joml.Vector3d;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

public class HologramListPage extends InteractiveCustomUIPage<HologramListEventData> {

    private final VaryonHologramsPlugin plugin;
    @Nullable private String currentFolder;

    public HologramListPage(@Nonnull PlayerRef playerRef, @Nonnull VaryonHologramsPlugin plugin) {
        super(playerRef, CustomPageLifetime.CanDismiss, HologramListEventData.CODEC);
        this.plugin = plugin;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder evt, @Nonnull Store<EntityStore> store) {
        cmd.append("HologramListPage.ui");
        cmd.set("#CreateNameInput.Value", "");
        cmd.set("#CreateGroupInput.Value", "");
        cmd.set("#CreateHoloGroupInput.Value", currentFolder == null ? "" : currentFolder);
        buildList(cmd, evt);
        bindFooterEvents(evt);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#ListCloseButton",
            EventData.of("Action", "close"));
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                                 @Nonnull HologramListEventData data) {
        String action = data.getAction();
        if (action == null) return;

        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        Player player = store.getComponent(ref, Player.getComponentType());
        if (playerRef == null || player == null) return;

        switch (action) {
            case "edit" -> {
                String name = data.getHoloName();
                if (name == null) return;
                Hologram h = plugin.getHologramManager().getHologram(name);
                if (h == null) { refreshUI(ref, store); return; }
                player.getPageManager().openCustomPage(ref, store,
                    new HologramEditorPage(playerRef, plugin, h));
            }
            case "openFolder" -> {
                String path = data.getGroupPath();
                if (path == null || path.isBlank()) { refreshUI(ref, store); return; }
                currentFolder = path;
                refreshUI(ref, store);
            }
            case "folderBack" -> {
                currentFolder = parentFolder(currentFolder);
                refreshUI(ref, store);
            }
            case "createGroup" -> {
                if (!canCreate(playerRef)) { refreshUI(ref, store); return; }
                try {
                    String raw = data.getCreateGroup();
                    if (raw == null || raw.isBlank()) {
                        playerRef.sendMessage(Message.raw("Entrez un nom de dossier.").color("#FF5555"));
                        refreshUI(ref, store);
                        return;
                    }
                    String group = resolveGroupPath(raw, currentFolder);
                    plugin.getHologramManager().createGroup(group);
                    playerRef.sendMessage(Message.raw("Dossier '").color("#55FF55")
                        .insert(Message.raw(group).color("#FFFF55"))
                        .insert(Message.raw("' créé.").color("#55FF55")));
                } catch (IllegalArgumentException e) {
                    playerRef.sendMessage(Message.raw(e.getMessage()).color("#FF5555"));
                }
                refreshUI(ref, store);
            }
            case "create" -> {
                if (!canCreate(playerRef)) { refreshUI(ref, store); return; }
                TransformComponent tc = store.getComponent(ref, TransformComponent.getComponentType());
                if (tc == null) { refreshUI(ref, store); return; }
                Vector3d pos = tc.getPosition();
                try {
                    String rawName = data.getCreateName();
                    if (rawName == null || rawName.isBlank()) {
                        playerRef.sendMessage(Message.raw("Entrez un nom pour l'hologramme.").color("#FF5555"));
                        refreshUI(ref, store);
                        return;
                    }
                    String name = HologramNames.requireValid(rawName);
                    String groupInput = data.getCreateHoloGroup();
                    if ((groupInput == null || groupInput.isBlank()) && currentFolder != null) {
                        groupInput = currentFolder;
                    }
                    Hologram h = plugin.getHologramManager().createHologram(name,
                        new Vector3d(pos.x, pos.y + 1.5, pos.z),
                        plugin.getHologramManager().resolveWorldId(ref, store),
                        playerRef.getUuid(),
                        groupInput);
                    player.getPageManager().openCustomPage(ref, store,
                        new HologramEditorPage(playerRef, plugin, h));
                } catch (IllegalArgumentException e) {
                    playerRef.sendMessage(Message.raw(e.getMessage()).color("#FF5555"));
                    refreshUI(ref, store);
                }
            }
            case "close" -> closePage(ref, store);
        }
    }

    @Override
    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {}

    private void buildList(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt) {
        cmd.clear("#HologramList");
        List<Hologram> holos = new ArrayList<>(plugin.getHologramManager().getAllHolograms());
        List<String> groupPaths = plugin.getHologramManager().getSortedGroupPaths();
        List<String> childFolders = getDirectChildFolders(groupPaths, currentFolder);
        List<Hologram> folderHolos = holos.stream()
            .filter(h -> Objects.equals(normalizeGroup(h.getGroup()), normalizeGroup(currentFolder)))
            .sorted(Comparator.comparing(Hologram::getName, String.CASE_INSENSITIVE_ORDER))
            .toList();

        if (childFolders.isEmpty() && folderHolos.isEmpty() && currentFolder == null && holos.isEmpty() && groupPaths.isEmpty()) {
            cmd.set("#EmptyLabel.Visible", true);
            return;
        }

        cmd.set("#EmptyLabel.Visible", childFolders.isEmpty() && folderHolos.isEmpty());

        int row = 0;
        if (currentFolder != null) {
            String selector = "#HologramList[" + row + "]";
            cmd.append("#HologramList", "Pages/HologramFolderBackItem.ui");
            evt.addEventBinding(CustomUIEventBindingType.Activating, selector + " #BackButton",
                EventData.of("Action", "folderBack"), false);
            row++;
        }

        for (String folder : childFolders) {
            String selector = "#HologramList[" + row + "]";
            cmd.append("#HologramList", "Pages/HologramGroupItem.ui");
            cmd.set(selector + " #GroupLabel.Text", escape(folderDisplayName(folder, currentFolder)));
            evt.addEventBinding(CustomUIEventBindingType.Activating, selector + " #OpenFolderButton",
                EventData.of("Action", "openFolder").append("GroupPath", folder), false);
            row++;
        }

        for (Hologram h : folderHolos) {
            String selector = "#HologramList[" + row + "]";
            Vector3d p = h.getPosition();
            String rowText = h.getName() + "  |  " + h.getLineCount() + " ligne(s) - "
                + String.format("%.1f, %.1f, %.1f", p.x, p.y, p.z);

            cmd.append("#HologramList", "Pages/HologramListItem.ui");
            cmd.set(selector + " #RowLabel.Text", escape(rowText));
            evt.addEventBinding(CustomUIEventBindingType.Activating, selector + " #EditButton",
                EventData.of("Action", "edit").append("HoloName", h.getName()), false);
            row++;
        }
    }

    @Nonnull
    private static List<String> getDirectChildFolders(@Nonnull List<String> allPaths, @Nullable String parent) {
        TreeSet<String> children = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        String prefix = parent == null ? "" : parent + "/";
        for (String path : allPaths) {
            if (parent == null) {
                int slash = path.indexOf('/');
                children.add(slash < 0 ? path : path.substring(0, slash));
            } else if (path.startsWith(prefix)) {
                String rest = path.substring(prefix.length());
                if (rest.isEmpty()) continue;
                int slash = rest.indexOf('/');
                children.add(slash < 0 ? parent + "/" + rest : parent + "/" + rest.substring(0, slash));
            }
        }
        return new ArrayList<>(children);
    }

    @Nonnull
    private static String folderDisplayName(@Nonnull String fullPath, @Nullable String parent) {
        if (parent == null) {
            int slash = fullPath.indexOf('/');
            return slash < 0 ? fullPath : fullPath.substring(0, slash);
        }
        String prefix = parent + "/";
        String rest = fullPath.startsWith(prefix) ? fullPath.substring(prefix.length()) : fullPath;
        int slash = rest.indexOf('/');
        return slash < 0 ? rest : rest.substring(0, slash);
    }

    @Nonnull
    private static String resolveGroupPath(@Nonnull String raw, @Nullable String parent) {
        String segment = HologramGroups.normalize(raw);
        if (segment == null) {
            throw new IllegalArgumentException("Nom de dossier invalide.");
        }
        if (segment.contains("/")) {
            return segment;
        }
        if (parent == null || parent.isBlank()) {
            return segment;
        }
        return HologramGroups.normalize(parent + "/" + segment);
    }

    @Nullable
    private static String parentFolder(@Nullable String folder) {
        if (folder == null || folder.isBlank()) return null;
        int slash = folder.lastIndexOf('/');
        return slash < 0 ? null : folder.substring(0, slash);
    }

    @Nullable
    private static String normalizeGroup(@Nullable String group) {
        if (group == null || group.isBlank()) return null;
        return group;
    }

    private void bindFooterEvents(@Nonnull UIEventBuilder evt) {
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CreateButton",
            new EventData().append("Action", "create")
                .append("@CreateName", "#CreateNameInput.Value")
                .append("@CreateHoloGroup", "#CreateHoloGroupInput.Value"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CreateGroupButton",
            new EventData().append("Action", "createGroup")
                .append("@CreateGroup", "#CreateGroupInput.Value"));
    }

    private void refreshUI(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder evt = new UIEventBuilder();
        cmd.set("#CreateHoloGroupInput.Value", currentFolder == null ? "" : currentFolder);
        buildList(cmd, evt);
        bindFooterEvents(evt);
        sendUpdate(cmd, evt, false);
    }

    private static boolean canCreate(@Nonnull PlayerRef playerRef) {
        return playerRef.hasPermission("*") || playerRef.hasPermission("varyon.holograms.create");
    }

    private void closePage(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) player.getPageManager().setPage(ref, store, Page.None);
    }

    private static String escape(@Nonnull String text) {
        return text.replace(";", ",").replace("{", "(").replace("}", ")").replace("\"", "'");
    }
}
