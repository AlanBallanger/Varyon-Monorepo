package com.varyon.debugging.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.varyon.debugging.SpawnRegistry;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.LongAdder;
import javax.annotation.Nonnull;

/**
 * /spawnreport - dumps spawn/removal/live counts per world and per world+type, to compare
 * worlds (e.g. Haven vs a fresh copy) without re-profiling with spark each time.
 */
public final class SpawnReportCommand extends CommandBase {

    public SpawnReportCommand() {
        super("spawnreport", "Affiche les compteurs de spawn d'entites par monde depuis le demarrage.");
        this.requirePermission("varyon.admin");
        this.addSubCommand(new SpawnReportResetCommand());
        this.addSubCommand(new SpawnReportLiveCommand());
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        SpawnRegistry registry = SpawnRegistry.get();

        Map<String, Long> spawnsByWorld = snapshot(registry.getSpawnsByWorld());
        Map<String, Long> removalsByWorld = snapshot(registry.getRemovalsByWorld());

        if (spawnsByWorld.isEmpty()) {
            context.sendMessage(Message.raw("Aucun spawn enregistre depuis le demarrage."));
            return;
        }

        context.sendMessage(Message.raw("=== Spawn report (depuis demarrage) ==="));
        for (Map.Entry<String, Long> entry : spawnsByWorld.entrySet()) {
            String world = entry.getKey();
            long spawns = entry.getValue();
            long removals = removalsByWorld.getOrDefault(world, 0L);
            long netLive = spawns - removals;
            context.sendMessage(Message.raw(String.format(
                    "%s : %d spawns, %d removals, net vivant approx %d",
                    world, spawns, removals, netLive)));
        }

        context.sendMessage(Message.raw("--- Top types par monde ---"));
        Map<String, Long> byType = snapshot(registry.getSpawnsByWorldAndType());
        Map<String, TreeMap<Long, String>> topByWorld = new TreeMap<>();
        for (Map.Entry<String, Long> entry : byType.entrySet()) {
            String[] parts = entry.getKey().split("\\|", 2);
            if (parts.length != 2) {
                continue;
            }
            topByWorld.computeIfAbsent(parts[0], w -> new TreeMap<>(Comparator.reverseOrder()))
                    .put(entry.getValue(), parts[1]);
        }
        for (Map.Entry<String, TreeMap<Long, String>> worldEntry : topByWorld.entrySet()) {
            context.sendMessage(Message.raw(worldEntry.getKey() + " :"));
            int shown = 0;
            for (Map.Entry<Long, String> typeEntry : worldEntry.getValue().entrySet()) {
                if (shown++ >= 5) {
                    break;
                }
                context.sendMessage(Message.raw(String.format(
                        "   %s : %d spawns", typeEntry.getValue(), typeEntry.getKey())));
            }
        }
    }

    @Nonnull
    private static Map<String, Long> snapshot(@Nonnull Map<String, LongAdder> source) {
        Map<String, Long> result = new TreeMap<>();
        for (Map.Entry<String, LongAdder> entry : source.entrySet()) {
            result.put(entry.getKey(), entry.getValue().sum());
        }
        return result;
    }
}
