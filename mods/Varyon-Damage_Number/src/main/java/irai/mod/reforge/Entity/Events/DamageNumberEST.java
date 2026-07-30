package irai.mod.reforge.Entity.Events;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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

import fr.varyon.damagenumber.DamageNumberDisplaySettings;
import irai.mod.DynamicFloatingDamageFormatter.DamageNumberMeta;
import irai.mod.DynamicFloatingDamageFormatter.DamageNumbers;

/**
 * Minimal adapter system for standalone usage.
 * When a kind defines {@code particleFont} in config, spawns FloatingDamage particle digits (per-kind colour).
 * Otherwise uses CombatText + UI swap. Runs BEFORE {@link DamageSystems.EntityUIEvents}.
 */
public class DamageNumberEST extends DamageEventSystem {
    private static final float NON_DOT_RANDOM_JITTER_DEGREES = 240f;
    private static final String[] MOD_COMBAT_TEXT_COMPONENT_IDS = {
        "CombatText_Flat",
        "CombatText_Critical",
        "CombatText_Ice",
        "CombatText_Ice_Critical",
        "CombatText_Burn",
        "CombatText_Burn_Alt",
        "CombatText_Bleed",
        "CombatText_Bleed_Alt",
        "CombatText_Poison",
        "CombatText_Poison_Alt",
        "CombatText_Shock",
        "CombatText_Water",
        "CombatText_Void",
        "CombatText_Heal",
    };

    public static final AtomicInteger DBG_HANDLE_CALLS   = new AtomicInteger();
    public static final AtomicInteger DBG_SKIP_AMOUNT0   = new AtomicInteger();
    public static final AtomicInteger DBG_SKIP_COMBAT_TXT= new AtomicInteger();
    public static final AtomicInteger DBG_SKIP_NO_COMP   = new AtomicInteger();
    public static final AtomicInteger DBG_SKIP_NO_VIEWER = new AtomicInteger();
    public static final AtomicInteger DBG_EMITTED        = new AtomicInteger();
    public static final AtomicInteger DBG_ZEROED         = new AtomicInteger();

    private static final boolean DEBUG_ENABLED = Boolean.getBoolean("varyon.damagenumbers.debug");

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
        if (DEBUG_ENABLED) {
            DBG_HANDLE_CALLS.incrementAndGet();
        }
        if (damage == null) {
            return;
        }
        float rawAmount = damage.getAmount();
        if (rawAmount == 0f) {
            if (DEBUG_ENABLED) {
                DBG_SKIP_AMOUNT0.incrementAndGet();
            }
            return;
        }
        float displayAmount = Math.abs(rawAmount);
        if (displayAmount <= 0f) {
            if (DEBUG_ENABLED) {
                DBG_SKIP_AMOUNT0.incrementAndGet();
            }
            return;
        }
        if (DamageNumberMeta.shouldSkipCombatText(damage)) {
            if (DEBUG_ENABLED) {
                DBG_SKIP_COMBAT_TXT.incrementAndGet();
            }
            damage.setAmount(0f);
            return;
        }

        ensureComponentTypes();
        if (visibleComponentType == null || uiComponentListComponentType == null) {
            if (DEBUG_ENABLED) {
                DBG_SKIP_NO_COMP.incrementAndGet();
            }
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
            if (DEBUG_ENABLED) {
                DBG_SKIP_NO_COMP.incrementAndGet();
            }
            return;
        }

        restoreDisabledViewerUi(store, commandBuffer, visible, targetRef, uiList);

        Ref<EntityStore> attackerRef = resolveAttackerRef(damage);
        UUID attackerUuid = DamageNumberDisplaySettings.resolvePlayerUuid(store, commandBuffer, attackerRef);
        if (attackerUuid == null) {
            attackerUuid = DamageNumberDisplaySettings.resolveAttackerUuid(store, commandBuffer, damage);
        }
        if (attackerUuid != null && !DamageNumberDisplaySettings.isEnabled(attackerUuid)) {
            restoreViewerUiIfPresent(store, commandBuffer, visible, targetRef, uiList, attackerRef);
            if (DEBUG_ENABLED) {
                DBG_SKIP_NO_VIEWER.incrementAndGet();
            }
            return;
        }

