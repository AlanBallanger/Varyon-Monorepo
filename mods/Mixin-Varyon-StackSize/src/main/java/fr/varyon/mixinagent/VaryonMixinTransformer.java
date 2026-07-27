package fr.varyon.mixinagent;

import com.hypixel.hytale.plugin.early.ClassTransformer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class VaryonMixinTransformer implements ClassTransformer {

    private static final String ITEM = "com.hypixel.hytale.server.core.asset.type.item.config.Item";
    private static final String INVENTORY_PACKET_HANDLER =
            "com.hypixel.hytale.server.core.io.handlers.game.InventoryPacketHandler";

    public VaryonMixinTransformer() {
        System.out.println("[Varyon-MixinAgent] Early-plugin 0.1.0 active — stack-by-permission hook (Wood_Ash_Trunk)");
    }

    @Override
    public int priority() {
        return 0;
    }

    @Nullable
    @Override
    public byte[] transform(@Nonnull String className, @Nonnull String internalName, @Nonnull byte[] classBytes) {
        try {
            if (ITEM.equals(className)) {
                byte[] out = MaxStackPermissionInjector.inject(classBytes);
                System.out.println("[Varyon-MixinAgent] Patched Item#getMaxStack (permission-aware)");
                return out;
            }
            if (INVENTORY_PACKET_HANDLER.equals(className)) {
                byte[] out = InventoryHandlerContextInjector.inject(classBytes);
                System.out.println("[Varyon-MixinAgent] Patched InventoryPacketHandler (context propagation)");
                return out;
            }
        } catch (Throwable t) {
            System.err.println("[Varyon-MixinAgent] Failed to transform " + className + ": " + t);
            t.printStackTrace();
        }
        return null;
    }
}
