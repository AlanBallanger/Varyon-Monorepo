package com.varyon;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.DrainPlayerFromWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.modules.entity.damage.event.KillFeedEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.events.AddWorldEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.worldmap.provider.IWorldMapProvider;
import com.varyon.command.PointsCommand;
import com.varyon.command.ExtractCommand;
import com.varyon.command.JoinCommand;
import com.varyon.command.VaryonCommand;
import com.varyon.command.RtpzCommand;
import com.varyon.command.RtpvCommand;
import com.varyon.command.RtphCommand;
import com.varyon.command.RtpsCommand;
import com.varyon.command.ReturnCommand;
import com.varyon.announce.ChatAnnouncementScheduler;
import com.varyon.component.MobScalingComponent;
import com.varyon.config.ConfigManager;
import com.varyon.config.EssenceRewardsConfig;
import com.varyon.death.DeathDetectionSystem;
import com.varyon.death.DeathPointManager;
import com.varyon.deposit.DepositBlockInteractionSystem;
import com.varyon.portal.ZonesPortalInteractionSystem;
import com.varyon.portal.ArenasPortalInteractionSystem;
import com.varyon.rtpv.RtpvConfirmManager;
import com.varyon.rtpv.RtpvCooldownStore;
import com.varyon.util.VaryonPlayerWorldPresence;
import com.varyon.util.VaryonWorldAccess;
import com.varyon.rtpv.RtpvJoinManager;
import com.varyon.deposit.DepositBlockManager;
import com.varyon.deposit.DepositUIManager;
import com.varyon.essence.EssenceKillSystem;
import com.varyon.essence.EssenceManager;
import com.varyon.essence.GlobalRewardsManager;
import com.varyon.extraction.ExtractionPortalManager;
import com.varyon.system.ExtractionPortalTickSystem;
import com.varyon.essence.EssenceMiningSystem;
import com.varyon.faction.FactionManager;
import com.varyon.hud.ZoneHUDManager;
import com.varyon.map.ZoneWorldMapProvider;
import com.varyon.safezone.SafeZoneManager;
import com.varyon.safezone.SafeZoneNotificationSystem;
import com.varyon.safezone.SafeZonePvpSystem;
import com.varyon.system.BreakOreCleanupListener;
import com.varyon.system.MobDamageScalingSystem;
import com.varyon.system.MiningLootScalingSystem;
import com.varyon.system.MobLootScalingSystem;
import com.varyon.system.MobScalingRefSystem;
import com.varyon.system.PlaceOreListener;
import com.varyon.system.VaryonBedPlaceBlockSystem;
import com.varyon.system.PlacedOreTracker;
import com.varyon.nameplate.ZoneLevelNameplateSystem;
import com.varyon.system.MobFragmentDropSystem;
import com.varyon.system.ZoneTitleTickingSystem;
import com.varyon.item.AmbassadeOrbInteraction;

import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;

import javax.annotation.Nullable;
import java.awt.Color;
import java.util.UUID;
import java.util.logging.Level;

