package com.varyon.elite;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central lookup of active elites. Feeds the admin dashboard's live view and lets Varyon-RPG
 * resolve an elite tier for XP scaling (see ELITE_MOBS_DESIGN.md §6).
 */
public final class EliteRegistry {

    private static final EliteRegistry INSTANCE = new EliteRegistry();

    private final Map<UUID, ActiveElite> byEntity = new ConcurrentHashMap<>();

    private EliteRegistry() {}

    @Nonnull
    public static EliteRegistry get() {
        return INSTANCE;
    }

    public void register(@Nonnull UUID entityId, @Nonnull ActiveElite elite) {
        byEntity.put(entityId, elite);
    }

    public void unregister(@Nonnull UUID entityId) {
        byEntity.remove(entityId);
    }

    public int getEliteTier(@Nonnull UUID entityId) {
        ActiveElite e = byEntity.get(entityId);
        return e != null ? e.tier() : 0;
    }

    public int getEliteTier(@Nonnull Ref<EntityStore> ref) {
        return 0;
    }

    @Nullable
    public ActiveElite get(@Nonnull UUID entityId) {
        return byEntity.get(entityId);
    }

    @Nonnull
    public Collection<ActiveElite> activeElites() {
        return byEntity.values();
    }

    public int size() {
        return byEntity.size();
    }

    public record ActiveElite(@Nonnull UUID entityId, @Nonnull MobClass mobClass, int tier,
                              @Nonnull String worldName, @Nullable String namedId) {
    }
}
