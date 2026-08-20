package fr.varyon.stacktiers.research;

import fr.varyon.stacktiers.OverrideBridge;
import fr.varyon.stacktiers.StackCategories;

import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cycle de vie des recherches : éligibilité, démarrage (consomme les ressources), résolution
 * des recherches arrivées à échéance (tick périodique + résolution opportuniste), application
 * du palier de stack via le pont réflexif OverrideBridge.
 *
 * research-state.json (completedNodeIds par joueur) est la SEULE source de vérité pour les
 * paliers de stack — pas de fichier séparé (tier-overrides.txt a existé un temps, supprimé :
 * un palier par catégorie n'est jamais qu'un résumé du plus haut tier complété dans cette
 * catégorie, donc le stocker à part aurait dupliqué l'information et pu diverger). Le palier
 * effectif d'une catégorie se calcule à la volée via tierOf()/applyBridge().
 *
 * Le tick (5s, même cadence que BossTimedSpawnScheduler côté Varyon-BossArena) garantit
 * qu'une recherche se termine même si le joueur ne rouvre jamais l'UI ni ne se reconnecte —
 * requis pour la progression hors ligne en temps réel.
 */
public final class ResearchManager {
    private static final Logger LOGGER = Logger.getLogger("Varyon-StackTiers");
    private static final long TICK_SECONDS = 5L;

    private final OverrideBridge bridge = OverrideBridge.attach();
    private final ResearchFileStorage storage;
    private final Map<UUID, PlayerResearchState> states = new ConcurrentHashMap<>();
    private final Map<UUID, fr.varyon.stacktiers.ui.ResearchTreeUI> openPages = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Varyon-StackTiers-Research");
        t.setDaemon(true);
        return t;
    });

    public ResearchManager(Path dataDirectory) {
        this.storage = new ResearchFileStorage(dataDirectory);
    }

    public boolean isBridgeActive() {
        return bridge.isActive();
    }

    public void loadFromDisk() {
        states.putAll(storage.load());
        for (Map.Entry<UUID, PlayerResearchState> e : states.entrySet()) {
            applyBridge(e.getKey(), e.getValue());
        }
    }

    /** Palier effectif (0 si aucun) d'une catégorie pour un joueur, dérivé de completedNodeIds. */
    public int tierOf(UUID playerUuid, String category) {
        PlayerResearchState state = snapshot(playerUuid);
        int best = 0;
        for (int t = 1; t <= 3; t++) {
            ResearchNode n = ResearchTree.byCategoryTier(category, t);
            if (n != null && state.completedNodeIds.contains(n.id())) {
                best = t;
            }
        }
        return best;
    }

    /** Paliers effectifs de toutes les catégories ayant au moins un tier complété. */
    public Map<String, Integer> listTiers(UUID playerUuid) {
        Map<String, Integer> result = new HashMap<>();
        for (String category : StackCategories.KNOWN) {
            int tier = tierOf(playerUuid, category);
            if (tier > 0) {
                result.put(category, tier);
            }
        }
        return result;
    }

    /**
     * Marque toute la chaîne de prérequis jusqu'au tier donné comme complétée (utilisé par
     * /varyonstack set) et pousse immédiatement le résultat dans le bridge.
     * @return le palier précédent (0 si aucun).
     */
    public synchronized int setTier(UUID playerUuid, String category, int tier) {
        int previous = tierOf(playerUuid, category);
        PlayerResearchState state = snapshot(playerUuid);
        for (String nodeId : ResearchTree.chainUpTo(category, tier)) {
            state.completedNodeIds.add(nodeId);
        }
        applyBridge(playerUuid, state);
        persist();
        return previous;
    }

    /** Retire tous les nœuds complétés de cette catégorie (désenclenche le palier entièrement). */
    public synchronized boolean removeCategory(UUID playerUuid, String category) {
        PlayerResearchState state = snapshot(playerUuid);
        boolean removed = false;
        for (int t = 1; t <= 3; t++) {
            ResearchNode n = ResearchTree.byCategoryTier(category, t);
            if (n != null) {
                removed |= state.completedNodeIds.remove(n.id());
            }
        }
        if (removed) {
            applyBridge(playerUuid, state);
            persist();
        }
        return removed;
    }

    /** Recalcule et pousse dans le bridge le palier effectif de toutes les catégories d'un joueur. */
    private void applyBridge(UUID playerUuid, PlayerResearchState state) {
        for (String category : StackCategories.KNOWN) {
            int tier = 0;
            for (int t = 1; t <= 3; t++) {
                ResearchNode n = ResearchTree.byCategoryTier(category, t);
                if (n != null && state.completedNodeIds.contains(n.id())) {
                    tier = t;
                }
            }
            if (tier > 0) {
                bridge.put(playerUuid.toString(), category, tier);
            } else {
                bridge.remove(playerUuid.toString(), category);
            }
        }
    }

    public void start() {
        executor.scheduleAtFixedRate(this::tickSafely, TICK_SECONDS, TICK_SECONDS, TimeUnit.SECONDS);
    }

    public void shutdown() {
        persist();
        executor.shutdownNow();
    }

    public enum Eligibility {
        OK, ALREADY_COMPLETED, PREREQ_MISSING, RESEARCH_IN_PROGRESS, INSUFFICIENT_RESOURCES, UNKNOWN_NODE
    }

    /** Quantité de l'item requis actuellement détenue par le joueur (stockage + barre d'action). */
    public int countOwned(ResearchNode node, ItemContainer storageContainer, ItemContainer hotbarContainer) {
        int owned = 0;
        if (storageContainer != null) {
            owned += storageContainer.countItemStacks(s -> node.resourceItemId().equals(s.getItemId()));
        }
        if (hotbarContainer != null) {
            owned += hotbarContainer.countItemStacks(s -> node.resourceItemId().equals(s.getItemId()));
        }
        return owned;
    }

    public synchronized PlayerResearchState snapshot(UUID playerUuid) {
        return states.computeIfAbsent(playerUuid, u -> new PlayerResearchState());
    }

    /** Enregistre la page ouverte par un joueur pour recevoir le countdown live ; à appeler à l'ouverture. */
    public void registerOpenPage(UUID playerUuid, fr.varyon.stacktiers.ui.ResearchTreeUI page) {
        openPages.put(playerUuid, page);
    }

    /** À appeler à la fermeture de la page pour arrêter les mises à jour inutiles. */
    public void unregisterOpenPage(UUID playerUuid) {
        openPages.remove(playerUuid);
    }

    public synchronized Eligibility checkEligibility(UUID playerUuid, String nodeId,
                                                       ItemContainer storageContainer, ItemContainer hotbarContainer) {
        ResearchNode node = ResearchTree.byId(nodeId);
        if (node == null) {
            return Eligibility.UNKNOWN_NODE;
        }
        PlayerResearchState state = snapshot(playerUuid);
        if (state.completedNodeIds.contains(nodeId)) {
            return Eligibility.ALREADY_COMPLETED;
        }
        if (state.inProgressNodeId != null) {
            return Eligibility.RESEARCH_IN_PROGRESS;
        }
        for (String prereq : node.prerequisiteIds()) {
            if (!state.completedNodeIds.contains(prereq)) {
                return Eligibility.PREREQ_MISSING;
            }
        }
        int available = 0;
        if (storageContainer != null) {
            available += storageContainer.countItemStacks(s -> node.resourceItemId().equals(s.getItemId()));
        }
        if (hotbarContainer != null) {
            available += hotbarContainer.countItemStacks(s -> node.resourceItemId().equals(s.getItemId()));
        }
        if (available < node.resourceQuantity()) {
            return Eligibility.INSUFFICIENT_RESOURCES;
        }
        return Eligibility.OK;
    }

    /** Consomme les ressources et démarre le minuteur. Le caller doit avoir résolu les conteneurs sur le world thread. */
    public synchronized Eligibility startResearch(UUID playerUuid, String nodeId,
                                                    ItemContainer storageContainer, ItemContainer hotbarContainer) {
        Eligibility check = checkEligibility(playerUuid, nodeId, storageContainer, hotbarContainer);
        if (check != Eligibility.OK) {
            return check;
        }
        ResearchNode node = ResearchTree.byId(nodeId);
        if (!consumeResources(storageContainer, hotbarContainer, node.resourceItemId(), node.resourceQuantity())) {
            return Eligibility.INSUFFICIENT_RESOURCES;
        }
        PlayerResearchState state = states.get(playerUuid);
        long now = System.currentTimeMillis();
        state.inProgressNodeId = nodeId;
        state.startEpochMs = now;
        state.completionEpochMs = now + node.durationMillis();
        persist();
        return Eligibility.OK;
    }

    /** Retire qty depuis storage puis hotbar (tout-ou-rien), rembourse en cas d'échec partiel. */
    private boolean consumeResources(ItemContainer storage, ItemContainer hotbar, String itemId, int qty) {
        if (qty <= 0) {
            return true;
        }
        int remaining = qty;
        if (storage != null && remaining > 0) {
            remaining -= removeUpTo(storage, itemId, remaining);
        }
        if (hotbar != null && remaining > 0) {
            remaining -= removeUpTo(hotbar, itemId, remaining);
        }
        if (remaining > 0) {
            giveBack(storage, hotbar, itemId, qty - remaining);
            return false;
        }
        return true;
    }

    private int removeUpTo(ItemContainer container, String itemId, int qty) {
        try {
            var tx = container.removeItemStack(new ItemStack(itemId, qty), false, true);
            ItemStack remainder = tx != null ? tx.getRemainder() : null;
            int notRemoved = remainder == null ? 0 : remainder.getQuantity();
            return qty - notRemoved;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private void giveBack(ItemContainer storage, ItemContainer hotbar, String itemId, int qty) {
        if (qty <= 0) {
            return;
        }
        int remaining = qty;
        remaining -= giveUpTo(storage, itemId, remaining);
        if (remaining > 0) {
            giveUpTo(hotbar, itemId, remaining);
        }
    }

    private int giveUpTo(ItemContainer container, String itemId, int qty) {
        if (container == null || qty <= 0) {
            return 0;
        }
        try {
            var tx = container.addItemStack(new ItemStack(itemId, qty), false, false, true);
            int remainder = tx != null && tx.getRemainder() != null ? tx.getRemainder().getQuantity() : 0;
            return qty - remainder;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private void tickSafely() {
        try {
            tick();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur pendant le tick de résolution des recherches", e);
        }
    }

    private void tick() {
        long now = System.currentTimeMillis();
        boolean changed = false;
        for (Map.Entry<UUID, PlayerResearchState> e : states.entrySet()) {
            changed |= resolveIfDue(e.getKey(), e.getValue(), now);
        }
        if (changed) {
            persist();
        }
        for (Map.Entry<UUID, fr.varyon.stacktiers.ui.ResearchTreeUI> e : openPages.entrySet()) {
            try {
                e.getValue().refreshNodeState();
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Échec du rafraîchissement de l'UI de recherche pour " + e.getKey(), ex);
            }
        }
    }

    /** Résout une recherche arrivée à échéance. Idempotent — sans effet si rien n'est dû. */
    public synchronized boolean resolveIfDue(UUID playerUuid, PlayerResearchState state, long now) {
        if (state.inProgressNodeId == null || now < state.completionEpochMs) {
            return false;
        }
        ResearchNode node = ResearchTree.byId(state.inProgressNodeId);
        if (node != null) {
            state.completedNodeIds.add(node.id());
            applyBridge(playerUuid, state);
        }
        state.inProgressNodeId = null;
        state.startEpochMs = 0L;
        state.completionEpochMs = 0L;
        return true;
    }

    private void persist() {
        try {
            storage.save(states);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Échec de sauvegarde de research-state.json", e);
        }
    }
}
