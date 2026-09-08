package fr.varyon.musiczones.gui;

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
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.musiczones.MusicZone;
import fr.varyon.musiczones.MusicZoneGroups;
import fr.varyon.musiczones.MusicZoneNames;
import fr.varyon.musiczones.VaryonMusicZonesPlugin;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

public class MusicZoneListPage extends InteractiveCustomUIPage<MusicZoneListEventData> {

    private static final double DEFAULT_HALF_SIZE = 8.0;

    private final VaryonMusicZonesPlugin plugin;
    @Nullable private String currentFolder;

    public MusicZoneListPage(@Nonnull PlayerRef playerRef, @Nonnull VaryonMusicZonesPlugin plugin) {
        super(playerRef, CustomPageLifetime.CanDismiss, MusicZoneListEventData.CODEC);
        this.plugin = plugin;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder evt, @Nonnull Store<EntityStore> store) {
        cmd.append("MusicZoneListPage.ui");
        cmd.set("#CreateNameInput.Value", "");
        cmd.set("#CreateGroupInput.Value", "");
        cmd.set("#CreateZoneGroupInput.Value", currentFolder == null ? "" : currentFolder);
        buildList(ref, store, cmd, evt);
        bindFooterEvents(evt);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#ListCloseButton",
                EventData.of("Action", "close"));
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                                 @Nonnull MusicZoneListEventData data) {
        String action = data.getAction();
        if (action == null) {
            return;
        }
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        Player player = store.getComponent(ref, Player.getComponentType());
        if (playerRef == null || player == null) {
            return;
        }
        String worldName = worldName(playerRef);

        switch (action) {
            case "edit" -> {
                String id = data.getZoneId();
                if (id == null) {
                    return;
                }
                MusicZone zone = plugin.getRepository().find(worldName, id);
                if (zone == null) {
                    refreshUI(ref, store);
                    return;
                }
                player.getPageManager().openCustomPage(ref, store,
                        new MusicZoneEditorPage(playerRef, plugin, worldName, id));
            }
            case "openFolder" -> {
                String path = data.getGroupPath();
                if (path == null || path.isBlank()) {
                    refreshUI(ref, store);
                    return;
                }
                currentFolder = path;
                refreshUI(ref, store);
            }
            case "folderBack" -> {
                currentFolder = parentFolder(currentFolder);
                refreshUI(ref, store);
            }
            case "createGroup" -> {
                if (!canManage(playerRef)) {
                    deny(playerRef, ref, store);
                    return;
                }
                try {
                    String raw = data.getCreateGroup();
                    if (raw == null || raw.isBlank()) {
                        playerRef.sendMessage(Message.raw("Entrez un nom de dossier.").color("#FF5555"));
                        refreshUI(ref, store);
                        return;
                    }
                    String group = resolveGroupPath(raw, currentFolder);
                    plugin.getRepository().addEmptyGroup(group);
                    plugin.getRepository().save();
                    playerRef.sendMessage(Message.raw("Dossier '").color("#55FF55")
                            .insert(Message.raw(group).color("#FFFF55"))
                            .insert(Message.raw("' créé.").color("#55FF55")));
                } catch (IllegalArgumentException e) {
                    playerRef.sendMessage(Message.raw(e.getMessage()).color("#FF5555"));
                }
                refreshUI(ref, store);
            }
            case "create" -> {
                if (!canManage(playerRef)) {
                    deny(playerRef, ref, store);
                    return;
                }
                TransformComponent tc = store.getComponent(ref, TransformComponent.getComponentType());
                if (tc == null) {
                    refreshUI(ref, store);
                    return;
                }
                try {
                    String rawName = data.getCreateName();
                    if (rawName == null || rawName.isBlank()) {
                        playerRef.sendMessage(Message.raw("Entrez un nom pour la zone.").color("#FF5555"));
                        refreshUI(ref, store);
                        return;
                    }
                    String id = MusicZoneNames.requireValid(rawName);
                    if (plugin.getRepository().find(worldName, id) != null) {
                        playerRef.sendMessage(Message.raw("Une zone « " + id + " » existe déjà dans "
                                + worldName + ".").color("#FF5555"));
                        refreshUI(ref, store);
                        return;
                    }
                    String groupInput = data.getCreateZoneGroup();
                    if ((groupInput == null || groupInput.isBlank()) && currentFolder != null) {
                        groupInput = currentFolder;
                    }
                    String group = MusicZoneGroups.normalize(groupInput);

                    VaryonMusicZonesPlugin.PendingBox box = playerRef.getUuid() != null
                            ? plugin.takePendingBox(playerRef.getUuid(), worldName)
                            : null;
                    MusicZone zone = box != null
                            ? new MusicZone(id, worldName, group,
                                    box.x1(), box.y1(), box.z1(), box.x2(), box.y2(), box.z2(),
                                    firstMusicOrEmpty(), 0.0).withBox(
                                    box.x1(), box.y1(), box.z1(), box.x2(), box.y2(), box.z2())
                            : defaultBoxZone(id, worldName, group, tc.getPosition());

                    plugin.getRepository().addOrReplace(zone);
                    plugin.getRepository().save();
                    MusicZoneEdits.rebuildPackAsync(plugin);
                    player.getPageManager().openCustomPage(ref, store,
                            new MusicZoneEditorPage(playerRef, plugin, worldName, id));
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

    private MusicZone defaultBoxZone(String id, String worldName, @Nullable String group, Vector3d pos) {
        return new MusicZone(id, worldName, group,
                pos.x - DEFAULT_HALF_SIZE, pos.y - DEFAULT_HALF_SIZE, pos.z - DEFAULT_HALF_SIZE,
                pos.x + DEFAULT_HALF_SIZE, pos.y + DEFAULT_HALF_SIZE, pos.z + DEFAULT_HALF_SIZE,
                firstMusicOrEmpty(), 0.0);
    }

    private String firstMusicOrEmpty() {
        List<String> files = plugin.listMusicFileNames();
        return files.isEmpty() ? "" : files.get(0);
    }

    private void buildList(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                           @Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt) {
        cmd.clear("#ZoneList");
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        String worldName = playerRef != null ? worldName(playerRef) : "";

        List<MusicZone> zones = new ArrayList<>(plugin.getRepository().zonesForWorld(worldName));
        List<String> groupPaths = plugin.getRepository().getSortedGroupPaths();
        List<String> childFolders = getDirectChildFolders(groupPaths, currentFolder);
        List<MusicZone> folderZones = zones.stream()
                .filter(z -> Objects.equals(z.getGroup(), currentFolder))
                .sorted(Comparator.comparing(MusicZone::getId, String.CASE_INSENSITIVE_ORDER))
                .toList();

        boolean nothing = childFolders.isEmpty() && folderZones.isEmpty();
        cmd.set("#EmptyLabel.Visible", nothing);
        cmd.set("#WorldLabel.Text", "Monde : " + (worldName.isBlank() ? "?" : worldName));

        int row = 0;
        if (currentFolder != null) {
            String selector = "#ZoneList[" + row + "]";
            cmd.append("#ZoneList", "Pages/MusicZoneFolderBackItem.ui");
            evt.addEventBinding(CustomUIEventBindingType.Activating, selector + " #BackButton",
                    EventData.of("Action", "folderBack"), false);
            row++;
        }

        for (String folder : childFolders) {
            String selector = "#ZoneList[" + row + "]";
            cmd.append("#ZoneList", "Pages/MusicZoneGroupItem.ui");
            cmd.set(selector + " #GroupLabel.Text", escape(folderDisplayName(folder, currentFolder)));
            evt.addEventBinding(CustomUIEventBindingType.Activating, selector + " #OpenFolderButton",
                    EventData.of("Action", "openFolder").append("GroupPath", folder), false);
            row++;
        }

        for (MusicZone z : folderZones) {
            String selector = "#ZoneList[" + row + "]";
            String music = z.getMusicFileName() == null || z.getMusicFileName().isBlank()
                    ? "(aucune)" : z.getMusicFileName();
            String rowText = z.getId() + "  |  " + music + "  |  "
                    + String.format("%.0f%%", MusicZone.dbToPercent(z.getVolumeDb()));
            cmd.append("#ZoneList", "Pages/MusicZoneListItem.ui");
            cmd.set(selector + " #RowLabel.Text", escape(rowText));
            evt.addEventBinding(CustomUIEventBindingType.Activating, selector + " #EditButton",
                    EventData.of("Action", "edit").append("ZoneId", z.getId()), false);
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
                if (rest.isEmpty()) {
                    continue;
                }
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
        String segment = MusicZoneGroups.normalize(raw);
        if (segment == null) {
            throw new IllegalArgumentException("Nom de dossier invalide.");
        }
        if (segment.contains("/")) {
            return segment;
        }
        if (parent == null || parent.isBlank()) {
            return segment;
        }
        return MusicZoneGroups.normalize(parent + "/" + segment);
    }

    @Nullable
    private static String parentFolder(@Nullable String folder) {
        if (folder == null || folder.isBlank()) {
            return null;
        }
        int slash = folder.lastIndexOf('/');
        return slash < 0 ? null : folder.substring(0, slash);
    }

    private void bindFooterEvents(@Nonnull UIEventBuilder evt) {
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CreateButton",
                new EventData().append("Action", "create")
                        .append("@CreateName", "#CreateNameInput.Value")
                        .append("@CreateZoneGroup", "#CreateZoneGroupInput.Value"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CreateGroupButton",
                new EventData().append("Action", "createGroup")
                        .append("@CreateGroup", "#CreateGroupInput.Value"));
    }

    private void refreshUI(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder evt = new UIEventBuilder();
        cmd.set("#CreateZoneGroupInput.Value", currentFolder == null ? "" : currentFolder);
        buildList(ref, store, cmd, evt);
        bindFooterEvents(evt);
        sendUpdate(cmd, evt, false);
    }

    private String worldName(@Nonnull PlayerRef playerRef) {
        World w = Universe.get().getWorld(playerRef.getWorldUuid());
        return w != null ? w.getName() : "";
    }

    private static boolean canManage(@Nonnull PlayerRef playerRef) {
        return playerRef.hasPermission("*") || playerRef.hasPermission("varyon.musiczones.admin");
    }

    private void deny(@Nonnull PlayerRef playerRef, @Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        playerRef.sendMessage(Message.raw("Op ou permission varyon.musiczones.admin requise.").color("#FF5555"));
        refreshUI(ref, store);
    }

    private void closePage(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            player.getPageManager().setPage(ref, store, Page.None);
        }
    }

    private static String escape(@Nonnull String text) {
        return text.replace(";", ",").replace("{", "(").replace("}", ")").replace("\"", "'");
    }
}
