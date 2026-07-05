package fr.varyon.travelingcamera;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class TravelCamStopForCommand extends CommandBase {

    private final OptionalArg<PlayerRef> playerArg =
        withOptionalArg("player", "Joueur cible (par defaut : toi-meme)", ArgTypes.PLAYER_REF);

    public TravelCamStopForCommand() {
        super("travelcamstop", "Arrete la lecture du traveling en cours pour un joueur");
        addAliases(new String[] {"tcstop"});
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        PlayerRef targetPlayerRef = resolveTarget(context);
        if (targetPlayerRef == null) {
            context.sendMessage(Message.raw(
                "Precise un joueur : /travelcamstop --player=<pseudo>.")
                .color("#FF5555"));
            return;
        }

        if (!TravelingCameraManager.isPlaying(targetPlayerRef.getUuid())) {
            context.sendMessage(Message.raw("Aucune lecture en cours pour ").color("#FF5555")
                .insert(Message.raw(targetPlayerRef.getUsername()).color("#FFFFFF"))
                .insert(Message.raw(".").color("#FF5555")));
            return;
        }
        TravelingCameraManager.stop(targetPlayerRef.getUuid(), targetPlayerRef);
        context.sendMessage(Message.raw("Lecture arretee pour ").color("#AAAAAA")
            .insert(Message.raw(targetPlayerRef.getUsername()).color("#FFFFFF"))
            .insert(Message.raw(".").color("#AAAAAA")));
    }

    @Nullable
    private PlayerRef resolveTarget(CommandContext context) {
        if (playerArg.provided(context)) {
            return playerArg.get(context);
        }
        if (context.isPlayer()) {
            return context.senderAs(PlayerRef.class);
        }
        return null;
    }
}
