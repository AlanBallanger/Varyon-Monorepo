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
import fr.varyon.holograms.animation.HologramAnimGroup;
import fr.varyon.holograms.carousel.CarouselManager;
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
import java.util.Set;
import java.util.TreeSet;
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
    private final Set<String> groups = ConcurrentHashMap.newKeySet();
    private final ImageManager imageManager;
    private final BillboardManager billboardManager;
    private final CarouselManager carouselManager;
    private final Map<UUID, Map<UUID, Vector3d>> carouselBasePositions = new ConcurrentHashMap<>();
    private boolean spawned = false;

    public HologramManager(@Nonnull VaryonHologramsPlugin plugin) {
        this.plugin = plugin;
        this.imageManager = new ImageManager(plugin);
        this.billboardManager = new BillboardManager();
        this.carouselManager = new CarouselManager(plugin);
        imageManager.initialize();
        billboardManager.start();
        carouselManager.start();
        plugin.getAnimationManager().start();
    }

    public void shutdown() {
        carouselManager.stop();
        billboardManager.stop();
        plugin.getAnimationManager().stop();
        removeAllHolograms();
    }

    @Nonnull public ImageManager getImageManager() { return imageManager; }
    @Nonnull public BillboardManager getBillboardManager() { return billboardManager; }
    @Nonnull public CarouselManager getCarouselManager() { return carouselManager; }

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
        return createHologram(name, position, worldId, creatorId, null);
    }

    @Nonnull
    public Hologram createHologram(@Nonnull String name, @Nonnull Vector3d position,
                                    @Nonnull UUID worldId, @Nullable UUID creatorId,
                                    @Nullable String groupPath) {
        String validName = HologramNames.requireValid(name);
        if (hologramExists(validName)) {
            throw new IllegalArgumentException("Un hologramme nommé '" + validName + "' existe déjà.");
        }
        String group = HologramGroups.normalize(groupPath);
        if (group != null) {
            groups.add(group);
        }
        Hologram hologram = new Hologram(validName, position, worldId);
        hologram.setCreatorId(creatorId);
        hologram.setGroup(group);
        hologram.addLine("Nouvel hologramme");
        hologram.addLine("Editez les lignes ci-dessous");
        holograms.put(hologram.getId(), hologram);
        byName.put(validName.toLowerCase(), hologram.getId());
        spawnHologram(hologram);
        saveHolograms();
        return hologram;
    }

    public void renameHologram(@Nonnull String oldName, @Nonnull String newName) {
        String validName = HologramNames.requireValid(newName);
        UUID id = byName.get(oldName.toLowerCase());
        if (id == null) throw new IllegalArgumentException("Hologramme introuvable: " + oldName);
        if (hologramExists(validName) && !validName.equalsIgnoreCase(oldName)) {
            throw new IllegalArgumentException("Le nom '" + validName + "' est déjà utilisé.");
        }
        Hologram hologram = holograms.get(id);
        if (hologram == null) throw new IllegalArgumentException("Hologramme introuvable: " + oldName);
        byName.remove(oldName.toLowerCase());
        hologram.setName(validName);
        byName.put(validName.toLowerCase(), id);
        saveHolograms();
    }

    public void createGroup(@Nonnull String groupPath) {
        String group = HologramGroups.normalize(groupPath);
        if (group == null) {
            throw new IllegalArgumentException("Nom de groupe vide.");
        }
        groups.add(group);
        saveHolograms();
    }

    public void setHologramAnimation(@Nonnull String hologramName, @Nullable String animationName) {
        Hologram hologram = getHologram(hologramName);
        if (hologram == null) {
            throw new IllegalArgumentException("Hologramme introuvable: " + hologramName);
        }
        String animation = animationName == null || animationName.isBlank() ? null : animationName.trim();
        hologram.setAnimation(animation);
        updateHologram(hologram);
    }

    public void setHologramLayout(@Nonnull String hologramName, @Nonnull HologramLayout layout) {
        Hologram hologram = getHologram(hologramName);
        if (hologram == null) {
            throw new IllegalArgumentException("Hologramme introuvable: " + hologramName);
        }
        hologram.setLayout(layout);
        updateHologram(hologram);
    }

    public void setHologramBillboard(@Nonnull String hologramName, boolean billboard) {
        Hologram hologram = getHologram(hologramName);
        if (hologram == null) {
            throw new IllegalArgumentException("Hologramme introuvable: " + hologramName);
        }
        hologram.setBillboard(billboard);
        updateHologram(hologram);
    }

    public void setHologramFacing(@Nonnull String hologramName, @Nonnull HologramFacing facing) {
        Hologram hologram = getHologram(hologramName);
        if (hologram == null) {
            throw new IllegalArgumentException("Hologramme introuvable: " + hologramName);
        }
        hologram.setFacing(facing);
        updateHologram(hologram);
    }

    @Nonnull
    public String getWorldName(@Nonnull Hologram hologram) {
        World world = findWorld(hologram.getWorldId());
        if (world == null) return hologram.getWorldId().toString().substring(0, 8) + "…";
        String display = world.getWorldConfig().getDisplayName();
        if (display != null && !display.isBlank()) return display;
        String folderName = world.getName();
        return (folderName != null && !folderName.isBlank()) ? folderName : hologram.getWorldId().toString().substring(0, 8) + "…";
    }

    public void setHologramCarousel(@Nonnull String hologramName, boolean enabled,
                                     float intervalSeconds, @Nonnull CarouselTransition transition) {
        Hologram hologram = getHologram(hologramName);
        if (hologram == null) {
            throw new IllegalArgumentException("Hologramme introuvable: " + hologramName);
        }
        hologram.setCarouselEnabled(enabled);
        hologram.setCarouselIntervalSeconds(intervalSeconds);
        hologram.setCarouselTransition(transition);
        updateHologram(hologram);
    }

    public void setHologramGroup(@Nonnull String hologramName, @Nullable String groupPath) {
        Hologram hologram = getHologram(hologramName);
        if (hologram == null) {
            throw new IllegalArgumentException("Hologramme introuvable: " + hologramName);
        }
        String group = HologramGroups.normalize(groupPath);
        if (group != null) {
            groups.add(group);
        }
        hologram.setGroup(group);
        saveHolograms();
    }

    @Nonnull
    public List<String> getSortedGroupPaths() {
        TreeSet<String> all = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        all.addAll(groups);
        for (Hologram hologram : holograms.values()) {
            if (hologram.getGroup() != null && !hologram.getGroup().isBlank()) {
                all.add(hologram.getGroup());
            }
        }
        return new ArrayList<>(all);
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
        if (!hologram.isVisible()) { LOGGER.at(Level.FINE).log("[Varyon-Holograms] spawnHologram: %s non visible, skip", hologram.getName()); return; }
        World world = findWorld(hologram.getWorldId());
        if (world == null) { LOGGER.at(Level.WARNING).log("[Varyon-Holograms] spawnHologram: monde introuvable pour %s worldId=%s", hologram.getName(), hologram.getWorldId()); return; }

        LOGGER.at(Level.FINE).log("[Varyon-Holograms] spawnHologram: %s -> world=%s pos=%.1f,%.1f,%.1f", hologram.getName(), world.getName(), hologram.getPosition().x, hologram.getPosition().y, hologram.getPosition().z);
        world.execute(() -> {
            try {
                Vector3d pos = hologram.getPosition();
                int chunkX = (int) Math.floor(pos.x) >> 5;
                int chunkZ = (int) Math.floor(pos.z) >> 5;
                Ref<?> chunkRef = world.getChunkStore().getChunkReference(ChunkUtil.indexChunk(chunkX, chunkZ));
                if (chunkRef == null || !chunkRef.isValid()) { LOGGER.at(Level.FINE).log("[Varyon-Holograms] spawnHologram: chunk non chargé chunkX=%d chunkZ=%d pour %s, skip", chunkX, chunkZ, hologram.getName()); return; }

                hologram.clearLineEntityIds();
                carouselBasePositions.remove(hologram.getId());
                spawnActivePage(hologram, 0, new Vector3d());
                if (hologram.isCarouselActive()) {
                    carouselManager.register(hologram);
                } else {
                    carouselManager.unregister(hologram.getId());
                }
            } catch (Exception e) {
                LOGGER.at(Level.WARNING).log("[Varyon-Holograms] Erreur spawn hologram %s: %s", hologram.getName(), e.getMessage());
            }
        });
    }

    public void spawnActivePage(@Nonnull Hologram hologram, int pageIndex, @Nonnull Vector3d slideOffset) {
        spawnPageEntities(hologram, pageIndex, slideOffset, true);
    }

    @Nonnull
    public List<UUID> spawnPageEntities(@Nonnull Hologram hologram, int pageIndex,
                                         @Nonnull Vector3d slideOffset, boolean attachToHologram) {
        World world = findWorld(hologram.getWorldId());
        if (world == null) return List.of();

        Vector3d pos = hologram.getPosition();
        Vector3d anchor = new Vector3d(pos);
        HologramLayout layout = hologram.getLayout();
        Rotation3f spawnRotation = layout.spawnRotationWithFacing(hologram.getFacing());
        org.joml.Vector3f animRotation = layout.animBaseRotationDegrees();
        List<String> lines = hologram.getPageLines(pageIndex);
        List<UUID> entityIds = new ArrayList<>();
        List<HologramAnimGroup.Member> animMembers = new ArrayList<>();
        Map<UUID, Vector3d> basePositions = new ConcurrentHashMap<>();

        if (attachToHologram) {
            hologram.clearLineEntityIds();
        }

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            HologramLineType type = HologramLineType.fromLine(line);
            Vector3d lineOffset = layout.lineOffset(i, hologram.getLineSpacing(), type);
            Vector3d linePos = new Vector3d(
                pos.x + lineOffset.x + slideOffset.x,
                pos.y + lineOffset.y + slideOffset.y,
                pos.z + lineOffset.z + slideOffset.z);
            Vector3d centerPos = new Vector3d(
                pos.x + lineOffset.x,
                pos.y + lineOffset.y,
                pos.z + lineOffset.z);
            float lineScale = resolveLineScale(line, type);

            UUID entityId = switch (type) {
                case IMAGE -> spawnImageLine(linePos, line, hologram, world, spawnRotation, layout);
                case ITEM  -> spawnItemLine(linePos, line, world, spawnRotation);
                default    -> spawnTextLine(linePos, line, world, spawnRotation);
            };

            if (entityId != null) {
                entityIds.add(entityId);
                basePositions.put(entityId, centerPos);
                if (attachToHologram) {
                    hologram.addLineEntityId(entityId);
                }
                if (hologram.isBillboard()) {
                    billboardManager.register(entityId, hologram.getWorldId(), -1f);
                }
                animMembers.add(new HologramAnimGroup.Member(
                    entityId, lineOffset, animRotation, lineScale));
            }
        }

        carouselBasePositions.computeIfAbsent(hologram.getId(), k -> new ConcurrentHashMap<>())
            .putAll(basePositions);
        if (attachToHologram && !animMembers.isEmpty()) {
            registerHologramAnimation(hologram, anchor, animMembers);
        }
        return entityIds;
    }

    public void registerPageAnimation(@Nonnull Hologram hologram, int pageIndex) {
        Vector3d anchor = new Vector3d(hologram.getPosition());
        HologramLayout layout = hologram.getLayout();
        org.joml.Vector3f animRotation = layout.animBaseRotationDegrees();
        List<HologramAnimGroup.Member> animMembers = new ArrayList<>();
        List<String> lines = hologram.getPageLines(pageIndex);
        List<UUID> entityIds = hologram.getLineEntityIds();
        for (int i = 0; i < entityIds.size() && i < lines.size(); i++) {
            UUID entityId = entityIds.get(i);
            HologramLineType type = HologramLineType.fromLine(lines.get(i));
            Vector3d lineOffset = layout.lineOffset(i, hologram.getLineSpacing(), type);
            float lineScale = resolveLineScale(lines.get(i), type);
            animMembers.add(new HologramAnimGroup.Member(entityId, lineOffset, animRotation, lineScale));
        }
        if (!animMembers.isEmpty()) {
            registerHologramAnimation(hologram, anchor, animMembers);
        }
    }

    public void despawnActivePage(@Nonnull Hologram hologram) {
        despawnEntityIds(hologram, hologram.getLineEntityIds());
        hologram.clearLineEntityIds();
        carouselBasePositions.remove(hologram.getId());
        plugin.getAnimationManager().unregisterHologramAnimation(hologram.getId());
    }

    public void despawnEntityIds(@Nonnull Hologram hologram, @Nonnull List<UUID> entityIds) {
        World world = findWorld(hologram.getWorldId());
        if (world == null || entityIds.isEmpty()) return;
        entityIds.forEach(billboardManager::unregister);
        Runnable logic = () -> removeEntities(world, entityIds);
        if (world.isInThread()) logic.run();
        else world.execute(logic);
        Map<UUID, Vector3d> bases = carouselBasePositions.get(hologram.getId());
        if (bases != null) {
            entityIds.forEach(bases::remove);
        }
    }

    @Nonnull
    public Map<UUID, Vector3d> getCarouselBasePositions(@Nonnull UUID hologramId) {
        Map<UUID, Vector3d> bases = carouselBasePositions.get(hologramId);
        return bases != null ? bases : Map.of();
    }

    private static float resolveLineScale(@Nonnull String line, @Nonnull HologramLineType type) {
        return switch (type) {
            case IMAGE -> HologramLineType.parseImageLine(line).scale;
            case ITEM -> HologramLineType.parseItemLine(line).scale;
            default -> 1f;
        };
    }

    private void registerHologramAnimation(@Nonnull Hologram hologram, @Nonnull Vector3d anchor,
                                            @Nonnull List<HologramAnimGroup.Member> members) {
        String animName = hologram.getAnimation();
        if (animName == null || animName.isBlank() || members.isEmpty()) return;
        AnimationData animation = plugin.getAnimationRegistry().getAnimation(animName);
        if (animation == null) {
            LOGGER.at(Level.WARNING).log("[Varyon-Holograms] Animation inconnue: %s", animName);
            return;
        }
        plugin.getAnimationManager().registerHologramAnimation(
            hologram.getId(), hologram.getWorldId(), animation, anchor, members);
    }

    @Nullable
    private UUID spawnTextLine(@Nonnull Vector3d position, @Nonnull String text,
                                @Nonnull World world, @Nonnull Rotation3f rotation) {
        try {
            UUID entityUuid = UUID.randomUUID();
            Runnable logic = () -> {
                try {
                    Store<EntityStore> store = world.getEntityStore().getStore();
                    Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
                    holder.putComponent(TransformComponent.getComponentType(),
                        new TransformComponent(new org.joml.Vector3d(position), rotation));
                    ProjectileComponent proj = new ProjectileComponent("Projectile");
                    holder.putComponent(ProjectileComponent.getComponentType(), proj);
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
                                @Nonnull World world, @Nonnull Rotation3f rotation) {
        HologramLineType.ItemLineData data = HologramLineType.parseItemLine(line);
        UUID entityUuid = UUID.randomUUID();
        Runnable logic = () -> {
            try {
                Store<EntityStore> store = world.getEntityStore().getStore();
                Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
                holder.addComponent(TransformComponent.getComponentType(),
                    new TransformComponent(new org.joml.Vector3d(position), rotation));

                Item item = (Item) Item.getAssetMap().getAsset(data.itemId);
                if (item == null) {
                    ProjectileComponent proj = new ProjectileComponent("Projectile");
                    holder.putComponent(ProjectileComponent.getComponentType(), proj);
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
                                 @Nonnull Hologram hologram, @Nonnull World world,
                                 @Nonnull Rotation3f rotation, @Nonnull HologramLayout layout) {
        HologramLineType.ImageLineData data = HologramLineType.parseImageLine(line);
        if (data.imageName.isBlank()) {
            LOGGER.at(Level.WARNING).log("[Varyon-Holograms] Nom d'image vide dans la ligne: %s", line);
            return spawnTextLine(position, line, world, rotation);
        }

        Model model = imageManager.createImageModel(data.imageName, data.scale, data.billboard, data.doubleSided);
        if (model == null) {
            LOGGER.at(Level.WARNING).log("[Varyon-Holograms] Fallback texte pour image '%s' (billboard=%s)", data.imageName, data.billboard);
            return spawnTextLine(position, "[Image: " + data.imageName + "]", world, rotation);
        }

        UUID entityUuid = UUID.randomUUID();
        Runnable logic = () -> {
            try {
                Store<EntityStore> store = world.getEntityStore().getStore();
                Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
                holder.addComponent(TransformComponent.getComponentType(),
                    new TransformComponent(new org.joml.Vector3d(position), rotation));
                holder.addComponent(UUIDComponent.getComponentType(), new UUIDComponent(entityUuid));
                holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
                holder.addComponent(PersistentModel.getComponentType(),
                    new PersistentModel(new Model.ModelReference(model.getModelAssetId(), data.scale, null, true)));
                holder.addComponent(EntityScaleComponent.getComponentType(), new EntityScaleComponent(data.scale));
                holder.addComponent(PropComponent.getComponentType(), PropComponent.get());
                holder.addComponent(NetworkId.getComponentType(), new NetworkId(((EntityStore) store.getExternalData()).takeNextNetworkId()));
                holder.ensureComponent(EntityModule.get().getVisibleComponentType());
                holder.ensureComponent(EntityStore.REGISTRY.getNonSerializedComponentType());
                Ref<EntityStore> ref = store.addEntity(holder, AddReason.SPAWN);
                if (ref != null && layout == HologramLayout.WALL && !data.billboard) {
                    store.ensureComponent(ref, Frozen.getComponentType());
                }
                if (ref != null && layout == HologramLayout.WALL && data.billboard) {
                    billboardManager.register(entityUuid, hologram.getWorldId(), data.trackingDistance);
                }
                if (ref != null && layout == HologramLayout.FLOOR) {
                    store.ensureComponent(ref, Frozen.getComponentType());
                }
            } catch (Exception e) {
                LOGGER.at(Level.WARNING).log("[Varyon-Holograms] Erreur spawn ligne image: %s", e.getMessage());
            }
        };
        if (world.isInThread()) logic.run(); else world.execute(logic);
        return entityUuid;
    }

    private void despawnHologram(@Nonnull Hologram hologram) {
        World world = findWorld(hologram.getWorldId());
        List<UUID> entityIds = new ArrayList<>(hologram.getLineEntityIds());
        hologram.clearLineEntityIds();
        carouselManager.unregister(hologram.getId());
        carouselBasePositions.remove(hologram.getId());
        plugin.getAnimationManager().unregisterHologramAnimation(hologram.getId());
        entityIds.forEach(billboardManager::unregister);
        if (world != null && !entityIds.isEmpty()) {
            world.execute(() -> removeEntities(world, entityIds));
        }
    }

    private void despawnHologramSync(@Nonnull Hologram hologram) {
        World world = findWorld(hologram.getWorldId());
        List<UUID> entityIds = new ArrayList<>(hologram.getLineEntityIds());
        hologram.clearLineEntityIds();
        carouselManager.unregister(hologram.getId());
        carouselBasePositions.remove(hologram.getId());
        plugin.getAnimationManager().unregisterHologramAnimation(hologram.getId());
        entityIds.forEach(billboardManager::unregister);
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
        groups.clear();
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
                boolean hasContent = false;
                for (int p = 0; p < h.getPageCount(); p++) {
                    if (!h.getPageLines(p).isEmpty()) { hasContent = true; break; }
                }
                boolean needsRespawn = h.getLineEntityIds().isEmpty() && hasContent;
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
        sb.append("{\n  \"groups\": [");
        List<String> groupList = getSortedGroupPaths();
        for (int i = 0; i < groupList.size(); i++) {
            sb.append("\"").append(escJson(groupList.get(i))).append("\"");
            if (i + 1 < groupList.size()) sb.append(", ");
        }
        sb.append("],\n  \"holograms\": [\n");
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
            groups.clear();
            groups.addAll(extractStringArray(json, "groups"));
            Matcher m = HOLO_PATTERN.matcher(json);
            while (m.find()) {
                Hologram h = fromJson(m.group(1));
                if (h != null) {
                    migrateAnimation(h);
                    holograms.put(h.getId(), h);
                    byName.put(h.getName().toLowerCase(), h.getId());
                }
            }
            LOGGER.at(Level.INFO).log("[Varyon-Holograms] Chargé %s hologramme(s)", holograms.size());
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("[Varyon-Holograms] Échec chargement holograms: %s", e.getMessage());
        }
    }

    private static void migrateAnimation(@Nonnull Hologram hologram) {
        if (hologram.getAnimation() == null || hologram.getAnimation().isBlank()) {
            for (int p = 0; p < hologram.getPageCount(); p++) {
                for (String line : hologram.getPageLines(p)) {
                    String anim = HologramLineType.extractAnimationName(line);
                    if (anim != null && !anim.isBlank()) {
                        hologram.setAnimation(anim);
                        break;
                    }
                }
                if (hologram.getAnimation() != null && !hologram.getAnimation().isBlank()) break;
            }
        }
        for (int p = 0; p < hologram.getPageCount(); p++) {
            List<String> cleaned = new ArrayList<>();
            for (String line : hologram.getPageLines(p)) {
                cleaned.add(HologramLineType.stripAnimation(line));
            }
            hologram.setPageLines(p, cleaned);
        }
    }

    @Nullable
    public UUID resolveWorldId(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        PlayerRef pr = store.getComponent(ref, PlayerRef.getComponentType());
        return pr != null ? pr.getWorldUuid() : null;
    }

    @Nullable
    private World findWorld(@Nonnull UUID worldId) {
        return Universe.get().getWorld(worldId);
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
        sb.append("      \"visible\": ").append(h.isVisible());
        if (h.getCreatorId() != null) sb.append(",\n      \"creatorId\": \"").append(h.getCreatorId()).append("\"");
        if (h.getGroup() != null) sb.append(",\n      \"group\": \"").append(escJson(h.getGroup())).append("\"");
        if (h.getAnimation() != null) sb.append(",\n      \"animation\": \"").append(escJson(h.getAnimation())).append("\"");
        if (h.getLayout() != HologramLayout.WALL) {
            sb.append(",\n      \"layout\": \"").append(h.getLayout().jsonValue()).append("\"");
        }
        if (h.getFacing() != HologramFacing.NORTH) {
            sb.append(",\n      \"facing\": \"").append(h.getFacing().jsonValue()).append("\"");
        }
        if (h.isBillboard()) {
            sb.append(",\n      \"billboard\": true");
        }
        if (h.getPageCount() > 1 || h.isCarouselEnabled()) {
            sb.append(",\n      \"pages\": [\n");
            for (int p = 0; p < h.getPageCount(); p++) {
                sb.append("        [");
                appendLinesJson(sb, h.getPageLines(p));
                sb.append("]");
                if (p + 1 < h.getPageCount()) sb.append(',');
                sb.append('\n');
            }
            sb.append("      ]");
            if (h.isCarouselEnabled()) {
                sb.append(",\n      \"carouselEnabled\": true");
                sb.append(",\n      \"carouselIntervalSeconds\": ").append(h.getCarouselIntervalSeconds());
                sb.append(",\n      \"carouselTransition\": \"").append(h.getCarouselTransition().jsonValue()).append("\"");
            }
        } else {
            sb.append(",\n      \"lines\": [");
            appendLinesJson(sb, h.getLines());
            sb.append(']');
        }
        sb.append("\n    }");
        return sb.toString();
    }

    private static void appendLinesJson(@Nonnull StringBuilder sb, @Nonnull List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            sb.append("\"").append(escJson(lines.get(i))).append("\"");
            if (i + 1 < lines.size()) sb.append(", ");
        }
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
            String group = HologramGroups.normalize(extractStrNullable(body, "group"));
            String animation = extractStrNullable(body, "animation");
            if ("none".equalsIgnoreCase(animation)) animation = null;
            HologramLayout layout = HologramLayout.parse(extractStrNullable(body, "layout"));
            HologramFacing facing = HologramFacing.parse(extractStrNullable(body, "facing"));
            boolean billboard = extractBool(body, "billboard");
            List<List<String>> pages = extractPages(body);
            List<String> lines = extractStringArray(body, "lines");
            if (pages.isEmpty() && !lines.isEmpty()) {
                pages = new ArrayList<>();
                pages.add(lines);
            }
            if (pages.isEmpty()) {
                pages = new ArrayList<>();
                pages.add(new ArrayList<>());
            }
            boolean carouselEnabled = extractBool(body, "carouselEnabled");
            float carouselInterval = (float) extractDouble(body, "carouselIntervalSeconds");
            if (carouselInterval <= 0f) carouselInterval = 5f;
            CarouselTransition carouselTransition = CarouselTransition.parse(extractStrNullable(body, "carouselTransition"));
            return new Hologram(id, name, new Vector3d(x, y, z), worldId, pages, lineSpacing, visible, creatorId,
                group, animation, layout, facing, billboard, carouselEnabled, carouselInterval, carouselTransition);
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
    private static List<List<String>> extractPages(@Nonnull String json) {
        int keyIdx = json.indexOf("\"pages\"");
        if (keyIdx < 0) return List.of();
        int start = json.indexOf('[', keyIdx);
        if (start < 0) return List.of();
        int depth = 0;
        int end = -1;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') {
                depth--;
                if (depth == 0) { end = i; break; }
            }
        }
        if (end < 0) return List.of();
        String content = json.substring(start + 1, end);
        List<List<String>> pages = new ArrayList<>();
        Matcher innerM = Pattern.compile("\\[([^\\]]*)\\]").matcher(content);
        while (innerM.find()) {
            List<String> lines = new ArrayList<>();
            Matcher itemM = Pattern.compile("\"((?:\\\\.|[^\"])*)\"").matcher(innerM.group(1));
            while (itemM.find()) lines.add(unescJson(itemM.group(1)));
            pages.add(lines);
        }
        return pages;
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
