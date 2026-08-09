package com.varyon.bossarena.shop;

import com.varyon.bossarena.config.BossArenaConfig;
import com.varyon.bossarena.BossArenaPlugin;
import com.varyon.bossarena.data.Arena;
import com.varyon.bossarena.data.ArenaRegistry;
import com.varyon.bossarena.data.BossDefinition;
import com.varyon.bossarena.data.BossRegistry;
import com.varyon.bossarena.spawn.BossSpawnService;
import com.varyon.bossarena.system.BossWaveNotificationService;
import com.varyon.bossarena.util.EntityComponents;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.player.pages.choices.ChoiceInteraction;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public final class BossArenaShopPurchaseInteraction extends ChoiceInteraction {
    private static final Logger LOGGER = Logger.getLogger("BossArena");
    private static final ScheduledExecutorService GRACE_EXECUTOR =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "BossArena-ShopGrace");
                t.setDaemon(true);
                return t;
            });
    private static final long GRACE_TITLE_REFRESH_SECONDS = 5L;

    private final String bossId;
    private final String arenaId;
    private final int cost;
    private final boolean silent;

    public BossArenaShopPurchaseInteraction(String bossId, String arenaId, int cost) {
        this(bossId, arenaId, cost, false);
    }

    public BossArenaShopPurchaseInteraction(String bossId, String arenaId, int cost, boolean silent) {
        this.bossId = bossId;
        this.arenaId = arenaId;
        this.cost = Math.max(cost, 0);
        this.silent = silent;
    }

    @Override
    public void run(Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef) {
        BossArenaPlugin plugin = BossArenaPlugin.getInstance();
        if (plugin == null) {
            return;
        }

        if (bossId == null || bossId.isBlank()) {
            playerRef.sendMessage(Message.raw("Entrée marchand sans bossId."));
            return;
        }
        if (arenaId == null || arenaId.isBlank()) {
            playerRef.sendMessage(Message.raw("Entrée marchand sans arenaId."));
            return;
        }

        BossDefinition def = BossRegistry.get(bossId);
        if (def == null) {
            playerRef.sendMessage(Message.raw("Boss introuvable : " + bossId));
            return;
        }

        Arena arena = ArenaRegistry.get(arenaId);
        if (arena == null) {
            playerRef.sendMessage(Message.raw("Arène introuvable : " + arenaId));
            return;
        }

        World world = com.hypixel.hytale.server.core.universe.Universe.get().getWorld(playerRef.getWorldUuid());
        if (world == null) {
            playerRef.sendMessage(Message.raw("Impossible de résoudre le monde."));
            return;
        }

        BossArenaConfig.TimedBossSpawn timedRule = findMatchingTimedRule(plugin, bossId, arenaId);

        world.execute(() -> {
            BossSpawnService spawnService = plugin.getBossSpawnService();
            if (spawnService == null) {
                playerRef.sendMessage(Message.raw("Service de spawn boss indisponible."));
                return;
            }

            if (spawnService.hasAnyEventInProgress()) {
                playerRef.sendMessage(Message.raw("Un événement boss est déjà en cours. Attendez que tous les boss et vagues soient terminés."));
                return;
            }

            if (cost > 0) {
                ChargeResult result = chargeCost(plugin, store, ref, playerRef, cost);
                if (!result.success()) {
                    playerRef.sendMessage(Message.raw(result.message()));
                    return;
                }
            }

            long graceSeconds = (!silent && timedRule != null && timedRule.gracePeriodEnabled)
                    ? Math.max(0L, timedRule.gracePeriodSeconds)
                    : 0L;
            if (graceSeconds > 0L) {
                playerRef.sendMessage(Message.raw("Invocation classique : grâce de " + graceSeconds + "s…"));
                beginGraceThenSpawn(spawnService, playerRef, world, arena, timedRule, graceSeconds);
                return;
            }

            finishSpawn(spawnService, playerRef, world, arena, timedRule, !silent);
        });

        LOGGER.info("Shop purchase: " + playerRef + " -> " + bossId + " @ " + arenaId
                + ", cost=" + cost + ", silent=" + silent);
    }

    private void beginGraceThenSpawn(BossSpawnService spawnService,
                                     PlayerRef playerRef,
                                     World world,
                                     Arena arena,
                                     BossArenaConfig.TimedBossSpawn timedRule,
                                     long graceSeconds) {
        final long deadlineMs = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(graceSeconds);
        final double radius = Math.max(0.0d, arena.getProximityRadius());
        final Vector3d center = arena.getPosition();
        final String graceText = timedRule != null ? timedRule.graceTitleText : null;

        Runnable tick = new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                long remaining = Math.max(0L, deadlineMs - now);
                if (remaining > 0L && radius > 0.0d && center != null) {
                    BossWaveNotificationService.notifyTimedGraceTitle(
                            world,
                            center,
                            radius,
                            resolveBossDisplayName(bossId),
                            arenaId,
                            graceText,
                            remaining
                    );
                }
                if (now < deadlineMs) {
                    GRACE_EXECUTOR.schedule(this, GRACE_TITLE_REFRESH_SECONDS, TimeUnit.SECONDS);
                    return;
                }
                world.execute(() -> {
                    if (spawnService.hasAnyEventInProgress()) {
                        playerRef.sendMessage(Message.raw("Un événement boss a démarré pendant la grâce. Invocation annulée (paiement non remboursé)."));
                        return;
                    }
                    finishSpawn(spawnService, playerRef, world, arena, timedRule, true);
                });
            }
        };
        GRACE_EXECUTOR.execute(tick);
    }

    private void finishSpawn(BossSpawnService spawnService,
                             PlayerRef playerRef,
                             World world,
                             Arena arena,
                             BossArenaConfig.TimedBossSpawn timedRule,
                             boolean allowAnnounce) {
        var uuid = spawnService.spawnBossFromJson(
                playerRef,
                bossId,
                world,
                arena.getPosition(),
                arenaId
        );

        if (uuid == null) {
            playerRef.sendMessage(Message.raw("Échec du spawn du boss : " + bossId));
            return;
        }

        if (allowAnnounce && timedRule != null
                && (timedRule.announceWorldWide || timedRule.announceCurrentWorld)) {
            BossWaveNotificationService.notifyTimedSpawn(
                    resolveBossDisplayName(bossId),
                    arenaId,
                    world,
                    timedRule.worldAnnouncementText,
                    timedRule.announceWorldWide,
                    timedRule.announceCurrentWorld,
                    timedRule.announceMinTier
            );
        }

        if (BossSpawnService.DEFERRED_SPAWN_UUID.equals(uuid)) {
            playerRef.sendMessage(Message.raw((silent ? "Invocation silencieuse" : "Séquence")
                    + " lancée pour le boss : " + bossId + ". Le boss apparaîtra après les vagues pré-boss."));
        } else {
            playerRef.sendMessage(Message.raw((silent ? "Boss invoqué en silence : " : "Boss invoqué : ") + bossId));
        }
    }

    private static BossArenaConfig.TimedBossSpawn findMatchingTimedRule(BossArenaPlugin plugin,
                                                                        String bossId,
                                                                        String arenaId) {
        BossArenaConfig config = plugin.getConfig();
        if (config == null) {
            return null;
        }
        List<BossArenaConfig.TimedBossSpawn> rules = config.getTimedBossSpawns();
        if (rules == null || rules.isEmpty()) {
            return null;
        }
        String boss = bossId == null ? "" : bossId.trim();
        String arena = arenaId == null ? "" : arenaId.trim();
        BossArenaConfig.TimedBossSpawn fallback = null;
        for (BossArenaConfig.TimedBossSpawn rule : rules) {
            if (rule == null) {
                continue;
            }
            String ruleArena = rule.arenaId == null ? "" : rule.arenaId.trim();
            if (!arena.equalsIgnoreCase(ruleArena) || !rule.poolContainsBoss(boss)) {
                continue;
            }
            if (rule.enabled) {
                return rule;
            }
            if (fallback == null) {
                fallback = rule;
            }
        }
        return fallback;
    }

    private static String resolveBossDisplayName(String configuredBossId) {
        BossDefinition def = BossRegistry.get(configuredBossId);
        if (def != null && def.bossName != null && !def.bossName.isBlank()) {
            return def.bossName.trim();
        }
        return configuredBossId == null ? "Boss" : configuredBossId.trim();
    }

    private static ChargeResult chargeCost(BossArenaPlugin plugin, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef, int amount) {
        CurrencySettings currency = resolveCurrencySettings(plugin);
        String provider = ShopCurrencySupport.sanitizeProvider(currency.provider);

        if (ShopCurrencySupport.PROVIDER_ECONOMY_SYSTEM.equals(provider)) {
            return tryChargeEconomySystem(EntityComponents.uuid(playerRef), amount);
        }

        if (ShopCurrencySupport.PROVIDER_ECOTALE.equals(provider)) {
            return tryChargeEcotale(EntityComponents.uuid(playerRef), amount);
        }

        if (ShopCurrencySupport.PROVIDER_HYMARKET.equals(provider)) {
            return tryChargeHyMarket(EntityComponents.uuid(playerRef), amount);
        }

        if (ShopCurrencySupport.PROVIDER_ITEM.equals(provider)) {
            return tryChargeItemCurrency(store, ref, currency.itemId, amount);
        }

        // Auto mode: prefer HyMarket, then Ecotale, then EconomySystem, then item currency.
        String autoProvider = ShopCurrencySupport.resolveAutoProvider();
        if (ShopCurrencySupport.PROVIDER_ECONOMY_SYSTEM.equals(autoProvider)) {
            return tryChargeEconomySystem(EntityComponents.uuid(playerRef), amount);
        }
        if (ShopCurrencySupport.PROVIDER_ECOTALE.equals(autoProvider)) {
            return tryChargeEcotale(EntityComponents.uuid(playerRef), amount);
        }
        if (ShopCurrencySupport.PROVIDER_HYMARKET.equals(autoProvider)) {
            return tryChargeHyMarket(EntityComponents.uuid(playerRef), amount);
        }
        if (currency.itemId != null && !currency.itemId.isBlank()) {
            return tryChargeItemCurrency(store, ref, currency.itemId, amount);
        }
        return ChargeResult.fail("No currency provider is available. Configure item currency or enable Ecotale/EconomySystem/HyMarketPlus.");
    }

    private static CurrencySettings resolveCurrencySettings(BossArenaPlugin plugin) {
        BossShopConfig shopConfig = plugin.getShopConfig();
        String provider = "auto";

        if (shopConfig != null) {
            if (shopConfig.currencyProvider != null && !shopConfig.currencyProvider.isBlank()) {
                provider = shopConfig.currencyProvider;
            }
            if (shopConfig.currencyItemId != null && !shopConfig.currencyItemId.isBlank()) {
                return new CurrencySettings(provider, shopConfig.currencyItemId);
            }
        }

        BossArenaConfig global = plugin.getConfig();
        if (global != null && global.currencyItemId != null && !global.currencyItemId.isBlank()) {
            return new CurrencySettings(provider, global.currencyItemId);
        }
        if (global != null && global.fallbackCurrencyItemId != null && !global.fallbackCurrencyItemId.isBlank()) {
            return new CurrencySettings(provider, global.fallbackCurrencyItemId);
        }
        return new CurrencySettings(provider, "Ingredient_Bar_Iron");
    }

    private static ChargeResult tryChargeItemCurrency(Store<EntityStore> store, Ref<EntityStore> ref, String currencyItemId, int amount) {
        if (currencyItemId == null || currencyItemId.isBlank()) {
            return ChargeResult.fail("Shop currency is not configured.");
        }
        if (!consumeCurrency(store, ref, currencyItemId, amount)) {
            return ChargeResult.fail("Not enough currency. Need "
                    + amount + " " + ItemNameResolver.resolveCommonName(currencyItemId) + ".");
        }
        return ChargeResult.ok();
    }

    private static ChargeResult tryChargeHyMarket(UUID playerUuid, int amountCopper) {
        if (!ShopCurrencySupport.isHyMarketActive()) {
            return ChargeResult.fail("HyMarketPlus currency is not available. Configure item currency or enable HyMarketPlus.");
        }
        if (!ShopCurrencySupport.removeHyMarketCopper(playerUuid, amountCopper)) {
            return ChargeResult.fail("Not enough HyMarket currency. Need " + ShopCurrencySupport.formatHyMarketCost(amountCopper) + ".");
        }
        return ChargeResult.ok();
    }

    private static ChargeResult tryChargeEconomySystem(UUID playerUuid, int amount) {
        if (!ShopCurrencySupport.isEconomySystemActive()) {
            return ChargeResult.fail("EconomySystem currency is not available. Configure item currency or enable EconomySystem.");
        }
        if (!ShopCurrencySupport.removeEconomySystemBalance(playerUuid, amount)) {
            return ChargeResult.fail("Not enough EconomySystem currency. Need "
                    + ShopCurrencySupport.formatEconomySystemCost(amount) + ".");
        }
        return ChargeResult.ok();
    }

    private static ChargeResult tryChargeEcotale(UUID playerUuid, int amount) {
        if (!ShopCurrencySupport.isEcotaleActive()) {
            return ChargeResult.fail("Ecotale currency is not available. Configure item currency or enable Ecotale.");
        }
        if (!ShopCurrencySupport.removeEcotaleBalance(playerUuid, amount)) {
            return ChargeResult.fail("Not enough Ecotale currency. Need "
                    + ShopCurrencySupport.formatEcotaleCost(amount) + ".");
        }
        return ChargeResult.ok();
    }

    private static boolean consumeCurrency(Store<EntityStore> store, Ref<EntityStore> ref, String currencyItemId, int amount) {
        com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer container = com.hypixel.hytale.server.core.inventory.InventoryComponent.getCombined(
                store,
                ref,
                com.hypixel.hytale.server.core.inventory.InventoryComponent.Storage.getComponentType(),
                com.hypixel.hytale.server.core.inventory.InventoryComponent.Hotbar.getComponentType(),
                com.hypixel.hytale.server.core.inventory.InventoryComponent.Backpack.getComponentType()
        );
        if (container == null) {
            return false;
        }

        int total = countItem(container, currencyItemId);
        if (total < amount) {
            return false;
        }

        int remaining = amount;
        short capacity = container.getCapacity();
        for (short slot = 0; slot < capacity && remaining > 0; slot++) {
            ItemStack stack = container.getItemStack(slot);
            if (ItemStack.isEmpty(stack)) {
                continue;
            }
            if (!currencyItemId.equalsIgnoreCase(stack.getItemId())) {
                continue;
            }

            int removeAmount = Math.min(remaining, stack.getQuantity());
            container.removeItemStackFromSlot(slot, removeAmount);
            remaining -= removeAmount;
        }

        return remaining == 0;
    }

    private static int countItem(ItemContainer container, String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return 0;
        }

        AtomicInteger total = new AtomicInteger();
        container.forEach((slot, stack) -> {
            if (!ItemStack.isEmpty(stack) && itemId.equalsIgnoreCase(stack.getItemId())) {
                total.addAndGet(stack.getQuantity());
            }
        });
        return total.get();
    }

    private static final class CurrencySettings {
        private final String provider;
        private final String itemId;

        private CurrencySettings(String provider, String itemId) {
            this.provider = provider;
            this.itemId = itemId;
        }
    }

    private static final class ChargeResult {
        private final boolean success;
        private final String message;

        private ChargeResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        private static ChargeResult ok() {
            return new ChargeResult(true, "");
        }

        private static ChargeResult fail(String message) {
            return new ChargeResult(false, message);
        }

        private boolean success() {
            return success;
        }

        private String message() {
            return message;
        }
    }
}
