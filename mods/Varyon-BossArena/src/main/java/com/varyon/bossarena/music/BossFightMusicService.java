package com.varyon.bossarena.music;

import com.hypixel.hytale.server.core.universe.world.World;
import com.varyon.bossarena.BossArenaPlugin;
import com.varyon.bossarena.data.BossDefinition;
import org.joml.Vector3d;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Facade for boss fight music (OGGs in the mod {@code music/} folder).
 */
public final class BossFightMusicService {

    private BossFightMusicService() {}

    public static List<String> listMusicFileNames() {
        BossFightMusicManager manager = manager();
        return manager != null ? manager.listMusicFileNames() : Collections.emptyList();
    }

    public static void startForEvent(UUID eventId, World world, Vector3d center, BossDefinition def) {
        BossFightMusicManager manager = manager();
        if (manager != null) {
            manager.startForEvent(eventId, world, center, def);
        }
    }

    public static void stopForEvent(UUID eventId) {
        BossFightMusicManager manager = manager();
        if (manager != null) {
            manager.stopForEvent(eventId);
        }
    }

    private static BossFightMusicManager manager() {
        BossArenaPlugin plugin = BossArenaPlugin.getInstance();
        return plugin != null ? plugin.getFightMusicManager() : null;
    }
}
