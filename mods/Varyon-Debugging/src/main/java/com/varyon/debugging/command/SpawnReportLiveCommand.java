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
 * /spawnreport live - snapshot of entities alive right now (not cumulative spawns), per
 * world and per type. Answers "what is EntityTickingSystem/SpatialSystem/RoleSystems
 * actually iterating over right now" without re-reading a spark flamegraph.
 */
public final class SpawnReportLiveCommand extends CommandBase {

    public SpawnReportLiveCommand() {
        super("live", "Affiche le nombre d'entites NPC actuellement vivantes, par monde et par type.");
        this.requirePermission("varyon.admin");
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        SpawnRegistry registry = SpawnRegistry.get();
        Map<String, LongAdder> live = registry.getLiveCountByWorldAndType();

        Map<String, Long> totalByWorld = new TreeMap<>();
        Map<String, TreeMap<Long, String>> topByWorld = new TreeMap<>();

        for (Map.Entry<String, LongAdder> entry : live.entrySet()) {
            long count = entry.getValue().sum();
            if (count <= 0) {
                continue;
            }
            String[] parts = entry.getKey().split("\\|", 2);
            if (parts.length != 2) {
                continue;
            }
            String world = parts[0];
            String type = parts[1];
            totalByWorld.merge(world, count, Long::sum);
            topByWorld.computeIfAbsent(world, w -> new TreeMap<>(Comparator.reverseOrder()))
                    .put(count, type);
        }

        if (totalByWorld.isEmpty()) {
            context.sendMessage(Message.raw("Aucune entite vivante trackee actuellement."));
            return;
        }

        context.sendMessage(Message.raw("=== Entites vivantes (snapshot actuel) ==="));
        for (Map.Entry<String, Long> entry : totalByWorld.entrySet()) {
            context.sendMessage(Message.raw(String.format(
                    "%s : %d entite(s) vivante(s)", entry.getKey(), entry.getValue())));
        }

        context.sendMessage(Message.raw("--- Top types vivants par monde ---"));
        for (Map.Entry<String, TreeMap<Long, String>> worldEntry : topByWorld.entrySet()) {
            context.sendMessage(Message.raw(worldEntry.getKey() + " :"));
            int shown = 0;
            for (Map.Entry<Long, String> typeEntry : worldEntry.getValue().entrySet()) {
                if (shown++ >= 8) {
                    break;
                }
                context.sendMessage(Message.raw(String.format(
                        "   %s : %d vivant(s)", typeEntry.getValue(), typeEntry.getKey())));
            }
        }
    }
}