        List<Map.Entry<Ref<EntityStore>, EntityViewer>> enabledViewers =
            collectEnabledViewerEntries(store, commandBuffer, visible);
        if (enabledViewers.isEmpty()) {
            if (DEBUG_ENABLED) {
                DBG_SKIP_NO_VIEWER.incrementAndGet();
            }
            return;
        }

        String kindId = DamageNumbers.resolveKindId(damage);
        if (DEBUG_ENABLED) {
            System.out.println("[DmgNum] amt=" + displayAmount
                    + " kind=" + kindId
                    + " crit=" + DamageNumberMeta.isCritical(damage)
                    + " impactCrit=" + DamageNumberMeta.inferCriticalFromImpactVfx(damage));
        }
        List<Ref<EntityStore>> viewerRefs = enabledViewers.stream().map(Map.Entry::getKey).toList();
        if (FloatingDamageParticles.trySpawn(store, commandBuffer, targetRef, displayAmount, kindId,
                viewerRefs, damage)) {
            if (DEBUG_ENABLED) {
                DBG_EMITTED.incrementAndGet();
            }
            if ("HEAL".equalsIgnoreCase(kindId)) {
                HealFloatCoordinator.markFromDamageEvent(targetRef);
            }
            DamageNumberMeta.markSkipCombatText(damage);
            damage.setAmount(0f);
            if (DEBUG_ENABLED) {
                DBG_ZEROED.incrementAndGet();
            }
            return;
        }

        float angle = resolveAngle(kindId);
        String text = DamageNumbers.format(displayAmount, kindId);
        CombatTextUpdate update = new CombatTextUpdate(angle, text);

        for (Map.Entry<Ref<EntityStore>, EntityViewer> entry : enabledViewers) {
            EntityViewer viewer = entry.getValue();
            if (viewer == null) {
                continue;
            }
            queueCombatTextComponentSwap(viewer, targetRef, uiList, kindId);
            viewer.queueUpdate(targetRef, update);
        }

