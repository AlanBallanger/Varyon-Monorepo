package fr.varyon.holograms.gui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
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
import org.joml.Vector3d;
import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HologramListPage extends InteractiveCustomUIPage<HologramListEventData> {

    private final VaryonHologramsPlugin plugin;

    public HologramListPage(@Nonnull PlayerRef playerRef, @Nonnull VaryonHologramsPlugin plugin) {
        super(playerRef, CustomPageLifetime.CanDismiss, HologramListEventData.CODEC);
        this.plugin = plugin;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder evt, @Nonnull Store<EntityStore> store) {
        cmd.append("HologramListPage.ui");
        buildList(cmd, evt);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CreateButton",
            EventData.of("Action", "create"));
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
            case "create" -> {
                boolean canCreate = playerRef.hasPermission("*") || playerRef.hasPermission("varyon.holograms.create");
                if (!canCreate) { refreshUI(ref, store); return; }
                TransformComponent tc = store.getComponent(ref, TransformComponent.getComponentType());
                if (tc == null) { refreshUI(ref, store); return; }
                Vector3d pos = tc.getPosition();
                String name = "holo_" + System.currentTimeMillis();
                Hologram h = plugin.getHologramManager().createHologram(name,
                    new Vector3d(pos.x, pos.y + 1.5, pos.z),
                    plugin.getHologramManager().resolveWorldId(ref, store),
                    playerRef.getUuid());
                player.getPageManager().openCustomPage(ref, store,
                    new HologramEditorPage(playerRef, plugin, h));
            }
            case "close" -> closePage(ref, store);
        }
    }

    @Override
    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {}

    private void buildList(@Nonnull UICommandBuilder cmd, @Nonnull UIEventBuilder evt) {
        cmd.clear("#HologramList");
        List<Hologram> holos = new ArrayList<>(plugin.getHologramManager().getAllHolograms());
        holos.sort(Comparator.comparing(Hologram::getName, String.CASE_INSENSITIVE_ORDER));

        if (holos.isEmpty()) {
            cmd.set("#EmptyLabel.Visible", true);
            return;
        }

        cmd.set("#EmptyLabel.Visible", false);
        for (int i = 0; i < holos.size(); i++) {
            Hologram h = holos.get(i);
            String selector = "#HologramList[" + i + "]";
            Vector3d p = h.getPosition();
            String rowText = h.getName() + "  |  " + h.getLineCount() + " ligne(s) - "
                + String.format("%.1f, %.1f, %.1f", p.x, p.y, p.z);

            cmd.append("#HologramList", "Pages/HologramListItem.ui");
            cmd.set(selector + " #RowLabel.Text", escape(rowText));
            evt.addEventBinding(CustomUIEventBindingType.Activating, selector + " #EditButton",
                EventData.of("Action", "edit").append("HoloName", h.getName()), false);
        }
    }

    private void refreshUI(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        UICommandBuilder cmd = new UICommandBuilder();
        UIEventBuilder evt = new UIEventBuilder();
        buildList(cmd, evt);
        sendUpdate(cmd, evt, false);
    }

    private void closePage(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) player.getPageManager().setPage(ref, store, Page.None);
    }

    private static String escape(@Nonnull String text) {
        return text.replace(";", ",").replace("{", "(").replace("}", ")").replace("\"", "'");
    }
}
