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
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MusicZoneEditorPage extends InteractiveCustomUIPage<MusicZoneEditorEventData> {

    private static final double NUDGE = 1.0;

    private final VaryonMusicZonesPlugin plugin;
    private final String worldName;
    private String zoneId;

    public MusicZoneEditorPage(@Nonnull PlayerRef playerRef, @Nonnull VaryonMusicZonesPlugin plugin,
                               @Nonnull String worldName, @Nonnull String zoneId) {
        super(playerRef, CustomPageLifetime.CanDismiss, MusicZoneEditorEventData.CODEC);
        this.plugin = plugin;
        this.worldName = worldName;
        this.zoneId = zoneId;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder evt, @Nonnull Store<EntityStore> store) {
        MusicZone zone = zone();
        if (zone == null) {
            closePage(ref, store);
            return;
        }
        cmd.append("Pages/MusicZoneEditorPage.ui");
        applyZone(zone, cmd);
        bindEvents(evt);
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                                 @Nonnull MusicZoneEditorEventData data) {
        String action = data.getAction();
        if (action == null) {
            return;
        }
        MusicZone zone = zone();
        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (player == null || playerRef == null) {
            return;
        }
        if (zone == null) {
            closePage(ref, store);
            return;
        }
        if (!canManage(playerRef)) {
            playerRef.sendMessage(Message.raw("Op ou permission varyon.musiczones.admin requise.").color("#FF5555"));
            refreshUI(ref, store);
            return;
        }

        switch (action) {
            case "rename" -> {
                String raw = data.getZoneId();
                if (raw == null || raw.isBlank()) {
                    playerRef.sendMessage(Message.raw("Nom vide.").color("#FF5555"));
                    refreshUI(ref, store);
                    return;
                }
                try {
                    String newId = MusicZoneNames.requireValid(raw);
                    if (newId.equals(zoneId)) {
                        refreshUI(ref, store);
                        return;
                    }
                    if (!plugin.getRepository().rename(worldName, zoneId, newId)) {
                        playerRef.sendMessage(Message.raw("Renommage impossible (nom déjà pris ?).").color("#FF5555"));
                        refreshUI(ref, store);
                        return;
                    }
                    zoneId = newId;
                    plugin.getRepository().save();
                    MusicZoneEdits.rebuildPackAsync(plugin);
                    playerRef.sendMessage(Message.raw("Zone renommée en '").color("#55FF55")
                            .insert(Message.raw(newId).color("#FFFF55"))
                            .insert(Message.raw("'.").color("#55FF55")));
                } catch (IllegalArgumentException e) {
                    playerRef.sendMessage(Message.raw(e.getMessage()).color("#FF5555"));
                }
                refreshUI(ref, store);
            }
            case "setGroup" -> {
                try {
                    String group = MusicZoneGroups.normalize(data.getZoneGroup());
                    plugin.getRepository().addOrReplace(zone.withGroup(group));
                    plugin.getRepository().save();
                    playerRef.sendMessage(Message.raw("Groupe : ").color("#55FF55")
                            .insert(Message.raw(MusicZoneGroups.display(group)).color("#FFFF55")));
                } catch (IllegalArgumentException e) {
                    playerRef.sendMessage(Message.raw(e.getMessage()).color("#FF5555"));
                }
                refreshUI(ref, store);
            }
            case "setBox" -> {
                MusicZone updated = zone.withBox(
                        data.getP1X(zone.getMinX()), data.getP1Y(zone.getMinY()), data.getP1Z(zone.getMinZ()),
                        data.getP2X(zone.getMaxX()), data.getP2Y(zone.getMaxY()), data.getP2Z(zone.getMaxZ()));
                applyBox(ref, store, updated);
            }
            case "p1Here", "p2Here" -> {
                TransformComponent tc = store.getComponent(ref, TransformComponent.getComponentType());
                if (tc == null) {
                    refreshUI(ref, store);
                    return;
                }
                Vector3d p = tc.getPosition();
                MusicZone updated = "p1Here".equals(action)
                        ? zone.withBox(p.x, p.y, p.z, zone.getMaxX(), zone.getMaxY(), zone.getMaxZ())
                        : zone.withBox(zone.getMinX(), zone.getMinY(), zone.getMinZ(), p.x, p.y, p.z);
                applyBox(ref, store, updated);
            }
            case "p1xUp" -> nudgeMin(ref, store, zone, NUDGE, 0, 0);
            case "p1xDown" -> nudgeMin(ref, store, zone, -NUDGE, 0, 0);
            case "p1yUp" -> nudgeMin(ref, store, zone, 0, NUDGE, 0);
            case "p1yDown" -> nudgeMin(ref, store, zone, 0, -NUDGE, 0);
            case "p1zUp" -> nudgeMin(ref, store, zone, 0, 0, NUDGE);
            case "p1zDown" -> nudgeMin(ref, store, zone, 0, 0, -NUDGE);
            case "p2xUp" -> nudgeMax(ref, store, zone, NUDGE, 0, 0);
            case "p2xDown" -> nudgeMax(ref, store, zone, -NUDGE, 0, 0);
            case "p2yUp" -> nudgeMax(ref, store, zone, 0, NUDGE, 0);
            case "p2yDown" -> nudgeMax(ref, store, zone, 0, -NUDGE, 0);
            case "p2zUp" -> nudgeMax(ref, store, zone, 0, 0, NUDGE);
            case "p2zDown" -> nudgeMax(ref, store, zone, 0, 0, -NUDGE);
            case "setMusic" -> {
                String music = data.getZoneMusic();
                if (music == null) {
                    refreshUI(ref, store);
                    return;
                }
                music = music.trim();
                if (!music.isEmpty() && plugin.resolveMusicFile(music) == null) {
                    playerRef.sendMessage(Message.raw("Fichier introuvable dans music/ : " + music).color("#FF5555"));
                    refreshUI(ref, store);
                    return;
                }
                plugin.getRepository().addOrReplace(zone.withMusicFileName(music));
                plugin.getRepository().save();
                MusicZoneEdits.rebuildPackAsync(plugin);
                playerRef.sendMessage(Message.raw("Musique : ").color("#55FF55")
                        .insert(Message.raw(music.isEmpty() ? "(aucune)" : music).color("#FFFF55")));
                refreshUI(ref, store);
            }
            case "setIntensity" -> {
                float pct = clampPercent(data.getZoneIntensity((float) MusicZone.dbToPercent(zone.getVolumeDb())));
                double db = MusicZone.percentToDb(pct);
                plugin.getRepository().addOrReplace(zone.withVolumeDb(db));
                plugin.getRepository().save();
                MusicZoneEdits.rebuildContainersAsync(plugin);
                softUpdateIntensityLabel(pct);
            }
            case "delete" -> {
                plugin.getRepository().remove(worldName, zoneId);
                plugin.getRepository().save();
                MusicZoneEdits.rebuildPackAsync(plugin);
                playerRef.sendMessage(Message.raw("Zone « " + zoneId + " » supprimée.").color("#55FF55"));
                backToList(ref, store, player, playerRef);
            }
            case "backToList" -> backToList(ref, store, player, playerRef);
            case "exit" -> closePage(ref, store);
        }
    }

    @Override
    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {}

    private void applyBox(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull MusicZone updated) {
        plugin.getRepository().addOrReplace(updated);
        plugin.getRepository().save();
        MusicZoneEdits.rebuildPackAsync(plugin);
        refreshUI(ref, store);
    }

    private void nudgeMin(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull MusicZone z,
                          double dx, double dy, double dz) {
        applyBox(ref, store, z.withBox(z.getMinX() + dx, z.getMinY() + dy, z.getMinZ() + dz,
                z.getMaxX(), z.getMaxY(), z.getMaxZ()));
    }

    private void nudgeMax(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull MusicZone z,
                          double dx, double dy, double dz) {
        applyBox(ref, store, z.withBox(z.getMinX(), z.getMinY(), z.getMinZ(),
                z.getMaxX() + dx, z.getMaxY() + dy, z.getMaxZ() + dz));
    }

    private void applyZone(@Nonnull MusicZone zone, @Nonnull UICommandBuilder cmd) {
        cmd.set("#ZoneIdInput.Value", zone.getId());
        cmd.set("#ZoneGroupInput.Value", zone.getGroup() == null ? "" : zone.getGroup());
        cmd.set("#WorldLabel.Text", worldName.isBlank() ? "?" : worldName);

        cmd.set("#P1XInput.Value", fmt(zone.getMinX()));
        cmd.set("#P1YInput.Value", fmt(zone.getMinY()));
        cmd.set("#P1ZInput.Value", fmt(zone.getMinZ()));
        cmd.set("#P2XInput.Value", fmt(zone.getMaxX()));
        cmd.set("#P2YInput.Value", fmt(zone.getMaxY()));
        cmd.set("#P2ZInput.Value", fmt(zone.getMaxZ()));

        List<String> files = plugin.listMusicFileNames();
        String current = zone.getMusicFileName() == null ? "" : zone.getMusicFileName();
        cmd.set("#ZoneMusic.Entries", musicEntries(files, current));
        cmd.set("#ZoneMusic.Value", current);
        cmd.set("#MusicHint.Text", files.isEmpty()
                ? "Aucun .ogg dans Varyon-MusicZones/music/"
                : "Fichiers dans Varyon-MusicZones/music/*.ogg");

        float pct = clampPercent((float) MusicZone.dbToPercent(zone.getVolumeDb()));
        cmd.set("#ZoneIntensity.Value", pct);
        cmd.set("#ZoneIntensityValue.Text", String.format(Locale.US, "%.0f %%", pct));
    }

    @Nonnull
    private static List<DropdownEntryInfo> musicEntries(@Nonnull List<String> files, @Nonnull String current) {
        List<DropdownEntryInfo> entries = new ArrayList<>();
        entries.add(new DropdownEntryInfo(LocalizableString.fromString("(aucune)"), ""));
        boolean known = current.isEmpty();
        for (String file : files) {
            if (file == null || file.isBlank()) {
                continue;
            }
            entries.add(new DropdownEntryInfo(LocalizableString.fromString(file), file));
            if (file.equalsIgnoreCase(current)) {
                known = true;
            }
        }
        if (!known) {
            entries.add(new DropdownEntryInfo(LocalizableString.fromString(current + " (introuvable)"), current));
        }
        return entries;
    }

    private void bindEvents(@Nonnull UIEventBuilder evt) {
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#RenameButton",
                new EventData().append("Action", "rename").append("@ZoneId", "#ZoneIdInput.Value"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#SetGroupButton",
                new EventData().append("Action", "setGroup").append("@ZoneGroup", "#ZoneGroupInput.Value"));

        EventData boxSnapshot = new EventData().append("Action", "setBox")
                .append("@P1X", "#P1XInput.Value").append("@P1Y", "#P1YInput.Value").append("@P1Z", "#P1ZInput.Value")
                .append("@P2X", "#P2XInput.Value").append("@P2Y", "#P2YInput.Value").append("@P2Z", "#P2ZInput.Value");
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#SetBoxButton", boxSnapshot);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#P1HereButton",
                EventData.of("Action", "p1Here"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#P2HereButton",
                EventData.of("Action", "p2Here"));

        bindNudge(evt, "#P1XUpButton", "p1xUp");
        bindNudge(evt, "#P1XDownButton", "p1xDown");
        bindNudge(evt, "#P1YUpButton", "p1yUp");
        bindNudge(evt, "#P1YDownButton", "p1yDown");
        bindNudge(evt, "#P1ZUpButton", "p1zUp");
        bindNudge(evt, "#P1ZDownButton", "p1zDown");
        bindNudge(evt, "#P2XUpButton", "p2xUp");
        bindNudge(evt, "#P2XDownButton", "p2xDown");
        bindNudge(evt, "#P2YUpButton", "p2yUp");
        bindNudge(evt, "#P2YDownButton", "p2yDown");
        bindNudge(evt, "#P2ZUpButton", "p2zUp");
        bindNudge(evt, "#P2ZDownButton", "p2zDown");

        evt.addEventBinding(CustomUIEventBindingType.ValueChanged, "#ZoneMusic",
                new EventData().append("Action", "setMusic").append("@ZoneMusic", "#ZoneMusic.Value"));
        evt.addEventBinding(CustomUIEventBindingType.ValueChanged, "#ZoneIntensity",
                new EventData().append("Action", "setIntensity").append("@ZoneIntensity", "#ZoneIntensity.Value"));

        evt.addEventBinding(CustomUIEventBindingType.Activating, "#BackToListButton",
                EventData.of("Action", "backToList"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#DeleteButton",
                EventData.of("Action", "delete"));
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#ExitButton",
                EventData.of("Action", "exit"));
    }

    private static void bindNudge(@Nonnull UIEventBuilder evt, @Nonnull String selector, @Nonnull String action) {
        evt.addEventBinding(CustomUIEventBindingType.Activating, selector, EventData.of("Action", action));
    }

    private void refreshUI(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        MusicZone zone = zone();
        if (zone == null) {
            closePage(ref, store);
            return;
        }
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder evt = new UIEventBuilder();
        applyZone(zone, cmd);
        bindEvents(evt);
        sendUpdate(cmd, evt, false);
    }

    private void softUpdateIntensityLabel(float pct) {
        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set("#ZoneIntensityValue.Text", String.format(Locale.US, "%.0f %%", pct));
        sendUpdate(cmd, new UIEventBuilder(), false);
    }

    private void backToList(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                            @Nonnull Player player, @Nonnull PlayerRef playerRef) {
        player.getPageManager().openCustomPage(ref, store, new MusicZoneListPage(playerRef, plugin));
    }

    private MusicZone zone() {
        return plugin.getRepository().find(worldName, zoneId);
    }

    private static boolean canManage(@Nonnull PlayerRef playerRef) {
        return playerRef.hasPermission("*") || playerRef.hasPermission("varyon.musiczones.admin");
    }

    private void closePage(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            player.getPageManager().setPage(ref, store, Page.None);
        }
    }

    private static float clampPercent(float pct) {
        if (Float.isNaN(pct)) {
            return 100f;
        }
        return Math.max(0f, Math.min(100f, pct));
    }

    @Nonnull
    private static String fmt(double v) {
        return String.format(Locale.US, "%.1f", v);
    }
}