        if (DEBUG_ENABLED) {
            DBG_EMITTED.incrementAndGet();
        }
        if ("HEAL".equalsIgnoreCase(kindId)) {
            HealFloatCoordinator.markFromDamageEvent(targetRef);
        }
        DamageNumberMeta.markSkipCombatText(damage);
        damage.setAmount(0f);
        if (DEBUG_ENABLED) {
            DBG_ZEROED.incrementAndGet();
        }
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
        queueCombatTextDirect(store, commandBuffer, targetRef, amount, kindId, null);
    }

    public static void queueCombatTextDirect(Store<EntityStore> store,
                                             @Nullable CommandBuffer<EntityStore> commandBuffer,
                                             Ref<EntityStore> targetRef,
                                             float amount,
                                             String kindId,
                                             @Nullable Damage damage) {
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

        restoreDisabledViewerUi(store, commandBuffer, visible, targetRef, uiList);

        Ref<EntityStore> attackerRef = damage == null ? null : resolveAttackerRef(damage);
        UUID attackerUuid = DamageNumberDisplaySettings.resolvePlayerUuid(store, commandBuffer, attackerRef);
        if (attackerUuid == null && damage != null) {
            attackerUuid = DamageNumberDisplaySettings.resolveAttackerUuid(store, commandBuffer, damage);
        }
        if (attackerUuid != null && !DamageNumberDisplaySettings.isEnabled(attackerUuid)) {
            restoreViewerUiIfPresent(store, commandBuffer, visible, targetRef, uiList, attackerRef);
            return;
        }

        List<Map.Entry<Ref<EntityStore>, EntityViewer>> enabledViewers =
            collectEnabledViewerEntries(store, commandBuffer, visible);
        if (enabledViewers.isEmpty()) {
            return;
        }
        String resolvedKind = (kindId == null || kindId.isBlank()) ? "FLAT" : kindId;
        List<Ref<EntityStore>> viewerRefs = enabledViewers.stream().map(Map.Entry::getKey).toList();
        if (FloatingDamageParticles.trySpawn(store, commandBuffer, targetRef, displayAmount, resolvedKind, viewerRefs, null)) {
            if ("HEAL".equalsIgnoreCase(resolvedKind)) {
                HealFloatCoordinator.markFromDamageEvent(targetRef);
            }
            return;
        }

        float angle = resolveAngle(resolvedKind);
        String text = DamageNumbers.format(displayAmount, resolvedKind);
        CombatTextUpdate update = new CombatTextUpdate(angle, text);

        for (Map.Entry<Ref<EntityStore>, EntityViewer> entry : enabledViewers) {
            EntityViewer viewer = entry.getValue();
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

    private static void restoreDisabledViewerUi(Store<EntityStore> store,
                                                @Nullable CommandBuffer<EntityStore> commandBuffer,
                                                Visible visible,
                                                Ref<EntityStore> targetRef,
                                                UIComponentList uiList) {
        for (Map.Entry<Ref<EntityStore>, EntityViewer> entry : collectAllPlayerViewerEntries(store, commandBuffer, visible)) {
            Ref<EntityStore> viewerRef = entry.getKey();
            if (DamageNumberDisplaySettings.isViewerEnabled(store, commandBuffer, viewerRef)) {
                continue;
            }
            restoreBaseUiComponents(entry.getValue(), targetRef, uiList);
        }
    }

    private static void restoreBaseUiComponents(EntityViewer viewer,
                                                Ref<EntityStore> targetRef,
                                                UIComponentList uiList) {
        if (viewer == null || uiList == null) {
            return;
        }
        int[] baseComponentIds = uiList.getComponentIds();
        if (baseComponentIds == null || baseComponentIds.length == 0) {
            return;
        }
        IndexedLookupTableAssetMap<String, EntityUIComponent> assetMap = EntityUIComponent.getAssetMap();
        if (assetMap == null) {
            viewer.queueUpdate(targetRef, new UIComponentsUpdate(Arrays.copyOf(baseComponentIds, baseComponentIds.length)));
            return;
        }
        int[] restored = buildVanillaCombatTextList(baseComponentIds, assetMap);
        viewer.queueUpdate(targetRef, new UIComponentsUpdate(restored));
    }

    @Nullable
    private static Ref<EntityStore> resolveAttackerRef(@Nullable Damage damage) {
        if (damage == null) {
            return null;
        }
        Damage.Source source = damage.getSource();
        if (!(source instanceof Damage.EntitySource entitySource)) {
            return null;
        }
        Ref<EntityStore> attackerRef = entitySource.getRef();
        if (attackerRef == null || !attackerRef.isValid()) {
            return null;
        }
        return attackerRef;
    }

    private static void restoreViewerUiIfPresent(Store<EntityStore> store,
                                                   @Nullable CommandBuffer<EntityStore> commandBuffer,
                                                   Visible visible,
                                                   Ref<EntityStore> targetRef,
                                                   UIComponentList uiList,
                                                   @Nullable Ref<EntityStore> viewerRef) {
        if (viewerRef == null || !viewerRef.isValid()) {
            return;
        }
        if (DamageNumberDisplaySettings.isViewerEnabled(store, commandBuffer, viewerRef)) {
            return;
        }
        EntityViewer viewer = findViewer(visible, viewerRef);
        if (viewer != null) {
            restoreBaseUiComponents(viewer, targetRef, uiList);
        }
    }

    @Nullable
    private static EntityViewer findViewer(@Nullable Visible visible, Ref<EntityStore> viewerRef) {
        if (visible == null || viewerRef == null) {
            return null;
        }
        EntityViewer viewer = lookupViewer(visible.visibleTo, viewerRef);
        if (viewer != null) {
            return viewer;
        }
        viewer = lookupViewer(visible.newlyVisibleTo, viewerRef);
        if (viewer != null) {
            return viewer;
        }
        return lookupViewer(visible.previousVisibleTo, viewerRef);
    }

    @Nullable
    private static EntityViewer lookupViewer(@Nullable Map<Ref<EntityStore>, EntityViewer> viewerMap,
                                             Ref<EntityStore> viewerRef) {
        if (viewerMap == null || viewerMap.isEmpty()) {
            return null;
        }
        for (Map.Entry<Ref<EntityStore>, EntityViewer> entry : viewerMap.entrySet()) {
            Ref<EntityStore> ref = entry.getKey();
            if (ref != null && ref.equals(viewerRef)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static int[] buildVanillaCombatTextList(int[] baseComponentIds,
                                                    IndexedLookupTableAssetMap<String, EntityUIComponent> assetMap) {
        int[] filtered = new int[baseComponentIds.length];
        int count = 0;
        boolean keptCombatText = false;
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
                if (isModCombatTextIndex(assetMap, id)) {
                    continue;
                }
                if (keptCombatText) {
                    continue;
                }
                keptCombatText = true;
            }
            filtered[count++] = id;
        }
        if (count == 0) {
            return Arrays.copyOf(baseComponentIds, baseComponentIds.length);
        }
        return count == filtered.length ? filtered : Arrays.copyOf(filtered, count);
    }

    private static boolean isModCombatTextIndex(IndexedLookupTableAssetMap<String, EntityUIComponent> assetMap,
                                                int index) {
        for (String modId : MOD_COMBAT_TEXT_COMPONENT_IDS) {
            if (assetMap.getIndexOrDefault(modId, -1) == index) {
                return true;
            }
        }
        return false;
    }

    private static List<Map.Entry<Ref<EntityStore>, EntityViewer>> collectAllPlayerViewerEntries(
            Store<EntityStore> store,
            @Nullable CommandBuffer<EntityStore> commandBuffer,
            Visible visible) {
        if (visible == null) {
            return List.of();
        }
        List<Map.Entry<Ref<EntityStore>, EntityViewer>> entries =
            playerViewerEntriesFromMap(store, commandBuffer, visible.visibleTo);
        if (!entries.isEmpty()) {
            return entries;
        }
        entries = playerViewerEntriesFromMap(store, commandBuffer, visible.newlyVisibleTo);
        if (!entries.isEmpty()) {
            return entries;
        }
        return playerViewerEntriesFromMap(store, commandBuffer, visible.previousVisibleTo);
    }

    private static List<Map.Entry<Ref<EntityStore>, EntityViewer>> collectEnabledViewerEntries(
            Store<EntityStore> store,
            @Nullable CommandBuffer<EntityStore> commandBuffer,
            Visible visible) {
        if (visible == null) {
            return List.of();
        }
        List<Map.Entry<Ref<EntityStore>, EntityViewer>> entries =
            enabledViewerEntriesFromMap(store, commandBuffer, visible.visibleTo);
        if (!entries.isEmpty()) {
            return entries;
        }
        entries = enabledViewerEntriesFromMap(store, commandBuffer, visible.newlyVisibleTo);
        if (!entries.isEmpty()) {
            return entries;
        }
        return enabledViewerEntriesFromMap(store, commandBuffer, visible.previousVisibleTo);
    }

    private static List<Map.Entry<Ref<EntityStore>, EntityViewer>> playerViewerEntriesFromMap(
            Store<EntityStore> store,
            @Nullable CommandBuffer<EntityStore> commandBuffer,
            Map<Ref<EntityStore>, EntityViewer> viewerMap) {
        if (viewerMap == null || viewerMap.isEmpty()) {
            return List.of();
        }
        List<Map.Entry<Ref<EntityStore>, EntityViewer>> entries = new ArrayList<>(viewerMap.size());
        for (Map.Entry<Ref<EntityStore>, EntityViewer> entry : viewerMap.entrySet()) {
            Ref<EntityStore> viewerRef = entry.getKey();
            EntityViewer viewer = entry.getValue();
            if (viewerRef == null || !viewerRef.isValid() || viewer == null) {
                continue;
            }
            if (DamageNumberDisplaySettings.resolvePlayerUuid(store, commandBuffer, viewerRef) == null) {
                continue;
            }
            entries.add(entry);
        }
        return entries;
    }

    private static List<Map.Entry<Ref<EntityStore>, EntityViewer>> enabledViewerEntriesFromMap(
            Store<EntityStore> store,
            @Nullable CommandBuffer<EntityStore> commandBuffer,
            Map<Ref<EntityStore>, EntityViewer> viewerMap) {
        if (viewerMap == null || viewerMap.isEmpty()) {
            return List.of();
        }
        List<Map.Entry<Ref<EntityStore>, EntityViewer>> entries = new ArrayList<>(viewerMap.size());
        for (Map.Entry<Ref<EntityStore>, EntityViewer> entry : viewerMap.entrySet()) {
            Ref<EntityStore> viewerRef = entry.getKey();
            EntityViewer viewer = entry.getValue();
            if (viewerRef == null || !viewerRef.isValid() || viewer == null) {
                continue;
            }
            if (!DamageNumberDisplaySettings.isViewerEnabled(store, commandBuffer, viewerRef)) {
                continue;
            }
            entries.add(entry);
        }
        return entries;
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
