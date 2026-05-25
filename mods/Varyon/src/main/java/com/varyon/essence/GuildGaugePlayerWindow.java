package com.varyon.essence;

import java.util.ArrayDeque;
import java.util.function.IntSupplier;

public final class GuildGaugePlayerWindow {
    public static final long WINDOW_MS = 15L * 60L * 1000L;
    public static final long SAMPLE_INTERVAL_MS = 60_000L;

    private static final class Sample {
        final long timeMs;
        final int online;

        Sample(long timeMs, int online) {
            this.timeMs = timeMs;
            this.online = online;
        }
    }

    private final Object lock = new Object();
    private final ArrayDeque<Sample> samples = new ArrayDeque<>();

    public void recordSample(int rawOnlineCount) {
        long now = System.currentTimeMillis();
        synchronized (lock) {
            pruneLocked(now);
            samples.addLast(new Sample(now, rawOnlineCount));
        }
    }

    private void pruneLocked(long now) {
        long cutoff = now - WINDOW_MS;
        while (!samples.isEmpty() && samples.peekFirst().timeMs < cutoff) {
            samples.pollFirst();
        }
    }

    public int effectivePlayerCount(IntSupplier currentOnlineIfEmpty) {
        long now = System.currentTimeMillis();
        synchronized (lock) {
            pruneLocked(now);
            if (samples.isEmpty()) {
                return Math.max(1, currentOnlineIfEmpty.getAsInt());
            }
            long sum = 0;
            for (Sample s : samples) {
                sum += s.online;
            }
            return Math.max(1, (int) Math.round((double) sum / samples.size()));
        }
    }

    public void clear() {
        synchronized (lock) {
            samples.clear();
        }
    }
}
