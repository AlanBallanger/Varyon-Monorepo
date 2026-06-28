package fr.varyon.vrpg.classes;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Function;

public final class EntityStoreCommandBuffers {

    private EntityStoreCommandBuffers() {}

    public static boolean run(@Nonnull Store<EntityStore> store,
                            @Nonnull Function<CommandBuffer<EntityStore>, Boolean> action) {
        return Boolean.TRUE.equals(runWithResult(store, action));
    }

    @Nullable
    public static <T> T runWithResult(@Nonnull Store<EntityStore> store,
                                      @Nonnull Function<CommandBuffer<EntityStore>, T> action) {
        try {
            java.lang.reflect.Method takeCmd = store.getClass().getDeclaredMethod("takeCommandBuffer");
            takeCmd.setAccessible(true);
            @SuppressWarnings("unchecked")
            CommandBuffer<EntityStore> cb = (CommandBuffer<EntityStore>) takeCmd.invoke(store);
            if (cb == null) return null;
            try {
                return action.apply(cb);
            } finally {
                consume(cb);
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void consume(@Nonnull CommandBuffer<EntityStore> cb) {
        try {
            java.lang.reflect.Method consume = cb.getClass().getDeclaredMethod("consume");
            consume.setAccessible(true);
            consume.invoke(cb);
        } catch (Exception ignored) {}
    }
}
