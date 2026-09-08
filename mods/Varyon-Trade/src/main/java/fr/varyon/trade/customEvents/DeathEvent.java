package fr.varyon.trade.customEvents;


import fr.varyon.trade.VaryonTradePlugin;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathSystems;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;

public class DeathEvent extends DeathSystems.OnDeathSystem
{
    @Nonnull
    @Override
    public Query<EntityStore> getQuery()
    {
        return Query.and(Player.getComponentType());
    }

    @Override
    public void onComponentAdded(@Nonnull Ref ref, @Nonnull DeathComponent component, @Nonnull Store store, @Nonnull CommandBuffer commandBuffer)
    {
        PlayerRef playerRef = (PlayerRef) store.getComponent(ref, PlayerRef.getComponentType());

        if (playerRef != null)
        {
            Universe.get().getWorld(playerRef.getWorldUuid()).execute(() -> {
                VaryonTradePlugin.cancelPlayerTrades(playerRef);
            });
        }
    }
}

