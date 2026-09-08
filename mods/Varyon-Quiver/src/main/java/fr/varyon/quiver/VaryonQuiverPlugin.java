package fr.varyon.quiver;

import com.hypixel.hytale.component.system.ISystem;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import javax.annotation.Nonnull;

public final class VaryonQuiverPlugin extends JavaPlugin {
    public VaryonQuiverPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        QuiverSupplySystem supplySystem = new QuiverSupplySystem();
        this.getEntityStoreRegistry().registerSystem((ISystem) supplySystem);
        this.getEntityStoreRegistry().registerSystem((ISystem) new QuiverDamageBonusSystem());

        this.getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, event -> {
            PlayerRef playerRef = event.getPlayerRef();
            if (playerRef != null) {
                supplySystem.removePlayer(playerRef.getUuid());
            }
        });
    }
}
