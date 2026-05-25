package fr.varyon.musiczones.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public final class MusicZoneRootCommand extends AbstractCommandCollection {

    public MusicZoneRootCommand(String name) {
        super(name, "Zones musicales 3D (opérateurs)");
        addSubCommand(new Pos1SubCommand());
        addSubCommand(new Pos2SubCommand());
        addSubCommand(new CreateZoneSubCommand());
        addSubCommand(new RemoveZoneSubCommand());
        addSubCommand(new ListZonesSubCommand());
        addSubCommand(new ReloadMusicZonesSubCommand());
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }
}
