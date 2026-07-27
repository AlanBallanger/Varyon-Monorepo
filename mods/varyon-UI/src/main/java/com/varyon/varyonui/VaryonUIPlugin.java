package com.varyon.varyonui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.varyonui.hud.VaryonMenuHud;
import com.varyon.varyonui.input.VaryonAccueilAltKeyFilter;
import com.varyon.varyonui.input.VaryonAccueilKeyHelper;
import com.varyon.varyonui.input.VaryonAccueilOKeyFilter;
import com.varyon.varyonui.config.AccueilShortcutConfig;
import com.varyon.varyonui.config.ActuStateConfig;
import com.varyon.varyonui.config.MenuShortcutTargetConfig;
import com.varyon.varyonui.config.AdminCommandsConfig;
import com.varyon.varyonui.config.CommandsConfig;
import com.varyon.varyonui.config.NewsConfig;
import com.varyon.varyonui.config.HomeConfig;
import com.varyon.varyonui.config.TutorielConfig;
import com.varyon.varyonui.config.VaryonConfig;
import com.varyon.varyonui.integration.HytlSkinPreview;
import com.varyon.varyonui.ui.SimpleUIPage;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VaryonUIPlugin extends JavaPlugin {

    private static final Logger LOG = Logger.getLogger("VaryonUI");

    /**
     * Attacher le HUD trop tôt après {@link PlayerReadyEvent} fait souvent crasher le client
     * (« UI pas encore prête »). Aligné avec job_var/DEV_GUIDE (délai ~3 s).
     */
    private static final long MENU_HUD_ATTACH_DELAY_MS = 3000L;

    private static VaryonUIPlugin instance;
    private File dataFolder;
    private PacketFilter accueilOKeyPacketFilter;
    private PacketFilter accueilAltPacketFilter;
    private VaryonAccueilAltKeyFilter accueilAltKeyFilter;

    public VaryonUIPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        instance = this;
        this.dataFolder = this.getDataDirectory().toFile();

        CommandsConfig.getInstance();
        AdminCommandsConfig.getInstance();
        NewsConfig.getInstance();
        HomeConfig.getInstance();
        TutorielConfig.getInstance();
        VaryonConfig.getInstance();
        AccueilShortcutConfig.getInstance();
        MenuShortcutTargetConfig.getInstance();
        ActuStateConfig.getInstance();

        CommandManager mgr = CommandManager.get();
        mgr.register(new UICommand("commands", "Ouvre les commandes", "commandes", "c", "command"));
        mgr.register(new UICommand("tutorial", "Ouvre le tutoriel", "tutoriel", "tuto", "tutoriel"));
        mgr.register(new UICommand("acc", "Ouvre l'accueil", "home", "accueil"));
        mgr.register(new UICommand("ad", "Ouvre le panneau admin", "admin"));
        mgr.register(new UICommand("actus", "Ouvre les actualites", "misesajour", "actualites", "actualités", "actu"));
        mgr.register(new UICommand("profil", "Ouvre le menu (accueil)", "home", "profile"));
        mgr.register(new UICommand("param", "Ouvre les paramètres", "parametres", "parametres"));
        mgr.register(new UICommand("var", "Ouvre la page Varyon", "varyon", "varyon"));
        mgr.register(new UICommand("playtime-ui", "Ouvre les statistiques de temps de jeu", "playtime", "ptui"));
        mgr.register(new ReloadCommand());
        mgr.register(new ActuUpdateCommand());
        mgr.register(new ActuNotifCommand());

        accueilOKeyPacketFilter = PacketAdapters.registerInbound(new VaryonAccueilOKeyFilter());
        accueilAltKeyFilter = new VaryonAccueilAltKeyFilter();
        accueilAltPacketFilter = PacketAdapters.registerInbound(accueilAltKeyFilter);

        getEventRegistry().registerGlobal(PlayerReadyEvent.class, this::onPlayerReady);
        getEventRegistry().register(PlayerDisconnectEvent.class, this::onPlayerDisconnect);

        HytlSkinPreview.preparePortraitPackAtStartup(this);
    }

    @Override
    protected void shutdown() {
        if (accueilOKeyPacketFilter != null) {
            PacketAdapters.deregisterInbound(accueilOKeyPacketFilter);
            accueilOKeyPacketFilter = null;
        }
        if (accueilAltPacketFilter != null) {
            PacketAdapters.deregisterInbound(accueilAltPacketFilter);
            accueilAltPacketFilter = null;
        }
        accueilAltKeyFilter = null;
    }

    private void onPlayerDisconnect(PlayerDisconnectEvent event) {
        if (event.getPlayerRef() == null) {
            return;
        }
        UUID uuid = event.getPlayerRef().getUuid();
        if (uuid != null) {
            VaryonAccueilKeyHelper.clearOpenKeyDebounce(uuid);
        }
        if (accueilAltKeyFilter != null && uuid != null) {
            accueilAltKeyFilter.clearPlayer(uuid);
        }
    }

    private void onPlayerReady(PlayerReadyEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        UUID uuid = player.getUuid();
        if (uuid == null) {
            return;
        }
        HytaleServer.SCHEDULED_EXECUTOR.schedule(
                () -> attachMenuHudDeferred(uuid),
                MENU_HUD_ATTACH_DELAY_MS,
                TimeUnit.MILLISECONDS);
    }

    private void attachMenuHudDeferred(@Nonnull UUID uuid) {
        Universe universe = Universe.get();
        if (universe == null) {
            return;
        }
        PlayerRef foundRef = null;
        for (PlayerRef pref : universe.getPlayers()) {
            if (pref != null && uuid.equals(pref.getUuid())) {
                foundRef = pref;
                break;
            }
        }
        if (foundRef == null) {
            return;
        }
        Ref<?> entityRef = foundRef.getReference();
        if (entityRef == null || !entityRef.isValid()) {
            return;
        }
        @SuppressWarnings("rawtypes")
        Store store = entityRef.getStore();
        if (store == null) {
            return;
        }
        World world = ((EntityStore) store.getExternalData()).getWorld();
        if (world == null) {
            return;
        }
        final PlayerRef playerRef = foundRef;
        world.execute(() -> {
            try {
                if (!playerRef.isValid()) {
                    return;
                }
                Player player = playerRef.getComponent(Player.getComponentType());
                if (player == null) {
                    return;
                }
                VaryonMenuHud.attach(player, playerRef);
                @SuppressWarnings("unchecked")
                Store<EntityStore> typedStore = (Store<EntityStore>) store;
                maybeOpenActuPopup(uuid, playerRef, player, typedStore);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Failed to register Varyon menu HUD", e);
            }
        });
    }

    private void maybeOpenActuPopup(
            @Nonnull UUID uuid,
            @Nonnull PlayerRef playerRef,
            @Nonnull Player player,
            @Nonnull Store<EntityStore> store) {
        ActuStateConfig actuState = ActuStateConfig.getInstance();
        if (!actuState.wantsPopup(uuid) || !actuState.hasUnseenUpdate(uuid)) {
            return;
        }
        Ref<EntityStore> ref = (Ref<EntityStore>) playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return;
        }
        SimpleUIPage uiPage = new SimpleUIPage(playerRef, "misesajour", false);
        player.getPageManager().openCustomPage(ref, store, uiPage);
        player.getHudManager().getCustomHuds().values().forEach(
                com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud::show
        );
        actuState.markSeen(uuid);
    }

    public static VaryonUIPlugin getInstance() {
        return instance;
    }

    public File getDataFolder() {
        return dataFolder;
    }
}
