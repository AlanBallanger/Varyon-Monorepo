package com.varyon.easyhunger;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.Config;
import com.varyon.easyhunger.commands.SetHungerCommand;
import com.varyon.easyhunger.components.HungerComponent;
import com.varyon.easyhunger.config.EasyHungerConfig;
import com.varyon.easyhunger.config.FoodsConfig;
import com.varyon.easyhunger.config.DrinksConfig;
import com.varyon.easyhunger.config.BiomeModifiersConfig;
import com.varyon.easyhunger.events.GameModeChangeListener;
import com.varyon.easyhunger.events.EasyHungerPlayerReady;
import com.varyon.easyhunger.systems.OnDeathSystem;
import com.varyon.easyhunger.systems.StarveSystem;
import com.varyon.easyhunger.systems.WellFedSystem;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.logging.Level;
import java.util.Set;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.varyon.easyhunger.commands.EasyHungerCommand;
import com.varyon.easyhunger.commands.EasyHungerHideCommand;
import com.varyon.easyhunger.commands.EasyHungerShowCommand;

public class EasyHunger extends JavaPlugin {
    private static EasyHunger instance;
    private final Config<EasyHungerConfig> config;
    private final Config<FoodsConfig> foodsConfig;
    private final Config<DrinksConfig> drinksConfig;
    private final Config<BiomeModifiersConfig> biomeConfig;
    private ComponentType<EntityStore, HungerComponent> hungerComponentType;
    private ComponentType<EntityStore, com.varyon.easyhunger.components.ThirstComponent> thirstComponentType;

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public EasyHunger(@NonNullDecl JavaPluginInit init) {
        super(init);
        instance = this;
        this.config = this.withConfig("HungerConfig", EasyHungerConfig.CODEC);
        this.foodsConfig = this.withConfig("Foods", FoodsConfig.CODEC);
        this.drinksConfig = this.withConfig("Drinks", DrinksConfig.CODEC);
        this.biomeConfig = this.withConfig("BiomeModifiers", BiomeModifiersConfig.CODEC);
    }

    @Override
    protected void setup () {
        super.setup();

        this.config.save();
        
        // Merge new default values without overwriting user customizations
        boolean foodsChanged = this.foodsConfig.get().mergeDefaults();
        boolean drinksChanged = this.drinksConfig.get().mergeDefaults();
        
        this.foodsConfig.save();
        this.drinksConfig.save();
        this.biomeConfig.save();


        // register hunger component
        this.hungerComponentType = this.getEntityStoreRegistry()
                .registerComponent(HungerComponent.class, "HungerComponent", HungerComponent.CODEC);
        
        // register thirst component
        this.thirstComponentType = this.getEntityStoreRegistry()
                .registerComponent(com.varyon.easyhunger.components.ThirstComponent.class, "ThirstComponent", com.varyon.easyhunger.components.ThirstComponent.CODEC);

        // register starve system
        final var entityStoreRegistry = this.getEntityStoreRegistry();
        entityStoreRegistry.registerSystem(StarveSystem.create());
        entityStoreRegistry.registerSystem(new OnDeathSystem());
        entityStoreRegistry.registerSystem(new com.varyon.easyhunger.systems.EasyHungerBlockBreakSystem());
        entityStoreRegistry.registerSystem(new com.varyon.easyhunger.systems.EasyHungerJumpSystem());
        entityStoreRegistry.registerSystem(new com.varyon.easyhunger.systems.EasyHungerBlockPlaceSystem());

        // ENABLED: Food Handler (Used ONLY for Infinite Food Mods like Eternal Meat)
        entityStoreRegistry.registerSystem(new com.varyon.easyhunger.systems.EasyHungerFoodHandler(this.hungerComponentType));
        
        // Thirst Handler
        entityStoreRegistry.registerSystem(new com.varyon.easyhunger.systems.EasyThirstHandler(this.thirstComponentType));
        
        // register thirst system
        entityStoreRegistry.registerSystem(com.varyon.easyhunger.systems.EasyThirstSystem.create());
        
        // register well fed regeneration system
        if (this.config.get().isWellFedEnabled()) {
            entityStoreRegistry.registerSystem(WellFedSystem.create());
            logInfo("WellFed regeneration system enabled (threshold: " + this.config.get().getWellFedThreshold() + "%)");
        }

        // Interactions
        final var interactionRegistry = this.getCodecRegistry(Interaction.CODEC);
        interactionRegistry.register("EasyHunger_DrinkWater", com.varyon.easyhunger.interactions.DrinkWaterInteraction.class, com.varyon.easyhunger.interactions.DrinkWaterInteraction.CODEC);
        interactionRegistry.register("EasyHunger_ConsumeFood", com.varyon.easyhunger.interactions.ConsumeFoodInteraction.class, com.varyon.easyhunger.interactions.ConsumeFoodInteraction.CODEC);
        interactionRegistry.register("EasyHunger_RefillWaterskin", com.varyon.easyhunger.interactions.RefillWaterskinInteraction.class, com.varyon.easyhunger.interactions.RefillWaterskinInteraction.CODEC);
        interactionRegistry.register("EasyHunger_StartFeeding", com.varyon.easyhunger.interactions.StartFeedingInteraction.class, com.varyon.easyhunger.interactions.StartFeedingInteraction.CODEC);
        interactionRegistry.register("EasyHunger_FailedFeeding", com.varyon.easyhunger.interactions.FailedFeedingInteraction.class, com.varyon.easyhunger.interactions.FailedFeedingInteraction.CODEC);
        interactionRegistry.register("EasyHunger_StartDrinking", com.varyon.easyhunger.interactions.StartDrinkingInteraction.class, com.varyon.easyhunger.interactions.StartDrinkingInteraction.CODEC);
        interactionRegistry.register("EasyHunger_FailedDrinking", com.varyon.easyhunger.interactions.FailedDrinkingInteraction.class, com.varyon.easyhunger.interactions.FailedDrinkingInteraction.CODEC);

        // setup hunger component and hud on player join
        this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, EasyHungerPlayerReady::handle);

