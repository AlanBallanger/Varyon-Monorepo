package fr.varyon.death.systeme;

import java.lang.reflect.Field;

import javax.annotation.Nonnull;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.selector.Selector;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;

/**
 * Efface, au moment ou un joueur passe a terre, la cible memorisee de tout NPC proche qui le
 * visait deja. {@link AttitudeProviderATerre} empeche tout NOUVEAU ciblage tant que le joueur
 * reste a terre, mais un NPC ayant deja verrouille sa cible avant cet instant la conserverait
 * jusqu'a sa prochaine reevaluation naturelle : ce reset ponctuel la vide immediatement.
 *
 * <p>Meme technique que {@code OmbreStealthAggroResetSystem} dans Varyon-RPG : acces par
 * reflexion aux champs prives {@code entityTargets}/{@code defaultTargetSlot} de
 * {@code MarkedEntitySupport}, faute d'API publique pour vider un slot de cible memorisee.
 */
public final class ResetAggroATerre {

    private static final HytaleLogger LOGGER =
            HytaleLogger.getLogger().getSubLogger("VaryonDeath-ResetAggro");
    private static final double RAYON_RESET_BLOCS = 30.0;

    private static Field entityTargetsField;
    private static Field defaultTargetSlotField;
    private static Field markedEntitySupportField;

    static {
        try {
            Class<?> supportClass = Class.forName("com.hypixel.hytale.server.npc.role.support.MarkedEntitySupport");
            entityTargetsField = supportClass.getDeclaredField("entityTargets");
            entityTargetsField.setAccessible(true);
            defaultTargetSlotField = supportClass.getDeclaredField("defaultTargetSlot");
            defaultTargetSlotField.setAccessible(true);

            Field trouve = null;
            for (Field f : Role.class.getDeclaredFields()) {
                if (supportClass.isAssignableFrom(f.getType())) {
                    trouve = f;
                    break;
                }
            }
            if (trouve == null) {
                for (Field f : Role.class.getSuperclass().getDeclaredFields()) {
                    if (supportClass.isAssignableFrom(f.getType())) {
                        trouve = f;
                        break;
                    }
                }
            }
            if (trouve != null) {
                trouve.setAccessible(true);
            }
            markedEntitySupportField = trouve;
        } catch (Exception e) {
            LOGGER.at(java.util.logging.Level.WARNING).log("Reflexion MarkedEntitySupport echouee: %s", e.getMessage());
        }
    }

    private ResetAggroATerre() {}

    /** Vide la cible memorisee de tout NPC proche qui visait deja {@code playerRef}. */
    public static void resetAggroAutourDe(@Nonnull Ref<EntityStore> playerRef,
                                          @Nonnull Store<EntityStore> store) {
        if (entityTargetsField == null || defaultTargetSlotField == null || markedEntitySupportField == null) {
            return;
        }
        try {
            TransformComponent tc = store.getComponent(playerRef, TransformComponent.getComponentType());
            if (tc == null) {
                return;
            }
            long playerIdx = playerRef.getIndex();
            Selector.selectNearbyEntities(store, tc.getPosition(), RAYON_RESET_BLOCS, npcRef -> {
                try {
                    if (npcRef.getIndex() == playerIdx) {
                        return;
                    }
                    NPCEntity npc = store.getComponent(npcRef, NPCEntity.getComponentType());
                    if (npc == null) {
                        return;
                    }
                    Role role = npc.getRole();
                    if (role == null) {
                        return;
                    }
                    Object support = markedEntitySupportField.get(role);
                    if (support == null) {
                        return;
                    }
                    int slot = defaultTargetSlotField.getInt(support);
                    if (slot < 0) {
                        return;
                    }
                    @SuppressWarnings("unchecked")
                    Ref<EntityStore>[] targets = (Ref<EntityStore>[]) entityTargetsField.get(support);
                    if (targets == null || slot >= targets.length) {
                        return;
                    }
                    Ref<EntityStore> cible = targets[slot];
                    if (cible == null || !cible.isValid() || cible.getIndex() != playerIdx) {
                        return;
                    }
                    targets[slot] = null;
                } catch (Exception ignored) {
                    // Un NPC invalide entre-temps ne doit pas interrompre le reset des autres.
                }
            }, ref -> ref.getIndex() != playerIdx);
        } catch (Exception ignored) {
            // Reflexion best-effort : un echec ne doit jamais empecher la mise a terre.
        }
    }
}
