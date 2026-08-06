/*
 * Decompiled with CFR 0.152.
 */
package fr.varyon.weaponstats.util;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class ModLangLoader {
    private static final Logger LOGGER = Logger.getLogger("ModLangLoader");
    private static final Map<String, String> LANG_CACHE = new ConcurrentHashMap<String, String>();

    public static void load(List<Path> assetRoots) {
        LANG_CACHE.clear();
        AtomicInteger loadedFiles = new AtomicInteger(0);
        AtomicInteger loadedKeys = new AtomicInteger(0);
        assetRoots.parallelStream().forEach(root -> {
            Path langDir = root.resolve("Server/Languages/en-US");
            if (!Files.exists(langDir, new LinkOption[0]) || !Files.isDirectory(langDir, new LinkOption[0])) {
                return;
            }
            try (Stream<Path> stream = Files.list(langDir)) {
                stream.filter(p -> p.toString().endsWith(".lang")).forEach(file -> {
                    loadedKeys.addAndGet(ModLangLoader.parseLangFile(file));
                    loadedFiles.incrementAndGet();
                });
            }
            catch (Exception e) {
                LOGGER.warning("Failed to list/read lang files in " + String.valueOf(langDir) + ": " + e.getMessage());
            }
        });
        LOGGER.info("ModLangLoader loaded " + String.valueOf(loadedKeys) + " translations from " + String.valueOf(loadedFiles) + " files.");
    }

    private static int parseLangFile(Path file) {
        int count = 0;
        String fileName = file.getFileName().toString();
        String namespace = fileName.replace(".lang", "");
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            while ((line = reader.readLine()) != null) {
                int eqIndex;
                if ((line = line.trim()).isEmpty() || line.startsWith("#") || (eqIndex = line.indexOf(61)) <= 0) continue;
                String key = line.substring(0, eqIndex).trim();
                String value = line.substring(eqIndex + 1).trim();
                value = value.replace("\\n", "\n");
                LANG_CACHE.put(key, value);
                if (!key.startsWith(namespace + ".")) {
                    LANG_CACHE.put(namespace + "." + key, value);
                }
                ++count;
            }
        }
        catch (Exception e) {
            LOGGER.warning("Error parsing lang file " + String.valueOf(file) + ": " + e.getMessage());
        }
        return count;
    }

    public static String get(String key) {
        return LANG_CACHE.get(key);
    }
}

