package com.varyon.craftrestrict;

import com.varyon.craftrestrict.commands.CraftRestrictCommand;
import com.varyon.craftrestrict.config.CraftRestrictConfig;
import com.varyon.craftrestrict.events.EventContainerBlockOpen;
import com.varyon.craftrestrict.events.EventInventoryActiveSlotRequest;
import com.varyon.craftrestrict.events.EventPlayerInteractBlock;
import com.varyon.craftrestrict.events.EventPlayerJoinWorld;
import com.varyon.craftrestrict.inventory.PossessionInventoryListenerRegistry;
import com.varyon.craftrestrict.packets.PacketCraftInterceptor;
import com.varyon.craftrestrict.packets.PacketOpenWindowInterceptor;
import com.varyon.craftrestrict.packets.PacketUpdateWindowInterceptor;
import com.varyon.craftrestrict.recipes.RecipesManager;
import com.hypixel.hytale.component.system.ISystem;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.event.events.BootEvent;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import javax.annotation.Nonnull;

public class Main extends JavaPlugin {

    private static Config<CraftRestrictConfig> CONFIG;
    private static Main PLUGIN_INSTANCE;
    private static PacketCraftInterceptor packetCraftInterceptor;
    private static PacketOpenWindowInterceptor packetOpenWindowInterceptor;
    private static PacketUpdateWindowInterceptor packetUpdateWindowInterceptor;

    public Main(@Nonnull JavaPluginInit init) {
        super(init);
        this.getLogger().at(Level.INFO).log("=====================================");
        this.getLogger().at(Level.INFO).log("  CraftRestrict -> Initializing...");
        this.getLogger().at(Level.INFO).log("=====================================");
        CONFIG = this.withConfig("CraftRestrict", CraftRestrictConfig.CODEC);
        PLUGIN_INSTANCE = this;
        this.getLogger().at(Level.INFO).log("[Initialization] Plugin instance created");
    }

    @Override
    protected void setup() {
        super.setup();
        this.getLogger().at(Level.INFO).log("[Setup] -> Starting plugin setup...");
        this.loadingConfig();
        this.registeringCommands();
        this.enablingPackets();
        this.loadingEvents();
        this.loadingPlayerJoinListener();
        this.getEventRegistry().registerGlobal(BootEvent.class, event -> this.loadingManagers());
    }

    @Override
    protected void shutdown() {
        Main.unregisterPackets();
        super.shutdown();
    }

    private void loadingConfig() {
        this.getLogger().at(Level.INFO).log("[Setup] -> Loading configuration...");
        try {
            CraftRestrictConfig config = CONFIG.get();
            CONFIG.save();
            this.getLogger().at(Level.INFO).log("[Setup] -> Configuration loaded successfully:");
            this.getLogger().at(Level.INFO).log("  - RestrictionMode: " + config.getRestrictionMode());
            this.getLogger().at(Level.INFO).log("  - IsDebugActive: " + config.isDebug());
        } catch (Exception e) {
            this.getLogger().at(Level.SEVERE).log("[Setup] -> ERROR: Could not load configuration: " + e.getMessage());
        }
    }

    private void registeringCommands() {
        this.getLogger().at(Level.INFO).log("[Setup] -> Registering commands...");
        try {
            this.getCommandRegistry().registerCommand(new CraftRestrictCommand());
            this.getLogger().at(Level.INFO).log("[Setup] -> 'craftrestrict' command registered successfully.");
        } catch (Exception e) {
            this.getLogger().at(Level.SEVERE).log("[Setup] -> ERROR: Could not register 'craftrestrict' command: " + e.getMessage());
        }
    }

    private void enablingPackets() {
        this.getLogger().at(Level.INFO).log("[Setup] -> Enabling packets systems...");
        this.getLogger().at(Level.INFO).log("[Setup] -> Enabling PacketCraftInterceptor...");
        try {
            packetCraftInterceptor = new PacketCraftInterceptor();
            packetCraftInterceptor.register();
            this.getLogger().at(Level.INFO).log("[Setup] -> PacketCraftInterceptor enabled.");
        } catch (Exception e) {
            this.getLogger().at(Level.SEVERE).log("[Setup] -> PacketCraftInterceptor cannot be enabled.");
        }
        this.getLogger().at(Level.INFO).log("[Setup] -> Enabling PacketOpenWindowInterceptor...");
        try {
            packetOpenWindowInterceptor = new PacketOpenWindowInterceptor();
            packetOpenWindowInterceptor.register();
            this.getLogger().at(Level.INFO).log("[Setup] -> PacketOpenWindowInterceptor enabled.");
        } catch (Exception e) {
            this.getLogger().at(Level.SEVERE).log("[Setup] -> PacketOpenWindowInterceptor cannot be enabled.");
        }
        this.getLogger().at(Level.INFO).log("[Setup] -> Enabling PacketUpdateWindowInterceptor...");
        try {
            packetUpdateWindowInterceptor = new PacketUpdateWindowInterceptor();
            packetUpdateWindowInterceptor.register();
            this.getLogger().at(Level.INFO).log("[Setup] -> PacketUpdateWindowInterceptor enabled.");
        } catch (Exception e) {
            this.getLogger().at(Level.SEVERE).log("[Setup] -> PacketUpdateWindowInterceptor cannot be enabled.");
        }
    }

