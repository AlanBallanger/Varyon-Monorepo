package fr.varyon.quiver;

import com.hypixel.hytale.component.system.ISystem;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import javax.annotation.Nonnull;

public final class VaryonQuiverPlugin extends JavaPlugin {
    public VaryonQuiverPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        this.getEntityStoreRegistry().registerSystem((ISystem) new QuiverSupplySystem());
        this.getEntityStoreRegistry().registerSystem((ISystem) new QuiverDamageBonusSystem());
    }
}
