/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  com.hypixel.hytale.assetstore.AssetPack
 *  com.hypixel.hytale.assetstore.event.LoadedAssetsEvent
 *  com.hypixel.hytale.assetstore.map.DefaultAssetMap
 *  com.hypixel.hytale.server.core.asset.AssetModule
 *  com.hypixel.hytale.server.core.asset.type.item.config.Item
 *  com.hypixel.hytale.server.core.plugin.JavaPlugin
 *  com.hypixel.hytale.server.core.plugin.PluginBase
 *  com.hypixel.hytale.server.core.plugin.PluginManager
 *  javax.annotation.Nonnull
 *  javax.annotation.Nullable
 */
package fr.varyon.weaponstats;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import fr.varyon.weaponstats.util.ModLangLoader;
import fr.varyon.weaponstats.util.WeaponTooltipInjector;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class WeaponDamageCache {
    private static final List<Path> assetRoots = new ArrayList<Path>();
    private static final Map<String, WeaponDamage> weaponDamageCache = new HashMap<String, WeaponDamage>();
    private static final Map<String, Path> jsonFileIndex = new ConcurrentHashMap<String, Path>();
    private static boolean fileSystemInitialized = false;
    private static final List<FileSystem> pluginFileSystems = new CopyOnWriteArrayList<FileSystem>();

    public static void cacheWeaponDamages(@Nonnull LoadedAssetsEvent<String, Item, DefaultAssetMap<String, Item>> event) {
        if (!fileSystemInitialized) {
            try {
                assetRoots.clear();
                List<AssetPack> packs = AssetModule.get().getAssetPacks();
                if (packs != null) {
                    for (AssetPack pack : packs) {
                        Path root = pack.getRoot();
                        if (root == null || assetRoots.contains(root)) continue;
                        assetRoots.add(root);
                    }
                } else {
                    Path baseRoot = AssetModule.get().getBaseAssetPack().getRoot();
                    assetRoots.add(baseRoot);
                }
                WeaponDamageCache.buildJsonIndex();
                ModLangLoader.load(assetRoots);
                fileSystemInitialized = true;
            }
            catch (Exception e) {
                System.err.println("[WeaponDamageCache] Failed to configure asset roots: " + e.getMessage());
            }
        }
        int newWeaponsCount = 0;
        Map<String, Item> items = ((DefaultAssetMap)event.getAssetMap()).getAssetMap();
        for (Map.Entry<String, Item> entry : items.entrySet()) {
            WeaponDamage damage;
            Item item = (Item)entry.getValue();
            String itemId = (String)entry.getKey();
            if (weaponDamageCache.containsKey(itemId) || item.getWeapon() == null || (damage = WeaponDamageCache.loadDamageFromJson(itemId)) == null) continue;
            weaponDamageCache.put(itemId, damage);
            ++newWeaponsCount;
        }
        if (newWeaponsCount > 0) {
            WeaponTooltipInjector.injectTooltips();
        }
    }

    public static Map<String, WeaponDamage> getAllWeapons() {
        return Collections.unmodifiableMap(weaponDamageCache);
    }

    @Nullable
    private static WeaponDamage loadDamageFromJson(String itemId) {
        JsonObject transProps;
        double signatureEnergy;
        double randomModifier;
        boolean hasRandomDamage;
        HashMap<String, Double> attackTypes;
        int damageCount;
        double totalDamage;
        double maxDamage;
        String quality;
        JsonObject root;
        block14: {
            Path jsonPath;
            block13: {
                if (assetRoots.isEmpty()) {
                    return null;
                }
                jsonPath = WeaponDamageCache.findWeaponJsonPath(itemId);
                if (jsonPath != null && Files.exists(jsonPath, new LinkOption[0])) break block13;
                return null;
            }
            try {
                String jsonContent = Files.readString(jsonPath);
                JsonParser parser = new JsonParser();
                root = parser.parse(jsonContent).getAsJsonObject();
                String string = quality = root.has("Quality") ? root.get("Quality").getAsString() : "Common";
                if (!root.has("InteractionVars")) {
                    JsonObject transProps2;
                    Object descriptionKey = "server.items." + itemId + ".description";
                    if (root.has("TranslationProperties") && (transProps2 = root.getAsJsonObject("TranslationProperties")).has("Description")) {
                        descriptionKey = transProps2.get("Description").getAsString();
                    }
                    return new WeaponDamage(0.0, 0.0, quality, new HashMap<String, Double>(), (String)descriptionKey);
                }
                JsonObject interactionVars = root.getAsJsonObject("InteractionVars");
                maxDamage = 0.0;
                totalDamage = 0.0;
                damageCount = 0;
                attackTypes = new HashMap<String, Double>();
                hasRandomDamage = false;
                randomModifier = 0.0;
                signatureEnergy = 0.0;
                for (Map.Entry varEntry : interactionVars.entrySet()) {
                    JsonElement interactionsElem;
                    JsonObject varObj;
                    String varName = (String)varEntry.getKey();
                    if (!varName.toLowerCase().contains("damage") || !(varObj = ((JsonElement)varEntry.getValue()).getAsJsonObject()).has("Interactions") || !(interactionsElem = varObj.get("Interactions")).isJsonArray()) continue;
                    for (JsonElement interactionElem : interactionsElem.getAsJsonArray()) {
                        JsonObject damageCalc;
                        JsonObject interaction = interactionElem.getAsJsonObject();
                        if (!interaction.has("DamageCalculator") || !(damageCalc = interaction.getAsJsonObject("DamageCalculator")).has("BaseDamage")) continue;
                        if (damageCalc.has("RandomPercentageModifier")) {
                            hasRandomDamage = true;
                            randomModifier = damageCalc.get("RandomPercentageModifier").getAsDouble();
                        }
                        JsonObject baseDamage = damageCalc.getAsJsonObject("BaseDamage");
                        double interactionDamage = 0.0;
                        for (Map.Entry damageEntry : baseDamage.entrySet()) {
                            interactionDamage += ((JsonElement)damageEntry.getValue()).getAsDouble();
                        }
                        attackTypes.put(varName, interactionDamage);
                        if (interactionDamage > maxDamage) {
                            maxDamage = interactionDamage;
                        }
                        totalDamage += interactionDamage;
                        ++damageCount;
                        if (!damageCalc.has("SignatureEnergy")) continue;
                        signatureEnergy = Math.max(signatureEnergy, damageCalc.get("SignatureEnergy").getAsDouble());
                    }
                }
                if (damageCount != 0) break block14;
                return null;
            }
            catch (Exception e) {
                return null;
            }
        }
        Object descriptionKey = "server.items." + itemId + ".description";
        if (root.has("TranslationProperties") && (transProps = root.getAsJsonObject("TranslationProperties")).has("Description")) {
            descriptionKey = transProps.get("Description").getAsString();
        }
        double avgDamage = totalDamage / (double)damageCount;
        return new WeaponDamage(maxDamage, avgDamage, quality, attackTypes, hasRandomDamage, randomModifier, (String)descriptionKey, signatureEnergy);
    }

    @Nullable
    private static Path findWeaponJsonPath(String itemId) {
        return jsonFileIndex.get(itemId + ".json");
    }

    private static void buildJsonIndex() {
        jsonFileIndex.clear();
        assetRoots.parallelStream().forEach(root -> {
            try (Stream<Path> stream = Files.walk(root, new FileVisitOption[0])) {
                stream.filter(p -> p.toString().endsWith(".json")).forEach(p -> {
                    jsonFileIndex.put(p.getFileName().toString(), p);
                });
            }
            catch (Exception exception) {
                // empty catch block
            }
        });
        for (PluginBase plugin : PluginManager.get().getPlugins()) {
            try {
                JavaPlugin javaPlugin;
                Path pluginPath;
                if (!(plugin instanceof JavaPlugin) || (pluginPath = (javaPlugin = (JavaPlugin)plugin).getFile()) == null || !pluginPath.toString().endsWith(".jar") || !Files.exists(pluginPath, new LinkOption[0])) continue;
                try {
                    FileSystem fs = FileSystems.newFileSystem(pluginPath, (ClassLoader) null);
                    pluginFileSystems.add(fs);
                    for (Path root2 : fs.getRootDirectories()) {
                        try (Stream<Path> stream = Files.walk(root2, new FileVisitOption[0])) {
                            stream.filter(p -> p.toString().endsWith(".json")).forEach(p -> {
                                jsonFileIndex.put(p.getFileName().toString(), p);
                            });
                        }
                    }
                }
                catch (Exception e) {
                    System.err.println("[WeaponInfo] Failed to open plugin JAR: " + String.valueOf(pluginPath));
                }
            }
            catch (Exception e) {
                System.err.println("[WeaponInfo] Failed to scan plugin: " + plugin.getName());
                e.printStackTrace();
            }
        }
    }

    @Nullable
    public static WeaponDamage getWeaponDamage(String itemId) {
        return weaponDamageCache.get(itemId);
    }

    public static final class WeaponDamage {
        public final double maxDamage;
        public final double avgDamage;
        public final String quality;
        public final Map<String, Double> attackTypes;
        public final boolean hasRandomDamage;
        public final double randomModifier;
        public final String descriptionKey;
        public final double signatureEnergy;

        public WeaponDamage(double maxDamage, double avgDamage, String quality, Map<String, Double> attackTypes, boolean hasRandomDamage, double randomModifier, String descriptionKey, double signatureEnergy) {
            this.maxDamage = maxDamage;
            this.avgDamage = avgDamage;
            this.quality = quality != null ? quality : "Common";
            this.attackTypes = attackTypes != null ? attackTypes : new HashMap();
            this.hasRandomDamage = hasRandomDamage;
            this.randomModifier = randomModifier;
            this.descriptionKey = descriptionKey;
            this.signatureEnergy = signatureEnergy;
        }

        public WeaponDamage(double maxDamage, double avgDamage, String quality, Map<String, Double> attackTypes, String descriptionKey) {
            this(maxDamage, avgDamage, quality, attackTypes, false, 0.0, descriptionKey, 0.0);
        }
    }
}
