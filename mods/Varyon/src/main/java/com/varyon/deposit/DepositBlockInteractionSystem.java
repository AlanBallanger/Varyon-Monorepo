package com.varyon.deposit;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.InteractionChain;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.InteractionManager;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.VaryonPlugin;
import com.varyon.command.CreateDepositSubCommand;
import com.varyon.config.ZoneConfig;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.Color;
import java.util.logging.Level;

public class DepositBlockInteractionSystem extends EntityEventSystem<EntityStore, UseBlockEvent.Post> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final DepositBlockManager depositBlockManager;
    private final DepositUIManager depositUIManager;
    
    public DepositBlockInteractionSystem(DepositBlockManager depositBlockManager, DepositUIManager depositUIManager) {
        super(UseBlockEvent.Post.class);
        this.depositBlockManager = depositBlockManager;
        this.depositUIManager = depositUIManager;
    }
    
    @Override
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, 
                      @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, 
                      @Nonnull UseBlockEvent.Post event) {
        
        BlockType blockType = event.getBlockType();
        if (blockType == null || blockType.getId() == null) {
            return;
        }
        
        Ref ref = archetypeChunk.getReferenceTo(index);
        if (ref == null) {
            return;
        }
        
        Player player = (Player) store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        
        Vector3i pos = event.getTargetBlock();
        if (pos == null) {
            return;
        }
        
        String world = "world";
        try {
            if (store.getExternalData() != null && ((EntityStore)store.getExternalData()).getWorld() != null) {
                world = ((EntityStore)store.getExternalData()).getWorld().getName();
            } else if (player.getWorld() != null) {
                world = player.getWorld().getName();
            }
        } catch (Exception e) {
            if (player.getWorld() != null) {
                world = player.getWorld().getName();
            }
        }
        
        InteractionType interactionType = event.getInteractionType();
        
        CreateDepositSubCommand.PendingDepositBind pending = CreateDepositSubCommand.getPendingBind(player.getUuid());
        if (pending != null) {
            // Mode création: utiliser la touche F (Use)
            if (interactionType == InteractionType.Use) {
                if (pending.isCreation()) {
                    boolean added = depositBlockManager.addDepositBlock(world, pos);
                    if (added) {
                        player.sendMessage(Message.raw("Bloc de dépôt créé avec succès! (Position: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + ")").color(Color.GREEN));
                        LOGGER.at(Level.INFO).log("Created deposit block at " + world + ":" + pos.getX() + "," + pos.getY() + "," + pos.getZ());
                    } else {
                        player.sendMessage(Message.raw("Ce bloc est déjà un bloc de dépôt.").color(Color.YELLOW));
                    }
                    CreateDepositSubCommand.clearPendingBind(player.getUuid());
                }
                cancelInteractionChain(event.getContext());
            }
            return;
        }
        
        // Mode utilisation normale: vérifier que c'est un clic droit (Secondary) ou F (Use)
        if (interactionType != InteractionType.Secondary && interactionType != InteractionType.Use) {
            return;
        }
        
        if (!depositBlockManager.isDepositBlock(world, pos)) {
            return;
        }

        ZoneConfig zoneConfig = VaryonPlugin.getStaticConfigManager() != null ? VaryonPlugin.getStaticConfigManager().getZoneConfig() : null;
        if (zoneConfig != null && !zoneConfig.isWorldEnabled(world)) {
            return;
        }
        
        cancelInteractionChain(event.getContext());
        depositUIManager.openDepositUI(player);
        LOGGER.at(Level.INFO).log("Opened deposit UI for player at " + world + ":" + pos.getX() + "," + pos.getY() + "," + pos.getZ());
    }
    
    private void cancelInteractionChain(InteractionContext context) {
        if (context == null) {
            return;
        }
        try {
            InteractionChain chain = context.getChain();
            if (chain == null) {
                return;
            }
            InteractionManager manager = context.getInteractionManager();
            if (manager == null) {
                return;
            }
            manager.cancelChains(chain);
        } catch (Exception e) {
        }
    }
    
    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
