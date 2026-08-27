package com.varyon.tiers;

import com.varyon.config.ZonePermissionsConfig;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class TierLeaderboardManager {
    public static final int MAX_TIER = 10;

    private final KnownPlayersDatabase database;
    private final ZonePermissionsConfig zonePermissionsConfig;
    private final Executor resolverExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "varyon-tier-leaderboard-resolver");
        t.setDaemon(true);
        return t;
    });

    public TierLeaderboardManager(@Nonnull File pluginFolder, @Nonnull ZonePermissionsConfig zonePermissionsConfig) {
        this.database = new KnownPlayersDatabase(pluginFolder);
        this.database.initialize();
        this.zonePermissionsConfig = zonePermissionsConfig;
    }

    public void recordPlayer(UUID playerUuid, String playerName) {
        database.recordPlayer(playerUuid, playerName);
    }

    public CompletableFuture<List<TierEntry>> getLeaderboard() {
        return CompletableFuture.supplyAsync(() -> {
            List<KnownPlayersDatabase.KnownPlayer> players = database.getAllPlayers();
            List<TierEntry> entries = new ArrayList<>(players.size());
            for (KnownPlayersDatabase.KnownPlayer player : players) {
                int tier = TierPermissionResolver.highestTier(player.uuid(), zonePermissionsConfig, MAX_TIER);
                entries.add(new TierEntry(player.uuid(), player.name(), tier));
            }
            entries.sort(Comparator.comparingInt(TierEntry::tier).reversed()
                .thenComparing(e -> e.name().toLowerCase()));
            return entries;
        }, resolverExecutor);
    }

    public void close() {
        database.close();
    }

    public record TierEntry(UUID uuid, String name, int tier) {}
}
