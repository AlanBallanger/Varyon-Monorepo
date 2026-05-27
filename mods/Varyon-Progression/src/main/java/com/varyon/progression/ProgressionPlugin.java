package com.varyon.progression;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.varyon.progression.systems.ArmorProgressionSystem;
import com.varyon.progression.systems.BreakOreProgressionSystem;
import com.varyon.progression.systems.DamageOreProgressionSystem;
import org.jetbrains.annotations.NotNull;

public class ProgressionPlugin extends JavaPlugin {

    public ProgressionPlugin(@NotNull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void start() {
        getEntityStoreRegistry().registerSystem(new ArmorProgressionSystem());
        getEntityStoreRegistry().registerSystem(new BreakOreProgressionSystem());
        getEntityStoreRegistry().registerSystem(new DamageOreProgressionSystem());
    }
}
