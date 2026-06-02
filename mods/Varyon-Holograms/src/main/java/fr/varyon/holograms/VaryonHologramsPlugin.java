package fr.varyon.holograms;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.event.events.player.AddPlayerToWorldEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import fr.varyon.holograms.animation.AnimationManager;
import fr.varyon.holograms.animation.AnimationRegistry;
import fr.varyon.holograms.commands.HologramCommand;
import fr.varyon.holograms.hologram.HologramManager;
import fr.varyon.holograms.placeholder.PlaceholderIntegration;
import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public final class VaryonHologramsPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final long INIT_DELAY_SECONDS = 3L;

    private static VaryonHologramsPlugin instance;

    private final AnimationRegistry animationRegistry = new AnimationRegistry();
    private final AnimationManager animationManager = new AnimationManager();
    private final PlaceholderIntegration placeholderIntegration = new PlaceholderIntegration();
    private final AtomicBoolean playerInitScheduled = new AtomicBoolean(false);

    private HologramManager hologramManager;

    public VaryonHologramsPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    public static VaryonHologramsPlugin getInstance() { return instance; }

    @Override
    protected void setup() {
        getCommandRegistry().registerCommand(new HologramCommand(this));
    }

    @Override
    protected void start() {
        hologramManager = new HologramManager(this);
        hologramManager.loadHolograms();
        getEventRegistry().registerGlobal(AddPlayerToWorldEvent.class, this::onPlayerAddToWorld);
        HytaleServer.SCHEDULED_EXECUTOR.schedule(this::registerImageAssets, 2, TimeUnit.SECONDS);
        LOGGER.at(Level.INFO).log("[Varyon-Holograms] Démarré — %s hologramme(s) chargé(s)",
            hologramManager.getHologramCount());
    }

    private void registerImageAssets() {
        try {
            hologramManager.getImageManager().liveLoadAll();
            LOGGER.at(Level.INFO).log("[Varyon-Holograms] Assets images enregistrés côté serveur");
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("[Varyon-Holograms] Échec enregistrement assets: %s", e.getMessage());
        }
    }

    private void onPlayerAddToWorld(@Nonnull AddPlayerToWorldEvent event) {
        UUID worldId = event.getWorld().getWorldConfig().getUuid();
        if (!playerInitScheduled.compareAndSet(false, true)) {
            HytaleServer.SCHEDULED_EXECUTOR.schedule(
                () -> hologramManager.ensureHologramsVisibleInWorld(worldId),
                INIT_DELAY_SECONDS, TimeUnit.SECONDS);
            return;
        }
        HytaleServer.SCHEDULED_EXECUTOR.schedule(() -> initForFirstPlayer(worldId),
            INIT_DELAY_SECONDS, TimeUnit.SECONDS);
    }

    private void initForFirstPlayer(@Nonnull UUID worldId) {
        try {
            hologramManager.getImageManager().pushAssetsToOnlinePlayers();
            if (!hologramManager.areHologramsSpawned()) {
                hologramManager.spawnAllHolograms();
            } else {
                hologramManager.ensureHologramsVisibleInWorld(worldId);
            }
            LOGGER.at(Level.INFO).log("[Varyon-Holograms] Init joueur terminée");
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("[Varyon-Holograms] Init joueur échouée: %s", e.getMessage());
        }
    }

    @Override
    protected void shutdown() {
        if (hologramManager != null) {
            hologramManager.saveHolograms();
            hologramManager.shutdown();
        }
        instance = null;
        LOGGER.at(Level.INFO).log("[Varyon-Holograms] Arrêté.");
    }

    @Nonnull public HologramManager getHologramManager() { return hologramManager; }
    @Nonnull public AnimationRegistry getAnimationRegistry() { return animationRegistry; }
    @Nonnull public AnimationManager getAnimationManager() { return animationManager; }
    @Nonnull public PlaceholderIntegration getPlaceholderIntegration() { return placeholderIntegration; }

    @Nonnull
    public Path getImagesDir() {
        return getDataDirectory().resolve("images");
    }
}
