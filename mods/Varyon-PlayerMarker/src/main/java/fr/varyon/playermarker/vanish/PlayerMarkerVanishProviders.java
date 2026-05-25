package fr.varyon.playermarker;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class PlayerMarkerVanishProviders {

    private static final long VANISH_CACHE_TTL_MS = 750L;
    private static final long PROVIDER_CACHE_TTL_MS = 250L;
    private static final int VANISH_CACHE_MAX_ENTRIES = 1024;
    private static final List<PlayerMarkerVanishProvider> PROVIDERS = List.of(
            new PlayerMarkerEliteEssentialsVanishProvider(),
            new PlayerMarkerEssentialsPlusVanishProvider(),
            new PlayerMarkerHyEssentialsXVanishProvider());

    private static final ConcurrentHashMap<UUID, CacheEntry> vanishCache = new ConcurrentHashMap<>();
    private static volatile ProviderState providerState = new ProviderState(false, 0L);

    private PlayerMarkerVanishProviders() {}

    static void removeCacheFor(UUID playerUuid) {
        if (playerUuid != null) {
            vanishCache.remove(playerUuid);
        }
    }

    static boolean hasActiveProvider() {
        long now = System.currentTimeMillis();
        ProviderState cached = providerState;
        if (cached.expiresAtMs() >= now) {
            return cached.active();
        }

        boolean active = false;
        for (PlayerMarkerVanishProvider provider : PROVIDERS) {
            if (provider == null) {
                continue;
            }
            try {
                if (provider.isAvailable()) {
                    active = true;
                    break;
                }
            } catch (Throwable ignored) {
            }
        }

        providerState = new ProviderState(active, now + PROVIDER_CACHE_TTL_MS);
        return active;
    }

    static boolean isVanished(UUID playerUuid) {
        if (playerUuid == null || !hasActiveProvider()) {
            return false;
        }

        long now = System.currentTimeMillis();
        CacheEntry cached = vanishCache.get(playerUuid);
        if (cached != null && cached.expiresAtMs() >= now) {
            return cached.vanished();
        }

        boolean vanished = false;
        for (PlayerMarkerVanishProvider provider : PROVIDERS) {
            if (provider == null) {
                continue;
            }
            try {
                if (!provider.isAvailable()) {
                    continue;
                }
                if (provider.isVanished(playerUuid)) {
                    vanished = true;
                    break;
                }
            } catch (Throwable ignored) {
            }
        }

        vanishCache.put(playerUuid, new CacheEntry(vanished, now + VANISH_CACHE_TTL_MS));
        if (vanishCache.size() > VANISH_CACHE_MAX_ENTRIES) {
            pruneVanishCacheIfNeeded(now);
        }
        return vanished;
    }

    private static void pruneVanishCacheIfNeeded(long now) {
        vanishCache.entrySet().removeIf(e -> e.getValue().expiresAtMs() < now);
        while (vanishCache.size() > VANISH_CACHE_MAX_ENTRIES) {
            var it = vanishCache.keySet().iterator();
            if (!it.hasNext()) {
                break;
            }
            it.next();
            it.remove();
        }
    }

    private record CacheEntry(boolean vanished, long expiresAtMs) {}

    private record ProviderState(boolean active, long expiresAtMs) {}
}

