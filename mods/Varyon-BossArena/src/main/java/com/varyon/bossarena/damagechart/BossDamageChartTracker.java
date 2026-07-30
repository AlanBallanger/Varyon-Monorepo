package com.varyon.bossarena.damagechart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.DoubleAdder;

/**
 * Thread-safe per-event damage tally: eventId -> (playerUuid -> total HP removed).
 * Used by BossDamageChartRecordingSystem and read at event completion in BossLootHandler.
 */
public final class BossDamageChartTracker {

    /** eventId -> (playerUuid -> total HP removed). */
    private final Map<UUID, Map<UUID, DoubleAdder>> byEvent = new ConcurrentHashMap<>();

    /**
     * Add HP removed for a player in an event. Safe to call from the damage system (world thread).
     */
    public void addDamage(UUID eventId, UUID playerUuid, double amount) {
        if (eventId == null || playerUuid == null || !Double.isFinite(amount) || amount <= 0.0d) {
            return;
        }
        byEvent
                .computeIfAbsent(eventId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(playerUuid, k -> new DoubleAdder())
                .add(amount);
    }

    /** Snapshot of player UUIDs with a tally in this event, without clearing it. */
    public List<UUID> snapshotPlayerUuidsForEvent(UUID eventId) {
        if (eventId == null) {
            return List.of();
        }
        Map<UUID, DoubleAdder> perPlayer = byEvent.get(eventId);
        if (perPlayer == null || perPlayer.isEmpty()) {
            return List.of();
        }
        return List.copyOf(perPlayer.keySet());
    }

    /** Peek damage dealt by a player in an event without clearing the tally. */
    public long getDamage(UUID eventId, UUID playerUuid) {
        if (eventId == null || playerUuid == null) {
            return 0L;
        }
        Map<UUID, DoubleAdder> perPlayer = byEvent.get(eventId);
        if (perPlayer == null) {
            return 0L;
        }
        DoubleAdder adder = perPlayer.get(playerUuid);
        return adder == null ? 0L : Math.max(0L, Math.round(adder.sum()));
    }

    /**
     * Take a snapshot of damage per player for the event, then remove the event's data.
     * Call from loot handling when the event completes. Returns entries sorted by damage descending.
     */
    public List<DamageEntry> takeSnapshotAndRemove(UUID eventId) {
        if (eventId == null) {
            return List.of();
        }
        Map<UUID, DoubleAdder> perPlayer = byEvent.remove(eventId);
        if (perPlayer == null || perPlayer.isEmpty()) {
            return List.of();
        }
        List<DamageEntry> list = new ArrayList<>();
        for (Map.Entry<UUID, DoubleAdder> e : perPlayer.entrySet()) {
            long total = Math.round(e.getValue().sum());
            if (total > 0L) {
                list.add(new DamageEntry(e.getKey(), total));
            }
        }
        list.sort(Comparator.comparingLong(DamageEntry::damage).reversed());
        return Collections.unmodifiableList(list);
    }

    /** Discards an event's damage tally without snapshotting it. Call when an event ends without loot. */
    public void discard(UUID eventId) {
        if (eventId == null) {
            return;
        }
        byEvent.remove(eventId);
    }

    public static final class DamageEntry {
        private final UUID playerUuid;
        private final long damage;

        public DamageEntry(UUID playerUuid, long damage) {
            this.playerUuid = playerUuid;
            this.damage = damage;
        }

        public UUID playerUuid() {
            return playerUuid;
        }

        public long damage() {
            return damage;
        }
    }
}
