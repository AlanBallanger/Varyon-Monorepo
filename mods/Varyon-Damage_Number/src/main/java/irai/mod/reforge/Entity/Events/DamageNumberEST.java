package irai.mod.reforge.Entity.Events;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import com.hypixel.hytale.assetstore.map.IndexedLookupTableAssetMap;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.dependency.SystemGroupDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.protocol.CombatTextUpdate;
import com.hypixel.hytale.protocol.EntityUIType;
import com.hypixel.hytale.protocol.UIComponentsUpdate;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entity.tracker.EntityTrackerSystems.EntityViewer;
import com.hypixel.hytale.server.core.modules.entity.tracker.EntityTrackerSystems.Visible;
import com.hypixel.hytale.server.core.modules.entityui.EntityUIModule;
import com.hypixel.hytale.server.core.modules.entityui.UIComponentList;
import com.hypixel.hytale.server.core.modules.entityui.asset.EntityUIComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import irai.mod.DynamicFloatingDamageFormatter.DamageNumberMeta;
import irai.mod.DynamicFloatingDamageFormatter.DamageNumbers;

/**
 * Minimal adapter system for standalone usage.
 * When a kind defines {@code particleFont} in config, spawns FloatingDamage particle digits (per-kind colour).
 * Otherwise uses CombatText + UI swap. Runs BEFORE {@link DamageSystems.EntityUIEvents}.
 */
public class DamageNumberEST extends DamageEventSystem {
    private static final float NON_DOT_RANDOM_JITTER_DEGREES = 240f;

    public static final AtomicInteger DBG_HANDLE_CALLS   = new AtomicInteger();
    public static final AtomicInteger DBG_SKIP_AMOUNT0   = new AtomicInteger();
    public static final AtomicInteger DBG_SKIP_COMBAT_TXT= new AtomicInteger();
    public static final AtomicInteger DBG_SKIP_NO_COMP   = new AtomicInteger();
    public static final AtomicInteger DBG_SKIP_NO_VIEWER = new AtomicInteger();
    public static final AtomicInteger DBG_EMITTED        = new AtomicInteger();
    public static final AtomicInteger DBG_ZEROED         = new AtomicInteger();

    private volatile ComponentType<EntityStore, Visible> visibleComponentType;
    private volatile ComponentType<EntityStore, UIComponentList> uiComponentListComponentType;
    private final Query<EntityStore> query;
    private final Set<Dependency<EntityStore>> dependencies;

    public DamageNumberEST() {
        ComponentType<EntityStore, Visible> visibleType = null;
        ComponentType<EntityStore, UIComponentList> uiType = null;
        try {
            EntityModule entityModule = EntityModule.get();
            if (entityModule != null) {
                visibleType = entityModule.getVisibleComponentType();
            }
        } catch (Throwable ignored) {
            // Module may not be ready during plugin init.
        }
        try {
            EntityUIModule uiModule = EntityUIModule.get();
            if (uiModule != null) {
                uiType = uiModule.getUIComponentListType();
            }
        } catch (Throwable ignored) {
            // Module may not be ready during plugin init.
        }
        this.visibleComponentType = visibleType;
        this.uiComponentListComponentType = uiType;
        this.query = (visibleType != null && uiType != null) ? Query.and(visibleType, uiType) : Query.any();
        this.dependencies = buildDependencies();
    }

