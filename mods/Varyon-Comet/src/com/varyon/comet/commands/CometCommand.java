package com.varyon.comet.commands;

import com.varyon.comet.*;
import com.varyon.comet.commands.*;
import com.varyon.comet.services.*;
import com.varyon.comet.spawn.*;
import com.varyon.comet.systems.*;
import com.varyon.comet.wave.*;


import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

import javax.annotation.Nonnull;

public class CometCommand extends AbstractCommandCollection {

    public CometCommand() {
        super("comet", "Comet mod commands");
        addSubCommand(new CometSpawnCommand());
        addSubCommand(new CometThemesCommand());
        addSubCommand(new CometTestCommand());
        addSubCommand(new CometZoneCommand());
        addSubCommand(new CometDestroyAllCommand());
        addSubCommand(new CometReloadCommand());
    }
}
