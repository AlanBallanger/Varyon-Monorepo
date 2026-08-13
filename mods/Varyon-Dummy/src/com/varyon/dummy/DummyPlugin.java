package com.varyon.dummy;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.system.ISystem;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import com.varyon.dummy.systems.DestroyDummy;
import com.varyon.dummy.systems.FixLegacyDummies;
import com.varyon.dummy.systems.PreventWeaponDamage;

import javax.annotation.Nonnull;

public class DummyPlugin extends JavaPlugin {

    private static DummyPlugin instance;
    private ComponentType<EntityStore, DummyComponent> dummyComponentType;

    public static DummyPlugin getInstance() {
        return instance;
    }

    public DummyPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    public ComponentType<EntityStore, DummyComponent> getDummyComponentType() {
        return dummyComponentType;
    }

    @Override
    protected void setup() {
        instance = this;
        dummyComponentType = getEntityStoreRegistry().registerComponent(DummyComponent.class, "Drex_Dummy", DummyComponent.CODEC);
        getEntityStoreRegistry().registerSystem(new FixLegacyDummies());
        getEntityStoreRegistry().registerSystem(new DestroyDummy());
        getEntityStoreRegistry().registerSystem(new PreventWeaponDamage());
        Interaction.CODEC.register("Drex_SpawnNPC", SpawnDummyInteraction.class, SpawnDummyInteraction.CODEC);
    }
}
