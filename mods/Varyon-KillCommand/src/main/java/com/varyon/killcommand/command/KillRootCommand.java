package com.varyon.killcommand.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;

public final class KillRootCommand extends CommandBase {

    public KillRootCommand() {
        super("kill", "Affiche les options de la commande kill.");
        this.requirePermission("varyon.admin");
        this.addSubCommand(new KillMobsCommand());
        this.addSubCommand(new KillMobCommand());
        this.addSubCommand(new KillPlayerCommand());
        this.addUsageVariant(new KillDirectPlayerVariant());
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        context.sendMessage(Message.raw("Usage : /kill mobs <rayon>"));
        context.sendMessage(Message.raw("Mob précis : /kill mob <mob> (rayon fixe de 20 blocs)"));
        context.sendMessage(Message.raw("Mob précis (personnalisé) : /kill mob <mob> <quantité> <rayon>"));
        context.sendMessage(Message.raw("Joueur direct : /kill <joueur>"));
        context.sendMessage(Message.raw("Joueur : /kill player <joueur>"));
    }

    private static final class KillDirectPlayerVariant extends CommandBase {

        private final RequiredArg<PlayerRef> playerArg =
                this.withRequiredArg("joueur", "Joueur à tuer.", ArgTypes.PLAYER_REF);

        private KillDirectPlayerVariant() {
            super("Tue un joueur précis.");
        }

        @Override
        protected void executeSync(@Nonnull CommandContext context) {
            KillPlayerCommand.killTargetPlayer(context, this.playerArg.get(context));
        }
    }
}
