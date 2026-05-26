package com.varyon.varyonui.integration;

import com.hypixel.hytale.assetstore.AssetPack;
import com.hypixel.hytale.common.plugin.PluginManifest;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.asset.common.CommonAssetModule;
import com.hypixel.hytale.server.core.asset.common.CommonAssetRegistry;
import com.hypixel.hytale.server.core.asset.common.asset.FileCommonAsset;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;

import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

final class SkinPortraitRuntime {

    private static final Logger LOG = Logger.getLogger("VaryonUI");

    private static volatile String portraitPackId;
    private static volatile boolean portraitPackReady;

    private SkinPortraitRuntime() {}

    static void prepareAtStartup(JavaPlugin plugin) {
        ensurePortraitPack(plugin);
    }

    private static void ensurePortraitPack(JavaPlugin plugin) {
        if (plugin == null) {
            return;
        }
        if (portraitPackReady) {
            return;
        }
        synchronized (SkinPortraitRuntime.class) {
            if (portraitPackReady) {
                return;
            }
            try {
                PluginManifest base = plugin.getManifest();
                if (base == null) {
                    return;
                }
                String id = base.getGroup() + ":" + base.getName() + "-skin-portraits";
                Path root = plugin.getDataDirectory().resolve("skin_portrait_pack");
                Files.createDirectories(root);

                PluginManifest sub = new PluginManifest();
                sub.setGroup(base.getGroup());
                sub.setName(base.getName() + "-skin-portraits");
                sub.setDescription("Runtime skin portraits for Varyon menu");
                sub.setVersion(base.getVersion());

                AssetModule am = AssetModule.get();
                if (am == null) {
                    LOG.log(Level.WARNING, "AssetModule unavailable; skin portrait pack not registered");
                    return;
                }
                if (am.getAssetPack(id) == null) {
                    am.registerPack(id, root, sub, AssetPack.PackSource.MODS);
                    am.initPendingStores();
                }
                portraitPackId = id;
                portraitPackReady = true;
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Failed to register skin portrait asset pack", e);
            }
        }
    }

    @Nullable
    static String publishPng(JavaPlugin plugin, UUID uuid, byte[] pngBytes) {
        return publishPngWithKey(plugin, uuid.toString(), pngBytes);
    }

    @Nullable
    static String publishPngWithKey(JavaPlugin plugin, String key, byte[] pngBytes) {
        return publishPngWithKey(plugin, key, pngBytes, false);
    }

    @Nullable
    static String publishPngWithKey(JavaPlugin plugin, String key, byte[] pngBytes, boolean forceClientRebuild) {
        if (key == null || pngBytes == null || pngBytes.length < 24) {
            LOG.log(Level.WARNING, "[PortraitPack] publishPngWithKey bad args key=" + key
                    + " len=" + (pngBytes == null ? -1 : pngBytes.length));
            return null;
        }
        ensurePortraitPack(plugin);
        if (!portraitPackReady || portraitPackId == null) {
            LOG.log(Level.WARNING, "[PortraitPack] pack not ready key=" + key);
            return null;
        }
        try {
            String fileName = key + ".png";
            Path cacheDir = plugin.getDataDirectory().resolve("skin_portraits_cache");
            Files.createDirectories(cacheDir);
            Path file = cacheDir.resolve(fileName);
            Files.write(file, pngBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            String assetName = "UI/Custom/Portraits/" + fileName;
            FileCommonAsset asset = new FileCommonAsset(file, assetName, pngBytes);
            CommonAssetRegistry.addCommonAsset(portraitPackId, asset);
            CommonAssetModule module = CommonAssetModule.get();
            if (module != null) {
                module.sendAsset(asset, forceClientRebuild);
            }
            return "Portraits/" + key + ".png";
        } catch (Exception e) {
            LOG.log(Level.WARNING, "[PortraitPack] publish failed key=" + key, e);
            return null;
        }
    }
}
