package com.varyon.elite.combat;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** One {@link EliteThreatTable} per active elite, keyed by the elite's entity UUID. */
public final class EliteThreatRegistry {

    private static final Map<UUID, EliteThreatTable> TABLES = new ConcurrentHashMap<>();

    private EliteThreatRegistry() {}

    @Nonnull
    public static EliteThreatTable get(@Nonnull UUID eliteId) {
        return TABLES.computeIfAbsent(eliteId, k -> new EliteThreatTable());
    }

    public static void clear(@Nonnull UUID eliteId) {
        TABLES.remove(eliteId);
    }
}