    private void loadingEvents() {
        this.getLogger().at(Level.INFO).log("[Setup] -> Loading events...");
        this.getLogger().at(Level.INFO).log("[Setup] -> Loading EventPlayerInteractBlock...");
        try {
            this.getEntityStoreRegistry().registerSystem((ISystem) new EventPlayerInteractBlock());
            this.getLogger().at(Level.INFO).log("[Setup] -> EventPlayerInteractBlock loaded.");
        } catch (Exception e) {
            this.getLogger().at(Level.SEVERE).log("[Setup] -> EventPlayerInteractBlock cannot be loaded.");
        }
        this.getLogger().at(Level.INFO).log("[Setup] -> Loading EventInventoryActiveSlotRequest...");
        try {
            this.getEntityStoreRegistry().registerSystem((ISystem) new EventInventoryActiveSlotRequest());
            this.getLogger().at(Level.INFO).log("[Setup] -> EventInventoryActiveSlotRequest loaded.");
        } catch (Exception e) {
            this.getLogger().at(Level.SEVERE).log("[Setup] -> EventInventoryActiveSlotRequest cannot be loaded.");
        }
        this.getLogger().at(Level.INFO).log("[Setup] -> Loading EventContainerBlockOpen...");
        try {
            this.getEntityStoreRegistry().registerSystem((ISystem) new EventContainerBlockOpen());
            this.getLogger().at(Level.INFO).log("[Setup] -> EventContainerBlockOpen loaded.");
        } catch (Exception e) {
            this.getLogger().at(Level.SEVERE).log("[Setup] -> EventContainerBlockOpen cannot be loaded.");
        }
    }

    private void loadingPlayerJoinListener() {
        this.getLogger().at(Level.INFO).log("[Setup] -> Loading player join listener...");
        try {
            this.getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, EventPlayerJoinWorld::handle);
            this.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, EventPlayerJoinWorld::handleDisconnect);
            this.getLogger().at(Level.INFO).log("[Setup] -> Player join listener registered.");
        } catch (Exception e) {
            this.getLogger().at(Level.SEVERE).log("[Setup] -> ERROR: Could not register player join listener: " + e.getMessage());
        }
    }

    private void loadingManagers() {
        this.getLogger().at(Level.INFO).log("[Setup] -> Loading managers...");
        try {
            RecipesManager.init();
            this.getLogger().at(Level.INFO).log("[Setup] -> Recipes manager loaded successfully.");
            this.getLogger().at(Level.INFO).log("[Setup] -> Starting initial pre-loading...");
            RecipesManager.preWarmCache();
        } catch (Exception e) {
            this.getLogger().at(Level.SEVERE).log("[Setup] -> ERROR: Could not load recipes manager: " + e.getMessage());
        }
    }

    private static void unregisterPackets() {
        if (packetCraftInterceptor != null) {
            packetCraftInterceptor.unregister();
        }
        if (packetOpenWindowInterceptor != null) {
            packetOpenWindowInterceptor.unregister();
        }
        if (packetUpdateWindowInterceptor != null) {
            packetUpdateWindowInterceptor.register();
        }
    }

    public boolean reloadConfig() {
        try {
            CONFIG.load().thenAccept(newConfig -> {
                PacketCraftInterceptor.reloadConfig(CONFIG.get());
                PacketOpenWindowInterceptor.reloadConfig(CONFIG.get());
                PossessionInventoryListenerRegistry.enforceForAllOnlinePlayers();
                this.getLogger().at(Level.INFO).log("[Config] Reloaded successfully.");
            }).join();
            return true;
        } catch (Exception e) {
            this.getLogger().at(Level.SEVERE).log("[Config] Reload failed: " + e.getMessage());
            return false;
        }
    }

    public static CraftRestrictConfig getConfig() {
        return CONFIG.get();
    }

    public void saveConfig() {
        try {
            CONFIG.save();
        } catch (Exception e) {
            this.getLogger().at(Level.SEVERE).log("[Config] Save failed: " + e.getMessage());
        }
    }

    public static Main getPluginInstance() {
        return PLUGIN_INSTANCE;
    }
}
