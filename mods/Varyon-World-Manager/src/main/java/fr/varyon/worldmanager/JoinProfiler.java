package fr.varyon.worldmanager;

import com.hypixel.hytale.logger.HytaleLogger;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

/**
 * Lightweight timing instrumentation for every inventory scan path (join, disconnect,
 * world-change save, auto-save, live inventory-change cache refresh). Gated by groups.yml's
 * debug flag for the log line itself is NOT applied here - this always logs, since the whole
 * point is to catch an unexpected spike (like the BasicOutputBuffer.ensure/org.bson peaks seen
 * in spark's Mod Performance Profiler) without having to flip a config and reproduce again.
 *
 * Kept permanently (not a one-off test) so a future spike can be attributed to a specific call
 * site (join vs autosave vs disconnect) and to specific items (large BSON metadata) directly
 * from server logs, without needing a fresh spark capture each time.
 */
public final class JoinProfiler {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /** Only log when a scan crosses this threshold, to avoid spamming logs on cheap scans. */
    private static final double LOG_THRESHOLD_MS = 1.0;

    private JoinProfiler() {}

    public static final class Metadata {
        public final AtomicInteger itemsScanned = new AtomicInteger();
        public final AtomicInteger itemsWithMetadata = new AtomicInteger();
        public final AtomicLong metadataCloneNanos = new AtomicLong();
        public final AtomicLong metadataToJsonNanos = new AtomicLong();
        public final AtomicLong maxSingleItemNanos = new AtomicLong();
        public final AtomicInteger maxSingleItemJsonLength = new AtomicInteger();
        public volatile String maxSingleItemId = "-";

        void recordItem(String itemId, long cloneNanos, long toJsonNanos, int jsonLength) {
            itemsScanned.incrementAndGet();
            if (jsonLength > 0) {
                itemsWithMetadata.incrementAndGet();
            }
            metadataCloneNanos.addAndGet(cloneNanos);
            metadataToJsonNanos.addAndGet(toJsonNanos);
            long total = cloneNanos + toJsonNanos;
            long prevMax = maxSingleItemNanos.get();
            if (total > prevMax) {
                maxSingleItemNanos.compareAndSet(prevMax, total);
                maxSingleItemJsonLength.set(jsonLength);
                maxSingleItemId = itemId;
            }
        }
    }

    /** Wraps one fromHolder() scan: creates a fresh {@link Metadata}, times the scan, logs a summary. */
    public static PlayerInventoryData timeFromHolder(UUID playerId, String label, java.util.function.Function<Metadata, PlayerInventoryData> scan) {
        Metadata meta = new Metadata();
        long start = System.nanoTime();
        PlayerInventoryData result = scan.apply(meta);
        long totalNanos = System.nanoTime() - start;
        logSummary(playerId, label, totalNanos, meta);
        return result;
    }

    private static void logSummary(UUID playerId, String label, long totalNanos, Metadata meta) {
        double totalMs = totalNanos / 1_000_000.0;
        if (totalMs < LOG_THRESHOLD_MS) {
            return;
        }
        double cloneMs = meta.metadataCloneNanos.get() / 1_000_000.0;
        double jsonMs = meta.metadataToJsonNanos.get() / 1_000_000.0;
        LOGGER.at(Level.INFO).log(
                "[JoinProfiler] %s player=%s totalMs=%.2f items=%d withMeta=%d cloneMs=%.2f toJsonMs=%.2f worstItem=%s(%.2fms, %dB json)",
                label, playerId, totalMs, meta.itemsScanned.get(), meta.itemsWithMetadata.get(),
                cloneMs, jsonMs, meta.maxSingleItemId,
                meta.maxSingleItemNanos.get() / 1_000_000.0, meta.maxSingleItemJsonLength.get());
    }
}
