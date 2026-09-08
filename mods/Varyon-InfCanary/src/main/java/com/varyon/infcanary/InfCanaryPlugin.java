package com.varyon.infcanary;

import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.io.handlers.game.GamePacketHandler;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

/**
 * Registers a single outbound {@link PacketFilter}. For every packet about to be written to a
 * client it deep-scans the packet's object graph for a NaN / infinite float or double. On a hit it
 *
 * <ul>
 *   <li>logs the packet class, the exact field path, the offending value, the player, and a full
 *       stack trace of the thread that produced the packet;</li>
 *   <li>appends the same report to {@code <plugin data dir>/inf-canary.log};</li>
 *   <li>returns {@code true} so {@link com.hypixel.hytale.server.core.io.PacketHandler} drops the
 *       packet instead of handing it to the Netty encoder (which would throw
 *       {@code ProtocolException: ... is not finite} and kill the pipeline).</li>
 * </ul>
 *
 * The scan only runs on the writing thread, is depth- and node-bounded, and per-packet-class results
 * are not cached because the same packet type can be fine on one tick and broken on the next.
 */
public final class InfCanaryPlugin extends JavaPlugin {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /** Throttle: at most one full report per (packetClass) per this many ms, to avoid log spam on a repeating bug. */
    private static final long PER_TYPE_THROTTLE_MS = 3_000L;

    private final ConcurrentHashMap<String, AtomicLong> lastReportByType = new ConcurrentHashMap<>();
    private final AtomicLong totalHits = new AtomicLong();
    private final AtomicLong totalDropped = new AtomicLong();

    private volatile PacketFilter filter;
    private volatile Path logFile;

    public InfCanaryPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        try {
            Path dir = getDataDirectory();
            Files.createDirectories(dir);
            logFile = dir.resolve("inf-canary.log");
        } catch (IOException e) {
            getLogger().at(Level.WARNING).withCause(e).log("[InfCanary] Could not prepare log file; console only.");
        }

        filter = (handler, packet) -> handleOutbound(handler, packet);
        PacketAdapters.registerOutbound(filter);

        getLogger().at(Level.INFO).log(
                "[InfCanary] Armed. Scanning every outbound packet for non-finite floats; "
                        + "offenders are logged with a stack trace and dropped. Log: %s",
                logFile != null ? logFile.toString() : "(console only)");
    }

    @Override
    protected void shutdown() {
        try {
            if (filter != null) {
                PacketAdapters.deregisterOutbound(filter);
            }
        } catch (RuntimeException ignored) {
            // never registered / already gone
        }
        getLogger().at(Level.INFO).log("[InfCanary] Stopped. hits=%d dropped=%d", totalHits.get(), totalDropped.get());
        super.shutdown();
    }

    /** @return {@code true} to DROP the packet (stops it reaching the encoder). */
    private boolean handleOutbound(PacketHandler handler, Packet packet) {
        if (packet == null) {
            return false;
        }
        final NonFiniteScanner.Hit hit;
        try {
            hit = NonFiniteScanner.scan(packet);
        } catch (Throwable scanFailure) {
            // The canary must never be the thing that breaks networking.
            getLogger().at(Level.FINE).withCause(scanFailure).log("[InfCanary] scan failed for %s",
                    packet.getClass().getName());
            return false;
        }
        if (hit == null) {
            return false;
        }

        totalHits.incrementAndGet();
        String packetType = packet.getClass().getName();

        boolean shouldLogFull = shouldLogFull(packetType);
        String player = describePlayer(handler);

        if (shouldLogFull) {
            String stack = captureStack();
            String report = buildReport(packetType, hit, player, stack);

            getLogger().at(Level.SEVERE).log("%s", report);
            appendToFile(report);
        } else {
            getLogger().at(Level.WARNING).log(
                    "[InfCanary] (throttled) non-finite in %s at %s = %s -> %s (dropped)",
                    shortName(packetType), hit.path, fmt(hit.value), player);
        }

        totalDropped.incrementAndGet();
        return true; // drop it
    }

    private boolean shouldLogFull(String packetType) {
        long now = System.currentTimeMillis();
        AtomicLong last = lastReportByType.computeIfAbsent(packetType, k -> new AtomicLong(0L));
        long prev = last.get();
        if (now - prev >= PER_TYPE_THROTTLE_MS && last.compareAndSet(prev, now)) {
            return true;
        }
        return false;
    }

    private static String captureStack() {
        // Drop the top frames that are inside this plugin / the adapter dispatch so the first
        // interesting line is the actual packet producer.
        StackTraceElement[] frames = Thread.currentThread().getStackTrace();
        StringBuilder sb = new StringBuilder();
        boolean started = false;
        for (StackTraceElement fr : frames) {
            String cn = fr.getClassName();
            if (!started) {
                if (cn.equals(Thread.class.getName())
                        || cn.startsWith("com.varyon.infcanary.")
                        || cn.startsWith("com.hypixel.hytale.server.core.io.adapter.PacketAdapters")) {
                    continue;
                }
                started = true;
            }
            sb.append("\tat ").append(fr).append('\n');
        }
        return sb.toString();
    }

    private String buildReport(String packetType, NonFiniteScanner.Hit hit, String player, String stack) {
        StringWriter sw = new StringWriter(1024);
        PrintWriter pw = new PrintWriter(sw);
        pw.println("========== [InfCanary] non-finite outbound packet ==========");
        pw.println("time     : " + ZonedDateTime.now().format(TS));
        pw.println("packet   : " + packetType);
        pw.println("field    : " + hit.path + "   (declared in " + hit.declaringType + ")");
        pw.println("value    : " + fmt(hit.value));
        pw.println("player   : " + player);
        pw.println("thread   : " + Thread.currentThread().getName());
        pw.println("action   : packet DROPPED (not sent) to keep the pipeline alive");
        pw.println("--- producer stack trace ---");
        pw.print(stack);
        pw.println("===========================================================");
        return sw.toString();
    }

    private void appendToFile(String report) {
        Path f = logFile;
        if (f == null) {
            return;
        }
        try {
            Files.writeString(f, report + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            getLogger().at(Level.FINE).withCause(e).log("[InfCanary] could not append to log file");
        }
    }

    private static String describePlayer(PacketHandler handler) {
        try {
            if (handler instanceof GamePacketHandler gph) {
                PlayerRef ref = gph.getPlayerRef();
                if (ref != null) {
                    return ref.getUsername() + " (" + ref.getUuid() + ")";
                }
            }
        } catch (Throwable ignored) {
            // fall through
        }
        return handler == null ? "<null handler>" : handler.getClass().getSimpleName();
    }

    private static String fmt(double v) {
        if (Double.isNaN(v)) {
            return "NaN";
        }
        if (v == Double.POSITIVE_INFINITY) {
            return "+Inf";
        }
        if (v == Double.NEGATIVE_INFINITY) {
            return "-Inf";
        }
        return Double.toString(v);
    }

    private static String shortName(String fqcn) {
        int i = fqcn.lastIndexOf('.');
        return i >= 0 ? fqcn.substring(i + 1) : fqcn;
    }
}
