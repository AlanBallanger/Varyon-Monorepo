package fr.varyon.holograms.hologram;

import com.hypixel.hytale.assetstore.AssetStore;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.common.CommonAsset;
import com.hypixel.hytale.server.core.asset.common.CommonAssetModule;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.universe.Universe;
import fr.varyon.holograms.VaryonHologramsPlugin;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Stream;

public class ImageManager {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String ASSET_PREFIX = "VaryonHolograms_Image_";
    private static final String PACK_NAME = "VaryonHologramsAssets";

    private final VaryonHologramsPlugin plugin;
    private final Path imagesFolder;
    private final Map<String, ImageData> registry = new ConcurrentHashMap<>();
    private final Map<String, Model> modelCache = new ConcurrentHashMap<>();
    private final List<CommonAsset> pendingSend = new ArrayList<>();

    public ImageManager(@Nonnull VaryonHologramsPlugin plugin) {
        this.plugin = plugin;
        this.imagesFolder = plugin.getImagesDir();
    }

    public void initialize() {
        LOGGER.at(Level.INFO).log("[Varyon-Holograms] Images dossier: %s", imagesFolder.toAbsolutePath());
        try {
            Files.createDirectories(imagesFolder);
        } catch (IOException e) {
            LOGGER.at(Level.WARNING).log("[Varyon-Holograms] Erreur création dossier images: %s", e.getMessage());
        }
        List<ImageFileInfo> files = scan();
        for (ImageFileInfo info : files) {
            registry.put(info.imageName, new ImageData(info, false));
            LOGGER.at(Level.INFO).log("[Varyon-Holograms] Image trouvée: %s -> assetId=%s (%dx%d)", info.imageName, info.modelAssetId, info.width, info.height);
        }
        LOGGER.at(Level.INFO).log("[Varyon-Holograms] Images: %s trouvées dans images/", registry.size());
    }

    public void liveLoadAll() {
        liveLoad(scan());
    }

    public void reload() {
        registry.clear();
        modelCache.clear();
        liveLoadAll();
    }

    public boolean hasImage(@Nonnull String name) {
        return registry.containsKey(normalize(name));
    }

    @Nonnull
    public Set<String> getAvailableImages() {
        return Collections.unmodifiableSet(new HashSet<>(registry.keySet()));
    }

    @Nullable
    public Model createImageModel(@Nonnull String imageName, float scale, boolean billboard) {
        String key = normalize(imageName);
        String cacheKey = key + "_" + scale + "_" + billboard;
        Model cached = modelCache.get(cacheKey);
        if (cached != null) return cached;

        ImageData data = registry.get(key);
        if (data == null) {
            LOGGER.at(Level.WARNING).log("[Varyon-Holograms] Image inconnue: '%s' — placez le PNG dans images/ et faites /holo reload", imageName);
            return null;
        }

        String modelAssetId = billboard ? data.modelAssetId() + "_Billboard" : data.modelAssetId();
        if (!isLoaded(modelAssetId) && billboard) modelAssetId = data.modelAssetId();
        if (!isLoaded(modelAssetId)) {
            LOGGER.at(Level.WARNING).log("[Varyon-Holograms] ModelAsset '%s' pas encore dans le registry — liveLoad peut-être échoué", modelAssetId);
            return null;
        }

        try {
            ModelAsset asset = (ModelAsset) ModelAsset.getAssetMap().getAsset(modelAssetId);
            if (asset == null) return null;
            Model model = Model.createStaticScaledModel(asset, scale);
            modelCache.put(cacheKey, model);
            return model;
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("[Varyon-Holograms] Erreur createImageModel %s: %s", imageName, e.getMessage());
            return null;
        }
    }

