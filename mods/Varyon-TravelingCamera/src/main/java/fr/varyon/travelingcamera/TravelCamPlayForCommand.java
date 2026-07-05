package fr.varyon.travelingcamera;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class TravelCamPlayForCommand extends CommandBase {

    private final RequiredArg<String> nameArg =
        withRequiredArg("name", "Nom du traveling a jouer", ArgTypes.STRING);
    private final OptionalArg<PlayerRef> playerArg =
        withOptionalArg("player", "Joueur cible (par defaut : toi-meme)", ArgTypes.PLAYER_REF);

    public TravelCamPlayForCommand() {
        super("travelcamplay", "Joue un traveling de camera pour un joueur");
        addAliases(new String[] {"tcplay"});
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        PlayerRef targetPlayerRef = resolveTarget(context);
        if (targetPlayerRef == null) {
            context.sendMessage(Message.raw(
                "Precise un joueur : /travelcamplay <nom> --player=<pseudo>.")
                .color("#FF5555"));
            return;
        }

        String name = nameArg.get(context);
        CameraPathStore pathStore = TravelingCameraManager.store();
        CameraPath path = pathStore != null ? pathStore.get(name) : null;
        if (path == null) {
            context.sendMessage(Message.raw("Traveling introuvable : ")
                .color("#FF5555")
                .insert(Message.raw(name).color("#FFFFFF")));
            return;
        }
        if (!TravelingCameraManager.play(targetPlayerRef.getUuid(), targetPlayerRef, path)) {
            context.sendMessage(Message.raw("Ce traveling ne contient aucun point.").color("#FF5555"));
            return;
        }
        context.sendMessage(Message.raw("Lecture du traveling '")
            .color("#55FF55")
            .insert(Message.raw(name).color("#FFFFFF"))
            .insert(Message.raw("' pour ").color("#55FF55"))
            .insert(Message.raw(targetPlayerRef.getUsername()).color("#FFFFFF"))
            .insert(Message.raw(".").color("#55FF55")));
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
