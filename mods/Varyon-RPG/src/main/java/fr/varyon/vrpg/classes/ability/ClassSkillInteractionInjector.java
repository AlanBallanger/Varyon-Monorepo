package fr.varyon.vrpg.classes.ability;

import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.UnarmedInteractions;
import fr.varyon.vrpg.VaryonRpgPlugin;
import fr.varyon.vrpg.classes.WeaponCategory;
import java.lang.reflect.Field;
import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public final class ClassSkillInteractionInjector {

    static final String TRIGGER_ABILITY_1 = "Vrpg_Trigger_Ability1";
    static final String TRIGGER_ABILITY_2 = "Vrpg_Trigger_Ability2";
    static final String TRIGGER_ABILITY_3 = "Vrpg_Trigger_Ability3";

    private static final AtomicBoolean TRIGGERS_REGISTERED = new AtomicBoolean(false);

    private ClassSkillInteractionInjector() {}

    public static void register() {
        HytaleServer.get().getEventBus().register(LoadedAssetsEvent.class, Item.class,
            ClassSkillInteractionInjector::onItemsLoaded);
        injectLoadedItems();
    }

    public static void injectLoadedItems() {
        if (!ensureTriggerAssets()) return;
        injectItems();
        injectUnarmedEmpty();
    }

    private static void onItemsLoaded(LoadedAssetsEvent<?, Item, ?> event) {
        if (!ensureTriggerAssets()) return;
        injectItems();
        injectUnarmedEmpty();
    }

    private static boolean ensureTriggerAssets() {
        if (!TRIGGERS_REGISTERED.compareAndSet(false, true)) return true;
        try {
            Interaction.getAssetStore().loadAssets("Hytale:Hytale", List.of(
                new ClassSkillTriggerInteraction(ClassSkillTriggerInteraction.INNER_ID)));
            RootInteraction.getAssetStore().loadAssets("Hytale:Hytale", List.of(
                triggerRoot(TRIGGER_ABILITY_1),
                triggerRoot(TRIGGER_ABILITY_2),
                triggerRoot(TRIGGER_ABILITY_3)));
            return true;
        } catch (Exception e) {
            TRIGGERS_REGISTERED.set(false);
            VaryonRpgPlugin plugin = VaryonRpgPlugin.getInstance();
            if (plugin != null) {
                plugin.getLogger().at(Level.SEVERE).withCause(e)
                    .log("[VaryonRPG] Failed to register skill trigger assets");
            }
            return false;
        }
    }

    private static RootInteraction triggerRoot(String id) {
        RootInteraction root = new RootInteraction(id, ClassSkillTriggerInteraction.INNER_ID);
        try {
            Field requireNewClick = RootInteraction.class.getDeclaredField("requireNewClick");
            requireNewClick.setAccessible(true);
            requireNewClick.setBoolean(root, true);
        } catch (Exception ignored) {
        }
        return root;
    }

    private static boolean shouldForceAbilityOverride(@Nonnull Item item) {
        WeaponCategory cat = WeaponCategory.fromItemId(item.getId());
        return cat == WeaponCategory.MAGIE || cat == WeaponCategory.DISTANCE;
    }

    private static void injectItems() {
        try {
            Field interactionsField = Item.class.getDeclaredField("interactions");
            interactionsField.setAccessible(true);
            Field cachedPacketField = Item.class.getDeclaredField("cachedPacket");
            cachedPacketField.setAccessible(true);
            if (Item.getAssetMap() == null) return;

            int injected = 0;
            for (Item item : Item.getAssetMap().getAssetMap().values()) {
                try {
                    Map<InteractionType, String> current = item.getInteractions();
                    EnumMap<InteractionType, String> next = new EnumMap<>(InteractionType.class);
                    if (current != null) next.putAll(current);
                    boolean modified = false;
                    boolean forceOverride = shouldForceAbilityOverride(item);
                    if (forceOverride || !next.containsKey(InteractionType.Ability1)) {
                        if (!TRIGGER_ABILITY_1.equals(next.get(InteractionType.Ability1))) {
                            next.put(InteractionType.Ability1, TRIGGER_ABILITY_1);
                            modified = true;
                        }
                    }
                    if (forceOverride || !next.containsKey(InteractionType.Ability2)) {
                        if (!TRIGGER_ABILITY_2.equals(next.get(InteractionType.Ability2))) {
                            next.put(InteractionType.Ability2, TRIGGER_ABILITY_2);
                            modified = true;
                        }
                    }
                    if (forceOverride || !next.containsKey(InteractionType.Ability3)) {
                        if (!TRIGGER_ABILITY_3.equals(next.get(InteractionType.Ability3))) {
                            next.put(InteractionType.Ability3, TRIGGER_ABILITY_3);
                            modified = true;
                        }
                    }
                    if (!modified) continue;
                    interactionsField.set(item, Collections.unmodifiableMap(next));
                    cachedPacketField.set(item, null);
                    injected++;
                } catch (Exception e) {
                    HytaleLogger.getLogger().at(Level.WARNING).log(
                        "[VaryonRPG] Ability inject item=%s: %s", item.getId(), e.getMessage());
                }
            }
            HytaleLogger.getLogger().at(Level.INFO).log(
                "[VaryonRPG] Ability interactions injected on %d items", injected);
        } catch (NoSuchFieldException e) {
            HytaleLogger.getLogger().at(Level.SEVERE).log(
                "[VaryonRPG] Ability inject — Item field mismatch: %s", e.getMessage());
        } catch (Exception e) {
            VaryonRpgPlugin plugin = VaryonRpgPlugin.getInstance();
            if (plugin != null) {
                plugin.getLogger().at(Level.SEVERE).withCause(e).log("[VaryonRPG] Ability inject items failed");
            }
        }
    }

    private static void injectUnarmedEmpty() {
        try {
            UnarmedInteractions empty = UnarmedInteractions.getAssetMap().getAsset("Empty");
            if (empty == null) return;

            Field interactionsField = UnarmedInteractions.class.getDeclaredField("interactions");
            interactionsField.setAccessible(true);
            Map<InteractionType, String> current = empty.getInteractions();
            EnumMap<InteractionType, String> next = new EnumMap<>(InteractionType.class);
            if (current != null) next.putAll(current);
            boolean modified = false;
            if (!next.containsKey(InteractionType.Ability1)) {
                next.put(InteractionType.Ability1, TRIGGER_ABILITY_1);
                modified = true;
            }
            if (!next.containsKey(InteractionType.Ability2)) {
                next.put(InteractionType.Ability2, TRIGGER_ABILITY_2);
                modified = true;
            }
            if (!next.containsKey(InteractionType.Ability3)) {
                next.put(InteractionType.Ability3, TRIGGER_ABILITY_3);
                modified = true;
            }
            if (!modified) return;
            interactionsField.set(empty, Collections.unmodifiableMap(next));
            HytaleLogger.getLogger().at(Level.INFO).log(
                "[VaryonRPG] Ability interactions injected on UnarmedInteractions/Empty");
        } catch (Exception e) {
            VaryonRpgPlugin plugin = VaryonRpgPlugin.getInstance();
            if (plugin != null) {
                plugin.getLogger().at(Level.WARNING).withCause(e)
                    .log("[VaryonRPG] Ability inject unarmed failed");
            }
        }
    }
}
