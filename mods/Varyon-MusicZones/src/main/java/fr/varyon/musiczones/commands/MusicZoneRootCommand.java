package fr.varyon.musiczones.commands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public final class MusicZoneRootCommand extends AbstractCommandCollection {

    public MusicZoneRootCommand(String name) {
        super(name, "Zones musicales 3D (opérateurs)");
        requireNoPermission();
        addSubCommand(new OpenUiSubCommand());
        addSubCommand(new Pos1SubCommand());
        addSubCommand(new Pos2SubCommand());
        addSubCommand(new PreviewZoneSubCommand());
        addSubCommand(new ReloadMusicZonesSubCommand());
    }
}
