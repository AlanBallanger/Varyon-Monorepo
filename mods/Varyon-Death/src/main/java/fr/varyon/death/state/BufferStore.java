package fr.varyon.death.etat;

import java.lang.reflect.Method;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Obtient un {@link CommandBuffer} depuis un {@link Store} hors tick ECS (clic sur une page
 * UI, commande...), la ou seul un {@code Store} est accessible via {@code world.execute(...)}.
 *
 * <p>{@code takeCommandBuffer()}/{@code consume()} ne sont pas exposees par l'API publique du
 * serveur : meme pattern par reflexion deja utilise dans Varyon-RPG
 * ({@code EntityStoreCommandBuffers}) et Varyon ({@code CommandBufferUtil}), duplique ici faute
 * de module commun entre mods.
 */
public final class TamponsStore {

    private TamponsStore() {}

    public static boolean executer(@Nonnull Store<EntityStore> store,
                                   @Nonnull Function<CommandBuffer<EntityStore>, Boolean> action) {
        return Boolean.TRUE.equals(executerAvecResultat(store, action));
    }

    @Nullable
    public static <T> T executerAvecResultat(@Nonnull Store<EntityStore> store,
                                             @Nonnull Function<CommandBuffer<EntityStore>, T> action) {
        try {
            Method prendre = store.getClass().getDeclaredMethod("takeCommandBuffer");
            prendre.setAccessible(true);
            @SuppressWarnings("unchecked")
            CommandBuffer<EntityStore> tampon = (CommandBuffer<EntityStore>) prendre.invoke(store);
            if (tampon == null) {
                return null;
            }
            try {
                return action.apply(tampon);
            } finally {
                consommer(tampon);
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void consommer(@Nonnull CommandBuffer<EntityStore> tampon) {
        try {
            Method consommer = tampon.getClass().getDeclaredMethod("consume");
            consommer.setAccessible(true);
            consommer.invoke(tampon);
        } catch (Exception ignored) {
            // Rien a faire : au pire les changements ne seront visibles qu'au tick suivant.
        }
    }
}