    private static Set<Dependency<EntityStore>> buildDependencies() {
        try {
            DamageModule dm = DamageModule.get();
            if (dm != null) {
                return Set.of(
                        new SystemGroupDependency<>(Order.AFTER, dm.getFilterDamageGroup()),
                        new SystemDependency<>(Order.BEFORE, DamageSystems.EntityUIEvents.class));
            }
        } catch (Throwable ignored) {
        }
        return Set.of(new SystemDependency<>(Order.BEFORE, DamageSystems.EntityUIEvents.class));
    }

    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getInspectDamageGroup();
    }

    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return dependencies;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Override
    public void handle(int index,
                       ArchetypeChunk<EntityStore> chunk,
                       Store<EntityStore> store,
                       CommandBuffer<EntityStore> commandBuffer,
                       Damage damage) {
        DBG_HANDLE_CALLS.incrementAndGet();
        if (damage == null) {
            return;
        }
        float rawAmount = damage.getAmount();
        if (rawAmount == 0f) {
            DBG_SKIP_AMOUNT0.incrementAndGet();
            return;
        }
        float displayAmount = Math.abs(rawAmount);
        if (displayAmount <= 0f) {
            DBG_SKIP_AMOUNT0.incrementAndGet();
            return;
        }
        if (DamageNumberMeta.shouldSkipCombatText(damage)) {
            DBG_SKIP_COMBAT_TXT.incrementAndGet();
            damage.setAmount(0f);
            return;
        }

        ensureComponentTypes();
        if (visibleComponentType == null || uiComponentListComponentType == null) {
            DBG_SKIP_NO_COMP.incrementAndGet();
            return;
        }

        Ref<EntityStore> targetRef = chunk.getReferenceTo(index);
        Visible visible = (Visible) commandBuffer.getComponent(targetRef, visibleComponentType);
        if (visible == null) {
            visible = store.getComponent(targetRef, visibleComponentType);
        }
        UIComponentList uiList = (UIComponentList) commandBuffer.getComponent(targetRef, uiComponentListComponentType);
        if (uiList == null) {
            uiList = store.getComponent(targetRef, uiComponentListComponentType);
        }
        if (visible == null || uiList == null) {
            DBG_SKIP_NO_COMP.incrementAndGet();
            return;
        }

        EntityViewer[] viewers = resolveViewers(visible);
        if (viewers.length == 0) {
            DBG_SKIP_NO_VIEWER.incrementAndGet();
            return;
        }

        String kindId = DamageNumbers.resolveKindId(damage);
        List<Ref<EntityStore>> viewerRefs = collectViewerRefs(visible);
        if (FloatingDamageParticles.trySpawn(store, commandBuffer, targetRef, displayAmount, kindId,
                viewerRefs, damage)) {
            DBG_EMITTED.incrementAndGet();
            if ("HEAL".equalsIgnoreCase(kindId)) {
                HealFloatCoordinator.markFromDamageEvent(targetRef);
            }
            damage.setAmount(0f);
            DBG_ZEROED.incrementAndGet();
            return;
        }

        float angle = resolveAngle(kindId);
        String text = DamageNumbers.format(displayAmount, kindId);
        CombatTextUpdate update = new CombatTextUpdate(angle, text);

        for (EntityViewer viewer : viewers) {
            if (viewer == null) {
                continue;
            }
            queueCombatTextComponentSwap(viewer, targetRef, uiList, kindId);
            viewer.queueUpdate(targetRef, update);
        }

        DBG_EMITTED.incrementAndGet();
        if ("HEAL".equalsIgnoreCase(kindId)) {
            HealFloatCoordinator.markFromDamageEvent(targetRef);
        }
        damage.setAmount(0f);
        DBG_ZEROED.incrementAndGet();
    }

    public static void queueCombatTextDirect(Store<EntityStore> store,
                                             Ref<EntityStore> targetRef,
                                             float amount,
                                             String kindId) {
        queueCombatTextDirect(store, null, targetRef, amount, kindId);
    }

    public static void queueCombatTextDirect(Store<EntityStore> store,
                                             @Nullable CommandBuffer<EntityStore> commandBuffer,
                                             Ref<EntityStore> targetRef,
                                             float amount,
                                             String kindId) {
        if (store == null || targetRef == null) {
            return;
        }
        float displayAmount = Math.abs(amount);
        if (displayAmount <= 0f) {
            return;
        }
        ComponentType<EntityStore, Visible> visibleType = null;
        ComponentType<EntityStore, UIComponentList> uiType = null;
        try {
            EntityModule entityModule = EntityModule.get();
            visibleType = entityModule == null ? null : entityModule.getVisibleComponentType();
        } catch (Throwable ignored) {
            visibleType = null;
        }
        try {
            EntityUIModule uiModule = EntityUIModule.get();
            uiType = uiModule == null ? null : uiModule.getUIComponentListType();
        } catch (Throwable ignored) {
            uiType = null;
        }
        if (visibleType == null || uiType == null) {
            return;
        }
        Visible visible = commandBuffer != null
                ? (Visible) commandBuffer.getComponent(targetRef, visibleType)
                : store.getComponent(targetRef, visibleType);
        if (visible == null) {
            visible = store.getComponent(targetRef, visibleType);
        }
        UIComponentList uiList = commandBuffer != null
                ? (UIComponentList) commandBuffer.getComponent(targetRef, uiType)
                : store.getComponent(targetRef, uiType);
        if (uiList == null) {
            uiList = store.getComponent(targetRef, uiType);
        }
        if (visible == null || uiList == null) {
            return;
        }

        EntityViewer[] viewers = resolveViewers(visible);
        if (viewers.length == 0) {
            return;
        }
        String resolvedKind = (kindId == null || kindId.isBlank()) ? "FLAT" : kindId;
        List<Ref<EntityStore>> viewerRefs = collectViewerRefs(visible);
        if (FloatingDamageParticles.trySpawn(store, commandBuffer, targetRef, displayAmount, resolvedKind, viewerRefs, null)) {
            if ("HEAL".equalsIgnoreCase(resolvedKind)) {
                HealFloatCoordinator.markFromDamageEvent(targetRef);
            }
            return;
        }

        float angle = resolveAngle(resolvedKind);
        String text = DamageNumbers.format(displayAmount, resolvedKind);
        CombatTextUpdate update = new CombatTextUpdate(angle, text);

        for (EntityViewer viewer : viewers) {
            if (viewer == null) {
                continue;
            }
            queueCombatTextComponentSwap(viewer, targetRef, uiList, resolvedKind);
            viewer.queueUpdate(targetRef, update);
        }
        if ("HEAL".equalsIgnoreCase(resolvedKind)) {
            HealFloatCoordinator.markFromDamageEvent(targetRef);
        }
    }

    public static void queueCombatTextDirect(Store<EntityStore> store,
                                             Ref<EntityStore> targetRef,
                                             float amount,
                                             DamageNumbers.KindStyle kindStyle) {
        String kindId = kindStyle == null ? null : kindStyle.id();
        queueCombatTextDirect(store, targetRef, amount, kindId);
    }

    private static float resolveAngle(String kindId) {
        float angle;
        if (DamageNumbers.isDotKind(kindId)) {
            angle = (ThreadLocalRandom.current().nextFloat() * 360f) - 180f;
        } else {
            angle = (ThreadLocalRandom.current().nextFloat() - 0.5f) * NON_DOT_RANDOM_JITTER_DEGREES;
        }
        return normalizeAngle(angle);
    }

    private static float normalizeAngle(float angle) {
        if (angle > 180f) {
            return angle - 360f;
        }
        if (angle < -180f) {
            return angle + 360f;
        }
        return angle;
    }

    private static void queueCombatTextComponentSwap(EntityViewer viewer,
                                                     Ref<EntityStore> targetRef,
                                                     UIComponentList uiList,
                                                     String kindId) {
        if (viewer == null || uiList == null) {
            return;
        }
        int[] baseComponentIds = uiList.getComponentIds();
        if (baseComponentIds == null || baseComponentIds.length == 0) {
            return;
        }
        IndexedLookupTableAssetMap<String, EntityUIComponent> assetMap = EntityUIComponent.getAssetMap();
        if (assetMap == null) {
            return;
        }
        int desiredIndex = resolveDesiredCombatTextIndex(assetMap, kindId);
        int[] componentIds = buildSingleCombatTextList(baseComponentIds, assetMap, desiredIndex);
        if (componentIds == null || componentIds.length == 0) {
            return;
        }
        viewer.queueUpdate(targetRef, new UIComponentsUpdate(componentIds));
    }

    private static int resolveDesiredCombatTextIndex(IndexedLookupTableAssetMap<String, EntityUIComponent> assetMap,
                                                     String kindId) {
        if (kindId == null || kindId.isBlank()) {
            return -1;
        }
        DamageNumbers.KindStyle style = DamageNumbers.getKindStyle(kindId);
        String desiredId = style == null ? null : style.uiComponentId();
        String altId = style == null ? null : style.uiComponentAltId();
        if (altId != null && !altId.isBlank()) {
            String chosen = DamageNumbers.resolveUiComponentId(kindId, true);
            int index = chosen == null || chosen.isBlank() ? -1 : assetMap.getIndexOrDefault(chosen, -1);
            if (index >= 0) {
                return index;
            }
            return desiredId == null || desiredId.isBlank()
                    ? -1
                    : assetMap.getIndexOrDefault(desiredId, -1);
        }
        if (desiredId == null || desiredId.isBlank()) {
            return -1;
        }
        return assetMap.getIndexOrDefault(desiredId, -1);
    }

    private static int[] buildSingleCombatTextList(int[] baseComponentIds,
                                                   IndexedLookupTableAssetMap<String, EntityUIComponent> assetMap,
                                                   int desiredIndex) {
        int[] filtered = new int[baseComponentIds.length + (desiredIndex >= 0 ? 1 : 0)];
        int count = 0;
        boolean insertedCombatText = false;
        boolean sawCombatText = false;
        for (int id : baseComponentIds) {
            if (id < 0) {
                continue;
            }
            EntityUIComponent component = assetMap.getAsset(id);
            if (component == null) {
                filtered[count++] = id;
                continue;
            }
            EntityUIType type;
            try {
                type = component.toPacket().type;
            } catch (Throwable ignored) {
                filtered[count++] = id;
                continue;
            }
            if (type == EntityUIType.CombatText) {
                sawCombatText = true;
                if (!insertedCombatText) {
                    filtered[count++] = desiredIndex >= 0 ? desiredIndex : id;
                    insertedCombatText = true;
                }
            } else {
                filtered[count++] = id;
            }
        }
        if (!sawCombatText && desiredIndex >= 0) {
            filtered[count++] = desiredIndex;
        }
        if (count == 0) {
            return Arrays.copyOf(baseComponentIds, baseComponentIds.length);
        }
        return count == filtered.length ? filtered : Arrays.copyOf(filtered, count);
    }

    private static List<Ref<EntityStore>> collectViewerRefs(Visible visible) {
        if (visible == null) {
            return List.of();
        }
        List<Ref<EntityStore>> refs = refsFromViewerMap(visible.visibleTo);
        if (!refs.isEmpty()) {
            return refs;
        }
        refs = refsFromViewerMap(visible.newlyVisibleTo);
        if (!refs.isEmpty()) {
            return refs;
        }
        return refsFromViewerMap(visible.previousVisibleTo);
    }

    private static List<Ref<EntityStore>> refsFromViewerMap(Map<Ref<EntityStore>, EntityViewer> viewerMap) {
        if (viewerMap == null || viewerMap.isEmpty()) {
            return List.of();
        }
        List<Ref<EntityStore>> refs = new ArrayList<>(viewerMap.keySet());
        refs.removeIf(r -> r == null || !r.isValid());
        return refs;
    }

    private static EntityViewer[] resolveViewers(Visible visible) {
        if (visible == null) {
            return new EntityViewer[0];
        }
        EntityViewer[] viewers = collectViewers(visible.visibleTo);
        if (viewers.length == 0) {
            viewers = collectViewers(visible.newlyVisibleTo);
        }
        if (viewers.length == 0) {
            viewers = collectViewers(visible.previousVisibleTo);
        }
        return viewers;
    }

    private static EntityViewer[] collectViewers(java.util.Map<Ref<EntityStore>, EntityViewer> viewersMap) {
        if (viewersMap == null || viewersMap.isEmpty()) {
            return new EntityViewer[0];
        }
        EntityViewer[] viewers = new EntityViewer[viewersMap.size()];
        int count = 0;
        for (EntityViewer viewer : viewersMap.values()) {
            if (viewer == null) {
                continue;
            }
            viewers[count++] = viewer;
        }
        return count == viewers.length ? viewers : Arrays.copyOf(viewers, count);
    }

    private void ensureComponentTypes() {
        if (visibleComponentType == null) {
            try {
                EntityModule entityModule = EntityModule.get();
                if (entityModule != null) {
                    visibleComponentType = entityModule.getVisibleComponentType();
                }
            } catch (Throwable ignored) {
                // ignore
            }
        }
        if (uiComponentListComponentType == null) {
            try {
                EntityUIModule uiModule = EntityUIModule.get();
                if (uiModule != null) {
                    uiComponentListComponentType = uiModule.getUIComponentListType();
                }
            } catch (Throwable ignored) {
                // ignore
            }
        }
    }
}
