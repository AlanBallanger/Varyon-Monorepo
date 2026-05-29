package fr.varyon.vrpg.profession.forestier;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MaitriseForestierTracker {

    private final Map<UUID, Long> lastTreeChopMs = new ConcurrentHashMap<>();

    public void onTreeChopped(UUID uuid) {
        lastTreeChopMs.put(uuid, System.currentTimeMillis());
    }

    public long millisSinceLastChop(UUID uuid) {
        Long t = lastTreeChopMs.get(uuid);
        return t == null ? Long.MAX_VALUE : System.currentTimeMillis() - t;
    }

    public void remove(UUID uuid) {
        lastTreeChopMs.remove(uuid);
    }
}
