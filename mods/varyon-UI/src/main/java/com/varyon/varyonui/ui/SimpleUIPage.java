package com.varyon.varyonui.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.protocol.packets.interface_.OpenChatWithCommand;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.PatchStyle;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.universe.world.World;
import com.varyon.varyonui.config.AccueilShortcutConfig;
import com.varyon.varyonui.config.ActuStateConfig;
import com.varyon.varyonui.config.MenuShortcutTargetConfig;
import com.varyon.varyonui.config.AdminCommandsConfig;
import com.varyon.varyonui.config.CommandsConfig;
import com.varyon.varyonui.config.NewsConfig;
import com.varyon.varyonui.config.HomeConfig;
import com.varyon.varyonui.config.TutorielConfig;
import com.varyon.varyonui.config.VaryonConfig;
import com.varyon.varyonui.hud.VaryonMenuHud;
import com.varyon.varyonui.integration.CombatProfilBridge;
import com.varyon.varyonui.integration.DamageNumberBridge;
import com.varyon.varyonui.integration.EcotaleEconomyBridge;
import com.varyon.varyonui.integration.MenuRpgBridge;
import com.varyon.varyonui.integration.HytlSkinPreview;
import com.varyon.varyonui.integration.PlaytimeBridge;
import com.varyon.varyonui.VaryonUIPlugin;
import com.hypixel.hytale.server.core.modules.item.ItemModule;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleUIPage extends InteractiveCustomUIPage<SimpleUIPage.EventDataClass> {

    private static final Logger LOG = Logger.getLogger("VaryonUI");

    private static final String[] PLAYTIME_CHEST_VISUAL_ITEM_IDS = new String[] {
            "Furniture_Ancient_Chest_Small",
            "Furniture_Tavern_Chest_Small",
            "Furniture_Human_Ruins_Chest_Small",
            "Furniture_Temple_Dark_Chest_Small",
            "Furniture_Dungeon_Chest_Epic",
            "Furniture_Royal_Magic_Chest_Small",
    };

    private static final String PLAYTIME_CHEST_FALLBACK_ITEM_ID = "Furniture_Crude_Chest_Small";

    private static final String[] PLAYTIME_CHEST_LOCKED_INNER = {
            null,
            "#283B50",
            "#2D5E8E",
            "#583178",
            "#B17F3D",
            "#FE7D84"
    };

    private static final int PLAYTIME_CHEST_ICON_SIZE = 40;
    private static final int PLAYTIME_CHEST_ICON_FRAME_PAD = 2;
    private static final int PLAYTIME_CHEST_ITEM_ROW_HEIGHT =
            PLAYTIME_CHEST_ICON_SIZE + PLAYTIME_CHEST_ICON_FRAME_PAD * 2;

    private static final int PLAYTIME_CHEST_INNER_PAD_TOP = 8;
    private static final int PLAYTIME_CHEST_GAP_ICON_TO_LABEL = 3;
    private static final int PLAYTIME_CHEST_TIME_LABEL_HEIGHT = 16;

    private static final int PLAYTIME_CHEST_INNER_PAD_BOTTOM =
            PLAYTIME_CHEST_INNER_PAD_TOP;

    private static final String PLAYTIME_CHEST_MUTE_BASE = "#1c2533";
    private static final double PLAYTIME_CHEST_VIVID_WEIGHT = 0.2;

    private static final String PLAYTIME_UNAVAILABLE_USER_MSG =
            "Les données de temps de jeu ne peuvent pas être chargées pour le moment. Réessayez plus tard. "
                    + "Si le problème persiste, rendez-vous sur le site du serveur ou contactez le staff.";


    private static final long COMMAND_EXECUTE_DELAY_MS = 100L;
    private static final ConcurrentHashMap<UUID, Boolean> ACTIVITY_BUBBLE_ON = new ConcurrentHashMap<>();
    /**
     * Local cache mirroring the sibling Varyon-Damage_Number mod's per-player toggle state.
     * CommandManager.handleCommand() runs the "/dmgnum" command asynchronously (ForkJoinPool),
     * so re-reading DamageNumberBridge.isEnabled(uuid) immediately after firing the command could
     * race and read the pre-toggle value, making the switch render inverted for one frame/click.
     * Writing the intended state here immediately (same pattern as ACTIVITY_BUBBLE_ON) avoids that.
     */
    private static final ConcurrentHashMap<UUID, Boolean> DMG_NUM_ON = new ConcurrentHashMap<>();

    public static void clearPerPlayerTogglePreferences(UUID uuid) {
        if (uuid != null) {
            ACTIVITY_BUBBLE_ON.remove(uuid);
            DMG_NUM_ON.remove(uuid);
        }
    }

    private static final String SWITCH_ON_ACTIVE_BG = "#27AE60";
    private static final String SWITCH_ON_INACTIVE_BG = "#27AE6047";
    private static final String SWITCH_OFF_ACTIVE_BG = "#E74C3C";
    private static final String SWITCH_OFF_INACTIVE_BG = "#E74C3C47";
    private static final String SWITCH_ACTIVE_TEXT = "#FFFFFF";
    private static final String SWITCH_INACTIVE_TEXT = "#FFFFFF55";

    private static final int MAX_SLOTS = 10;
    private static final int MAX_BUTTONS = 50;
    private static final int BUTTONS_PER_ROW = 5;

    private static final DateTimeFormatter SIDEBAR_GAME_TIME =
            DateTimeFormatter.ofPattern("HH:mm", Locale.FRANCE);

    private String activeTab;
    private final boolean isAdmin;

    private final AtomicBoolean portraitRefreshInFlight = new AtomicBoolean(false);
    private final AtomicBoolean portraitRefreshNeededAgain = new AtomicBoolean(false);

    public SimpleUIPage(@Nonnull PlayerRef playerRef) {
        this(playerRef, "home", false);
    }

    public SimpleUIPage(@Nonnull PlayerRef playerRef, @Nonnull String initialTab) {
        this(playerRef, initialTab, false);
    }

    public SimpleUIPage(@Nonnull PlayerRef playerRef, @Nonnull String initialTab, boolean isAdmin) {
        super(playerRef, CustomPageLifetime.CanDismiss, EventDataClass.CODEC);
        this.isAdmin = isAdmin;
        this.activeTab = (initialTab.equals("admin") && !isAdmin) ? "home"
                : "profil".equals(initialTab) ? "home"
                : initialTab;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder commandBuilder,
                      @Nonnull UIEventBuilder eventBuilder,
                      @Nonnull Store<EntityStore> store) {
        commandBuilder.append("VaryonMainPage.ui");
        commandBuilder.append("#VaryonSidebarMount", "VaryonSidebar.ui");
        commandBuilder.append("#VaryonBodyMount", "VaryonBody.ui");
        commandBuilder.append("#VaryonTabBarMount", "VaryonTabBar.ui");
        commandBuilder.append("#VaryonBodyContentMount", "VaryonBodyContent.ui");
        commandBuilder.append("#VaryonPageHomeMount", "VaryonPageHome.ui");
        commandBuilder.append("#VaryonPageTutorielMount", "VaryonPageTutoriel.ui");
        commandBuilder.append("#VaryonPageCommandesMount", "VaryonPageCommandes.ui");
        commandBuilder.append("#VaryonPageMisesAJourMount", "VaryonPageMisesAJour.ui");
        commandBuilder.append("#VaryonPageVaryonMount", "VaryonPageVaryon.ui");
        commandBuilder.append("#VaryonPageParametresMount", "VaryonPageParametres.ui");
        commandBuilder.append("#VaryonPagePlaytimeMount", "VaryonPagePlaytime.ui");
        commandBuilder.append("#VaryonPageAdminMount", "VaryonPageAdmin.ui");
        buildContent(commandBuilder, eventBuilder, store, ref);
        buildTabBar(commandBuilder, eventBuilder, true, ref);
    }

    private void applyTabIconTextures(@Nonnull UICommandBuilder cb) {
        applyOneTabIcon(cb, "HomeIcon", "Icons/Accueil.png", "Icons/Accueil_Hovered.png", "home");
        applyOneTabIcon(cb, "TutorielIcon", "Icons/Tutoriel.png", "Icons/Tutoriel_Hovered.png", "tutoriel");
        applyOneTabIcon(cb, "CommandesIcon", "Icons/Commandes.png", "Icons/Commandes_Hovered.png", "commandes");
        applyOneTabIcon(cb, "MisesAJourIcon", "Icons/Mises_a_jour.png", "Icons/Mises_a_jour_Hovered.png", "misesajour");
        applyOneTabIcon(cb, "VaryonIcon", "Icons/Varyon.png", "Icons/Varyon_Hovered.png", "varyon");
        applyOneTabIcon(cb, "PlaytimeIcon", "Icons/Temps.png", "Icons/Temps_Hovered.png", "playtime");
        applyOneTabIcon(cb, "JournalIcon", "Icons/Quetes.png", "Icons/Quetes_Hovered.png", "__journal_launch__");
        applyOneTabIcon(cb, "ParametresIcon", "Icons/Parametres.png", "Icons/Parametres_Hovered.png", "parametres");
        if (isAdmin) {
            applyOneTabIcon(cb, "AdminIcon", "Icons/Commandes.png", "Icons/Commandes_Hovered.png", "admin");
        }
    }

    private void applyOneTabIcon(
            @Nonnull UICommandBuilder cb,
            @Nonnull String iconId,
            @Nonnull String normalPath,
            @Nonnull String hoveredPath,
            @Nonnull String tabKey) {
        boolean useHovered = tabKey.equals(activeTab);
        String path = useHovered ? hoveredPath : normalPath;
        PatchStyle ps =
            new PatchStyle().setTexturePath(Value.of(normalizeIconUrl(path))).setBorder(Value.of(0));
        cb.setObject("#" + iconId + ".Background", ps);
    }

    private void buildTabBar(
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder,
            boolean scheduleSkinFetch,
            @Nullable Ref<EntityStore> skinContextRef) {
        patchTabBarAppearance(commandBuilder);
        appendTabBarEvents(commandBuilder, eventBuilder);
        if (scheduleSkinFetch) {
            UUID uuid = playerRef.getUuid();
            // Skip the placeholder flash on menu open when we already have a fresh cached
            // portrait — the async refresh below will simply reapply the same (unchanged) image
            // via SkinPortraitRuntime's dedup, so clearing to a gray placeholder first only
            // produced a visible flicker with nothing gained.
            if (uuid == null || !HytlSkinPreview.hasFreshCachedPortrait(uuid)) {
                HytlSkinPreview.applyPlaceholder(commandBuilder);
            }
            scheduleAsyncSidebarPortraitRefresh(skinContextRef);
        }
    }

    private void patchTabBarAppearance(@Nonnull UICommandBuilder commandBuilder) {
        commandBuilder.set("#AdminTabContainer.Visible", isAdmin);

        applyTabIconTextures(commandBuilder);
        applyTabUnderlineVisibility(commandBuilder);
        commandBuilder.set("#PlaytimeRewardBadge.Visible", hasUnclaimedPlaytimeReward(playerRef.getUuid()));
    }

    private static boolean hasUnclaimedPlaytimeReward(@Nullable UUID uuid) {
        if (uuid == null || !PlaytimeBridge.isAvailable() || !PlaytimeBridge.isBackendOperational()) {
            return false;
        }
        long[] rdata = PlaytimeBridge.getDailyRewardData(uuid);
        long dailyMs = PlaytimeBridge.getDailyPlaytime(uuid);
        for (int i = 0; i + 1 < rdata.length; i += 2) {
            if (dailyMs >= rdata[i] && rdata[i + 1] != 1L) {
                return true;
            }
        }
        return false;
    }

    private void applyTabUnderlineVisibility(@Nonnull UICommandBuilder commandBuilder) {
        commandBuilder.set("#HomeTabUnderline.Visible", "home".equals(activeTab));
        commandBuilder.set("#TutorielTabUnderline.Visible", "tutoriel".equals(activeTab));
        commandBuilder.set("#CommandesTabUnderline.Visible", "commandes".equals(activeTab));
        commandBuilder.set("#MisesAJourTabUnderline.Visible", "misesajour".equals(activeTab));
        commandBuilder.set("#VaryonTabUnderline.Visible", "varyon".equals(activeTab));
        commandBuilder.set("#ParametresTabUnderline.Visible", "parametres".equals(activeTab));
        commandBuilder.set("#PlaytimeTabUnderline.Visible", "playtime".equals(activeTab));
        if (isAdmin) {
            commandBuilder.set("#AdminTabUnderline.Visible", "admin".equals(activeTab));
        }
    }

    private void appendTabBarEvents(
            @Nonnull UICommandBuilder commandBuilder,
            @Nonnull UIEventBuilder eventBuilder) {
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#HomeTab", EventData.of("Action", "tab").append("Tab", "home"));
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#TutorielTab", EventData.of("Action", "tab").append("Tab", "tutoriel"));
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CommandesTab", EventData.of("Action", "tab").append("Tab", "commandes"));
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#MisesAJourTab", EventData.of("Action", "tab").append("Tab", "misesajour"));
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#VaryonTab", EventData.of("Action", "tab").append("Tab", "varyon"));
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#PlaytimeTab", EventData.of("Action", "tab").append("Tab", "playtime"));
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#JournalTab",
            EventData.of("Action", "command").append("Command", "/journal"));
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#ParametresTab", EventData.of("Action", "tab").append("Tab", "parametres"));

        if (hasClaimablePlaytimeReward(playerRef.getUuid())) {
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#PtClaimAllBtn",  EventData.of("Action", "playtimeclaimall"));
        }
        if ("playtime".equals(activeTab) && PlaytimeBridge.isAvailable() && PlaytimeBridge.isBackendOperational()) {
            appendPlaytimeChestButtonEvents(eventBuilder, playerRef.getUuid());
        }
        eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", EventData.of("Action", "close"));

        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#MenuGenDetailBtn",
            EventData.of("Action", "command").append("Command", "/vrpg")
        );
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#MenuJob1DetailBtn",
            EventData.of("Action", "command").append("Command", "/vp")
        );
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#MenuJob2DetailBtn",
            EventData.of("Action", "command").append("Command", "/vp")
        );

        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#SidebarBalanceRow",
            EventData.of("Action", "command").append("Command", "/bank")
        );

        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#ParametresMenuShortcutAlt",
            EventData.of("Action", "accueilshortcut").append("ShortcutMode", "alt")
        );
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#ParametresMenuShortcutO",
            EventData.of("Action", "accueilshortcut").append("ShortcutMode", "o")
        );
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#ParametresMenuShortcutDisable",
            EventData.of("Action", "accueilshortcut").append("ShortcutMode", "disable")
        );

        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#ParametresMenuTargetAccueil",
            EventData.of("Action", "menushortcuttarget").append("MenuShortcutTarget", "accueil")
        );
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#ParametresMenuTargetCommandes",
            EventData.of("Action", "menushortcuttarget").append("MenuShortcutTarget", "commandes")
        );
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#ParametresMenuTargetVaryon",
            EventData.of("Action", "menushortcuttarget").append("MenuShortcutTarget", "varyon")
        );
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#ParametresDmgNumOn",
            EventData.of("Action", "dmgnumtoggle").append("DmgNumToggle", "on")
        );
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#ParametresDmgNumOff",
            EventData.of("Action", "dmgnumtoggle").append("DmgNumToggle", "off")
        );
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#ParametresActivityBubbleOn",
            EventData.of("Action", "bbubtoggle").append("ActivityBubbleToggle", "on")
        );
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#ParametresActivityBubbleOff",
            EventData.of("Action", "bbubtoggle").append("ActivityBubbleToggle", "off")
        );
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#ParametresRpgSettingsOpen",
            EventData.of("Action", "command").append("Command", "/vrpg param")
        );
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#ParametresPartyMenuOpen",
            EventData.of("Action", "command").append("Command", "/party")
        );
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#ParametresQuestMenuOpen",
            EventData.of("Action", "command").append("Command", "/qlconfig")
        );
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#ParametresActuPopupOn",
            EventData.of("Action", "actupopuptoggle").append("ActuPopupToggle", "on")
        );
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#ParametresActuPopupOff",
            EventData.of("Action", "actupopuptoggle").append("ActuPopupToggle", "off")
        );

        if (isAdmin) {
            eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#AdminTab", EventData.of("Action", "tab").append("Tab", "admin"));
        }

        if ("commandes".equals(activeTab)) {
            buildCommandButtons(commandBuilder, eventBuilder);
        }
        if ("varyon".equals(activeTab)) {
            buildVaryonQuickButtonEvents(eventBuilder);
        }
        if ("admin".equals(activeTab) && isAdmin) {
            buildAdminCommandButtons(commandBuilder, eventBuilder);
        }
    }

    private void scheduleAsyncSidebarPortraitRefresh(@Nullable Ref<EntityStore> skinContextRef) {
        UUID uuid = playerRef.getUuid();
        if (uuid == null) {
            return;
        }
        if (!portraitRefreshInFlight.compareAndSet(false, true)) {
            portraitRefreshNeededAgain.set(true);
            LOG.log(Level.FINE, "[PortraitPlaytime] refresh en cours, relance après la fin uuid=" + uuid);
            return;
        }
        HytaleServer.SCHEDULED_EXECUTOR.execute(() -> {
            try {
                byte[] avatarPng = HytlSkinPreview.fetchAvatarPng(uuid);
                byte[] skinFront = HytlSkinPreview.fetchHeadshotPng(uuid);
                UICommandBuilder cb = new UICommandBuilder();
                UIEventBuilder eb = new UIEventBuilder();
                patchTabBarAppearance(cb);
                appendTabBarEvents(cb, eb);
                EcotaleEconomyBridge.applySidebarBalance(playerRef, cb);
                HytlSkinPreview.applyPngToPreview(cb, uuid, skinFront, VaryonUIPlugin.getInstance());
                HytlSkinPreview.applyPlaytimeHeadFromAvatarPng(cb, uuid, avatarPng,
                        VaryonUIPlugin.getInstance(), "#PlaytimeHeadPreview");
                sendUpdate(cb, eb, false);
                restoreHuds();
            } catch (Throwable t) {
                LOG.log(Level.WARNING, "[PortraitPlaytime] async menu refresh failed uuid=" + uuid, t);
            } finally {
                portraitRefreshInFlight.set(false);
            }
            if (portraitRefreshNeededAgain.getAndSet(false)) {
                scheduleAsyncSidebarPortraitRefresh(null);
            }
        });
    }

    private static void buildVaryonQuickButtonEvents(@Nonnull UIEventBuilder eventBuilder) {
        for (int i = 0; i < VARYON_QUICK_COMMANDS.length; i++) {
            int n = i + 1;
            String action = VARYON_QUICK_CHAT[i] ? "chatcommand" : "command";
            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#VaryonQuickButton" + n,
                EventData.of("Action", action).append("Command", VARYON_QUICK_COMMANDS[i])
            );
        }
    }

    private static final String[] VARYON_QUICK_COMMANDS = {
        "/extract",
        "/return",
        "/join joueur ",
        "/rtpv 1",
        "/essence",
    };

    private static final boolean[] VARYON_QUICK_CHAT = {
        false,
        false,
        true,
        false,
        false,
    };

    private static final String[] VARYON_QUICK_LABELS = {
        "Extraction",
        "Retour mort",
        "Rejoindre ami",
        "TP aléatoire",
        "Qté essence",
    };

    private void buildCommandButtons(@Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder) {
        List<CommandsConfig.CommandCategory> categories = CommandsConfig.getInstance().getCategories();
        int slotIndex = 0;
        for (CommandsConfig.CommandCategory category : categories) {
            List<CommandsConfig.CommandButton> buttons = category.getButtons();
            for (int i = 0; i < buttons.size(); i += BUTTONS_PER_ROW) {
                slotIndex++;
                if (slotIndex > MAX_SLOTS) return;
                int baseIdx = (slotIndex - 1) * BUTTONS_PER_ROW;
                int count = Math.min(BUTTONS_PER_ROW, buttons.size() - i);
                for (int j = 0; j < count; j++) {
                    int btnIdx = baseIdx + j + 1;
                    if (btnIdx > MAX_BUTTONS) return;
                    CommandsConfig.CommandButton button = buttons.get(i + j);
                    String action = button.getType() == CommandsConfig.CommandType.CHAT ? "chatcommand" : "command";
                    eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CommandButton" + btnIdx, EventData.of("Action", action).append("Command", button.getCommand()));
                }
            }
        }
    }

    private void buildAdminCommandButtons(@Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder) {
        List<CommandsConfig.CommandCategory> categories = AdminCommandsConfig.getInstance().getCategories();
        int listIdx = 0;
        for (CommandsConfig.CommandCategory category : categories) {
            List<CommandsConfig.CommandButton> buttons = category.getButtons();
            if (buttons.isEmpty()) continue;
            listIdx++;
            for (int i = 0; i < buttons.size(); i += BUTTONS_PER_ROW) {
                int rowListIdx = listIdx;
                listIdx++;
                int count = Math.min(BUTTONS_PER_ROW, buttons.size() - i);
                for (int j = 0; j < count; j++) {
                    CommandsConfig.CommandButton button = buttons.get(i + j);
                    String action = button.getType() == CommandsConfig.CommandType.CHAT ? "chatcommand" : "command";
                    eventBuilder.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        "#AdminCommandesList[" + rowListIdx + "][" + j + "]",
                        EventData.of("Action", action).append("Command", button.getCommand())
                    );
                }
            }
        }
    }

    private static void applySidebarWorldAndPosition(
            @Nonnull UICommandBuilder commandBuilder,
            @Nullable Player player,
            @Nonnull PlayerRef playerRef,
            @Nullable Store<EntityStore> storeFallback) {
        String worldName = "";
        String coords = "";
        try {
            if (player != null) {
                World w = player.getWorld();
                if (w != null) {
                    String n = w.getName();
                    if (n != null && !n.isBlank()) {
                        worldName = n;
                    }
                }
            }
            var t = playerRef.getTransform();
            if (t != null) {
                var p = t.getPosition();
                if (p != null) {
                    coords = String.format(Locale.ROOT, "%d ; %d ; %d", (int) p.x, (int) p.y, (int) p.z);
                }
            }
        } catch (Throwable ignored) {
        }
        commandBuilder.set("#SidebarWorldName.TextSpans", Message.raw(worldName));
        commandBuilder.set("#SidebarPlayerCoords.TextSpans", Message.raw(coords));
        commandBuilder.set("#SidebarGameTime.TextSpans", Message.raw(formatSidebarGameTime(player, storeFallback)));
    }

    @Nonnull
    private static String formatSidebarGameTime(@Nullable Player player, @Nullable Store<EntityStore> storeFallback) {
        Store<EntityStore> store = resolveWorldStoreForTime(player, storeFallback);
        if (store == null) {
            return "";
        }
        try {
            WorldTimeResource tr = store.getResource(WorldTimeResource.getResourceType());
            if (tr == null) {
                return "";
            }
            var dt = tr.getGameDateTime();
            if (dt == null) {
                return "";
            }
            return SIDEBAR_GAME_TIME.format(dt);
        } catch (Throwable ignored) {
            return "";
        }
    }

    @Nullable
    private static Store<EntityStore> resolveWorldStoreForTime(
            @Nullable Player player,
            @Nullable Store<EntityStore> storeFallback) {
        try {
            if (player != null) {
                World w = player.getWorld();
                if (w != null && w.getEntityStore() != null) {
                    return w.getEntityStore().getStore();
                }
            }
        } catch (Throwable ignored) {
        }
        return storeFallback;
    }

    private void buildContent(@Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref) {
        commandBuilder.set("#VaryonPageHomeMount.Visible", "home".equals(activeTab));
        commandBuilder.set("#VaryonPageTutorielMount.Visible", "tutoriel".equals(activeTab));
        commandBuilder.set("#VaryonPageCommandesMount.Visible", "commandes".equals(activeTab));
        commandBuilder.set("#VaryonPageMisesAJourMount.Visible", "misesajour".equals(activeTab));
        commandBuilder.set("#VaryonPageVaryonMount.Visible", "varyon".equals(activeTab));
        commandBuilder.set("#VaryonPageParametresMount.Visible", "parametres".equals(activeTab));
        commandBuilder.set("#VaryonPagePlaytimeMount.Visible", "playtime".equals(activeTab));
        commandBuilder.set("#VaryonPageAdminMount.Visible", "admin".equals(activeTab) && isAdmin);

        commandBuilder.set("#HomeContent.Visible", "home".equals(activeTab));
        commandBuilder.set("#TutorielContent.Visible", "tutoriel".equals(activeTab));
        commandBuilder.set("#CommandesContent.Visible", "commandes".equals(activeTab));
        commandBuilder.set("#MisesAJourContent.Visible", "misesajour".equals(activeTab));
        commandBuilder.set("#VaryonContent.Visible", "varyon".equals(activeTab));
        commandBuilder.set("#ParametresContent.Visible", "parametres".equals(activeTab));
        commandBuilder.set("#PlaytimeContent.Visible", "playtime".equals(activeTab));
        commandBuilder.set("#AdminContent.Visible", "admin".equals(activeTab) && isAdmin);

        if ("commandes".equals(activeTab)) {
            buildCommandsContent(commandBuilder);
        }
        if ("admin".equals(activeTab) && isAdmin) {
            buildAdminCommandsContent(commandBuilder);
        }
        if ("misesajour".equals(activeTab)) {
            buildNewsContent(commandBuilder);
        }
        if ("tutoriel".equals(activeTab)) {
            buildTutorielContent(commandBuilder);
        }
        if ("varyon".equals(activeTab)) {
            buildVaryonContent(commandBuilder);
        }
        if ("home".equals(activeTab)) {
            buildHomeContent(commandBuilder);
        }
        if ("playtime".equals(activeTab)) {
            buildPlaytimeContent(commandBuilder);
        }
        Player player = store.getComponent(ref, Player.getComponentType());
        CombatProfilBridge.applyCombatProfil(playerRef, player, commandBuilder);
        EcotaleEconomyBridge.applySidebarBalance(playerRef, commandBuilder);
        applySidebarWorldAndPosition(commandBuilder, player, playerRef, store);
        MenuRpgBridge.applyMenuXp(playerRef.getUuid(), commandBuilder);
        if ("parametres".equals(activeTab)) {
            PlayerRef pref = store.getComponent(ref, PlayerRef.getComponentType());
            applyParametresShortcutAppearance(commandBuilder, pref != null ? pref.getUuid() : null);
        }
    }

    private static void applyParametresShortcutAppearance(
            @Nonnull UICommandBuilder commandBuilder,
            @Nullable UUID uuid) {
        AccueilShortcutConfig.Mode mode = uuid == null
            ? AccueilShortcutConfig.Mode.defaultMode()
            : AccueilShortcutConfig.getInstance().getMode(uuid);
        applyShortcutToggle(commandBuilder, "ParametresMenuShortcutAlt", "ParametresMenuShortcutAltLabel",
            mode == AccueilShortcutConfig.Mode.ALT);
        applyShortcutToggle(commandBuilder, "ParametresMenuShortcutO", "ParametresMenuShortcutOLabel",
            mode == AccueilShortcutConfig.Mode.O);
        applyShortcutToggle(commandBuilder, "ParametresMenuShortcutDisable", "ParametresMenuShortcutDisableLabel",
            mode == AccueilShortcutConfig.Mode.DISABLE);
        MenuShortcutTargetConfig.Target menuTarget = uuid == null
            ? MenuShortcutTargetConfig.Target.defaultTarget()
            : MenuShortcutTargetConfig.getInstance().getTarget(uuid);
        applyShortcutToggle(commandBuilder, "ParametresMenuTargetAccueil", "ParametresMenuTargetAccueilLabel",
            menuTarget == MenuShortcutTargetConfig.Target.ACCUEIL);
        applyShortcutToggle(commandBuilder, "ParametresMenuTargetCommandes", "ParametresMenuTargetCommandesLabel",
            menuTarget == MenuShortcutTargetConfig.Target.COMMANDES);
        applyShortcutToggle(commandBuilder, "ParametresMenuTargetVaryon", "ParametresMenuTargetVaryonLabel",
            menuTarget == MenuShortcutTargetConfig.Target.VARYON);
        applyParametresDmgNumAppearance(commandBuilder, uuid);
        applyParametresActivityBubbleAppearance(commandBuilder, uuid);
        applyParametresActuPopupAppearance(commandBuilder, uuid);
    }

    private static void applyParametresActuPopupAppearance(
            @Nonnull UICommandBuilder commandBuilder,
            @Nullable UUID uuid) {
        if (uuid == null) {
            return;
        }
        applyDualSwitch(commandBuilder,
            "ParametresActuPopupOn", "ParametresActuPopupOnLabel",
            "ParametresActuPopupOff", "ParametresActuPopupOffLabel",
            ActuStateConfig.getInstance().wantsPopup(uuid));
    }

    private static void applyParametresActivityBubbleAppearance(
            @Nonnull UICommandBuilder commandBuilder,
            @Nullable UUID uuid) {
        if (uuid == null) {
            return;
        }
        applyDualSwitch(commandBuilder,
            "ParametresActivityBubbleOn", "ParametresActivityBubbleOnLabel",
            "ParametresActivityBubbleOff", "ParametresActivityBubbleOffLabel",
            ACTIVITY_BUBBLE_ON.getOrDefault(uuid, true));
    }

    private static void applyParametresDmgNumAppearance(
            @Nonnull UICommandBuilder commandBuilder,
            @Nullable UUID uuid) {
        boolean available = DamageNumberBridge.isAvailable();
        commandBuilder.set("#ParametresDmgNumRow.Visible", available);
        if (!available || uuid == null) {
            return;
        }
        applyDualSwitch(commandBuilder,
            "ParametresDmgNumOn", "ParametresDmgNumOnLabel",
            "ParametresDmgNumOff", "ParametresDmgNumOffLabel",
            DMG_NUM_ON.computeIfAbsent(uuid, DamageNumberBridge::isEnabled));
    }

    private static void applyDualSwitch(
            @Nonnull UICommandBuilder ui,
            @Nonnull String onBtn,
            @Nonnull String onLabel,
            @Nonnull String offBtn,
            @Nonnull String offLabel,
            boolean enabled) {
        styleSwitchButton(ui, onBtn, onLabel, enabled, SWITCH_ON_ACTIVE_BG, SWITCH_ON_INACTIVE_BG);
        styleSwitchButton(ui, offBtn, offLabel, !enabled, SWITCH_OFF_ACTIVE_BG, SWITCH_OFF_INACTIVE_BG);
    }

    private static void styleSwitchButton(
            @Nonnull UICommandBuilder ui,
            @Nonnull String btnId,
            @Nonnull String labelId,
            boolean active,
            @Nonnull String activeBg,
            @Nonnull String inactiveBg) {
        String bg = active ? activeBg : inactiveBg;
        PatchStyle def = new PatchStyle().setColor(Value.of(bg));
        PatchStyle hov = new PatchStyle().setColor(Value.of(active ? activeBg : inactiveBg));
        String base = "#" + btnId + ".Style";
        ui.setObject(base + ".Default.Background", def);
        ui.setObject(base + ".Hovered.Background", hov);
        ui.setObject(base + ".Pressed.Background", def);
        ui.set("#" + labelId + ".Style.TextColor", active ? SWITCH_ACTIVE_TEXT : SWITCH_INACTIVE_TEXT);
    }

    private static void applyShortcutToggle(
            @Nonnull UICommandBuilder cb,
            @Nonnull String btnId,
            @Nonnull String labelId,
            boolean selected) {
        String selBg = "#D4AF3760";
        String idleBg = "#1a2838CC";
        String selHov = "#D4AF3788";
        String idleHov = "#243448CC";
        String bg = selected ? selBg : idleBg;
        String hov = selected ? selHov : idleHov;
        PatchStyle def = new PatchStyle().setColor(Value.of(bg));
        PatchStyle hovS = new PatchStyle().setColor(Value.of(hov));
        String base = "#" + btnId + ".Style";
        cb.setObject(base + ".Default.Background", def);
        cb.setObject(base + ".Hovered.Background", hovS);
        cb.setObject(base + ".Pressed.Background", def);
        cb.set("#" + labelId + ".Style.TextColor", selected ? "#FFFFFF" : "#8899aa");
        cb.set("#" + labelId + ".Style.RenderBold", selected);
    }

    private void buildCommandsContent(@Nonnull UICommandBuilder commandBuilder) {
        List<CommandsConfig.CommandCategory> categories = CommandsConfig.getInstance().getCategories();

        for (int i = 1; i <= MAX_SLOTS; i++) {
            commandBuilder.set("#CategoryHeader" + i + ".Visible", false);
            commandBuilder.set("#ButtonRow" + i + ".Visible", false);
        }
        for (int i = 1; i <= MAX_BUTTONS; i++) {
            commandBuilder.set("#CommandButton" + i + ".Visible", false);
        }

        int slotIndex = 0;

        for (CommandsConfig.CommandCategory category : categories) {
            List<CommandsConfig.CommandButton> buttons = category.getButtons();
            boolean isFirstRow = true;

            for (int i = 0; i < buttons.size(); i += BUTTONS_PER_ROW) {
                slotIndex++;
                if (slotIndex > MAX_SLOTS) break;

                if (isFirstRow) {
                    String catName = category.getName();
                    if (catName != null && !catName.isBlank()) {
                        commandBuilder.set("#CategoryHeader" + slotIndex + ".Visible", true);
                        commandBuilder.set("#CategoryTitle" + slotIndex + ".TextSpans", Message.raw(catName));
                        applyCategoryHeaderIcon(commandBuilder, slotIndex, category.getHeaderIcon());
                    }
                    isFirstRow = false;
                }

                commandBuilder.set("#ButtonRow" + slotIndex + ".Visible", true);

                int baseIdx = (slotIndex - 1) * BUTTONS_PER_ROW;
                int count = Math.min(BUTTONS_PER_ROW, buttons.size() - i);
                for (int j = 0; j < count; j++) {
                    int btnIdx = baseIdx + j + 1;
                    if (btnIdx > MAX_BUTTONS) break;
                    CommandsConfig.CommandButton button = buttons.get(i + j);
                    commandBuilder.set("#CommandButton" + btnIdx + ".Visible", true);
                    commandBuilder.set("#CommandButton" + btnIdx + "Label.TextSpans", Message.raw(button.getLabel()));
                    commandBuilder.set("#CommandButton" + btnIdx + "Cmd.TextSpans", Message.raw(button.getCommand()));
                    applyCommandButtonIconBackground(commandBuilder, btnIdx, button.getIconUrl());
                    applyCommandButtonSolidBackground(commandBuilder, btnIdx);
                }
            }
        }
    }

    private static final String COMMAND_CATEGORY_HEADER_ICON_COLOR = "#243548";
    private static final String COMMAND_BUTTON_ICON_SLOT_COLOR = "#1e3348";
    private static final String COMMAND_BUTTON_DEFAULT_COLOR = "#2a4a6a";
    private static final String COMMAND_BUTTON_HOVERED_COLOR = "#3a5a7a";

    private static void applySolidChromeButtonStyle(
            @Nonnull UICommandBuilder commandBuilder, @Nonnull String buttonSelector) {
        PatchStyle def = new PatchStyle().setColor(Value.of(COMMAND_BUTTON_DEFAULT_COLOR));
        PatchStyle hov = new PatchStyle().setColor(Value.of(COMMAND_BUTTON_HOVERED_COLOR));
        String base = buttonSelector + ".Style";
        commandBuilder.setObject(base + ".Default.Background", def);
        commandBuilder.setObject(base + ".Hovered.Background", hov);
        commandBuilder.setObject(base + ".Pressed.Background", hov);
    }

    private static void applyCommandButtonSolidBackground(@Nonnull UICommandBuilder commandBuilder, int btnIdx) {
        applySolidChromeButtonStyle(commandBuilder, "#CommandButton" + btnIdx);
    }

    private static boolean commandButtonHasIconSlot(int btnIdx) {
        return btnIdx >= 1 && btnIdx <= 35;
    }

    private static void applyCategoryHeaderIcon(
            @Nonnull UICommandBuilder commandBuilder, int slotIndex, @Nullable String headerIcon) {
        String path = headerIcon != null ? headerIcon.trim() : "";
        if (!path.isEmpty()) {
            PatchStyle style = new PatchStyle().setTexturePath(Value.of(normalizeIconUrl(path)));
            commandBuilder.setObject("#CategoryHeader" + slotIndex + "Icon.Background", style);
        } else {
            commandBuilder.setObject(
                "#CategoryHeader" + slotIndex + "Icon.Background",
                new PatchStyle().setColor(Value.of(COMMAND_CATEGORY_HEADER_ICON_COLOR))
            );
        }
    }

    private static void applyCommandButtonIconBackground(
            @Nonnull UICommandBuilder commandBuilder, int btnIdx, @Nullable String iconUrl) {
        if (!commandButtonHasIconSlot(btnIdx)) {
            return;
        }
        String path = iconUrl != null ? iconUrl.trim() : "";
        if (!path.isEmpty()) {
            PatchStyle style = new PatchStyle().setTexturePath(Value.of(normalizeIconUrl(path)));
            commandBuilder.setObject("#CommandButton" + btnIdx + "Icon.Background", style);
            return;
        }
        if (btnIdx > 5) {
            commandBuilder.setObject(
                "#CommandButton" + btnIdx + "Icon.Background",
                new PatchStyle().setColor(Value.of(COMMAND_BUTTON_ICON_SLOT_COLOR))
            );
        }
    }

    private static String normalizeIconUrl(String raw) {
        if (raw.startsWith("/")) {
            return raw.substring(1);
        }
        return raw;
    }

    private static final String ADMIN_BTN_STYLE =
        "Background: (Color: #2a4a6a); Style: ButtonStyle(" +
        "Default: (Background: (Color: #2a4a6a)), " +
        "Hovered: (Background: (Color: #3a5a7a)), " +
        "Pressed: (Background: (Color: #2a4a6a)));";

    private void buildAdminCommandsContent(@Nonnull UICommandBuilder commandBuilder) {
        commandBuilder.clear("#AdminCommandesList");

        List<CommandsConfig.CommandCategory> categories = AdminCommandsConfig.getInstance().getCategories();
        int listIdx = 0;

        for (CommandsConfig.CommandCategory category : categories) {
            List<CommandsConfig.CommandButton> buttons = category.getButtons();
            if (buttons.isEmpty()) continue;

            String titleText = escapeForUI(category.getName().toUpperCase());
            commandBuilder.appendInline("#AdminCommandesList",
                "Label { Text: \"" + titleText + "\"; Style: (FontSize: 20, RenderBold: true, TextColor: #aaddff); Anchor: (Top: 8, Bottom: 8); }");
            listIdx++;

            for (int i = 0; i < buttons.size(); i += BUTTONS_PER_ROW) {
                int rowListIdx = listIdx;
                commandBuilder.appendInline("#AdminCommandesList",
                    "Group { LayoutMode: Left; Anchor: (Height: 53, Bottom: 4); }");
                listIdx++;

                int count = Math.min(BUTTONS_PER_ROW, buttons.size() - i);
                for (int j = 0; j < count; j++) {
                    CommandsConfig.CommandButton button = buttons.get(i + j);
                    String btnLabel = escapeForUI(button.getLabel());
                    String btnCmd = escapeForUI(button.getCommand());
                    boolean isLast = (j == count - 1);
                    String anchor = isLast ?
                        "Anchor: (Width: 152, Height: 53);" :
                        "Anchor: (Width: 152, Height: 53, Right: 12);";
                    commandBuilder.appendInline("#AdminCommandesList[" + rowListIdx + "]",
                        "Button { " + anchor + " " + ADMIN_BTN_STYLE +
                        " Group { LayoutMode: Top; Padding: (Top: 6, Bottom: 6, Left: 6, Right: 6);" +
                        " Label { Text: \"" + btnLabel + "\"; Style: (FontSize: 14, TextColor: #ffffff, RenderBold: true, HorizontalAlignment: Center); Anchor: (Bottom: 2); }" +
                        " Label { Text: \"" + btnCmd + "\"; Style: (FontSize: 10, TextColor: #aaaaaa, HorizontalAlignment: Center); }" +
                        " } }");
                }
            }
        }
    }

    private static final String KW_GREEN = "#66bb6a";
    private static final String KW_PURPLE = "#ab47bc";
    private static final String KW_ORANGE = "#ffa726";
    private static final String KW_BLUE = "#42a5f5";

    private static final Pattern KEYWORD_PATTERN = Pattern.compile(
        "(?iu)\\b(Fraktale|Fracture|Haven|Gaïa|Gaia|Varyon|Novale|Noyau)\\b");

    private static Message buildRichSegment(@Nonnull String text, boolean boldPlainParts) {
        if (text.isEmpty()) {
            return Message.raw("");
        }
        Matcher m = KEYWORD_PATTERN.matcher(text);
        Message out = Message.empty();
        int last = 0;
        while (m.find()) {
            if (m.start() > last) {
                Message plain = Message.raw(text.substring(last, m.start()));
                if (boldPlainParts) {
                    plain = plain.bold(true);
                }
                out.insert(plain);
            }
            String matched = text.substring(m.start(), m.end());
            out.insert(Message.raw(matched).color(keywordColor(matched)).bold(true));
            last = m.end();
        }
        if (last < text.length()) {
            Message plain = Message.raw(text.substring(last));
            if (boldPlainParts) {
                plain = plain.bold(true);
            }
            out.insert(plain);
        }
        return out;
    }

    private static Message buildLineWithBoldMarkup(@Nonnull String line) {
        Message out = Message.empty();
        int pos = 0;
        while (true) {
            int b = line.indexOf("[B]", pos);
            if (b < 0) {
                out.insert(buildRichSegment(line.substring(pos), false));
                break;
            }
            if (b > pos) {
                out.insert(buildRichSegment(line.substring(pos, b), false));
            }
            int close = line.indexOf("[/B]", b + 3);
            if (close < 0) {
                out.insert(buildRichSegment(line.substring(b), false));
                break;
            }
            String inner = line.substring(b + 3, close);
            out.insert(buildRichSegment(inner, true));
            pos = close + 4;
        }
        return out;
    }

    private static String keywordColor(@Nonnull String word) {
        String w = word.toLowerCase(Locale.ROOT);
        return switch (w) {
            case "haven", "gaïa", "gaia" -> KW_GREEN;
            case "varyon" -> KW_PURPLE;
            case "fracture", "fraktale" -> KW_ORANGE;
            case "noyau", "novale" -> KW_BLUE;
            default -> "#dddddd";
        };
    }

    private static void appendRichLabelLine(
        @Nonnull UICommandBuilder commandBuilder,
        @Nonnull String containerId,
        @Nonnull String lineIdPrefix,
        @Nonnull int[] lineCounter,
        @Nonnull Message textSpans,
        int fontSize,
        @Nonnull String defaultTextColor,
        boolean bold,
        boolean useBottomAnchor
    ) {
        String id = lineIdPrefix + lineCounter[0]++;
        String anchor = useBottomAnchor ? "Anchor: (Bottom: 4, Left: 0, Right: 0);" : "Anchor: (Left: 0, Right: 0);";
        commandBuilder.appendInline(
            containerId,
            "Label #" + id + " { " + anchor + " Style: (FontSize: " + fontSize + ", TextColor: " + defaultTextColor + ", RenderBold: " + bold + ", Wrap: true); }");
        commandBuilder.set("#" + id + ".TextSpans", textSpans);
    }

    private void buildScrollableRichContent(
        @Nonnull UICommandBuilder commandBuilder,
        @Nonnull String containerId,
        @Nonnull String content,
        @Nonnull String lineIdPrefix,
        boolean useBottomAnchorOnLabels
    ) {
        commandBuilder.clear(containerId);
        int[] nextLine = {0};
        String[] lines = content.split("\\R");
        boolean previousWasTitle = false;

        for (String rawLine : lines) {
            String line = rawLine.trim();

            if (line.equals("[SEPARATOR]")) {
                commandBuilder.appendInline(containerId, "Group { Anchor: (Height: 8); }");
                commandBuilder.appendInline(containerId, "Group { Anchor: (Height: 1, Left: 0, Right: 0); Background: (Color: #4a5568); }");
                commandBuilder.appendInline(containerId, "Group { Anchor: (Height: 8); }");
                previousWasTitle = false;
            } else if (line.isEmpty()) {
                previousWasTitle = false;
            } else if (line.startsWith("[COLOR:") && line.contains("]")) {
                if (!previousWasTitle) {
                    commandBuilder.appendInline(containerId, "Group { Anchor: (Height: 8); }");
                }
                int colorEnd = line.indexOf("]");
                String titleText = line.substring(colorEnd + 1).replace("[/COLOR]", "");
                appendRichLabelLine(
                    commandBuilder,
                    containerId,
                    lineIdPrefix,
                    nextLine,
                    buildLineWithBoldMarkup(titleText),
                    18,
                    "#ffffff",
                    true,
                    useBottomAnchorOnLabels);
                commandBuilder.appendInline(containerId, "Group { Anchor: (Height: 4); }");
                previousWasTitle = true;
            } else {
                appendRichLabelLine(
                    commandBuilder,
                    containerId,
                    lineIdPrefix,
                    nextLine,
                    buildLineWithBoldMarkup(line),
                    14,
                    "#dddddd",
                    false,
                    useBottomAnchorOnLabels);
                previousWasTitle = false;
            }
        }
    }

    private void buildHomeContent(@Nonnull UICommandBuilder commandBuilder) {
        buildScrollableRichContent(
            commandBuilder,
            "#HomeTextContainer",
            HomeConfig.getInstance().getContent(),
            "HomeLn",
            false);
    }
    
    private String escapeForUI(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void buildNewsContent(@Nonnull UICommandBuilder commandBuilder) {
        List<NewsConfig.NewsEntry> entries = NewsConfig.getInstance().getEntries();

        commandBuilder.clear("#MisesAJourContent");

        for (int i = 0; i < entries.size(); i++) {
            NewsConfig.NewsEntry entry = entries.get(i);
            String idx = String.valueOf(i + 1);

            commandBuilder.appendInline(
                "#MisesAJourContent",
                "Group #NewsEntry" + idx + " { LayoutMode: Top; Anchor: (Bottom: 12); }");

            commandBuilder.appendInline(
                "#NewsEntry" + idx,
                "Label #NewsDate" + idx + " { Style: (FontSize: 11, TextColor: #6699bb); Anchor: (Bottom: 2); }");
            commandBuilder.set("#NewsDate" + idx + ".TextSpans", Message.raw(entry.getDate()));

            commandBuilder.appendInline(
                "#NewsEntry" + idx,
                "Label #NewsTitle" + idx + " { Style: (FontSize: 16, RenderBold: true, TextColor: #aaddff); Anchor: (Bottom: 4); }");
            commandBuilder.set("#NewsTitle" + idx + ".TextSpans", Message.raw(entry.getTitle()));

            commandBuilder.appendInline(
                "#NewsEntry" + idx,
                "Label #NewsContent" + idx + " { Style: (FontSize: 13, TextColor: #cccccc, Wrap: true); Anchor: (Bottom: 6, Width: 600); }");
            commandBuilder.set("#NewsContent" + idx + ".TextSpans", Message.raw(entry.getContent()));

            commandBuilder.appendInline(
                "#NewsEntry" + idx,
                "Group #NewsSeparator" + idx + " { Anchor: (Height: 1); Background: (Color: #2a3a4a); }");
        }
    }

    private void buildTutorielContent(@Nonnull UICommandBuilder commandBuilder) {
        buildScrollableRichContent(
            commandBuilder,
            "#TutorielTextContainer",
            TutorielConfig.getInstance().getContent(),
            "TutoLn",
            true);
    }

    private void buildVaryonContent(@Nonnull UICommandBuilder commandBuilder) {
        buildVaryonQuickRow(commandBuilder);
        buildScrollableTextContent(commandBuilder, "#VaryonTextContainer", VaryonConfig.getInstance().getContent());
    }

    private static final String VARYON_QUICK_ICON = "Icons/Varyon_Icon.png";

    private void buildVaryonQuickRow(@Nonnull UICommandBuilder commandBuilder) {
        PatchStyle iconStyle = new PatchStyle().setTexturePath(Value.of(normalizeIconUrl(VARYON_QUICK_ICON)));
        for (int i = 0; i < VARYON_QUICK_LABELS.length; i++) {
            int n = i + 1;
            applySolidChromeButtonStyle(commandBuilder, "#VaryonQuickButton" + n);
            commandBuilder.set("#VaryonQuickButton" + n + "Label.TextSpans", Message.raw(VARYON_QUICK_LABELS[i]));
            commandBuilder.set("#VaryonQuickButton" + n + "Cmd.TextSpans", Message.raw(VARYON_QUICK_COMMANDS[i].trim()));
            commandBuilder.setObject("#VaryonQuickButton" + n + "Icon.Background", iconStyle);
        }
    }

    private void applyPlaytimeUnavailableUi(@Nonnull UICommandBuilder cb) {
        cb.set("#PtRewardsTitle.TextSpans", Message.raw("Temps de jeu"));
        cb.set("#PtPlayerName.TextSpans", Message.raw(PLAYTIME_UNAVAILABLE_USER_MSG));
        cb.set("#PtTotalTime.TextSpans", Message.raw(""));
        cb.set("#PtSessionTime.TextSpans", Message.raw(""));
        cb.set("#PtLastLoginTitle.TextSpans", Message.raw(""));
        cb.set("#PtFirstLoginTitle.TextSpans", Message.raw(""));
        cb.set("#PtLastLoginDate.TextSpans", Message.raw(""));
        cb.set("#PtFirstLoginDate.TextSpans", Message.raw(""));
        cb.set("#PtClaimAllBtn.TextSpans", Message.raw(""));
        cb.set("#PtProgressText.TextSpans", Message.raw(""));
        cb.set("#PtNextRewardLabel.TextSpans", Message.raw(""));
        cb.clear("#PtProgressBarRow");
        cb.clear("#PtChestsRow");
        cb.clear("#PtChestActionsRow");
    }

    private void buildPlaytimeContent(@Nonnull UICommandBuilder cb) {
        if (!PlaytimeBridge.isAvailable()) {
            LOG.log(Level.FINE, "Playtime tab: module non présent");
            applyPlaytimeUnavailableUi(cb);
            return;
        }
        if (!PlaytimeBridge.isBackendOperational()) {
            LOG.log(Level.FINE, "Playtime tab: service indisponible (pas d'accès à l'API)");
            applyPlaytimeUnavailableUi(cb);
            return;
        }
        UUID uuid = playerRef.getUuid();
        String username = playerRef.getUsername();

        long total   = PlaytimeBridge.getTotalPlaytime(uuid);
        long daily   = PlaytimeBridge.getDailyPlaytime(uuid);
        long first   = PlaytimeBridge.getFirstLogin(uuid);
        long last    = PlaytimeBridge.getLastLogin(uuid);
        long[] rdata = PlaytimeBridge.getDailyRewardData(uuid);

        cb.set("#PtRewardsTitle.TextSpans",   Message.raw("Récompenses de temps de jeu"));
        cb.set("#PtPlayerName.TextSpans",   Message.raw(username));
        cb.set("#PtTotalTime.TextSpans",    Message.raw("Total : " + PlaytimeBridge.formatTime(total)));
        long sessionMin = (daily > 0) ? (daily % 3_600_000L) / 60_000L : 0;
        cb.set("#PtSessionTime.TextSpans",  Message.raw("Session : " + sessionMin + "m aujourd'hui"));
        cb.set("#PtLastLoginTitle.TextSpans", Message.raw("Dernière connexion"));
        cb.set("#PtFirstLoginTitle.TextSpans", Message.raw("Première connexion"));
        cb.set("#PtLastLoginDate.TextSpans", Message.raw(PlaytimeBridge.formatConnectionDate(last)));
        cb.set("#PtFirstLoginDate.TextSpans", Message.raw(PlaytimeBridge.formatConnectionDate(first)));
        boolean canClaimAll = hasClaimablePlaytimeReward(uuid);
        cb.set("#PtClaimAllBtn.TextSpans", canClaimAll
            ? Message.raw("Récupérer tout")
            : Message.raw("Récupérer tout").color("#8899aa"));
        cb.set("#PtClaimAllBtn.Disabled", !canClaimAll);

        buildPlaytimeProgressBar(cb, daily, rdata);
        buildPlaytimeChests(cb, daily, rdata);
    }

    private void buildPlaytimeProgressBar(@Nonnull UICommandBuilder cb, long dailyMs, long[] rdata) {
        int nextIdx = -1;
        for (int i = 0; i < rdata.length; i += 2) {
            if (rdata[i + 1] != 1L) {
                nextIdx = i;
                break;
            }
        }

        double progress;
        String progressText;
        String nextLabel;

        if (nextIdx < 0) {
            progress = 1.0;
            progressText = rdata.length == 0 ? "Aucune récompense configurée" : "Toutes les récompenses reçues !";
            nextLabel = "";
        } else {
            long nextThreshold = rdata[nextIdx];
            long prevThreshold = nextIdx >= 2 ? rdata[nextIdx - 2] : 0L;
            long range = nextThreshold - prevThreshold;
            long filled = Math.max(0L, Math.min(dailyMs - prevThreshold, range));
            progress = range > 0 ? (double) filled / (double) range : 0.0;
            long dailyMin = dailyMs / 60_000L;
            long nextMin  = nextThreshold / 60_000L;
            progressText = dailyMin + " / " + nextMin + " min";
            long remain = Math.max(0L, nextThreshold - dailyMs);
            nextLabel = "Prochaine récompense dans : " + PlaytimeBridge.formatRewardTime(remain) + " de jeu";
        }

        cb.set("#PtProgressText.TextSpans", Message.raw(progressText));
        cb.set("#PtNextRewardLabel.TextSpans", Message.raw(nextLabel));

        cb.clear("#PtProgressBarRow");
        int scale = 1000;
        int fillW = (int) Math.round(scale * Math.min(1.0, Math.max(0.0, progress)));
        int emptyW = scale - fillW;
        if (fillW > 0) {
            cb.appendInline("#PtProgressBarRow",
                "Group { FlexWeight: " + fillW + "; Anchor: (Top: 0, Bottom: 0); Background: (Color: #27ae60); }");
        }
        if (emptyW > 0) {
            cb.appendInline("#PtProgressBarRow",
                "Group { FlexWeight: " + emptyW + "; Anchor: (Top: 0, Bottom: 0); }");
        }
    }

    private void appendPlaytimeChestButtonEvents(@Nonnull UIEventBuilder eventBuilder, @Nullable UUID uuid) {
        if (uuid == null) {
            return;
        }
        long[] rdata = PlaytimeBridge.getDailyRewardData(uuid);
        long dailyMs = PlaytimeBridge.getDailyPlaytime(uuid);
        String[] ids = PlaytimeBridge.getDailyRewardIds();
        int slot = 0;
        for (int i = 0; i < rdata.length; i += 2) {
            boolean claimed = rdata[i + 1] == 1L;
            boolean eligible = dailyMs >= rdata[i];
            if (eligible && !claimed && slot < ids.length && ids[slot] != null && !ids[slot].isBlank()) {
                eventBuilder.addEventBinding(
                        CustomUIEventBindingType.Activating,
                        "#PtChestBtn" + slot,
                        EventData.of("Action", "playtimeclaim").append("PlaytimeRewardId", ids[slot]));
            }
            slot++;
        }
    }

    private boolean hasClaimablePlaytimeReward(@Nullable UUID uuid) {
        if (uuid == null || !PlaytimeBridge.isAvailable() || !PlaytimeBridge.isBackendOperational()) {
            return false;
        }
        long[] rdata = PlaytimeBridge.getDailyRewardData(uuid);
        long dailyMs = PlaytimeBridge.getDailyPlaytime(uuid);
        for (int i = 0; i < rdata.length; i += 2) {
            boolean claimed = rdata[i + 1] == 1L;
            boolean eligible = dailyMs >= rdata[i];
            if (eligible && !claimed) {
                return true;
            }
        }
        return false;
    }

    private void buildPlaytimeChests(@Nonnull UICommandBuilder cb, long dailyMs, long[] rdata) {
        cb.clear("#PtChestsRow");
        cb.clear("#PtChestActionsRow");
        if (rdata.length == 0) return;

        for (int i = 0; i < rdata.length; i += 2) {
            long threshold = rdata[i];
            boolean claimed = rdata[i + 1] == 1L;
            boolean eligible = dailyMs >= threshold;
            int slot = i / 2;

            if (i > 0) {
                cb.appendInline("#PtChestsRow",
                    "Group { LayoutMode: Middle; Anchor: (Width: 18, Top: 0, Bottom: 0); " +
                    "Label { Text: \">\"; Style: (FontSize: 16, TextColor: #3a4a5a, HorizontalAlignment: Center, VerticalAlignment: Center); } }");
                cb.appendInline("#PtChestActionsRow",
                    "Group { Anchor: (Width: 18, Top: 0, Bottom: 0); }");
            }

            String innerBg = playtimeChestInnerBg(slot, claimed, eligible);
            String borderBg = claimed ? "#3a7a5a" : playtimeBorderLighterThanInner(innerBg);
            String timeLabel = escapeForUI(PlaytimeBridge.formatRewardTime(threshold));
            String statusTxt = escapeForUI(claimed ? "Reçue" : eligible ? "Récupérer" : "Verrouillée");

            cb.appendInline("#PtChestsRow",
                "Group { LayoutMode: Top; Anchor: (Width: 110); " +
                "Background: (Color: " + borderBg + "); Padding: (Left: 2, Right: 2, Top: 2, Bottom: 2); " +
                "Group { LayoutMode: Top; Background: (Color: " + innerBg + "); " +
                "Padding: (Left: 6, Right: 6, Top: " + PLAYTIME_CHEST_INNER_PAD_TOP + ", Bottom: " + PLAYTIME_CHEST_INNER_PAD_BOTTOM + "); " +
                "Group { LayoutMode: Middle; Anchor: (Height: " + PLAYTIME_CHEST_ITEM_ROW_HEIGHT + ", Left: 0, Right: 0); " +
                "Group { Anchor: (Width: " + PLAYTIME_CHEST_ICON_SIZE + ", Height: " + PLAYTIME_CHEST_ICON_SIZE + "); Background: #1a2530; Padding: " + PLAYTIME_CHEST_ICON_FRAME_PAD + "; " +
                "ItemSlot #PtChestSlot" + slot + " { Anchor: (Full: 0); ShowQualityBackground: true; } } } " +
                "Label { Text: \"" + timeLabel + "\"; Anchor: (Height: " + PLAYTIME_CHEST_TIME_LABEL_HEIGHT + ", Top: " + PLAYTIME_CHEST_GAP_ICON_TO_LABEL + ", Left: 0, Right: 0); " +
                "Style: (FontSize: 12, TextColor: #dceeff, RenderBold: true, HorizontalAlignment: Center, VerticalAlignment: Center); } " +
                "} }");

            cb.appendInline("#PtChestActionsRow",
                "Group { LayoutMode: Middle; Anchor: (Width: 110); " +
                "TextButton #PtChestBtn" + slot + " { " +
                "Text: \"" + statusTxt + "\"; " +
                "Anchor: (Width: 102, Height: 32); " +
                "Style: TextButtonStyle(" +
                "Default: (Background: (Color: #2a3545), LabelStyle: (FontSize: 12, TextColor: #dceeff, RenderBold: true, HorizontalAlignment: Center, VerticalAlignment: Center)), " +
                "Hovered: (Background: (Color: #3d4d62), LabelStyle: (FontSize: 12, TextColor: #ffffff, RenderBold: true, HorizontalAlignment: Center, VerticalAlignment: Center)), " +
                "Pressed: (Background: (Color: #243040), LabelStyle: (FontSize: 12, TextColor: #dceeff, RenderBold: true, HorizontalAlignment: Center, VerticalAlignment: Center))" +
                "); " +
                "} }");
            cb.set("#PtChestSlot" + slot + ".ItemId", pickPlaytimeChestItemId(slot));
        }
    }

    private static String pickPlaytimeChestItemId(int slot) {
        String id = (slot >= 0 && slot < PLAYTIME_CHEST_VISUAL_ITEM_IDS.length)
                ? PLAYTIME_CHEST_VISUAL_ITEM_IDS[slot]
                : PLAYTIME_CHEST_FALLBACK_ITEM_ID;
        if (ItemModule.exists(id)) {
            return id;
        }
        return ItemModule.exists(PLAYTIME_CHEST_FALLBACK_ITEM_ID) ? PLAYTIME_CHEST_FALLBACK_ITEM_ID : id;
    }

    private static String playtimeBorderLighterThanInner(String innerHex) {
        int rgb = hexToRgb(innerHex);
        int r = (rgb >> 16) & 0xff;
        int g = (rgb >> 8) & 0xff;
        int b = rgb & 0xff;
        final double blend = 0.26;
        return rgbToHex(
                clamp255((int) Math.round(r + (255 - r) * blend)),
                clamp255((int) Math.round(g + (255 - g) * blend)),
                clamp255((int) Math.round(b + (255 - b) * blend)));
    }

    private static String playtimeChestInnerBg(int slot, boolean claimed, boolean eligible) {
        if (claimed) {
            return "#1a3a2a";
        }
        if (slot <= 0) {
            return eligible ? "#1a3050" : "#1a2230";
        }
        String vivid;
        if (slot < PLAYTIME_CHEST_LOCKED_INNER.length && PLAYTIME_CHEST_LOCKED_INNER[slot] != null) {
            vivid = PLAYTIME_CHEST_LOCKED_INNER[slot];
        } else {
            vivid = PLAYTIME_CHEST_LOCKED_INNER[PLAYTIME_CHEST_LOCKED_INNER.length - 1];
        }
        String locked = playtimeMutedChestFill(vivid);
        if (!eligible) {
            return locked;
        }
        return playtimeEligibleInnerFromLocked(locked);
    }

    private static String playtimeMutedChestFill(@Nonnull String vividHex) {
        return mixHexTowardBase(vividHex, PLAYTIME_CHEST_MUTE_BASE, PLAYTIME_CHEST_VIVID_WEIGHT);
    }

    private static String mixHexTowardBase(@Nonnull String colorHex, @Nonnull String baseHex, double colorWeight) {
        int c = hexToRgb(colorHex);
        int t = hexToRgb(baseHex);
        int cr = (c >> 16) & 0xff;
        int cg = (c >> 8) & 0xff;
        int cb = c & 0xff;
        int br = (t >> 16) & 0xff;
        int bg = (t >> 8) & 0xff;
        int bb = t & 0xff;
        double w = colorWeight;
        double inv = 1d - w;
        return rgbToHex(
                clamp255((int) Math.round(cr * w + br * inv)),
                clamp255((int) Math.round(cg * w + bg * inv)),
                clamp255((int) Math.round(cb * w + bb * inv)));
    }

    private static String playtimeEligibleInnerFromLocked(String lockedHex) {
        int rgb = hexToRgb(lockedHex);
        int r = (rgb >> 16) & 0xff;
        int g = (rgb >> 8) & 0xff;
        int b = rgb & 0xff;
        float avg = (r + g + b) / 3f;
        final int lighten = 22;
        final int darken = 26;
        if (avg >= 172f) {
            return rgbToHex(
                    clamp255(r - darken),
                    clamp255(g - (darken * 40 / 44)),
                    clamp255(b - (darken * 42 / 44)));
        }
        return rgbToHex(
                clamp255(r + lighten),
                clamp255(g + lighten - 4),
                clamp255(b + lighten + 4));
    }

    private static int hexToRgb(@Nonnull String hexWithHash) {
        String h = hexWithHash.startsWith("#") ? hexWithHash.substring(1) : hexWithHash;
        return Integer.parseInt(h, 16);
    }

    private static String rgbToHex(int r, int g, int b) {
        return String.format(Locale.ROOT, "#%02X%02X%02X", r, g, b);
    }

    private static int clamp255(int v) {
        return Math.min(255, Math.max(0, v));
    }

    private void buildScrollableTextContent(@Nonnull UICommandBuilder commandBuilder, @Nonnull String containerId, @Nonnull String content) {
        commandBuilder.clear(containerId);

        String[] lines = content.split("\\R");
        boolean previousWasTitle = false;

        for (String line : lines) {
            line = line.trim();

            if (line.equals("[SEPARATOR]")) {
                commandBuilder.appendInline(containerId, "Group { Anchor: (Height: 8); }");
                commandBuilder.appendInline(containerId, "Group { Anchor: (Height: 1, Left: 0, Right: 0); Background: (Color: #4a5568); }");
                commandBuilder.appendInline(containerId, "Group { Anchor: (Height: 8); }");
                previousWasTitle = false;
            } else if (line.isEmpty()) {
                previousWasTitle = false;
            } else if (line.startsWith("[COLOR:") && line.contains("]")) {
                if (!previousWasTitle) {
                    commandBuilder.appendInline(containerId, "Group { Anchor: (Height: 8); }");
                }
                int colorEnd = line.indexOf("]");
                String colorCode = line.substring(7, colorEnd);
                String text = line.substring(colorEnd + 1).replace("[/COLOR]", "");
                text = escapeForUI(text);
                commandBuilder.appendInline(containerId, "Label { Text: \"" + text + "\"; Style: (FontSize: 16, TextColor: " + colorCode + ", RenderBold: true, Wrap: true); Anchor: (Bottom: 4, Left: 0, Right: 0); }");
                commandBuilder.appendInline(containerId, "Group { Anchor: (Height: 4); }");
                previousWasTitle = true;
            } else {
                String text = escapeForUI(line);
                commandBuilder.appendInline(containerId, "Label { Text: \"" + text + "\"; Style: (FontSize: 14, TextColor: #dddddd, Wrap: true); Anchor: (Bottom: 4, Left: 0, Right: 0); }");
                previousWasTitle = false;
            }
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull EventDataClass data) {
        if ("tab".equals(data.action) && data.tab != null) {
            if ("admin".equals(data.tab) && !isAdmin) {
                return;
            }
            activeTab = data.tab;
            UICommandBuilder commandBuilder = new UICommandBuilder();
            UIEventBuilder eventBuilder = new UIEventBuilder();
            buildContent(commandBuilder, eventBuilder, store, ref);
            buildTabBar(commandBuilder, eventBuilder, false, ref);
            sendUpdate(commandBuilder, eventBuilder, false);
        } else if ("close".equals(data.action)) {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player != null) {
                closeAndRestoreHuds(ref, store, player);
            }
        } else if ("accueilshortcut".equals(data.action) && data.shortcutMode != null) {
            PlayerRef playerRefComp = store.getComponent(ref, PlayerRef.getComponentType());
            Player playerComp = store.getComponent(ref, Player.getComponentType());
            if (playerRefComp != null && playerRefComp.getUuid() != null) {
                AccueilShortcutConfig.Mode m = AccueilShortcutConfig.Mode.fromKey(data.shortcutMode);
                AccueilShortcutConfig.getInstance().setMode(playerRefComp.getUuid(), m);
                VaryonMenuHud.attach(playerComp, playerRefComp);
                UICommandBuilder commandBuilder = new UICommandBuilder();
                UIEventBuilder eventBuilder = new UIEventBuilder();
                buildContent(commandBuilder, eventBuilder, store, ref);
                buildTabBar(commandBuilder, eventBuilder, false, ref);
                sendUpdate(commandBuilder, eventBuilder, false);
            }
        } else if ("menushortcuttarget".equals(data.action) && data.menuShortcutTarget != null) {
            PlayerRef playerRefComp = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRefComp != null && playerRefComp.getUuid() != null) {
                MenuShortcutTargetConfig.Target t =
                    MenuShortcutTargetConfig.Target.fromKey(data.menuShortcutTarget);
                MenuShortcutTargetConfig.getInstance().setTarget(playerRefComp.getUuid(), t);
                UICommandBuilder commandBuilder = new UICommandBuilder();
                UIEventBuilder eventBuilder = new UIEventBuilder();
                buildContent(commandBuilder, eventBuilder, store, ref);
                buildTabBar(commandBuilder, eventBuilder, false, ref);
                sendUpdate(commandBuilder, eventBuilder, false);
            }
        } else if ("dmgnumtoggle".equals(data.action) && data.dmgNumToggle != null) {
            PlayerRef playerRefComp = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRefComp != null && playerRefComp.getUuid() != null && DamageNumberBridge.isAvailable()) {
                boolean wantOn = "on".equals(data.dmgNumToggle);
                UUID uuid = playerRefComp.getUuid();
                if (DMG_NUM_ON.computeIfAbsent(uuid, DamageNumberBridge::isEnabled) != wantOn) {
                    CommandManager.get().handleCommand(playerRefComp, "dmgnum");
                    DMG_NUM_ON.put(uuid, wantOn);
                }
                UICommandBuilder commandBuilder = new UICommandBuilder();
                UIEventBuilder eventBuilder = new UIEventBuilder();
                buildContent(commandBuilder, eventBuilder, store, ref);
                buildTabBar(commandBuilder, eventBuilder, false, ref);
                sendUpdate(commandBuilder, eventBuilder, false);
            }
        } else if ("bbubtoggle".equals(data.action) && data.activityBubbleToggle != null) {
            PlayerRef playerRefComp = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRefComp != null && playerRefComp.getUuid() != null) {
                boolean wantOn = "on".equals(data.activityBubbleToggle);
                UUID uuid = playerRefComp.getUuid();
                if (ACTIVITY_BUBBLE_ON.getOrDefault(uuid, true) != wantOn) {
                    CommandManager.get().handleCommand(playerRefComp, "bbub");
                    ACTIVITY_BUBBLE_ON.put(uuid, wantOn);
                }
                UICommandBuilder commandBuilder = new UICommandBuilder();
                UIEventBuilder eventBuilder = new UIEventBuilder();
                buildContent(commandBuilder, eventBuilder, store, ref);
                buildTabBar(commandBuilder, eventBuilder, false, ref);
                sendUpdate(commandBuilder, eventBuilder, false);
            }
        } else if ("actupopuptoggle".equals(data.action) && data.actuPopupToggle != null) {
            PlayerRef playerRefComp = store.getComponent(ref, PlayerRef.getComponentType());
            if (playerRefComp != null && playerRefComp.getUuid() != null) {
                boolean wantOn = "on".equals(data.actuPopupToggle);
                ActuStateConfig.getInstance().setWantsPopup(playerRefComp.getUuid(), wantOn);
                UICommandBuilder commandBuilder = new UICommandBuilder();
                UIEventBuilder eventBuilder = new UIEventBuilder();
                buildContent(commandBuilder, eventBuilder, store, ref);
                buildTabBar(commandBuilder, eventBuilder, false, ref);
                sendUpdate(commandBuilder, eventBuilder, false);
            }
        } else if ("playtimeclaim".equals(data.action)
                && data.playtimeRewardId != null
                && !data.playtimeRewardId.isBlank()) {
            PlayerRef pref = store.getComponent(ref, PlayerRef.getComponentType());
            if (pref != null && pref.getUuid() != null && PlaytimeBridge.isAvailable()
                    && PlaytimeBridge.isBackendOperational()) {
                PlaytimeBridge.claimReward(pref.getUuid(), data.playtimeRewardId.trim());
                UICommandBuilder commandBuilder = new UICommandBuilder();
                UIEventBuilder eventBuilder = new UIEventBuilder();
                buildContent(commandBuilder, eventBuilder, store, ref);
                buildTabBar(commandBuilder, eventBuilder, false, ref);
                sendUpdate(commandBuilder, eventBuilder, false);
            }
        } else if ("playtimeclaimall".equals(data.action)) {
            PlayerRef pref = store.getComponent(ref, PlayerRef.getComponentType());
            if (pref != null && pref.getUuid() != null && PlaytimeBridge.isAvailable()
                    && PlaytimeBridge.isBackendOperational()) {
                PlaytimeBridge.claimAllDailyRewards(pref.getUuid());
                UICommandBuilder commandBuilder = new UICommandBuilder();
                UIEventBuilder eventBuilder = new UIEventBuilder();
                buildContent(commandBuilder, eventBuilder, store, ref);
                buildTabBar(commandBuilder, eventBuilder, false, ref);
                sendUpdate(commandBuilder, eventBuilder, false);
            }
        } else if ("refreshplaytime".equals(data.action)) {
            UICommandBuilder commandBuilder = new UICommandBuilder();
            UIEventBuilder eventBuilder = new UIEventBuilder();
            buildContent(commandBuilder, eventBuilder, store, ref);
            buildTabBar(commandBuilder, eventBuilder, false, ref);
            sendUpdate(commandBuilder, eventBuilder, false);
        } else if ("chatcommand".equals(data.action) && data.command != null) {
            Player player = store.getComponent(ref, Player.getComponentType());
            PlayerRef playerRefComponent = store.getComponent(ref, PlayerRef.getComponentType());
            if (player != null && playerRefComponent != null) {
                closeAndRestoreHuds(ref, store, player);
                playerRefComponent.getPacketHandler().write(new OpenChatWithCommand(data.command));
            }
        } else if ("command".equals(data.action) && data.command != null) {
            Player player = store.getComponent(ref, Player.getComponentType());
            PlayerRef playerRefComponent = store.getComponent(ref, PlayerRef.getComponentType());
            if (player != null && playerRefComponent != null) {
                closeAndRestoreHuds(ref, store, player);
                String command = data.command.startsWith("/") ? data.command.substring(1) : data.command;
                String cmd = command;
                PlayerRef pref = playerRefComponent;
                World world = player.getWorld();
                Runnable runCommand = () -> CommandManager.get().handleCommand(pref, cmd);
                if (world != null) {
                    HytaleServer.SCHEDULED_EXECUTOR.schedule(
                        () -> world.execute(runCommand),
                        COMMAND_EXECUTE_DELAY_MS,
                        TimeUnit.MILLISECONDS
                    );
                } else {
                    HytaleServer.SCHEDULED_EXECUTOR.schedule(runCommand, COMMAND_EXECUTE_DELAY_MS, TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    @Override
    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        super.onDismiss(ref, store);
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            player.getHudManager().getCustomHuds().values().forEach(
                com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud::show
            );
        }
    }

    private void closeAndRestoreHuds(@Nonnull Ref<EntityStore> ref,
                                     @Nonnull Store<EntityStore> store,
                                     @Nonnull Player player) {
        player.getPageManager().setPage(ref, store, Page.None);
    }

    private void restoreHuds() {
        Ref<EntityStore> ref = (Ref<EntityStore>) playerRef.getReference();
        if (ref == null || !ref.isValid()) return;
        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();
        if (world == null) return;
        world.execute(() -> {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player != null) {
                player.getHudManager().getCustomHuds().values().forEach(
                    com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud::show
                );
            }
        });
    }

    public static class EventDataClass {
        public static final BuilderCodec<EventDataClass> CODEC =
            BuilderCodec.builder(EventDataClass.class, EventDataClass::new)
                .addField(new KeyedCodec<>("Action", Codec.STRING), (entry, s) -> entry.action = s, entry -> entry.action)
                .addField(new KeyedCodec<>("Tab", Codec.STRING), (entry, s) -> entry.tab = s, entry -> entry.tab)
                .addField(new KeyedCodec<>("Command", Codec.STRING), (entry, s) -> entry.command = s, entry -> entry.command)
                .addField(new KeyedCodec<>("ShortcutMode", Codec.STRING), (entry, s) -> entry.shortcutMode = s, entry -> entry.shortcutMode)
                .addField(new KeyedCodec<>("MenuShortcutTarget", Codec.STRING), (entry, s) -> entry.menuShortcutTarget = s, entry -> entry.menuShortcutTarget)
                .addField(new KeyedCodec<>("DmgNumToggle", Codec.STRING), (entry, s) -> entry.dmgNumToggle = s, entry -> entry.dmgNumToggle)
                .addField(new KeyedCodec<>("ActivityBubbleToggle", Codec.STRING), (entry, s) -> entry.activityBubbleToggle = s, entry -> entry.activityBubbleToggle)
                .addField(new KeyedCodec<>("ActuPopupToggle", Codec.STRING), (entry, s) -> entry.actuPopupToggle = s, entry -> entry.actuPopupToggle)
                .addField(new KeyedCodec<>("PlaytimeRewardId", Codec.STRING), (entry, s) -> entry.playtimeRewardId = s, entry -> entry.playtimeRewardId)
                .build();

        public String action;
        public String tab;
        public String command;
        public String shortcutMode;
        public String menuShortcutTarget;
        public String dmgNumToggle;
        public String activityBubbleToggle;
        public String actuPopupToggle;
        public String playtimeRewardId;
    }
}
