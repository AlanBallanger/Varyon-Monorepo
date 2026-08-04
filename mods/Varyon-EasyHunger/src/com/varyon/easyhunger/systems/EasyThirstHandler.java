package com.varyon.easyhunger.systems;

import com.varyon.easyhunger.EasyHunger;
import com.varyon.easyhunger.components.ThirstComponent;
import com.varyon.easyhunger.ui.EasyWaterHud;
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
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EasyThirstHandler extends EntityEventSystem<EntityStore, InventoryChangeEvent> {

    private static final Set<String> THIRST_KEYWORDS = new HashSet<>(Arrays.asList(
        "water", "drink", "potion", "bottle", "tea", "coffee", "juice", "mug", "milk", "ale", "beer", "wine"
    ));

    private static final Pattern ITEM_ID_PATTERN = Pattern.compile("itemId=([^,}]+)");
    
    private final com.hypixel.hytale.component.ComponentType<com.hypixel.hytale.server.core.universe.world.storage.EntityStore, com.varyon.easyhunger.components.ThirstComponent> thirstComponentType;

    public EasyThirstHandler(com.hypixel.hytale.component.ComponentType<com.hypixel.hytale.server.core.universe.world.storage.EntityStore, com.varyon.easyhunger.components.ThirstComponent> thirstComponentType) {
        super(InventoryChangeEvent.class);
        this.thirstComponentType = thirstComponentType;
    }

    @Override
    @Nullable
    public Query<EntityStore> getQuery() {
        return Query.and(Player.getComponentType(), this.thirstComponentType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull InventoryChangeEvent event) {
        if (this.thirstComponentType == null) {
             return;
        }
        // Skip if thirst system is disabled
        if (!EasyHunger.get().getConfig().isThirstEnabled()) return;
        
        try {
            Player player = archetypeChunk.getComponent(index, Player.getComponentType());
            if (player == null) return;

            Transaction transaction = event.getTransaction();
            if (transaction == null) return;

            String transactionInfo = transaction.toString();
            String lowerInfo = transactionInfo.toLowerCase();
            
            // Filter Move/Drops
            if (transactionInfo.contains("MoveTransaction")) return; 
            
            // Removed keyword check - rely on config prefix matching only
            
            if (transactionInfo.contains("query=null")) return;

            // Check if player just placed a block (to avoid restoring thirst on block placement)
            // Using the new system that tracks placement of configured drink items
            PlayerRef playerRef = getPlayerRef(player);
            if (playerRef != null) {
                Long lastPlace = com.varyon.easyhunger.systems.EasyHungerBlockPlaceSystem.LAST_PLACE_TIME.get(playerRef.getUuid());
                // Small window (100ms) is enough because inventory change happens immediately after placement in the same tick or next
                if (lastPlace != null && System.currentTimeMillis() - lastPlace < 100) {
                    return;
                }
            }

            // Check Last Item Consumption
            String itemId = getConsumedItemId(transaction);
            if (itemId != null) {
                // Remove leading asterisk if present (Hytale adds this for state variants)
                if (itemId != null && itemId.startsWith("*")) {
                    itemId = itemId.substring(1);
                }
                
                // Lookup drink value from config - only restore if configured
                Float drinkValue = EasyHunger.get().getDrinksConfig().getDrinkValue(itemId);
                
                // Skip EasyHunger items - they use EasyHunger_DrinkWater interaction which already handles thirst
                if (itemId != null && itemId.startsWith("EasyHunger_")) {
                    return;
                }
                
                // Skip items that use Root_Secondary_Consume_Potion - Consume_Charge_Potion_Fast now handles them
                // Only skip specific mod prefixes that use the potion consumption chain
                if (itemId != null && (
                    itemId.startsWith("NoCube_Drink") || 
                    itemId.startsWith("Brewery_") || 
                    itemId.startsWith("NoCube_Template_Juice")
                )) {
                    return;
                }
                
                // Apply thirst restoration if configured in config
                if (drinkValue != null && drinkValue > 0) {
                    
                    // Get player's thirst component and restore thirst (same pattern as FoodHandler)
                    try {
                        // Re-use logic to get accessor for thirst component
                        var entityStore = player.getReference().getStore();
                        
                        ComponentAccessor accessor = null;
                        if (entityStore instanceof ComponentAccessor) {
                            accessor = (ComponentAccessor) entityStore;
                        } else {
                            accessor = (ComponentAccessor) (Object) entityStore;
                        }

                        ThirstComponent thirst = (ThirstComponent) accessor.getComponent(
                            (Ref) player.getReference(), 
                            (ComponentType) (Object) this.thirstComponentType
                        );

                        if (thirst != null) {
                            float max = EasyHunger.get().getConfig().getMaxThirst();
                            if (thirst.getThirstLevel() < max) {
                                thirst.drink(drinkValue);
                                
                                // Update HUD
                                try {
                                    if (playerRef != null) { // Use cached playerRef
                                        EasyWaterHud.updatePlayerThirstLevel(playerRef, thirst.getThirstLevel());
                                    }
                                } catch (Exception e) {
                                    // HUD update failed, ignore
                                }
                            }
                        }
                    } catch (Exception e) {
                        EasyHunger.logInfo("[ThirstHandler] Error applying thirst: " + e.toString());
                    }
                }
            }

        } catch (Throwable e) {
            EasyHunger.get().getLogger().at(Level.SEVERE).log("Error in EasyThirstHandler: " + e.getMessage());
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
                // Return beforeId
                String beforeId = before.getItemId();
                int afterQty = (after != null) ? after.getQuantity() : 0;
                
                // Detection 1: quantity reduction
                if (afterQty < before.getQuantity()) {
                    return beforeId;
                }
                
                // Detection 2: item transformation
                if (after != null && afterQty == before.getQuantity()) {
                    String afterId = after.getItemId();
                    if (beforeId != null && afterId != null && !beforeId.equals(afterId)) {
                        // Check if it's a fill action instead of consume.
                        // Mostly, consuming makes it a base container (Deco_Mug) from a filled (Deco_Mug:*).
                        // If 'after' has a state variant and 'before' does not, it's likely a fill.
                        boolean beforeHasState = beforeId.contains(":") || beforeId.startsWith("*");
                        boolean afterHasState = afterId.contains(":") || afterId.startsWith("*");
                        
                        // Treat as consumption ONLY if before HAS state (e.g., Filled_Water) 
                        // and after does NOT HAVE state (e.g., Deco_Mug), or if item just completely changed names entirely.
                        if (beforeHasState && !afterHasState) {
                             return beforeId; // Example: *Deco_Mug:Filled_Water -> Deco_Mug
                        }
                        
                        // If it's Soup -> Bowl, names are totally different (Preparation_Soup -> Deco_Bowl)
                        if (!beforeHasState && !afterHasState) {
                            return beforeId;
                        }
                    }
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

    private boolean containsThirstKeyword(String text) {
        if (text == null) return false;
        String lower = text.toLowerCase();
        for (String keyword : THIRST_KEYWORDS) {
            if (lower.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private PlayerRef getPlayerRef(Player player) {
        try {
            var store = player.getReference().getStore();
            ComponentAccessor accessor = null;
            if (store instanceof ComponentAccessor) {
                accessor = (ComponentAccessor) store;
            } else {
                accessor = (ComponentAccessor) (Object) store;
            }
            return (PlayerRef) accessor.getComponent(
                (Ref) player.getReference(), 
                (ComponentType) (Object) PlayerRef.getComponentType()
            );
        } catch (Exception e) {
            return null;
        }
    }
}
