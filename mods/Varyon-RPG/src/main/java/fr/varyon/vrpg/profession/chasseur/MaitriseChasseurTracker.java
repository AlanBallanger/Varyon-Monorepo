package fr.varyon.vrpg.profession.chasseur;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MaitriseChasseurTracker {

    private final Map<UUID, Long> lastKillMs = new ConcurrentHashMap<>();

    public void onKill(UUID uuid) {
        lastKillMs.put(uuid, System.currentTimeMillis());
    }

    public long millisSinceLastKill(UUID uuid) {
        Long t = lastKillMs.get(uuid);
        return t == null ? Long.MAX_VALUE : System.currentTimeMillis() - t;
    }

    public void remove(UUID uuid) {
        lastKillMs.remove(uuid);
    }
}
