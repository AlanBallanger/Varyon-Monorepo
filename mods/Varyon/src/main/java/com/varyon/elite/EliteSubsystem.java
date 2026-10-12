package com.varyon.elite;

import com.frotty27.nameplatebuilder.api.NameplateData;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.config.ConfigManager;
import com.varyon.elite.ability.EliteAbilityTickSystem;
import com.varyon.elite.combat.EliteDamageDealtSystem;
import com.varyon.elite.combat.EliteDamageTakenSystem;
import com.varyon.elite.component.EliteAbilityStateComponent;
import com.varyon.elite.component.EliteComponent;
import com.varyon.elite.component.ElitePendingAbilityComponent;
import com.varyon.elite.component.ElitePendingLootComponent;
import com.varyon.elite.loot.EliteDeferredLootSystem;
import com.varyon.elite.loot.EliteLootSystem;
import com.varyon.safezone.SafeZoneManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.logging.Level;

/**
 * Single entry point that registers the elite subsystem's components, systems and command.
 * Call once from {@code VaryonPlugin} init after {@code MobScalingComponent} is registered
 * and {@code SafeZoneManager} exists.
 */
public final class EliteSubsystem {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final EliteConfig config;

    private EliteSubsystem(@Nonnull EliteConfig config) {
        this.config = config;
    }

    @Nonnull
    public EliteConfig getConfig() {
        return config;
    }

    public static EliteSubsystem register(@Nonnull JavaPlugin plugin,
                                          @Nonnull ConfigManager configManager,
                                          @Nullable SafeZoneManager safeZoneManager,
                                          @Nonnull EliteConfig config) {
        EliteComponent.setComponentType(
                plugin.getEntityStoreRegistry().registerComponent(EliteComponent.class,
                        () -> new EliteComponent(MobClass.BAGARREUR, 1, 0L, java.util.List.of(), null)));
        EliteAbilityStateComponent.setComponentType(
                plugin.getEntityStoreRegistry().registerComponent(EliteAbilityStateComponent.class,
                        EliteAbilityStateComponent::new));
        ElitePendingAbilityComponent.setComponentType(
                plugin.getEntityStoreRegistry().registerComponent(ElitePendingAbilityComponent.class,
                        ElitePendingAbilityComponent::new));
        ElitePendingLootComponent.setComponentType(
                plugin.getEntityStoreRegistry().registerComponent(ElitePendingLootComponent.class,
                        ElitePendingLootComponent::new));

        // Always registered: it stays inert unless a de-elite purge is requested, so it can
        // still tidy up existing elites after the subsystem is turned off via config reload.
        plugin.getEntityStoreRegistry().registerSystem(new EliteDeeliteSystem());

        // Spawn/combat/loot systems short-circuit on config.isEnabled(), so they are safe to
        // register unconditionally and pick up a later reload that turns the subsystem on.
        plugin.getEntityStoreRegistry().registerSystem(
                new EliteSpawnRefSystem(configManager, config, safeZoneManager));
        if (safeZoneManager == null) {
            LOGGER.at(Level.INFO).log("Elite subsystem: safe zones disabled, elites may spawn anywhere in a zone");
        }
        plugin.getEntityStoreRegistry().registerSystem(new EliteAbilityTickSystem(config));
        plugin.getEntityStoreRegistry().registerSystem(new EliteDamageDealtSystem());
        plugin.getEntityStoreRegistry().registerSystem(new EliteDamageTakenSystem());
        plugin.getEntityStoreRegistry().registerSystem(new EliteLootSystem(config));
        plugin.getEntityStoreRegistry().registerSystem(new EliteDeferredLootSystem());

        // Elite nameplate always runs (it drives the vanilla Nameplate); it also updates the
        // NameplateBuilder segment when that mod is available.
        ComponentType<EntityStore, NameplateData> nameplateType = null;
        try {
            nameplateType = com.frotty27.nameplatebuilder.api.NameplateAPI.getComponentType();
        } catch (Throwable ignored) {
        }
        plugin.getEntityStoreRegistry().registerSystem(new EliteNameplateSystem(nameplateType));
        LOGGER.at(Level.INFO).log("Elite nameplate system registered"
                + (nameplateType != null ? " (with NameplateBuilder)" : " (vanilla only)"));

        LOGGER.at(Level.INFO).log("Elite subsystem registered");
        return new EliteSubsystem(config);
    }
}