    public void liveLoad(@Nonnull List<ImageFileInfo> files) {
        if (files.isEmpty()) return;
        try {
            CommonAssetModule module = CommonAssetModule.get();
            if (module == null) {
                LOGGER.at(Level.WARNING).log("[Varyon-Holograms] CommonAssetModule indisponible");
                return;
            }

            AssetStore<String, ModelAsset, DefaultAssetMap<String, ModelAsset>> modelStore = ModelAsset.getAssetStore();
            List<ModelAsset> modelAssets = new ArrayList<>();
            List<CommonAsset> commonAssets = new ArrayList<>();

            for (ImageFileInfo info : files) {
                try {
                    byte[] texBytes = Files.readAllBytes(info.path);

                    commonAssets.add(addCommon(module, "Characters/VaryonHolograms/" + info.modelAssetId + ".png", texBytes));
                    commonAssets.add(addCommon(module, "Characters/VaryonHolograms/" + info.modelAssetId + ".blockymodel",
                        blockyModel(info.width, info.height, false).getBytes(StandardCharsets.UTF_8)));
                    commonAssets.add(addCommon(module, "Characters/VaryonHolograms/" + info.modelAssetId + "_Billboard.blockymodel",
                        blockyModel(info.width, info.height, true).getBytes(StandardCharsets.UTF_8)));

                    ModelAsset ma = buildModelAsset(info.modelAssetId, info.texturePath, info.width, info.height, false);
                    if (ma != null) modelAssets.add(ma);
                    ModelAsset maBb = buildModelAsset(info.modelAssetId + "_Billboard", info.texturePath, info.width, info.height, true);
                    if (maBb != null) modelAssets.add(maBb);

                    registry.put(info.imageName, new ImageData(info, true));
                    LOGGER.at(Level.INFO).log("[Varyon-Holograms] Image chargée: %s (%dx%d)", info.imageName, info.width, info.height);
                } catch (Exception e) {
                    LOGGER.at(Level.WARNING).log("[Varyon-Holograms] Erreur chargement image %s: %s", info.imageName, e.getMessage());
                }
            }

            if (!modelAssets.isEmpty()) {
                modelStore.loadAssets(PACK_NAME, modelAssets);
                LOGGER.at(Level.INFO).log("[Varyon-Holograms] %s ModelAssets injectés dans le registry", modelAssets.size());
            }

            pendingSend.clear();
            pendingSend.addAll(commonAssets);
            if (!commonAssets.isEmpty()) {
                LOGGER.at(Level.INFO).log("[Varyon-Holograms] %s common assets prêts (envoi différé)", commonAssets.size());
            }
            LOGGER.at(Level.INFO).log("[Varyon-Holograms] liveLoad terminé");
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("[Varyon-Holograms] Erreur liveLoad: %s", e.getMessage());
            e.printStackTrace();
        }
    }

    public void pushAssetsToOnlinePlayers() {
        List<CommonAsset> toSend;
        synchronized (pendingSend) {
            if (pendingSend.isEmpty()) return;
            toSend = new ArrayList<>(pendingSend);
        }
        try {
            CommonAssetModule module = CommonAssetModule.get();
            if (module == null) {
                LOGGER.at(Level.WARNING).log("[Varyon-Holograms] CommonAssetModule indisponible pour envoi assets");
                return;
            }
            if (Universe.get() == null || Universe.get().getPlayerCount() == 0) {
                LOGGER.at(Level.INFO).log("[Varyon-Holograms] Aucun joueur en ligne, envoi assets ignoré");
                return;
            }
            LOGGER.at(Level.INFO).log("[Varyon-Holograms] Envoi de %s assets aux joueurs...", toSend.size());
            module.sendAssets(toSend, true);
            LOGGER.at(Level.INFO).log("[Varyon-Holograms] Assets envoyés aux joueurs");
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("[Varyon-Holograms] Échec envoi assets: %s", e.getMessage());
        }
    }

    private CommonAsset addCommon(CommonAssetModule module, String name, byte[] data) {
        ByteArrayCommonAsset asset = new ByteArrayCommonAsset(name, data);
        module.addCommonAsset(PACK_NAME, asset, false);
        return asset;
    }

    @Nullable
    private ModelAsset buildModelAsset(String id, String texturePath, int width, int height, boolean billboard) {
        try {
            double ar = (double) width / height;
            double nw = width >= height ? 1.0 : ar;
            double nh = width >= height ? 1.0 / ar : 1.0;
            double hw = nw / 2.0;
            String suffix = billboard ? "_Billboard" : "";
            String modelPath = "Characters/VaryonHolograms/" + id.replace("_Billboard", "") + suffix + ".blockymodel";

            ModelAsset ma = new ModelAsset();
            setField(ma, "id", id);
            setField(ma, "model", modelPath);
            setField(ma, "texture", texturePath);
            setField(ma, "minScale", 0.01f);
            setField(ma, "maxScale", 100.0f);
            setField(ma, "boundingBox", new com.hypixel.hytale.math.shape.Box(-hw, 0.0, -0.01, hw, nh, 0.01));
            return ma;
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("[Varyon-Holograms] buildModelAsset ECHEC id=%s: %s", id, e.getMessage());
            return null;
        }
    }

