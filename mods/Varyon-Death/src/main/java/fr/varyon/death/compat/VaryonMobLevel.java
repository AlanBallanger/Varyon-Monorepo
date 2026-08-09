package fr.varyon.death.compat;

import javax.annotation.Nullable;

import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.common.semver.SemverRange;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Lecture optionnelle du niveau de mob fourni par le mod Varyon.
 *
 * <p>Certains mondes ne chargent pas Varyon : dans ce cas aucun niveau n'existe et le
 * recapitulatif affiche simplement le nom de la creature. La classe pont
 * {@link VaryonMobLevelBridge} n'est chargee que si le plugin est effectivement present,
 * afin qu'une absence de Varyon ne provoque jamais de {@code NoClassDefFoundError}.
 */
public final class VaryonMobLevel {

    public static final int NO_LEVEL = 0;
    private static final PluginIdentifier VARYON_PLUGIN_ID = new PluginIdentifier("Varyon", "Varyon");

    private static volatile Boolean varyonLoaded;
    private static volatile Boolean bridgeAvailable;

    private VaryonMobLevel() {}

    public static boolean isVaryonLoaded() {
        Boolean cached = varyonLoaded;
        if (cached != null) {
            return cached;
        }
        boolean loaded;
        try {
            PluginManager pluginManager = PluginManager.get();
            loaded = pluginManager != null && pluginManager.hasPlugin(VARYON_PLUGIN_ID, SemverRange.WILDCARD);
        } catch (Throwable ignored) {
            loaded = false;
        }
        varyonLoaded = loaded;
        return loaded;
    }

    /**
     * @return le niveau du mob, ou {@link #NO_LEVEL} si Varyon est absent ou si l'entite n'est
     *         pas une creature mise a l'echelle.
     */
    public static int read(@Nullable Store<EntityStore> store,
                           @Nullable CommandBuffer<EntityStore> commandBuffer,
                           @Nullable Ref<EntityStore> entityRef) {
        if (entityRef == null || !entityRef.isValid() || !isVaryonLoaded() || !ensureBridge()) {
            return NO_LEVEL;
        }
        try {
            return VaryonMobLevelBridge.read(store, commandBuffer, entityRef);
        } catch (Throwable ignored) {
            return NO_LEVEL;
        }
    }

    private static boolean ensureBridge() {
        Boolean cached = bridgeAvailable;
        if (cached != null) {
            return cached;
        }
        boolean available;
        try {
            Class.forName("com.varyon.component.MobScalingComponent", false,
                    VaryonMobLevel.class.getClassLoader());
            available = true;
        } catch (Throwable ignored) {
            available = false;
        }
        bridgeAvailable = available;
        return available;
    }
}
