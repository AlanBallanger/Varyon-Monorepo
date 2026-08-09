package com.varyon.killcommand.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class KillPlayerCommand extends CommandBase {

    private final RequiredArg<PlayerRef> playerArg =
            this.withRequiredArg("joueur", "Joueur à tuer.", ArgTypes.PLAYER_REF);

    public KillPlayerCommand() {
        super("player", "Tue un joueur précis.");
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        killTargetPlayer(context, this.playerArg.get(context));
    }

    static void killTargetPlayer(@Nonnull CommandContext context, @Nullable PlayerRef targetPlayer) {
        Ref<EntityStore> targetRef = targetPlayer == null ? null : targetPlayer.getReference();
        if (targetRef == null || !targetRef.isValid()) {
            context.sendMessage(Message.raw("Ce joueur n'est pas dans le monde."));
            return;
        }

        Store<EntityStore> store = targetRef.getStore();
        World world = store.getExternalData().getWorld();
        String targetName = targetPlayer.getUsername();
        world.execute(() -> {
            if (store.getComponent(targetRef, DeathComponent.getComponentType()) != null) {
                context.sendMessage(Message.raw("Joueur " + targetName + " déjà mort."));
                return;
            }
            DeathComponent.tryAddComponent(store, targetRef,
                    new Damage(new Damage.CommandSource(context.sender(), "kill"), DamageCause.COMMAND, Float.MAX_VALUE));
            context.sendMessage(Message.raw("Joueur " + targetName + " tué."));
        });
    }
}
