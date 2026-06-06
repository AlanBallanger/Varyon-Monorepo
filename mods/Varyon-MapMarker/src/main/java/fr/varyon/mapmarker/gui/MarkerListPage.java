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
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

public class MarkerListPage extends InteractiveCustomUIPage<MarkerEventData> {

    private final VaryonMapMarkerPlugin plugin;
    private final World world;
    @Nullable private String currentFolder;

    public MarkerListPage(@Nonnull PlayerRef playerRef, @Nonnull VaryonMapMarkerPlugin plugin,
                          @Nonnull World world) {
        super(playerRef, CustomPageLifetime.CanDismiss, MarkerEventData.CODEC);
        this.plugin = plugin;
        this.world = world;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder evt, @Nonnull Store<EntityStore> store) {
        cmd.append("MarkerListPage.ui");
        cmd.set("#CreateNameInput.Value", "");
        cmd.set("#CreateImageInput.Value", "");
        buildList(cmd, evt);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CreateButton",
            new EventData().append("Action", "create")
                .append("@CreateName", "#CreateNameInput.Value")
                .append("@CreateImage", "#CreateImageInput.Value"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#ListCloseButton",
            EventData.of("Action", "close"));
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                                @Nonnull MarkerEventData data) {
        String action = data.getAction();
        if (action == null) return;

        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        Player player = store.getComponent(ref, Player.getComponentType());
        if (playerRef == null || player == null) return;

        switch (action) {
            case "edit" -> {
                String id = data.getMarkerId();
                if (id == null) return;
                MarkerEntry entry = plugin.findMarkerById(id);
                if (entry == null) { refreshUI(ref, store); return; }
                player.getPageManager().openCustomPage(ref, store,
                    new MarkerEditorPage(playerRef, plugin, world, id));
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
            case "create" -> {
                TransformComponent tc = store.getComponent(ref, TransformComponent.getComponentType());
                if (tc == null) { refreshUI(ref, store); return; }
                String name = data.getCreateName();
                String image = data.getCreateImage();
                if (name == null || name.isBlank()) {
                    playerRef.sendMessage(Message.raw("Entrez un nom pour le marqueur.").color("#FF5555"));
                    refreshUI(ref, store);
                    return;
                }
                if (image == null || image.isBlank()) {
                    playerRef.sendMessage(Message.raw("Entrez un nom d'image.").color("#FF5555"));
                    refreshUI(ref, store);
                    return;
                }
                plugin.createSharedMarkerFromPlayer(playerRef, world, image, name);
                if (currentFolder != null) {
                    MarkerEntry created = plugin.listMarkersInWorld(world).stream()
                        .filter(e -> e.markerName().equalsIgnoreCase(name))
                        .findFirst().orElse(null);
                    if (created != null) {
                        plugin.updateMarkerGroup(world, created.id(), currentFolder);
                    }
                }
                refreshUI(ref, store);
            }
            case "close" -> closePage(ref, store);
        }
    }

    @Override
    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {}

    private void buildList(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt) {
        cmd.clear("#MarkerList");
        List<MarkerEntry> allMarkers = plugin.listMarkersInWorld(world);
        List<String> allGroups = plugin.listGroupsInWorld(world);
        List<String> childFolders = getDirectChildFolders(allGroups, currentFolder);
        List<MarkerEntry> folderMarkers = allMarkers.stream()
            .filter(e -> Objects.equals(normalizeGroup(e.group()), normalizeGroup(currentFolder)))
            .sorted(Comparator.comparing(MarkerEntry::markerName, String.CASE_INSENSITIVE_ORDER))
            .toList();

        boolean empty = childFolders.isEmpty() && folderMarkers.isEmpty();
        cmd.set("#EmptyLabel.Visible", empty);

        int row = 0;
        if (currentFolder != null) {
            String selector = "#MarkerList[" + row + "]";
            cmd.append("#MarkerList", "Pages/MarkerFolderBackItem.ui");
            evt.addEventBinding(CustomUIEventBindingType.Activating, selector + " #BackButton",
                EventData.of("Action", "folderBack"), false);
            row++;
        }

        for (String folder : childFolders) {
            String selector = "#MarkerList[" + row + "]";
            cmd.append("#MarkerList", "Pages/MarkerGroupItem.ui");
            cmd.set(selector + " #GroupLabel.Text", escape(folderDisplayName(folder, currentFolder)));
            evt.addEventBinding(CustomUIEventBindingType.Activating, selector + " #OpenFolderButton",
                EventData.of("Action", "openFolder").append("GroupPath", folder), false);
            row++;
        }

        for (MarkerEntry e : folderMarkers) {
            String selector = "#MarkerList[" + row + "]";
            String rowText = e.markerName()
                + "  |  X " + String.format("%.0f", e.x())
                + "  Z " + String.format("%.0f", e.z());
            cmd.append("#MarkerList", "Pages/MarkerListItem.ui");
            cmd.set(selector + " #RowLabel.Text", escape(rowText));
            evt.addEventBinding(CustomUIEventBindingType.Activating, selector + " #EditButton",
                EventData.of("Action", "edit").append("MarkerId", e.id()), false);
            row++;
        }
    }

    private void refreshUI(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder evt = new UIEventBuilder();
        buildList(cmd, evt);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CreateButton",
            new EventData().append("Action", "create")
                .append("@CreateName", "#CreateNameInput.Value")
                .append("@CreateImage", "#CreateImageInput.Value"));
        sendUpdate(cmd, evt, false);
    }

    @Nonnull
    private static List<String> getDirectChildFolders(@Nonnull List<String> allPaths,
                                                       @Nullable String parent) {
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

    @Nullable
    private static String parentFolder(@Nullable String folder) {
        if (folder == null || folder.isBlank()) return null;
        int slash = folder.lastIndexOf('/');
        return slash < 0 ? null : folder.substring(0, slash);
    }

    @Nullable
    private static String normalizeGroup(@Nullable String group) {
        return (group == null || group.isBlank()) ? null : group;
    }

    private void closePage(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) player.getPageManager().setPage(ref, store, Page.None);
    }

    private static String escape(@Nonnull String text) {
        return text.replace(";", ",").replace("{", "(").replace("}", ")").replace("\"", "'");
    }
}
