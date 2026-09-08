package fr.varyon.trade.customEvents;

import fr.varyon.trade.VaryonTradePlugin;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class DamageSystem extends DamageEventSystem
{
    @Override
    public void handle(int index, ArchetypeChunk<EntityStore> chunk, Store<EntityStore> store,
                       CommandBuffer<EntityStore> commandBuffer, Damage event)
    {
        Player nativePlayer = chunk.getComponent(index, Player.getComponentType());
        if (nativePlayer != null)
        {
            PlayerRef playerRef = chunk.getComponent(index, PlayerRef.getComponentType());
            if (playerRef != null)
            {
                Universe.get().getWorld(playerRef.getWorldUuid()).execute(() -> {
                    VaryonTradePlugin.cancelPlayerTrades(playerRef);
                });
            }
        }
    }

    @NullableDecl
    @Override
    public Query<EntityStore> getQuery()
    {
        return Query.and(Player.getComponentType());
    }
}