    private static void setField(Object obj, String name, Object value) throws Exception {
        Class<?> c = obj.getClass();
        while (c != null) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                f.set(obj, value);
                return;
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException("Champ '" + name + "' introuvable dans " + obj.getClass().getName());
    }

    private List<ImageFileInfo> scan() {
        List<ImageFileInfo> result = new ArrayList<>();
        if (!Files.exists(imagesFolder, LinkOption.NOFOLLOW_LINKS)) return result;
        try (Stream<Path> stream = Files.list(imagesFolder)) {
            stream.filter(p -> !Files.isDirectory(p))
                .filter(p -> {
                    String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
                    return n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg");
                })
                .forEach(p -> {
                    ImageFileInfo info = processFile(p);
                    if (info != null) result.add(info);
                });
        } catch (IOException e) {
            LOGGER.at(Level.WARNING).log("[Varyon-Holograms] Erreur scan images: %s", e.getMessage());
        }
        return result;
    }

    @Nullable
    private ImageFileInfo processFile(@Nonnull Path file) {
        try {
            String fileName = file.getFileName().toString();
            String base = fileName.substring(0, fileName.lastIndexOf('.'));
            String imageName = base.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
            String modelAssetId = ASSET_PREFIX + capitalize(imageName);
            String texturePath = "Characters/VaryonHolograms/" + modelAssetId + ".png";
            BufferedImage img = ImageIO.read(file.toFile());
            if (img == null) return null;
            return new ImageFileInfo(file, imageName, modelAssetId, texturePath, img.getWidth(), img.getHeight());
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("[Varyon-Holograms] Erreur traitement %s: %s", file.getFileName(), e.getMessage());
            return null;
        }
    }

    private boolean isLoaded(String modelAssetId) {
        try { return ModelAsset.getAssetMap().getAsset(modelAssetId) != null; } catch (Exception e) { return false; }
    }

    private static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
    }

    private static String capitalize(String s) {
        if (s.isEmpty()) return s;
        StringBuilder sb = new StringBuilder();
        boolean up = true;
        for (char c : s.toCharArray()) {
            if (c == '_') { sb.append(c); up = true; }
            else if (up) { sb.append(Character.toUpperCase(c)); up = false; }
            else sb.append(c);
        }
        return sb.toString();
    }

    private static String blockyModel(int width, int height, boolean billboard) {
        String lod = billboard ? "billboard" : "auto";
        return String.format(Locale.US,
            "{\n  \"nodes\": [\n    {\n      \"id\": \"1\",\n      \"name\": \"Plane\",\n" +
            "      \"position\": {\"x\": 0, \"y\": 0, \"z\": 0},\n" +
            "      \"orientation\": {\"x\": 0, \"y\": 0, \"z\": 0, \"w\": 1},\n" +
            "      \"shape\": {\n        \"type\": \"quad\",\n" +
            "        \"offset\": {\"x\": 0.0, \"y\": 0.0, \"z\": 0},\n" +
            "        \"stretch\": {\"x\": 1, \"y\": 1, \"z\": 1},\n" +
            "        \"settings\": {\"size\": {\"x\": %d, \"y\": %d}, \"normal\": \"+Z\"},\n" +
            "        \"visible\": true, \"doubleSided\": false, \"shadingMode\": \"flat\",\n" +
            "        \"unwrapMode\": \"custom\",\n" +
            "        \"textureLayout\": {\"front\": {\"offset\": {\"x\": 0, \"y\": 0},\n" +
            "          \"mirror\": {\"x\": false, \"y\": false}, \"angle\": 0}}\n" +
            "      }\n    }\n  ],\n  \"format\": \"character\",\n  \"lod\": \"%s\"\n}\n",
            width, height, lod);
    }

    public record ImageData(ImageFileInfo info, boolean loaded) {
        public String modelAssetId() { return info.modelAssetId; }
    }

    public record ImageFileInfo(Path path, String imageName, String modelAssetId,
                                String texturePath, int width, int height) {}

    private static class ByteArrayCommonAsset extends CommonAsset {
        private final byte[] data;
        ByteArrayCommonAsset(String name, byte[] data) { super(name, data); this.data = data; }
        @Override protected CompletableFuture<byte[]> getBlob0() { return CompletableFuture.completedFuture(data); }
    }
}
