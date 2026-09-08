package fr.varyon.musiczones.gui;

import com.hypixel.hytale.logger.HytaleLogger;
import fr.varyon.musiczones.VaryonMusicZonesPlugin;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ExecutorService;

// Les pages InteractiveCustomUIPage tournent sur le thread-tick du monde. Les (re)builds du
// pack audio (loadCommonAssets / loadAssetsFromDirectory) prennent des locks d'AssetStore
// partagés avec le file-watcher : les lancer sur ce thread fige le serveur. Ce helper les
// rejette sur un thread dédié UNIQUE : deux modifications rapprochées (ou un changement +
// le rebuild du boot) ne peuvent plus écrire les mêmes fichiers JSON en parallèle pendant
// qu'un loadAssetsFromDirectory les lit -> plus de JSON tronqué
// ("Unexpected character ... expected ','").
final class MusicZoneEdits {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final ExecutorService REBUILD_EXECUTOR = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "MusicZones-rebuild");
            t.setDaemon(true);
            return t;
        }
    });

    private MusicZoneEdits() {}

    interface AudioRebuild {
        void run() throws Exception;
    }

    static void rebuildAsync(VaryonMusicZonesPlugin plugin, AudioRebuild rebuild) {
        REBUILD_EXECUTOR.execute(() -> {
            try {
                rebuild.run();
            } catch (Exception e) {
                LOGGER.atWarning().withCause(e).log("[MusicZones] rebuild asynchrone échoué");
            }
        });
    }

    static void rebuildPackAsync(VaryonMusicZonesPlugin plugin) {
        rebuildAsync(plugin, () -> {
            plugin.rebuildAssetPack();
            // Le pack a pu (re)créer un .ogg : imposer une fenêtre de grâce avant de repousser un
            // index de MusicContainer aux clients, le temps qu'ils indexent le Common Asset.
            plugin.getApplySystem().beginPostRebuildGrace();
        });
    }

    static void rebuildContainersAsync(VaryonMusicZonesPlugin plugin) {
        rebuildAsync(plugin, () -> {
            plugin.rebuildContainersOnly();
            plugin.getApplySystem().forgetAll();
        });
    }
}
