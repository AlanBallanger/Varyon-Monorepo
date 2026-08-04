package com.varyon.easyhunger.systems;

import com.varyon.easyhunger.EasyHunger;
import com.varyon.easyhunger.EasyHungerUtils;
import com.varyon.easyhunger.components.HungerComponent;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.InventoryChangeEvent;
import com.hypixel.hytale.server.core.inventory.transaction.Transaction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EasyHungerFoodHandler extends EntityEventSystem<EntityStore, InventoryChangeEvent> {



    private static final Pattern QUANTITY_BEFORE_PATTERN = Pattern.compile("slotBefore=ItemStack\\{[^}]*quantity=(\\d+)");
    private static final Pattern QUANTITY_AFTER_PATTERN = Pattern.compile("slotAfter=ItemStack\\{[^}]*quantity=(\\d+)");
    private static final Pattern ITEM_ID_PATTERN = Pattern.compile("itemId=([^,}]+)");
    
    private final com.hypixel.hytale.component.ComponentType<com.hypixel.hytale.server.core.universe.world.storage.EntityStore, com.varyon.easyhunger.components.HungerComponent> hungerComponentType;

    public EasyHungerFoodHandler(com.hypixel.hytale.component.ComponentType<com.hypixel.hytale.server.core.universe.world.storage.EntityStore, com.varyon.easyhunger.components.HungerComponent> hungerComponentType) {
        super(InventoryChangeEvent.class);
        this.hungerComponentType = hungerComponentType;
    }

    @Override
    @Nullable
    public Query<EntityStore> getQuery() {
        return Query.and(Player.getComponentType(), this.hungerComponentType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull InventoryChangeEvent event) {
        if (this.hungerComponentType == null) {
             return;
        }
        try {
            Player player = archetypeChunk.getComponent(index, Player.getComponentType());
            if (player == null) return;

            Transaction transaction = event.getTransaction();
            if (transaction == null) return;

            String transactionInfo = transaction.toString();
            String lowerInfo = transactionInfo.toLowerCase();
            
            // Filter out MoveTransactions (Drops, Item Movement) safely without logging
            if (transactionInfo.contains("MoveTransaction")) {
                 return; 
            }

            // Filter null queries safely without logging
            if (transactionInfo.contains("query=null")) {
                return;
            }

            // QUICK EXIT: If the transaction doesn't even mention eternal meat, bail out immediately!
            // This prevents parsing and looping through standard foods like Pizza, perfectly optimizing performance snippet.
            if (!lowerInfo.contains("eternal_meat")) {
                return;
            }
            
            try {
                var entityStore = player.getReference().getStore();
                ComponentAccessor accessor = (entityStore instanceof ComponentAccessor) ? (ComponentAccessor) entityStore : (ComponentAccessor) (Object) entityStore;
                PlayerRef playerRef = (PlayerRef) accessor.getComponent((Ref) player.getReference(), (ComponentType) (Object) PlayerRef.getComponentType());
                
                if (playerRef != null) {
                    Long lastPlace = com.varyon.easyhunger.systems.EasyHungerBlockPlaceSystem.LAST_PLACE_TIME.get(playerRef.getUuid());
                    // If a block was placed in the last 100ms, abort the Food consumption Handler for this inventory change
                    if (lastPlace != null && System.currentTimeMillis() - lastPlace < 100) {
                        return;
                    }
                }
            } catch (Exception e) {}

            // EasyHunger.logInfo("[Debug FoodHandler] Eternal Meat Transaction Detected: " + transactionInfo);

            String itemId = getConsumedItemId(transaction);
            
            if (itemId != null) {
                // EasyHunger.logInfo("[Debug FoodHandler] Item Extracted Structurally: " + itemId);
                
                // Only handle specific infinite food items (e.g., Eternal Meat)
                boolean isEternalMeat = itemId.toLowerCase().contains("eternal_meat");
                
                if (isEternalMeat) {
                    Item itemAsset = Item.getAssetMap().getAsset(itemId);
                    if (itemAsset != null && itemAsset.isConsumable()) {
                        float hungerRestore = EasyHunger.get().getFoodsConfig().getFoodValue(itemId);
                        
                        if (hungerRestore > 0) {
                             try {
                                 var entityStore = player.getReference().getStore();
                                 ComponentAccessor accessor = (entityStore instanceof ComponentAccessor) ? (ComponentAccessor) entityStore : (ComponentAccessor) (Object) entityStore;
        
                                 HungerComponent hunger = (HungerComponent) accessor.getComponent(
                                     (Ref) player.getReference(), 
                                     (ComponentType) (Object) this.hungerComponentType
                                 );
        
                                if (hunger != null) {
                                    float current = hunger.getHungerLevel();
                                    float maxHunger = EasyHunger.get().getConfig().getMaxHunger();
                                    
                                    if (current < maxHunger) { 
                                       hunger.feed(hungerRestore);
                                       
                                       try {
                                           if (accessor != null) {
                                               com.hypixel.hytale.server.core.universe.PlayerRef playerRef = 
                                                    (com.hypixel.hytale.server.core.universe.PlayerRef) accessor.getComponent(
                                                       (Ref) player.getReference(), 
                                                       (ComponentType) (Object) com.hypixel.hytale.server.core.universe.PlayerRef.getComponentType()
                                                    );
                                                    
                                               if (playerRef != null) {
                                                   com.varyon.easyhunger.ui.EasyHungerHud.updatePlayerHungerLevel(
                                                       playerRef, 
                                                       hunger.getHungerLevel()
                                                   );
                                               }
                                           }
                                       } catch (Exception e) {}
                                    }
                                }
                             } catch (Exception e) {
                                  EasyHunger.get().getLogger().at(Level.SEVERE).log("Error applying hunger: " + e.toString());
                             }
                        }
                    }
                }
            }

        } catch (Throwable e) {
            EasyHunger.get().getLogger().at(Level.SEVERE).log("Error in EasyHungerFoodHandler: " + e.getMessage());
        }
    }

    private String getConsumedItemId(Transaction transaction) {
        // Case 1: Single Slot
        if (transaction instanceof com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction) {
             com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction slotTrans = 
                 (com.hypixel.hytale.server.core.inventory.transaction.ItemStackSlotTransaction) transaction;
             
             com.hypixel.hytale.server.core.inventory.ItemStack before = slotTrans.getSlotBefore();
             com.hypixel.hytale.server.core.inventory.ItemStack after = slotTrans.getSlotAfter();

             if (before != null && before.getQuantity() >= 1) {
                String beforeId = before.getItemId();
                int afterQty = (after != null) ? after.getQuantity() : 0;
                
                // Quantity strictly decreased (Consumption)
                if (afterQty < before.getQuantity()) {
                    return beforeId;
                }
             }
        }

        // Case 2: Multi-Slot
        if (transaction instanceof com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction) {
            com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction stackTransaction = 
                (com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction) transaction;
    
            for (Object slotTransObj : stackTransaction.getSlotTransactions()) {
                if (slotTransObj instanceof com.hypixel.hytale.server.core.inventory.transaction.SlotTransaction) {
                     com.hypixel.hytale.server.core.inventory.transaction.SlotTransaction slotTrans = 
                         (com.hypixel.hytale.server.core.inventory.transaction.SlotTransaction) slotTransObj;
                     com.hypixel.hytale.server.core.inventory.ItemStack before = slotTrans.getSlotBefore();
                     com.hypixel.hytale.server.core.inventory.ItemStack after = slotTrans.getSlotAfter();
        
                    if (before != null && before.getQuantity() >= 1) {
                        int afterQty = (after != null) ? after.getQuantity() : 0;
                        if (afterQty < before.getQuantity()) {
                            return before.getItemId();
                        }
                    }
                }
            }
        }
        return null;
    }


}
