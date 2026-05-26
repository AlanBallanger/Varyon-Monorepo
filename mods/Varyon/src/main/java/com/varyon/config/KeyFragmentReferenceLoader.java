package com.varyon.config;

import com.hypixel.hytale.logger.HytaleLogger;
import com.moandjiezana.toml.Toml;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

record FragmentReferenceData(Map<String, Integer> mobTierWeights, Map<String, Double> mineralWeights) {
}

final class KeyFragmentReferenceLoader {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private KeyFragmentReferenceLoader() {
    }

    @Nullable
    static FragmentReferenceData loadFragmentReferenceData(@Nonnull Path pluginDataFolder, @Nonnull ClassLoader cl) {
        EconomyReferenceTomls.ensureInstalled(pluginDataFolder, cl);
        Path mcPath = pluginDataFolder.resolve(EconomyReferenceTomls.MOB_CATEGORIES);
        Path tmPath = pluginDataFolder.resolve(EconomyReferenceTomls.TIER_MAPPING);
        Path mwPath = pluginDataFolder.resolve(EconomyReferenceTomls.MINERAL_WEIGHTS);
        if (EconomyReferenceTomls.allPresent(pluginDataFolder)) {
            try (
                    InputStream mc = Files.newInputStream(mcPath);
                    InputStream tm = Files.newInputStream(tmPath);
                    InputStream mw = Files.newInputStream(mwPath)) {
                Map<String, Integer> tierWeights = parseTierMapping(tm);
                Map<String, Integer> mobW = parseMobCategories(mc, tierWeights);
                Map<String, Double> mining = parseMineralWeights(mw);
                LOGGER.at(Level.INFO).log(
                        "Loaded economy reference TOMLs from plugin folder: %s mob ids, %s mining ids",
                        mobW.size(),
                        mining.size());
                return new FragmentReferenceData(Map.copyOf(mobW), Map.copyOf(mining));
            } catch (Exception e) {
                LOGGER.at(Level.WARNING).log("Failed reading reference TOMLs from disk, trying classpath: " + e.getMessage());
            }
        }
        return loadFragmentReferenceDataClasspath(cl);
    }

    @Nullable
    static FragmentReferenceData loadFragmentReferenceDataClasspath(@Nonnull ClassLoader cl) {
        try (
                InputStream mc = cl.getResourceAsStream(EconomyReferenceTomls.MOB_CATEGORIES);
                InputStream tm = cl.getResourceAsStream(EconomyReferenceTomls.TIER_MAPPING);
                InputStream mw = cl.getResourceAsStream(EconomyReferenceTomls.MINERAL_WEIGHTS)) {
            if (mc == null || tm == null || mw == null) {
                return null;
            }
            Map<String, Integer> tierWeights = parseTierMapping(tm);
            Map<String, Integer> mobW = parseMobCategories(mc, tierWeights);
            Map<String, Double> mining = parseMineralWeights(mw);
            return new FragmentReferenceData(Map.copyOf(mobW), Map.copyOf(mining));
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("Failed loading embedded economy reference TOMLs", e);
            return null;
        }
    }

    @Nullable
    static MobFragmentsConfig tryLoad(@Nonnull Path pluginDataFolder, @Nonnull ClassLoader cl) {
        FragmentReferenceData d = loadFragmentReferenceData(pluginDataFolder, cl);
        if (d == null) {
            return null;
        }
        return new MobFragmentsConfig(new HashMap<>(d.mobTierWeights()), new LinkedHashMap<>(d.mineralWeights()));
    }

    private static Map<String, Integer> parseTierMapping(@Nonnull InputStream in) {
        Toml toml = new Toml().read(in);
        Map<String, Integer> out = new HashMap<>();
        for (Map.Entry<String, Object> e : toml.toMap().entrySet()) {
            if (e.getValue() instanceof Number n) {
                out.put(e.getKey().toLowerCase(Locale.ROOT), n.intValue());
            }
        }
        return out;
    }

    private static Map<String, Integer> parseMobCategories(@Nonnull InputStream in,
                                                           @Nonnull Map<String, Integer> tierWeights) {
        Toml toml = new Toml().read(in);
        Map<String, Integer> out = new HashMap<>();
        for (Map.Entry<String, Object> e : toml.toMap().entrySet()) {
            String k = e.getKey().toLowerCase(Locale.ROOT);
            Object v = e.getValue();
            if (!(v instanceof String tierStr)) {
                continue;
            }
            tierStr = tierStr.toLowerCase(Locale.ROOT).trim();
            Integer w = tierWeights.get(tierStr);
            if (w == null) {
                LOGGER.at(Level.WARNING).log("mob_categories.toml: unknown tier \"%s\" for `%s`", tierStr, k);
                continue;
            }
            out.put(k, w);
        }
        return out;
    }

    private static Map<String, Double> parseMineralWeights(@Nonnull InputStream in) {
        Toml toml = new Toml().read(in);
        Map<String, Double> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : toml.toMap().entrySet()) {
            if (e.getValue() instanceof Number n) {
                out.put(e.getKey().toLowerCase(Locale.ROOT), n.doubleValue());
            }
        }
        return out;
    }
}