public class VaryonPlugin extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static ConfigManager staticConfigManager;
    private static EssenceManager staticEssenceManager;
    private static FactionManager staticFactionManager;
    private static SafeZoneManager staticSafeZoneManager;
    private static SafeZoneNotificationSystem staticSafeZoneNotificationSystem;
    private static GlobalRewardsManager staticGlobalRewardsManager;
    private static VaryonPlugin staticInstance;
    private ConfigManager configManager;
    private EssenceManager essenceManager;
    private FactionManager factionManager;
    private SafeZoneManager safeZoneManager;
    private SafeZoneNotificationSystem safeZoneNotificationSystem;
    private ZoneHUDManager hudManager;
    private ExtractionPortalManager extractionPortalManager;
    private EssenceRewardsConfig essenceRewardsConfig;
    private GlobalRewardsManager globalRewardsManager;
    private DepositBlockManager depositBlockManager;
    private DepositUIManager depositUIManager;
    private DeathPointManager deathPointManager;
    private ReturnCommand returnCommand;

    public VaryonPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        // NameplateBuilder ” describe segments before any tick system is registered.
        // Wrapped in try/catch: if NameplateBuilder is not installed the classes simply
        // won't be on the classpath and we log a warning instead of crashing.
        try {
            com.frotty27.nameplatebuilder.api.NameplateAPI.describe(
                    this, "monster_level", "Monster Level",
                    com.frotty27.nameplatebuilder.api.SegmentTarget.NPCS, "Nv.5");
            com.frotty27.nameplatebuilder.api.NameplateAPI.describeVariants(
                    this, "monster_level", java.util.List.of(
                            "Préfixé (ex: Nv.5)",
                            "Numéro  (ex: 5)"));
            LOGGER.at(Level.INFO).log("NameplateBuilder integration registered (monster_level)");
        } catch (Throwable t) {
            LOGGER.at(Level.INFO).log("NameplateBuilder not available, skipping nameplate integration (" + t.getClass().getSimpleName() + ")");
        }

        try {
            staticInstance = this;

            try {
                this.getCodecRegistry(Interaction.CODEC)
                    .register(AmbassadeOrbInteraction.TYPE_NAME,
                             AmbassadeOrbInteraction.class,
                             AmbassadeOrbInteraction.CODEC);
                LOGGER.at(Level.INFO).log("Interaction varyon_ambassade_orb registered");
            } catch (Exception e) {
                LOGGER.at(Level.WARNING).log("Failed to register varyon_ambassade_orb: " + e.getMessage());
            }

            ComponentType<EntityStore, MobScalingComponent> mobScalingComponentType =
                    this.getEntityStoreRegistry().registerComponent(MobScalingComponent.class,
                            () -> new MobScalingComponent(0, 1.0f, 1.0f, 1.0f, 1.0f));
            MobScalingComponent.setComponentType(mobScalingComponentType);

            configManager = new ConfigManager(this.getDataDirectory());
            configManager.load();
            staticConfigManager = configManager;
            ChatAnnouncementScheduler.init();

            // Initialiser le système d'essence
            essenceManager = new EssenceManager(this.getDataDirectory().toFile());
            staticEssenceManager = essenceManager;

            essenceRewardsConfig = new EssenceRewardsConfig();
            essenceRewardsConfig.attach(configManager.getMobFragmentsConfig(), configManager.getEssenceEconomyConfig());
            LOGGER.at(Level.INFO).log("Essence system initialized");

            // Initialiser le système de factions
            factionManager = new FactionManager();
            staticFactionManager = factionManager;
            LOGGER.at(Level.INFO).log("Faction system initialized");

            // Initialiser le système de récompenses de faction
            globalRewardsManager = new GlobalRewardsManager(
                configManager.getFactionRewardsConfig(),
                essenceManager,
                factionManager,
                configManager.getZonePermissionsConfig(),
                this.getDataDirectory());
            staticGlobalRewardsManager = globalRewardsManager;
            
            // Lier le rewards manager à l'essence manager
            essenceManager.setRewardsManager(globalRewardsManager);
            
            LOGGER.at(Level.INFO).log("Global rewards system initialized");
            
            // Vérifier les récompenses au démarrage
            globalRewardsManager.checkAndDistributeRewards();

            extractionPortalManager = new ExtractionPortalManager(configManager.getExtractionConfig());
            ExtractionPortalTickSystem extractionTickSystem = new ExtractionPortalTickSystem();
            this.getEntityStoreRegistry().registerSystem(extractionTickSystem);
            LOGGER.at(Level.INFO).log("Extraction portal system initialized (distance: " +
                configManager.getExtractionConfig().getMinDistance() + "-" +
                configManager.getExtractionConfig().getMaxDistance() + ", duration: " +
                configManager.getExtractionConfig().getPortalDurationSeconds() + "s, cooldown: " +
                configManager.getExtractionConfig().getCooldownSeconds() + "s)");

            MobScalingRefSystem mobScalingRefSystem = new MobScalingRefSystem(configManager);
            this.getEntityStoreRegistry().registerSystem(mobScalingRefSystem);

            MobDamageScalingSystem mobDamageScalingSystem = new MobDamageScalingSystem();
            this.getEntityStoreRegistry().registerSystem(mobDamageScalingSystem);

            MobLootScalingSystem mobLootScalingSystem = new MobLootScalingSystem();
            this.getEntityStoreRegistry().registerSystem(mobLootScalingSystem);

            MobFragmentDropSystem mobFragmentDropSystem = new MobFragmentDropSystem(configManager);
            this.getEntityStoreRegistry().registerSystem(mobFragmentDropSystem.createPlayerDamageTagger());
            this.getEntityStoreRegistry().registerSystem(mobFragmentDropSystem.createDropSystem());

            EssenceKillSystem essenceKillSystem = new EssenceKillSystem(essenceManager, configManager, essenceRewardsConfig);
            this.getEntityStoreRegistry().registerSystem(essenceKillSystem);

            PlacedOreTracker placedOreTracker = new PlacedOreTracker(this.getDataDirectory());

            this.getEntityStoreRegistry().registerSystem(new PlaceOreListener(
                placedOreTracker,
                configManager,
                essenceRewardsConfig));
            this.getEntityStoreRegistry().registerSystem(new VaryonBedPlaceBlockSystem());

            EssenceMiningSystem essenceMiningSystem = new EssenceMiningSystem(essenceManager, configManager, essenceRewardsConfig, placedOreTracker);
            this.getEntityStoreRegistry().registerSystem(essenceMiningSystem);

            com.varyon.system.MiningFragmentDropSystem miningFragmentDropSystem = new com.varyon.system.MiningFragmentDropSystem(
                configManager,
                placedOreTracker);
            this.getEntityStoreRegistry().registerSystem(miningFragmentDropSystem);

            this.getEntityStoreRegistry().registerSystem(new MiningLootScalingSystem(configManager, placedOreTracker));

            this.getEntityStoreRegistry().registerSystem(new BreakOreCleanupListener(placedOreTracker, configManager, essenceRewardsConfig));
            LOGGER.at(Level.INFO).log("Essence reward systems registered");

            // NameplateBuilder ” zone level tick system (optional, skipped if mod absent)
            try {
                ZoneLevelNameplateSystem zoneLevelNameplateSystem = new ZoneLevelNameplateSystem(
                        com.frotty27.nameplatebuilder.api.NameplateAPI.getComponentType(),
                        configManager);
                this.getEntityStoreRegistry().registerSystem(zoneLevelNameplateSystem);
                LOGGER.at(Level.INFO).log("NameplateBuilder zone_level system registered");
            } catch (Throwable t) {
                LOGGER.at(Level.INFO).log("NameplateBuilder tick system skipped (" + t.getClass().getSimpleName() + ")");
            }

            // Initialiser le système de dépôt d'essence
            depositBlockManager = new DepositBlockManager(this.getDataDirectory());
            depositUIManager = new DepositUIManager(essenceManager, factionManager);
            
            DepositBlockInteractionSystem depositInteractionSystem = new DepositBlockInteractionSystem(depositBlockManager, depositUIManager);
            this.getEntityStoreRegistry().registerSystem(depositInteractionSystem);
            LOGGER.at(Level.INFO).log("Deposit block system initialized");
            this.getEntityStoreRegistry().registerSystem(new com.varyon.portal.ZonesPortalPreInteractionSystem());
            ZonesPortalInteractionSystem zonesPortalInteractionSystem = new ZonesPortalInteractionSystem();
            this.getEntityStoreRegistry().registerSystem(zonesPortalInteractionSystem);
            this.getEntityStoreRegistry().registerSystem(new com.varyon.portal.ZonesPortalTickSystem());
            LOGGER.at(Level.INFO).log("Zones portal interaction system initialized");

            this.getEntityStoreRegistry().registerSystem(new com.varyon.portal.ArenasPortalPreInteractionSystem());
            ArenasPortalInteractionSystem arenasPortalInteractionSystem = new ArenasPortalInteractionSystem();
            this.getEntityStoreRegistry().registerSystem(arenasPortalInteractionSystem);
            this.getEntityStoreRegistry().registerSystem(new com.varyon.portal.ArenasPortalTickSystem());
            LOGGER.at(Level.INFO).log("Arenas portal interaction system initialized");


            // Initialiser le système de retour au point de mort
            deathPointManager = new DeathPointManager(this.getDataDirectory());
            
            DeathDetectionSystem deathDetectionSystem = new DeathDetectionSystem(deathPointManager);
            this.getEntityStoreRegistry().registerSystem(deathDetectionSystem);
            LOGGER.at(Level.INFO).log("Death point system initialized");

            // Initialiser le système de safe zone
            if (configManager.getSafeZoneConfig().isEnabled()) {
                safeZoneManager = new SafeZoneManager(configManager.getSafeZoneConfig(), configManager.getZoneConfig(), this.getDataDirectory());
                staticSafeZoneManager = safeZoneManager;
                
                SafeZonePvpSystem safeZonePvpSystem = new SafeZonePvpSystem();
                SafeZonePvpSystem.setSafeZoneManager(safeZoneManager);
                this.getEntityStoreRegistry().registerSystem(safeZonePvpSystem);
                
                safeZoneNotificationSystem = new SafeZoneNotificationSystem(configManager.getSafeZoneConfig(), configManager.getZoneConfig(), configManager.getMessagesConfig());
                SafeZoneNotificationSystem.setSafeZoneManager(safeZoneManager);
                this.getEntityStoreRegistry().registerSystem(safeZoneNotificationSystem);
                staticSafeZoneNotificationSystem = safeZoneNotificationSystem;
                
                // Activer le PvP dans tous les mondes pour que le système de SafeZone fonctionne
                this.getEventRegistry().registerGlobal(AddWorldEvent.class, event -> {
                    World world = event.getWorld();
                    if (!world.getWorldConfig().isDeleteOnRemove()) {
                        world.getWorldConfig().setPvpEnabled(true);
                        world.getWorldConfig().markChanged();
                        LOGGER.at(Level.INFO).log("PvP enabled for world: %s (controlled by SafeZone system)", world.getName());
                    }
                });
                
                // Activer aussi pour les mondes déjà chargés
                for (World world : Universe.get().getWorlds().values()) {
                    if (!world.getWorldConfig().isDeleteOnRemove()) {
                        world.getWorldConfig().setPvpEnabled(true);
                        world.getWorldConfig().markChanged();
                        LOGGER.at(Level.INFO).log("PvP enabled for existing world: %s (controlled by SafeZone system)", world.getName());
                    }
                }
                
                LOGGER.at(Level.INFO).log("Safe zone rotation system enabled");
            }

            ZoneTitleTickingSystem zoneTitleSystem = new ZoneTitleTickingSystem(configManager);
            this.getEntityStoreRegistry().registerSystem(zoneTitleSystem);

            if (configManager.getZoneConfig().isMinimapEnabled()) {
                setupMinimapProvider();
            }

            hudManager = new ZoneHUDManager(configManager.getZoneConfig(), configManager.getMessagesConfig(), configManager.getZonePermissionsConfig());

            this.getEventRegistry().registerGlobal(EventPriority.LAST, DrainPlayerFromWorldEvent.class, event -> {
                try {
                    PlayerRef playerRef = event.getHolder().getComponent(PlayerRef.getComponentType());
                    if (playerRef == null) {
                        return;
                    }
                    hudManager.removePlayer(playerRef.getUuid());
                } catch (Exception e) {
                    LOGGER.at(Level.WARNING).log("DrainPlayerFromWorld faction/HUD: " + e.getMessage());
                }
            });

            this.getEventRegistry().registerGlobal(EventPriority.FIRST, AddPlayerToWorldEvent.class, event -> {
                try {
                    PlayerRef playerRef = event.getHolder().getComponent(PlayerRef.getComponentType());
                    Player player = event.getHolder().getComponent(Player.getComponentType());
                    if (playerRef != null && player != null && player.getPlayerConfigData() != null) {
                        World destWorld = event.getWorld();
                        UUID uuid = playerRef.getUuid();
                        boolean wasInVaryon = VaryonPlayerWorldPresence.isInVaryonEnabledWorld(uuid);
                        boolean destInVaryon = VaryonWorldAccess.isVaryonEnabledWorld(destWorld);
                        if (wasInVaryon && !destInVaryon) {
                            int lost = essenceManager.clearCarriedFactionPoints(uuid, playerRef.getUsername());
                            if (lost > 0) {
                                playerRef.sendMessage(Message.raw(
                                    "Tu quittes un monde Varyon : " + lost + " points de faction sur toi ont été perdus.")
                                    .color(Color.YELLOW));
                            }
                        }
                        VaryonPlayerWorldPresence.update(uuid, destWorld);
                    }
                } catch (Exception e) {
                    LOGGER.at(Level.WARNING).log("AddPlayerToWorld faction/presence: " + e.getMessage());
                }
            });

            this.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, event -> {
                PlayerRef playerRef = event.getPlayerRef();
                if (playerRef != null) {
                    VaryonPlayerWorldPresence.clear(playerRef.getUuid());
                }
            });

            if (hudManager.isAvailable()) {
                LOGGER.at(Level.INFO).log("Zone HUD initialized with Objective system");
                
                this.getEventRegistry().registerGlobal(PlayerConnectEvent.class, event -> {
                    try {
                        PlayerRef playerRef = event.getPlayerRef();
                        // Charger l'essence du joueur depuis la base de données
                        essenceManager.loadPlayer(playerRef.getUuid());
                    } catch (Exception e) {
                        LOGGER.at(Level.WARNING).log("Failed to load player faction points: " + e.getMessage());
                    }
                });
                
                this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, event -> {
                    Player player = event.getPlayer();
                    Ref ref = event.getPlayerRef();
                    Store store = ref.getStore();
                    World world = ((EntityStore)store.getExternalData()).getWorld();

                    world.execute(() -> {
                        try {
                            PlayerRef playerRef = (PlayerRef)store.getComponent(ref, PlayerRef.getComponentType());
                            if (playerRef == null) return;
                            hudManager.registerPlayer(player, playerRef);
                            LOGGER.at(Level.INFO).log("Registered HUD for player: " + playerRef.getUuid());
                            if (globalRewardsManager != null) {
                                globalRewardsManager.onPlayerReady(playerRef, ref, store);
                            }
                        } catch (Exception e) {
                            LOGGER.at(Level.WARNING).log("Failed to register HUD for player: " + e.getMessage());
                        }
                    });
                });

                this.getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, event -> {
                    try {
                        PlayerRef playerRef = event.getHolder().getComponent(PlayerRef.getComponentType());
                        Player player = event.getHolder().getComponent(Player.getComponentType());
                        if (playerRef != null && player != null && player.getWorld() != null) {
                            hudManager.registerPlayer(player, playerRef);
                        }
                    } catch (Exception e) {
                        LOGGER.at(Level.WARNING).log("Failed to register HUD on world join: " + e.getMessage());
                    }
                });

                this.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, event -> {
                    PlayerRef playerRef = event.getPlayerRef();
                    if (playerRef == null) {
                        return;
                    }
                    hudManager.removePlayer(playerRef.getUuid());
                    RtpvJoinManager joinMgr = RtpvJoinManager.getInstance();
                    if (joinMgr != null) joinMgr.onPlayerDisconnect(playerRef.getUuid());
                    essenceManager.savePlayer(playerRef.getUuid());
                    if (safeZoneNotificationSystem != null) {
                        safeZoneNotificationSystem.removePlayer(playerRef.getUuid());
                    }
                    if (extractionPortalManager != null) {
                        extractionPortalManager.removePlayerPortal(playerRef.getUuid());
                    }
                    RtpvConfirmManager confirmMgr = RtpvConfirmManager.getInstance();
                    if (confirmMgr != null) {
                        confirmMgr.onPlayerDisconnect(playerRef.getUuid());
                    }
                    RtpvCooldownStore.onPlayerDisconnect(playerRef.getUuid());
                });
            } else {
                LOGGER.at(Level.WARNING).log("Zone HUD could not be initialized");
            }

            this.getCommandRegistry().registerCommand(new VaryonCommand(this, factionManager, depositBlockManager));
            this.getCommandRegistry().registerCommand(new ExtractCommand("extract"));
            this.getCommandRegistry().registerCommand(new ExtractCommand("ex"));
            this.returnCommand = new ReturnCommand();
            this.getCommandRegistry().registerCommand(this.returnCommand);
            this.getCommandRegistry().registerCommand(new PointsCommand(essenceManager, factionManager));
            this.getCommandRegistry().registerCommand(new RtpzCommand());
            this.getCommandRegistry().registerCommand(new RtpvCommand());
            this.getCommandRegistry().registerCommand(new RtphCommand());
            this.getCommandRegistry().registerCommand(new RtpsCommand());
            this.getCommandRegistry().registerCommand(new JoinCommand());
            RtpvJoinManager.setInstance(new RtpvJoinManager());
            RtpvConfirmManager.setInstance(new RtpvConfirmManager());
            LOGGER.at(Level.INFO).log("Commands registered");

            LOGGER.at(Level.INFO).log("Varyon initialized with %s zones",
                    configManager.getZoneConfig().getZones().size());
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("Failed to initialize Varyon", e);
            throw e;
        }
    }

    protected void onDisable() {
        if (essenceManager != null) {
            essenceManager.shutdown();
        }
        if (hudManager != null) {
            hudManager.shutdown();
        }
        if (safeZoneManager != null) {
            safeZoneManager.shutdown();
        }
        if (extractionPortalManager != null) {
            extractionPortalManager.shutdown();
        }
        if (deathPointManager != null) {
            deathPointManager.shutdown();
        }
        ChatAnnouncementScheduler.shutdown();
    }

    private void setupMinimapProvider() {
        // Register the world map provider codec
        IWorldMapProvider.CODEC.register(ZoneWorldMapProvider.ID, ZoneWorldMapProvider.class, ZoneWorldMapProvider.CODEC);

        // Listen for world creation events to apply the provider
        this.getEventRegistry().registerGlobal(AddWorldEvent.class, event -> {
            applyMinimapToWorld(event.getWorld());
        });

        // Also apply to any worlds that are already loaded
        for (World world : Universe.get().getWorlds().values()) {
            applyMinimapToWorld(world);
        }

        LOGGER.at(Level.INFO).log("Minimap zone overlay enabled");
    }

    private void applyMinimapToWorld(World world) {
        // Skip temporary/instance worlds
        if (world.getWorldConfig().isDeleteOnRemove()) {
            return;
        }

        // Check if this world is in our enabled worlds list
        if (!configManager.getZoneConfig().isWorldEnabled(world.getName())) {
            LOGGER.at(Level.INFO).log("Minimap not enabled for world: %s", world.getName());
            return;
        }

        // Set our world map provider
        world.getWorldConfig().setWorldMapProvider(new ZoneWorldMapProvider());
        
        // Register extraction portal marker provider
        world.getWorldMapManager().addMarkerProvider("extraction_portal", new com.varyon.extraction.ExtractionPortalMarkerProvider());
        
        LOGGER.at(Level.INFO).log("Set Varyon minimap for world: %s", world.getName());
    }

    public void onConfigurationReloaded() {
        essenceRewardsConfig.attach(configManager.getMobFragmentsConfig(), configManager.getEssenceEconomyConfig());
        if (extractionPortalManager != null) {
            extractionPortalManager.setConfig(configManager.getExtractionConfig());
        }
        if (globalRewardsManager != null) {
            globalRewardsManager.applyReloadedConfigs(
                configManager.getFactionRewardsConfig(),
                configManager.getZonePermissionsConfig());
        }
        if (hudManager != null) {
            hudManager.applyReloadedConfigs(configManager);
        }
        if (safeZoneManager != null) {
            safeZoneManager.applyReloadedConfigs(configManager.getSafeZoneConfig(), configManager.getZoneConfig());
        }
        if (safeZoneNotificationSystem != null) {
            safeZoneNotificationSystem.applyReloadedConfigs(
                configManager.getSafeZoneConfig(),
                configManager.getZoneConfig(),
                configManager.getMessagesConfig());
        }
        if (configManager.getZoneConfig().isMinimapEnabled()) {
            for (World world : Universe.get().getWorlds().values()) {
                applyMinimapToWorld(world);
            }
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    @Nullable
    public static ConfigManager getStaticConfigManager() {
        return staticConfigManager;
    }

    @Nullable
    public static EssenceManager getStaticEssenceManager() {
        return staticEssenceManager;
    }

    @Nullable
    public static FactionManager getStaticFactionManager() {
        return staticFactionManager;
    }

    @Nullable
    public static SafeZoneManager getStaticSafeZoneManager() {
        return staticSafeZoneManager;
    }

    @Nullable
    public static GlobalRewardsManager getStaticGlobalRewardsManager() {
        return staticGlobalRewardsManager;
    }

    public ZoneHUDManager getHudManager() {
        return hudManager;
    }
    
    public DeathPointManager getDeathPointManager() {
        return deathPointManager;
    }

    @Nullable
    public ReturnCommand getReturnCommand() {
        return returnCommand;
    }

    @Nullable
    public static VaryonPlugin getInstance() {
        return staticInstance;
    }
}


