package com.varyon.elite.combat;

import javax.annotation.Nonnull;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-elite threat accounting so an elite picks a deliberate target rather than the last
 * attacker. Threat decays over time; damage and healing near the elite add to it.
 */
public final class EliteThreatTable {

    private final Map<UUID, Double> threatByPlayer = new ConcurrentHashMap<>();

    public void addThreat(@Nonnull UUID playerId, double amount) {
        threatByPlayer.merge(playerId, amount, Double::sum);
    }

    public void decay(double factor) {
        threatByPlayer.replaceAll((id, v) -> v * factor);
        threatByPlayer.values().removeIf(v -> v < 0.5);
    }

    public UUID topThreat() {
        return threatByPlayer.entrySet().stream()
                .max(Comparator.comparingDouble(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    public void clear() {
        threatByPlayer.clear();
    }
}