        // listen to gamemode changes
        PacketAdapters.registerOutbound(new GameModeChangeListener());

        // register admin commands
        this.getCommandRegistry().registerCommand(new SetHungerCommand());
        this.getCommandRegistry().registerCommand(new com.varyon.easyhunger.commands.SetThirstCommand());
        
        // Register the main command (which handles subcommands like hide, show, config)
        this.getCommandRegistry().registerCommand(new com.varyon.easyhunger.commands.EasyHungerCommand());
        
        // Register the global config command (/ehconfig) for compatibility
        this.getCommandRegistry().registerCommand(new com.varyon.easyhunger.commands.EasyHungerValuesCommand());

        // Register version info command (/ehversion)
        this.getCommandRegistry().registerCommand(new com.varyon.easyhunger.commands.EhVersionCommand());


        // Try to prune recipes immediately, but also plan for a delayed pruning if needed
        this.pruneRecipes();
    }

    @Override
    protected void start() {
        super.start();
        
        // Grant permissions to adventure and creative players for basic commands
        final Set<String> playerPermissions = Set.of(
            EasyHungerCommand.requiredPermission,
            EasyHungerHideCommand.requiredPermission,
            EasyHungerShowCommand.requiredPermission
        );
        
        try {
            PermissionsModule.get().addGroupPermission("Adventure", playerPermissions);
            PermissionsModule.get().addGroupPermission("Creative", playerPermissions);
            logInfo("Added EasyHunger permissions to Adventure and Creative groups.");
        } catch (Exception e) {
            logInfo("Failed to add permissions: " + e.getMessage());
        }
    }

    public void pruneRecipes() {
        if (this.getConfig().isThirstEnabled()) {
            return;
        }

        try {
            java.lang.reflect.Field registriesField = com.hypixel.hytale.builtin.crafting.CraftingPlugin.class.getDeclaredField("registries");
            registriesField.setAccessible(true);
            java.util.Map<String, com.hypixel.hytale.builtin.crafting.BenchRecipeRegistry> registries = 
                (java.util.Map<String, com.hypixel.hytale.builtin.crafting.BenchRecipeRegistry>) registriesField.get(null);
            
            if (registries != null) {
                for (com.hypixel.hytale.builtin.crafting.BenchRecipeRegistry registry : registries.values()) {
                    boolean changed = false;
                    for (com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe recipe : registry.getAllRecipes()) {
                        String outputId = recipe.getPrimaryOutput() != null ? recipe.getPrimaryOutput().getItemId() : null;
                        if ("EasyHunger_Odre_Empty".equals(outputId) || "EasyHunger_WaterBowl_Empty".equals(outputId)) {
                            registry.removeRecipe(recipe.getId());
                            changed = true;
                        }
                    }
                    if (changed) {
                        registry.recompute();
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.at(java.util.logging.Level.SEVERE).log("Failed to robustly prune Waterskin recipes", e);
        }
    }

    public void saveConfig() {
        this.config.save();
    }

    public void saveFoodsConfig() {
        this.foodsConfig.save();
    }

    public void saveDrinksConfig() {
        this.drinksConfig.save();
    }

    public void saveAllConfigs() {
        this.config.save();
        this.foodsConfig.save();
        this.drinksConfig.save();
        this.biomeConfig.save();
    }

    public ComponentType<EntityStore, HungerComponent> getHungerComponentType() {
        return this.hungerComponentType;
    }

    public ComponentType<EntityStore, com.varyon.easyhunger.components.ThirstComponent> getThirstComponentType() {
        return this.thirstComponentType;
    }

    public EasyHungerConfig getConfig() {
        return this.config.get();
    }

    public BiomeModifiersConfig getBiomeConfig() {
        return this.biomeConfig.get();
    }

    public FoodsConfig getFoodsConfig() {
        return this.foodsConfig.get();
    }

    public DrinksConfig getDrinksConfig() {
        return this.drinksConfig.get();
    }

    public static EasyHunger get() {
        return instance;
    }

    public static void logInfo(String message) {
        LOGGER.at(Level.WARNING).log(message);
    }
}



