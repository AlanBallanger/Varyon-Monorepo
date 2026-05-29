package fr.varyon.vrpg.profession.fermier;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MaitriseFermierTracker {

    private final Map<UUID, Long> lastCropPickupMs = new ConcurrentHashMap<>();

    public void onCropPickup(UUID uuid) {
        lastCropPickupMs.put(uuid, System.currentTimeMillis());
    }

    public long millisSinceLastPickup(UUID uuid) {
        Long t = lastCropPickupMs.get(uuid);
        return t == null ? Long.MAX_VALUE : System.currentTimeMillis() - t;
    }

    public void remove(UUID uuid) {
        lastCropPickupMs.remove(uuid);
    }
}
