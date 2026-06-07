package fr.varyon.vrpg.classes.ability;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import fr.varyon.vrpg.VaryonRpgPlugin;
import fr.varyon.vrpg.classes.ClassAccount;
import fr.varyon.vrpg.classes.ClassManager;
import fr.varyon.vrpg.classes.PlayerClass;
import fr.varyon.vrpg.ui.classes.ClassUnlockedActiveSkills;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.Color;
import java.util.UUID;

public final class ClassSkillSlots {

    private ClassSkillSlots() {}

    @Nullable
    public static String resolveSkillId(@Nullable ClassAccount acc, @Nullable String itemId) {
        if (acc == null || itemId == null || itemId.isBlank()) return null;
        for (ClassUnlockedActiveSkills.Entry entry : ClassUnlockedActiveSkills.list(acc)) {
            if (!itemId.equals(entry.node().itemId())) continue;
            return ClassSkillRegistry.skillIdForNode(acc, entry.nodeIndex());
        }
        return null;
    }

    @Nullable
    public static String skillIdForSlot(@Nonnull ClassAccount acc,
                                        @Nonnull PlayerClass activeClass,
                                        @Nonnull String slotId) {
        String itemId = acc.getSkillSlot(activeClass, slotId);
        if (itemId == null || itemId.isBlank()) return null;
        return resolveSkillId(acc, itemId);
    }

    public static void tryCastAbility(@Nonnull PlayerRef playerRef, @Nonnull InteractionType interactionType) {
        UUID uuid = playerRef.getUuid();
        if (uuid == null) return;

        VaryonRpgPlugin plugin = VaryonRpgPlugin.getInstance();
        if (plugin == null) return;
        ClassManager classManager = plugin.getClassManager();
        ClassSkillService skills = plugin.getClassSkillService();
        if (classManager == null || skills == null) return;

        World world = resolveWorld(playerRef);
        if (world == null) return;

        world.execute(() -> {
            PlayerRef castRef = Universe.get().getPlayer(uuid);
            if (castRef == null) return;
            Ref<EntityStore> ref = castRef.getReference();
            if (ref == null || !ref.isValid()) return;
            Store<EntityStore> store = ref.getStore();
            if (store == null) return;

            classManager.ensureAccount(uuid, castRef.getUsername());
            ClassAccount acc = classManager.getOrLoad(uuid);
            PlayerClass activeClass = acc.getActiveClass();
            if (activeClass == null) return;

            boolean crouching = isCrouching(store, ref);
            String slotId = ClassSkillSlotIds.forAbility(interactionType, crouching);
            if (slotId == null) return;

            String boundItemId = acc.getSkillSlot(activeClass, slotId);
            if (boundItemId == null || boundItemId.isBlank()) return;

            String skillId = skillIdForSlot(acc, activeClass, slotId);
            if (skillId == null) {
                deny(castRef, "Sort assigné mais non disponible (talent ou spé requis).");
                return;
            }
            skills.tryCast(skillId, uuid, castRef, ref, store, null);
        });
    }

    private static boolean isCrouching(@Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref) {
        MovementStatesComponent mv = store.getComponent(ref, MovementStatesComponent.getComponentType());
        return mv != null && mv.getMovementStates() != null && mv.getMovementStates().crouching;
    }

    @Nullable
    private static World resolveWorld(@Nonnull PlayerRef playerRef) {
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref != null && ref.isValid()) {
            Store<EntityStore> store = ref.getStore();
            if (store != null) {
                World world = store.getExternalData().getWorld();
                if (world != null) return world;
            }
        }
        UUID worldUuid = playerRef.getWorldUuid();
        if (worldUuid != null) {
            return Universe.get().getWorld(worldUuid);
        }
        return null;
    }

    private static void deny(@Nonnull PlayerRef playerRef, @Nonnull String msg) {
        playerRef.sendMessage(Message.raw(msg).color(Color.RED));
    }
}
