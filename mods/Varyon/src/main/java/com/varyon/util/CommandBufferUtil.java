package com.varyon.util;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

public final class CommandBufferUtil {

    private CommandBufferUtil() {}

    @Nullable
    @SuppressWarnings("unchecked")
    public static CommandBuffer<EntityStore> take(Store<EntityStore> store) {
        if (store == null) return null;
        try {
            Method takeMethod = store.getClass().getDeclaredMethod("takeCommandBuffer");
            takeMethod.setAccessible(true);
            return (CommandBuffer<EntityStore>) takeMethod.invoke(store);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static void consume(@Nullable CommandBuffer<EntityStore> commandBuffer) {
        if (commandBuffer == null) return;
        try {
            Method consumeMethod = commandBuffer.getClass().getDeclaredMethod("consume");
            consumeMethod.setAccessible(true);
            consumeMethod.invoke(commandBuffer);
        } catch (Exception ignored) {
        }
    }
}
