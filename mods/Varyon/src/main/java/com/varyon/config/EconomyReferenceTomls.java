package com.varyon.config;

import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

final class EconomyReferenceTomls {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    static final String MOB_CATEGORIES = "mob_categories.toml";
    static final String TIER_MAPPING = "tier_mapping.toml";
    static final String MINERAL_WEIGHTS = "mineral_weights.toml";

    private static final String[] ALL = { MOB_CATEGORIES, TIER_MAPPING, MINERAL_WEIGHTS };

    private EconomyReferenceTomls() {
    }

    static void ensureInstalled(@Nonnull Path pluginDataFolder, @Nonnull ClassLoader cl) {
        try {
            Files.createDirectories(pluginDataFolder);
            for (String name : ALL) {
                Path dest = pluginDataFolder.resolve(name);
                if (Files.isRegularFile(dest)) {
                    continue;
                }
                try (InputStream in = cl.getResourceAsStream(name)) {
                    if (in == null) {
                        LOGGER.at(Level.WARNING).log("Bundled resource missing: %s", name);
                        continue;
                    }
                    Files.copy(in, dest);
                    LOGGER.at(Level.INFO).log("Installed reference config: %s", dest);
                }
            }
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("Could not install economy reference TOMLs: " + e.getMessage());
        }
    }

    static boolean allPresent(@Nonnull Path pluginDataFolder) {
        for (String name : ALL) {
            if (!Files.isRegularFile(pluginDataFolder.resolve(name))) {
                return false;
            }
        }
        return true;
    }
}
