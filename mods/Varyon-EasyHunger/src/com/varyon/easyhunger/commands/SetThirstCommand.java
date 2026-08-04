package com.varyon.easyhunger.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.easyhunger.components.ThirstComponent;
import com.varyon.easyhunger.ui.EasyWaterHud;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class SetThirstCommand extends AbstractPlayerCommand {
    public static final String requiredPermission = "easyhunger.thirst.set.self";
    private final RequiredArg<Float> thirstLevel = this.withRequiredArg("thirstLevel", "A value between 0 and 100", ArgTypes.FLOAT);

    public SetThirstCommand() {
        super("wset", "Set own thirst/water level", false);
        this.requirePermission(requiredPermission);
        this.addUsageVariant(new SetThirstOtherCommand());
    }

    private static void setThirstLevel (
        @NonNullDecl CommandContext context,
        @NonNullDecl Store<EntityStore> store,
        @NonNullDecl Ref<EntityStore> ref,
        @NonNullDecl PlayerRef targetPlayerRef,
        float newThirstLevel
    ) {
        ThirstComponent thirstComponent = store.getComponent(ref, ThirstComponent.getComponentType());
        if (thirstComponent == null) {
            context.sendMessage(Message.raw("Thirst component not found."));
            return;
        }

        if (newThirstLevel < 0 || newThirstLevel > 100) {
            context.sendMessage(Message.raw("Thirst level must be between 0 and 100."));
            return;
        }
        thirstComponent.setThirstLevel(newThirstLevel);
        context.sendMessage(Message.raw("Thirst level has been set to " + newThirstLevel + " for player " + targetPlayerRef.getUsername() + "."));
        EasyWaterHud.updatePlayerThirstLevel(targetPlayerRef, newThirstLevel);
    }

    @Override
    protected void execute(
            @NonNullDecl CommandContext context,
            @NonNullDecl Store<EntityStore> store,
            @NonNullDecl Ref<EntityStore> ref,
            @NonNullDecl PlayerRef playerRef,
            @NonNullDecl World world
    ) {
        float newThirstLevel = this.thirstLevel.get(context);
        setThirstLevel(context, store, ref, playerRef, newThirstLevel);
    }

    private static class SetThirstOtherCommand extends CommandBase {
        public static final String requiredPermission = "easyhunger.thirst.set.other";
        private final RequiredArg<PlayerRef> playerArg = this.withRequiredArg("player", "The target player", ArgTypes.PLAYER_REF);
        private final RequiredArg<Float> thirstLevel = this.withRequiredArg("thirstLevel", "A value between 0 and 100", ArgTypes.FLOAT);

        public SetThirstOtherCommand() {
            super("Set another player's thirst level");
            this.requirePermission(requiredPermission);
        }

        @Override
        protected void executeSync(@NonNullDecl CommandContext context) {
            PlayerRef targetPlayerRef = this.playerArg.get(context);
            float newThirstLevel = this.thirstLevel.get(context);
            Ref<EntityStore> ref = targetPlayerRef.getReference();
            if (ref == null || !ref.isValid()) {
                context.sendMessage(Message.raw("Player not found or not in the world."));
                return;
            }
            Store<EntityStore> store = ref.getStore();
            World world = store.getExternalData().getWorld();
            world.execute(() -> setThirstLevel(context, store, ref, targetPlayerRef, newThirstLevel));
        }
    }
}
