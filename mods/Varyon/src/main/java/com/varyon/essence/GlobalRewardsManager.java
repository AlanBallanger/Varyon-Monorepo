package com.varyon.essence;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.config.FactionRewardsConfig;
import com.varyon.config.ZonePermissionsConfig;
import com.varyon.faction.FactionManager;
import com.varyon.util.CommandBufferUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.Color;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class GlobalRewardsManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private FactionRewardsConfig config;
    private final EssenceManager essenceManager;
    private final FactionManager factionManager;
    private ZonePermissionsConfig zonePermsConfig;
    private final PendingRewardsStore pendingStore;

    /**
     * Participation tracking per faction since last tier trigger.
     * Key = player UUID, Value = essence deposited this cycle.
     */
    private final Map<UUID, Double> fractureParticipation = new ConcurrentHashMap<>();
    private final Map<UUID, Double> noyauParticipation    = new ConcurrentHashMap<>();

    private static class TierState {
        private boolean positiveRewarded = false;
        private boolean negativeRewarded = false;
        private long positiveLastRewardTime = 0;
        private long negativeLastRewardTime = 0;

        boolean canRewardPositive(long cooldownMs) {
            return !positiveRewarded || (System.currentTimeMillis() - positiveLastRewardTime >= cooldownMs);
        }

        boolean canRewardNegative(long cooldownMs) {
            return !negativeRewarded || (System.currentTimeMillis() - negativeLastRewardTime >= cooldownMs);
        }

        void markPositiveRewarded() { positiveRewarded = true; positiveLastRewardTime = System.currentTimeMillis(); }
        void markNegativeRewarded() { negativeRewarded = true; negativeLastRewardTime = System.currentTimeMillis(); }
        void resetPositive()        { positiveRewarded = false; }
        void resetNegative()        { negativeRewarded = false; }
    }

    private final Map<Integer, TierState> tierStates = new ConcurrentHashMap<>();

    public GlobalRewardsManager(@Nonnull FactionRewardsConfig config,
                                @Nonnull EssenceManager essenceManager,
                                @Nonnull FactionManager factionManager,
                                @Nonnull ZonePermissionsConfig zonePermsConfig,
                                @Nonnull Path dataFolder) {
        this.config = config;
        this.essenceManager = essenceManager;
        this.factionManager = factionManager;
        this.zonePermsConfig = zonePermsConfig;
        this.pendingStore = new PendingRewardsStore(dataFolder);

        for (int i = 0; i < config.getTiers().size(); i++) {
            tierStates.put(i, new TierState());
        }
    }

    public synchronized void applyReloadedConfigs(@Nonnull FactionRewardsConfig factionRewards,
                                                  @Nonnull ZonePermissionsConfig zonePerms) {
        this.config = factionRewards;
        this.zonePermsConfig = zonePerms;
        int n = config.getTiers().size();
        for (int i = 0; i < n; i++) {
            tierStates.computeIfAbsent(i, k -> new TierState());
        }
        tierStates.keySet().removeIf(k -> k >= n);
    }

    /**
     * Called when a player deposits essence for their faction.
     */
    public void recordDeposit(@Nonnull UUID uuid, @Nonnull FactionManager.Faction faction, double amount) {
        Map<UUID, Double> map = faction == FactionManager.Faction.FRACTURE ? fractureParticipation : noyauParticipation;
        map.merge(uuid, amount, Double::sum);
    }

    public void checkAndDistributeRewards() {
        int globalBalance = essenceManager.getGlobalBalance();
        int gaugeAbsMax = Math.max(1, essenceManager.getGuildGaugeAbsMax());
        int referenceMax = tierGaugeReferenceMax();
        long cooldownMs = config.getCooldownMinutes() * 60_000L;

        boolean fractureRewardedThisWave = false;
        boolean noyauRewardedThisWave = false;

        for (int i = 0; i < config.getTiers().size(); i++) {
            FactionRewardsConfig.RewardTier tier = config.getTiers().get(i);
            TierState state = tierStates.computeIfAbsent(i, k -> new TierState());
            int scaledThreshold = scaledTierThreshold(tier.getThreshold(), gaugeAbsMax, referenceMax);

            if (globalBalance >= scaledThreshold && state.canRewardPositive(cooldownMs)) {
                LOGGER.at(Level.INFO).log("Tier " + (i + 1) + " reached (+" + scaledThreshold + " scaled, raw " + tier.getThreshold() + "/" + referenceMax + " ref, gauge max " + gaugeAbsMax + ") — rewarding Fracture");
                distributeFactionReward(FactionManager.Faction.FRACTURE, tier, i + 1);
                state.markPositiveRewarded();
                fractureRewardedThisWave = true;
            }

            if (globalBalance <= -scaledThreshold && state.canRewardNegative(cooldownMs)) {
                LOGGER.at(Level.INFO).log("Tier " + (i + 1) + " reached (-" + scaledThreshold + " scaled, raw " + tier.getThreshold() + "/" + referenceMax + " ref, gauge max " + gaugeAbsMax + ") — rewarding Noyau");
                distributeFactionReward(FactionManager.Faction.NOYAU, tier, i + 1);
                state.markNegativeRewarded();
                noyauRewardedThisWave = true;
            }
        }

        if (fractureRewardedThisWave) {
            fractureParticipation.clear();
            LOGGER.at(Level.INFO).log("Participation reset for Fracture after reward wave");
        }
        if (noyauRewardedThisWave) {
            noyauParticipation.clear();
            LOGGER.at(Level.INFO).log("Participation reset for Noyau after reward wave");
        }
    }

    private int tierGaugeReferenceMax() {
        int maxT = 0;
        for (FactionRewardsConfig.RewardTier t : config.getTiers()) {
            maxT = Math.max(maxT, t.getThreshold());
        }
        return Math.max(1, maxT);
    }

    private static int scaledTierThreshold(int rawConfiguredThreshold, int gaugeAbsMax, int referenceMax) {
        long scaled = Math.round(rawConfiguredThreshold * (double) gaugeAbsMax / (double) referenceMax);
        int s = (int) scaled;
        if (s < 1) s = 1;
        if (s > gaugeAbsMax) s = gaugeAbsMax;
        return s;
    }

    private static String fragmentGainChatLine(int quantity, int keyTier, @Nonnull String pctLabel) {
        String noun = quantity == 1 ? "Fragment" : "Fragments";
        return "Tu reçois : " + quantity + " " + noun + " de clé de palier " + keyTier + " (" + pctLabel + ")";
    }

    private static int remainderQuantity(@Nullable ItemStack remainder) {
        return ItemStack.isEmpty(remainder) ? 0 : remainder.getQuantity();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean dropItemStacksAtPlayerFeet(@Nonnull Store store, @Nonnull Ref ref,
                                                      @Nonnull List<ItemStack> stacks) {
        TransformComponent transform = (TransformComponent) store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null || stacks.isEmpty()) {
            return false;
        }
        CommandBuffer<EntityStore> cb = CommandBufferUtil.take((Store<EntityStore>) store);
        if (cb == null) {
            return false;
        }
        try {
            org.joml.Vector3d pos = new org.joml.Vector3d(transform.getPosition()).add(0.0, 1.0, 0.0);
            HeadRotation headRotation = (HeadRotation) store.getComponent(ref, HeadRotation.getComponentType());
            com.hypixel.hytale.math.vector.Rotation3fc rot = headRotation != null ? headRotation.getRotation() : com.hypixel.hytale.math.vector.Rotation3f.ZERO;
            Holder[] drops = ItemComponent.generateItemDrops(store, stacks, pos, rot);
            cb.addEntities(drops, AddReason.SPAWN);
            return true;
        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("dropItemStacksAtPlayerFeet failed: " + e.getMessage());
            return false;
        } finally {
            CommandBufferUtil.consume(cb);
        }
    }

    private void distributeFactionReward(@Nonnull FactionManager.Faction faction,
                                         @Nonnull FactionRewardsConfig.RewardTier tier,
                                         int tierNumber) {
        Map<UUID, Double> participation = faction == FactionManager.Faction.FRACTURE
            ? fractureParticipation : noyauParticipation;
        double minPart   = config.getMinParticipationEssence();
        double passRate  = config.getPassiveRewardRate();
        int    fullAmt   = tier.getFragmentAmount();

        Set<UUID> onlineUuids = new java.util.HashSet<>();

        for (PlayerRef playerRef : Universe.get().getPlayers()) {
            if (playerRef == null || !playerRef.getReference().isValid()) continue;
            UUID uuid = playerRef.getUuid();

            if (factionManager.getFaction(playerRef) != faction) continue;

            Ref ref = playerRef.getReference();
            Store store = ref.getStore();

            onlineUuids.add(uuid);

            double deposited = participation.getOrDefault(uuid, 0.0);
            boolean participated = deposited >= minPart;
            int fragments = participated ? fullAmt : (int) Math.floor(fullAmt * passRate);
            if (fragments <= 0) continue;

            try {
                World world = ((EntityStore) store.getExternalData()).getWorld();
                final int finalFragments = fragments;
                final boolean finalParticipated = participated;
                world.execute(() -> giveFragmentsOnline(playerRef, ref, store, finalFragments, finalParticipated, tierNumber, faction));
            } catch (Exception e) {
                LOGGER.at(Level.WARNING).log("Failed to schedule reward for " + playerRef.getUsername() + ": " + e.getMessage());
            }
        }

        // Offline players who participated → save pending
        for (Map.Entry<UUID, Double> entry : participation.entrySet()) {
            UUID uuid = entry.getKey();
            if (onlineUuids.contains(uuid)) continue;
            if (entry.getValue() < minPart) continue;

            pendingStore.add(uuid, fullAmt);
            LOGGER.at(Level.INFO).log("Stored " + fullAmt + " pending fragments for offline player " + uuid + " (tier " + tierNumber + ")");
        }

    }

    private void giveFragmentsOnline(@Nonnull PlayerRef playerRef, @Nonnull Ref ref,
                                     @Nonnull Store store, int fragments,
                                     boolean participated, int tierNumber,
                                     @Nonnull FactionManager.Faction faction) {
        try {
            Player playerComponent = (Player) store.getComponent(ref, Player.getComponentType());
            if (playerComponent == null || playerComponent.getInventory() == null) return;

            int maxZone = zonePermsConfig.getMaxAccessibleZone(playerRef);
            if (maxZone <= 0) maxZone = 1;
            String itemId = "Key_Fragment" + maxZone;

            ItemStack stack = new ItemStack(itemId, fragments);
            ItemStackTransaction tx = playerComponent.getInventory().getCombinedHotbarFirst().addItemStack(stack);
            ItemStack remainder = tx.getRemainder();
            int remainderQty = remainderQuantity(remainder);
            int acceptedQty = fragments - remainderQty;

            String factionColor = faction == FactionManager.Faction.NOYAU ? "#5555FF" : "#FF8800";
            playerRef.sendMessage(Message.raw("[Palier " + tierNumber + "] " + faction.getDisplayName() + " a atteint un seuil !").color(Color.decode(factionColor)));

            String pct = participated ? "100%" : ((int)(config.getPassiveRewardRate() * 100)) + "%";
            if (remainderQty == 0) {
                playerRef.sendMessage(Message.raw(fragmentGainChatLine(fragments, maxZone, pct)).color(Color.GREEN));
                LOGGER.at(Level.INFO).log("Gave " + fragments + "x " + itemId + " to " + playerRef.getUsername() + " (" + pct + ", tier " + tierNumber + ")");
            } else {
                boolean dropped = dropItemStacksAtPlayerFeet(store, ref, List.of(remainder));
                if (acceptedQty > 0) {
                    playerRef.sendMessage(Message.raw(fragmentGainChatLine(acceptedQty, maxZone, pct)).color(Color.GREEN));
                }
                String noun = remainderQty == 1 ? "fragment" : "fragments";
                if (dropped) {
                    playerRef.sendMessage(Message.raw(
                        remainderQty + " " + noun + " de clé de palier " + maxZone + " sont au sol (pile ou inventaire plein).").color(Color.YELLOW));
                    LOGGER.at(Level.INFO).log("Faction tier reward: " + acceptedQty + " inv + " + remainderQty + " dropped as " + itemId + " to "
                        + playerRef.getUsername() + " (" + pct + ", tier " + tierNumber + ")");
                } else {
                    LOGGER.at(Level.WARNING).log("Could not place remainder fragments for " + playerRef.getUsername() + " (" + remainderQty + "x " + itemId + " lost)");
                }
            }
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("Error giving fragments to " + playerRef.getUsername() + ": " + e.getMessage());
        }
    }

    /**
     * Called when a player connects. Delivers any pending fragments.
     */
    public void onPlayerReady(@Nonnull PlayerRef playerRef, @Nonnull Ref ref, @Nonnull Store store) {
        UUID uuid = playerRef.getUuid();
        if (!pendingStore.has(uuid)) return;

        int fragments = pendingStore.get(uuid);
        pendingStore.clear(uuid);

        try {
            Player playerComponent = (Player) store.getComponent(ref, Player.getComponentType());
            if (playerComponent == null || playerComponent.getInventory() == null) {
                // Restore in case delivery failed
                pendingStore.add(uuid, fragments);
                return;
            }

            int maxZone = zonePermsConfig.getMaxAccessibleZone(playerRef);
            if (maxZone <= 0) maxZone = 1;
            String itemId = "Key_Fragment" + maxZone;

            ItemStack stack = new ItemStack(itemId, fragments);
            ItemStackTransaction tx = playerComponent.getInventory().getCombinedHotbarFirst().addItemStack(stack);
            ItemStack remainder = tx.getRemainder();
            int remainderQty = remainderQuantity(remainder);
            int acceptedQty = fragments - remainderQty;

            if (remainderQty == 0) {
                playerRef.sendMessage(Message.raw("[Récompense en attente] " + fragmentGainChatLine(fragments, maxZone, "100%")).color(Color.GREEN));
                LOGGER.at(Level.INFO).log("Delivered " + fragments + "x " + itemId + " (pending) to " + playerRef.getUsername());
            } else {
                boolean dropped = dropItemStacksAtPlayerFeet(store, ref, List.of(remainder));
                if (acceptedQty > 0) {
                    playerRef.sendMessage(Message.raw("[Récompense en attente] " + fragmentGainChatLine(acceptedQty, maxZone, "100%")).color(Color.GREEN));
                }
                String noun = remainderQty == 1 ? "fragment" : "fragments";
                if (dropped) {
                    playerRef.sendMessage(Message.raw(
                        "[Récompense en attente] " + remainderQty + " " + noun + " de clé de palier " + maxZone + " sont au sol (pile ou inventaire plein).").color(Color.YELLOW));
                    LOGGER.at(Level.INFO).log("Delivered pending " + acceptedQty + " inv + " + remainderQty + " dropped as " + itemId + " to " + playerRef.getUsername());
                } else {
                    pendingStore.add(uuid, remainderQty);
                    playerRef.sendMessage(Message.raw("[Récompense en attente] Inventaire plein — réessayez plus tard.").color(Color.YELLOW));
                }
            }
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("Error delivering pending rewards to " + playerRef.getUsername() + ": " + e.getMessage());
            pendingStore.add(uuid, fragments);
        }
    }

    public void resetAllCooldowns() {
        for (TierState state : tierStates.values()) {
            state.resetPositive();
            state.resetNegative();
        }
        LOGGER.at(Level.INFO).log("All reward tier cooldowns have been reset");
    }

    @Nullable
    public PendingRewardsStore getPendingStore() {
        return pendingStore;
    }
}
