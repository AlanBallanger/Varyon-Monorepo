package fr.varyon.holograms.hologram;

import com.hypixel.hytale.math.vector.Rotation3f;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.Frozen;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.ProjectileComponent;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent;
import com.hypixel.hytale.server.core.modules.entity.component.Intangible;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.PropComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.entity.item.PreventItemMerging;
import com.hypixel.hytale.server.core.modules.entity.item.PreventPickup;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.holograms.VaryonHologramsPlugin;
import fr.varyon.holograms.animation.AnimationData;
import org.joml.Vector3d;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HologramManager {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Pattern HOLO_PATTERN = Pattern.compile("\\{(.*?)\\}", Pattern.DOTALL);

    private final VaryonHologramsPlugin plugin;
    private final Map<UUID, Hologram> holograms = new ConcurrentHashMap<>();
    private final Map<String, UUID> byName = new ConcurrentHashMap<>();
    private final ImageManager imageManager;
    private final BillboardManager billboardManager;
    private boolean spawned = false;

    public HologramManager(@Nonnull VaryonHologramsPlugin plugin) {
        this.plugin = plugin;
        this.imageManager = new ImageManager(plugin);
        this.billboardManager = new BillboardManager();
        imageManager.initialize();
        billboardManager.start();
        plugin.getAnimationManager().start();
    }

    public void shutdown() {
        billboardManager.stop();
        plugin.getAnimationManager().stop();
        removeAllHolograms();
    }

    @Nonnull public ImageManager getImageManager() { return imageManager; }
    @Nonnull public BillboardManager getBillboardManager() { return billboardManager; }

    @Nonnull
    public Collection<Hologram> getAllHolograms() {
        return Collections.unmodifiableCollection(holograms.values());
    }

    public int getHologramCount() { return holograms.size(); }

    public boolean hologramExists(@Nonnull String name) {
        return byName.containsKey(name.toLowerCase());
    }

    @Nullable
    public Hologram getHologram(@Nonnull String name) {
        UUID id = byName.get(name.toLowerCase());
        return id != null ? holograms.get(id) : null;
    }

    @Nullable
    public Hologram getHologram(@Nonnull UUID id) {
        return holograms.get(id);
    }

    @Nonnull
    public Hologram createHologram(@Nonnull String name, @Nonnull Vector3d position,
                                    @Nonnull UUID worldId, @Nullable UUID creatorId) {
        if (hologramExists(name)) throw new IllegalArgumentException("Un hologramme nommé '" + name + "' existe déjà.");
        Hologram hologram = new Hologram(name, position, worldId);
        hologram.setCreatorId(creatorId);
        hologram.addLine("Hologramme: " + name);
        hologram.addLine("Utilisez /holo addline " + name + " <texte>");
        holograms.put(hologram.getId(), hologram);
        byName.put(name.toLowerCase(), hologram.getId());
        spawnHologram(hologram);
        saveHolograms();
        return hologram;
    }

    public boolean deleteHologram(@Nonnull String name) {
        UUID id = byName.remove(name.toLowerCase());
        if (id == null) return false;
        Hologram h = holograms.remove(id);
        if (h == null) return false;
        despawnHologram(h);
        saveHolograms();
        return true;
    }

    public void updateHologram(@Nonnull Hologram hologram) {
        despawnHologramSync(hologram);
        spawnHologram(hologram);
        saveHolograms();
    }

    public void moveHologram(@Nonnull Hologram hologram, @Nonnull Vector3d newPosition) {
        hologram.setPosition(newPosition);
        updateHologram(hologram);
    }

    public void respawnAllHolograms() {
        for (Hologram h : holograms.values()) {
            despawnHologramSync(h);
            spawnHologram(h);
        }
        LOGGER.at(Level.INFO).log("[Varyon-Holograms] Respawned %s holograms", holograms.size());
    }

    private void spawnHologram(@Nonnull Hologram hologram) {
        if (!hologram.isVisible()) { LOGGER.at(Level.INFO).log("[Varyon-Holograms] spawnHologram: %s non visible, skip", hologram.getName()); return; }
        World world = findWorld(hologram.getWorldId());
        if (world == null) { LOGGER.at(Level.WARNING).log("[Varyon-Holograms] spawnHologram: monde introuvable pour %s worldId=%s", hologram.getName(), hologram.getWorldId()); return; }

        LOGGER.at(Level.INFO).log("[Varyon-Holograms] spawnHologram: %s -> world=%s pos=%.1f,%.1f,%.1f", hologram.getName(), world.getName(), hologram.getPosition().x, hologram.getPosition().y, hologram.getPosition().z);
        world.execute(() -> {
            try {
                Vector3d pos = hologram.getPosition();
                int chunkX = (int) Math.floor(pos.x) >> 5;
                int chunkZ = (int) Math.floor(pos.z) >> 5;
                Ref<?> chunkRef = world.getChunkStore().getChunkReference(ChunkUtil.indexChunk(chunkX, chunkZ));
                if (chunkRef == null || !chunkRef.isValid()) { LOGGER.at(Level.WARNING).log("[Varyon-Holograms] spawnHologram: chunk non chargé chunkX=%d chunkZ=%d pour %s", chunkX, chunkZ, hologram.getName()); return; }

                hologram.clearLineEntityIds();
                List<String> lines = hologram.getLines();
                double yOffset = 0;

                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    HologramLineType type = HologramLineType.fromLine(line);
                    if (i > 0) yOffset -= hologram.getLineSpacing() * (type == HologramLineType.TEXT ? 1 : 1.5);
                    Vector3d linePos = new Vector3d(pos.x, pos.y + yOffset, pos.z);

                    UUID entityId = switch (type) {
                        case IMAGE -> spawnImageLine(linePos, line, hologram, world);
                        case ITEM  -> spawnItemLine(linePos, line, hologram.getWorldId(), world);
                        default    -> spawnTextLine(linePos, line, hologram.getWorldId(), world);
                    };

                    if (entityId != null) {
                        hologram.addLineEntityId(entityId);
                        String animName = HologramLineType.extractAnimationName(line);
                        if (animName != null) {
                            AnimationData anim = plugin.getAnimationRegistry().getAnimation(animName);
                            if (anim != null) {
                                plugin.getAnimationManager().registerAnimation(entityId, anim, linePos, new org.joml.Vector3f(), 1f);
                            } else {
                                LOGGER.at(Level.WARNING).log("[Varyon-Holograms] Animation inconnue: %s", animName);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.at(Level.WARNING).log("[Varyon-Holograms] Erreur spawn hologram %s: %s", hologram.getName(), e.getMessage());
            }
        });
    }

    @Nullable
    private UUID spawnTextLine(@Nonnull Vector3d position, @Nonnull String text,
                                @Nonnull UUID worldId, @Nonnull World world) {
        try {
            UUID entityUuid = UUID.randomUUID();
            Runnable logic = () -> {
                try {
                    Store<EntityStore> store = world.getEntityStore().getStore();
                    Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
                    holder.putComponent(TransformComponent.getComponentType(),
                        new TransformComponent(new org.joml.Vector3d(position), Rotation3f.ZERO));
                    ProjectileComponent proj = new ProjectileComponent("Projectile");
                    holder.putComponent(ProjectileComponent.getComponentType(), proj);
                    if (proj.getProjectile() == null) proj.initialize();
                    holder.ensureComponent(Intangible.getComponentType());
                    holder.addComponent(Nameplate.getComponentType(), new Nameplate(stripColorCodes(text)));
                    holder.addComponent(EntityScaleComponent.getComponentType(), new EntityScaleComponent(1.0f));
                    holder.addComponent(NetworkId.getComponentType(), new NetworkId(((EntityStore) store.getExternalData()).takeNextNetworkId()));
                    holder.addComponent(UUIDComponent.getComponentType(), new UUIDComponent(entityUuid));
                    holder.ensureComponent(EntityStore.REGISTRY.getNonSerializedComponentType());
                    store.addEntity(holder, AddReason.SPAWN);
                } catch (Exception e) {
                    LOGGER.at(Level.WARNING).log("[Varyon-Holograms] Erreur spawn ligne texte: %s", e.getMessage());
                }
            };
            if (world.isInThread()) logic.run(); else world.execute(logic);
            return entityUuid;
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("[Varyon-Holograms] spawnTextLine erreur: %s", e.getMessage());
            return null;
        }
    }

    @Nullable
    private UUID spawnItemLine(@Nonnull Vector3d position, @Nonnull String line,
                                @Nonnull UUID worldId, @Nonnull World world) {
        HologramLineType.ItemLineData data = HologramLineType.parseItemLine(line);
        UUID entityUuid = UUID.randomUUID();
        Runnable logic = () -> {
            try {
                Store<EntityStore> store = world.getEntityStore().getStore();
                Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
                holder.addComponent(TransformComponent.getComponentType(),
                    new TransformComponent(new org.joml.Vector3d(position), Rotation3f.ZERO));

                Item item = (Item) Item.getAssetMap().getAsset(data.itemId);
                if (item == null) {
                    ProjectileComponent proj = new ProjectileComponent("Projectile");
                    holder.putComponent(ProjectileComponent.getComponentType(), proj);
                    if (proj.getProjectile() == null) proj.initialize();
                    holder.ensureComponent(Intangible.getComponentType());
                    holder.addComponent(Nameplate.getComponentType(), new Nameplate("[Item: " + data.itemId + "]"));
                } else {
                    String modelName = item.getModel();
                    boolean modelSpawned = false;
                    if (modelName != null && !modelName.isEmpty()) {
                        ModelAsset asset = (ModelAsset) ModelAsset.getAssetMap().getAsset(modelName);
                        if (asset != null) {
                            holder.addComponent(PersistentModel.getComponentType(),
                                new PersistentModel(new Model.ModelReference(modelName, 1.0f, null, true)));
                            holder.addComponent(EntityScaleComponent.getComponentType(),
                                new EntityScaleComponent(data.scale * item.getScale()));
                            holder.ensureComponent(Intangible.getComponentType());
                            modelSpawned = true;
                        }
                    }
                    if (!modelSpawned) {
                        ItemStack stack = new ItemStack(data.itemId, 1);
                        stack.setOverrideDroppedItemAnimation(true);
                        holder.addComponent(ItemComponent.getComponentType(), new ItemComponent(stack));
                        holder.addComponent(PreventPickup.getComponentType(), PreventPickup.INSTANCE);
                        holder.addComponent(PreventItemMerging.getComponentType(), PreventItemMerging.INSTANCE);
                        holder.addComponent(EntityScaleComponent.getComponentType(), new EntityScaleComponent(data.scale));
                        holder.ensureComponent(Intangible.getComponentType());
                    }
                }
                holder.addComponent(NetworkId.getComponentType(), new NetworkId(((EntityStore) store.getExternalData()).takeNextNetworkId()));
                holder.addComponent(UUIDComponent.getComponentType(), new UUIDComponent(entityUuid));
                holder.ensureComponent(EntityStore.REGISTRY.getNonSerializedComponentType());
                store.addEntity(holder, AddReason.SPAWN);
            } catch (Exception e) {
                LOGGER.at(Level.WARNING).log("[Varyon-Holograms] Erreur spawn ligne item: %s", e.getMessage());
            }
        };
        if (world.isInThread()) logic.run(); else world.execute(logic);
        return entityUuid;
    }

    @Nullable
    private UUID spawnImageLine(@Nonnull Vector3d position, @Nonnull String line,
                                 @Nonnull Hologram hologram, @Nonnull World world) {
        HologramLineType.ImageLineData data = HologramLineType.parseImageLine(line);
        Model model = imageManager.createImageModel(data.imageName, data.scale, data.billboard);
        if (model == null) {
            return spawnTextLine(position, "[Image: " + data.imageName + "]", hologram.getWorldId(), world);
        }

        UUID entityUuid = UUID.randomUUID();
        world.execute(() -> {
            try {
                Store<EntityStore> store = world.getEntityStore().getStore();
                Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
                holder.addComponent(TransformComponent.getComponentType(),
                    new TransformComponent(new org.joml.Vector3d(position), Rotation3f.ZERO));
                holder.addComponent(UUIDComponent.getComponentType(), new UUIDComponent(entityUuid));
                holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
                holder.addComponent(PersistentModel.getComponentType(),
                    new PersistentModel(new Model.ModelReference(model.getModelAssetId(), data.scale, null, true)));
                holder.addComponent(EntityScaleComponent.getComponentType(), new EntityScaleComponent(data.scale));
                holder.addComponent(NetworkId.getComponentType(), new NetworkId(((EntityStore) store.getExternalData()).takeNextNetworkId()));
                holder.ensureComponent(EntityModule.get().getVisibleComponentType());
                holder.ensureComponent(EntityStore.REGISTRY.getNonSerializedComponentType());
                Ref<EntityStore> ref = store.addEntity(holder, AddReason.SPAWN);
                if (ref != null && !data.billboard) {
                    store.ensureComponent(ref, Frozen.getComponentType());
                }
                if (data.billboard) {
                    billboardManager.register(entityUuid, hologram.getWorldId(), position, data.trackingDistance);
                }
            } catch (Exception e) {
                LOGGER.at(Level.WARNING).log("[Varyon-Holograms] Erreur spawn ligne image: %s", e.getMessage());
            }
        });
        return entityUuid;
    }

    private void despawnHologram(@Nonnull Hologram hologram) {
        World world = findWorld(hologram.getWorldId());
        List<UUID> entityIds = new ArrayList<>(hologram.getLineEntityIds());
        hologram.clearLineEntityIds();
        entityIds.forEach(id -> {
            billboardManager.unregister(id);
            plugin.getAnimationManager().unregisterAnimation(id);
        });
        if (world != null && !entityIds.isEmpty()) {
            world.execute(() -> removeEntities(world, entityIds));
        }
    }

    private void despawnHologramSync(@Nonnull Hologram hologram) {
        World world = findWorld(hologram.getWorldId());
        List<UUID> entityIds = new ArrayList<>(hologram.getLineEntityIds());
        hologram.clearLineEntityIds();
        entityIds.forEach(id -> {
            billboardManager.unregister(id);
            plugin.getAnimationManager().unregisterAnimation(id);
        });
        if (world == null || entityIds.isEmpty()) return;
        Runnable logic = () -> removeEntities(world, entityIds);
        try {
            if (world.isInThread()) {
                logic.run();
                return;
            }
            CountDownLatch latch = new CountDownLatch(1);
            world.execute(() -> {
                try { logic.run(); } finally { latch.countDown(); }
            });
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void removeEntities(@Nonnull World world, @Nonnull List<UUID> entityIds) {
        Store<EntityStore> store = world.getEntityStore().getStore();
        for (UUID id : entityIds) {
            try {
                Ref<EntityStore> ref = ((EntityStore) store.getExternalData()).getRefFromUUID(id);
                if (ref != null && ref.isValid()) store.removeEntity(ref, RemoveReason.REMOVE);
            } catch (Exception e) {
                LOGGER.at(Level.FINE).log("[Varyon-Holograms] Erreur suppression entité %s: %s", id, e.getMessage());
            }
        }
    }

    private void removeAllHolograms() {
        for (Hologram h : holograms.values()) {
            try { despawnHologramSync(h); } catch (Exception ignored) {}
        }
        holograms.clear();
        byName.clear();
        spawned = false;
    }

    public boolean areHologramsSpawned() { return spawned; }

    public void spawnAllHolograms() {
        if (spawned) return;
        spawned = true;
        for (Hologram h : holograms.values()) spawnHologram(h);
        LOGGER.at(Level.INFO).log("[Varyon-Holograms] Spawned %s holograms", holograms.size());
    }

    public void ensureHologramsVisibleInWorld(@Nonnull UUID worldId) {
        World world = findWorld(worldId);
        if (world == null) return;
        List<Hologram> toCheck = new ArrayList<>();
        for (Hologram h : holograms.values()) {
            if (h.getWorldId().equals(worldId) && h.isVisible()) toCheck.add(h);
        }
        if (toCheck.isEmpty()) return;
        world.execute(() -> {
            Store<EntityStore> store = world.getEntityStore().getStore();
            for (Hologram h : toCheck) {
                boolean needsRespawn = h.getLineEntityIds().isEmpty() && !h.getLines().isEmpty();
                if (!needsRespawn) {
                    for (UUID entityId : h.getLineEntityIds()) {
                        Ref<EntityStore> ref = ((EntityStore) store.getExternalData()).getRefFromUUID(entityId);
                        if (ref == null || !ref.isValid()) { needsRespawn = true; break; }
                    }
                }
                if (needsRespawn) {
                    h.clearLineEntityIds();
                    spawnHologram(h);
                }
            }
        });
    }

    public int cleanupOrphanedEntities() {
        AtomicInteger removed = new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(Universe.get().getWorlds().size());
        for (World world : Universe.get().getWorlds().values()) {
            world.execute(() -> {
                try {
                    Store<EntityStore> store = world.getEntityStore().getStore();
                    store.forEachEntityParallel((index, chunk, buf) -> {
                        try {
                            boolean hasNameplate = chunk.getArchetype().contains(Nameplate.getComponentType());
                            boolean hasProjectile = chunk.getArchetype().contains(ProjectileComponent.getComponentType());
                            boolean hasPersistentModel = chunk.getArchetype().contains(PersistentModel.getComponentType());
                            boolean hasIntangible = chunk.getArchetype().contains(Intangible.getComponentType());
                            boolean hasProp = chunk.getArchetype().contains(PropComponent.getComponentType());
                            if (!chunk.getArchetype().contains(UUIDComponent.getComponentType())) return;
                            UUIDComponent uuidComp = (UUIDComponent) chunk.getComponent(index, UUIDComponent.getComponentType());
                            if (uuidComp == null) return;
                            UUID entityUuid = uuidComp.getUuid();
                            boolean looksLikeHologram = (hasNameplate && hasProjectile) || (hasPersistentModel && hasIntangible) || (hasProp && hasIntangible);
                            if (looksLikeHologram && !isTracked(entityUuid)) {
                                buf.removeEntity(chunk.getReferenceTo(index), RemoveReason.REMOVE);
                                removed.incrementAndGet();
                            }
                        } catch (Exception ignored) {}
                    });
                } finally {
                    latch.countDown();
                }
            });
        }
        try { latch.await(5, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        return removed.get();
    }

    private boolean isTracked(@Nonnull UUID entityId) {
        for (Hologram h : holograms.values()) {
            if (h.getLineEntityIds().contains(entityId)) return true;
        }
        return false;
    }

    public void reload() {
        saveHolograms();
        removeAllHolograms();
        imageManager.reload();
        imageManager.pushAssetsToOnlinePlayers();
        loadHolograms();
        spawnAllHolograms();
    }

    public void saveHolograms() {
        Path file = plugin.getDataDirectory().resolve("holograms.json");
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"holograms\": [\n");
        List<Hologram> list = new ArrayList<>(holograms.values());
        for (int i = 0; i < list.size(); i++) {
            sb.append(toJson(list.get(i)));
            if (i + 1 < list.size()) sb.append(',');
            sb.append('\n');
        }
        sb.append("  ]\n}\n");
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, sb.toString(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("[Varyon-Holograms] Échec sauvegarde holograms: %s", e.getMessage());
        }
    }

    public void loadHolograms() {
        Path file = plugin.getDataDirectory().resolve("holograms.json");
        if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) return;
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            Matcher m = HOLO_PATTERN.matcher(json);
            while (m.find()) {
                Hologram h = fromJson(m.group(1));
                if (h != null) {
                    holograms.put(h.getId(), h);
                    byName.put(h.getName().toLowerCase(), h.getId());
                }
            }
            LOGGER.at(Level.INFO).log("[Varyon-Holograms] Chargé %s hologramme(s)", holograms.size());
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("[Varyon-Holograms] Échec chargement holograms: %s", e.getMessage());
        }
    }

    @Nullable
    public UUID resolveWorldId(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        PlayerRef pr = store.getComponent(ref, PlayerRef.getComponentType());
        return pr != null ? pr.getWorldUuid() : null;
    }

    @Nullable
    private World findWorld(@Nonnull UUID worldId) {
        for (World w : Universe.get().getWorlds().values()) {
            if (w.getWorldConfig().getUuid().equals(worldId)) return w;
        }
        return null;
    }

    @Nonnull
    public String formatText(@Nonnull String text) {
        return plugin.getPlaceholderIntegration().process(stripColorCodes(text));
    }

    @Nonnull
    private static String stripColorCodes(@Nonnull String text) {
        return text.replaceAll("(?i)[&§][0-9a-fk-orx]", "");
    }

    private static String toJson(@Nonnull Hologram h) {
        StringBuilder sb = new StringBuilder();
        sb.append("    {\n");
        sb.append("      \"id\": \"").append(h.getId()).append("\",\n");
        sb.append("      \"name\": \"").append(escJson(h.getName())).append("\",\n");
        sb.append("      \"worldId\": \"").append(h.getWorldId()).append("\",\n");
        sb.append("      \"x\": ").append(h.getPosition().x).append(",\n");
        sb.append("      \"y\": ").append(h.getPosition().y).append(",\n");
        sb.append("      \"z\": ").append(h.getPosition().z).append(",\n");
        sb.append("      \"lineSpacing\": ").append(h.getLineSpacing()).append(",\n");
        sb.append("      \"visible\": ").append(h.isVisible()).append(",\n");
        if (h.getCreatorId() != null) sb.append("      \"creatorId\": \"").append(h.getCreatorId()).append("\",\n");
        sb.append("      \"lines\": [");
        List<String> lines = h.getLines();
        for (int i = 0; i < lines.size(); i++) {
            sb.append("\"").append(escJson(lines.get(i))).append("\"");
            if (i + 1 < lines.size()) sb.append(", ");
        }
        sb.append("]\n    }");
        return sb.toString();
    }

    @Nullable
    private static Hologram fromJson(@Nonnull String body) {
        try {
            UUID id = UUID.fromString(extractStr(body, "id"));
            String name = extractStr(body, "name");
            UUID worldId = UUID.fromString(extractStr(body, "worldId"));
            double x = extractDouble(body, "x");
            double y = extractDouble(body, "y");
            double z = extractDouble(body, "z");
            double lineSpacing = extractDouble(body, "lineSpacing");
            boolean visible = extractBool(body, "visible");
            String creatorRaw = extractStrNullable(body, "creatorId");
            UUID creatorId = creatorRaw != null ? UUID.fromString(creatorRaw) : null;
            List<String> lines = extractStringArray(body, "lines");
            return new Hologram(id, name, new Vector3d(x, y, z), worldId, lines, lineSpacing, visible, creatorId);
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("[Varyon-Holograms] Erreur parsing hologram JSON: %s", e.getMessage());
            return null;
        }
    }

    private static String escJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    private static String extractStr(String json, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"").matcher(json);
        return m.find() ? unescJson(m.group(1)) : "";
    }

    @Nullable
    private static String extractStrNullable(String json, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"").matcher(json);
        return m.find() ? unescJson(m.group(1)) : null;
    }

    private static double extractDouble(String json, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?)").matcher(json);
        return m.find() ? Double.parseDouble(m.group(1)) : 0.0;
    }

    private static boolean extractBool(String json, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*(true|false)").matcher(json);
        return m.find() && Boolean.parseBoolean(m.group(1));
    }

    @Nonnull
    private static List<String> extractStringArray(String json, String key) {
        Matcher arrM = Pattern.compile("\"" + key + "\"\\s*:\\s*\\[([^\\]]*)\\]").matcher(json);
        List<String> result = new ArrayList<>();
        if (!arrM.find()) return result;
        String arr = arrM.group(1);
        Matcher itemM = Pattern.compile("\"((?:\\\\.|[^\"])*)\"").matcher(arr);
        while (itemM.find()) result.add(unescJson(itemM.group(1)));
        return result;
    }

    private static String unescJson(String s) {
        return s.replace("\\\"", "\"").replace("\\\\", "\\").replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t");
    }
}
