package fr.varyon.mapmarker.commands;

import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public final class MapMarkerRootCommand extends AbstractCommandCollection {

    public MapMarkerRootCommand(String rootName) {
        super(rootName, "Marqueurs personnalisés sur la carte (opérateurs)");
        addSubCommand(new SetMarkerSubCommand());
        addSubCommand(new UiMarkerSubCommand());
        addSubCommand(new EditMarkerSubCommand());
        addSubCommand(new InfoMarkerSubCommand());
        addSubCommand(new TeleportMarkerSubCommand());
        addSubCommand(new ClearMarkerSubCommand());
        addSubCommand(new ImportMarkersSubCommand());
        addSubCommand(new ReloadMarkerSubCommand());
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }
}
