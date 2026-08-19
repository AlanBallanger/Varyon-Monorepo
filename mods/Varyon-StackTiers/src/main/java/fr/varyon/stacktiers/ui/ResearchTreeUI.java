package fr.varyon.stacktiers.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import fr.varyon.stacktiers.research.PlayerResearchState;
import fr.varyon.stacktiers.research.ResearchItemNames;
import fr.varyon.stacktiers.research.ResearchLabels;
import fr.varyon.stacktiers.research.ResearchManager;
import fr.varyon.stacktiers.research.ResearchNode;
import fr.varyon.stacktiers.research.ResearchTree;
import fr.varyon.stacktiers.util.InventoryCompat;

import javax.annotation.Nonnull;
import java.awt.Color;
import java.util.List;
import java.util.UUID;

/**
 * Page de l'arbre de recherche : 39 nœuds, un bouton par nœud disponible pour lancer une
 * recherche. Countdown affiché en direct — voir ResearchTickSystem qui rappelle sendUpdate()
 * périodiquement pour les joueurs ayant cette page ouverte.
 */
public final class ResearchTreeUI extends InteractiveCustomUIPage<ResearchTreeUI.Data> {
    private final PlayerRef playerRef;
    private final ResearchManager researchManager;

    public ResearchTreeUI(@Nonnull PlayerRef playerRef, @Nonnull ResearchManager researchManager) {
        super(playerRef, CustomPageLifetime.CanDismiss, Data.CODEC);
        this.playerRef = playerRef;
        this.researchManager = researchManager;
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder uiBuilder,
                       @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        UUID uuid = playerRef.getUuid();
        researchManager.resolveIfDue(uuid, researchManager.snapshot(uuid), System.currentTimeMillis());
        PlayerResearchState state = researchManager.snapshot(uuid);

        uiBuilder.append("ResearchTreePage.ui");
        applyNodeState(uiBuilder, eventBuilder, state);
        researchManager.registerOpenPage(uuid, this);
    }

    @Override
    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        researchManager.unregisterOpenPage(playerRef.getUuid());
    }

    /** Rafraîchit uniquement l'état visuel des nœuds (countdown live) sans reconstruire toute la page. */
    public void refreshNodeState() {
        UUID uuid = playerRef.getUuid();
        researchManager.resolveIfDue(uuid, researchManager.snapshot(uuid), System.currentTimeMillis());
        PlayerResearchState state = researchManager.snapshot(uuid);

        UICommandBuilder cmd = new UICommandBuilder();
        applyNodeState(cmd, null, state);
        sendUpdate(cmd, null, false);
    }

    private void applyNodeState(UICommandBuilder uiBuilder, UIEventBuilder eventBuilder, PlayerResearchState state) {
        long now = System.currentTimeMillis();
        ResearchTreeLayout.drawEdges(uiBuilder);

        for (ResearchNode node : ResearchTree.all()) {
            String prefix = "#ResearchNode" + ResearchTreeLayout.uiId(node);
            NodeVisual visual = computeVisual(node, state);

            // L'état est porté par la couleur de fond, pas par un texte : sombre + voile
            // = verrouillé, marron = disponible, vert = débloqué. La ligne en bas à droite
            // affiche la durée, ou le compte à rebours si la recherche est en cours.
            uiBuilder.set(prefix + "StateText.TextSpans", Message.raw(labelFor(node, visual, state, now)));
            uiBuilder.set(prefix + "Veil.Visible", visual == NodeVisual.LOCKED);
            uiBuilder.set(prefix + "Ready.Visible",
                    visual == NodeVisual.AVAILABLE || visual == NodeVisual.IN_PROGRESS);
            uiBuilder.set(prefix + "Done.Visible", visual == NodeVisual.COMPLETED);
            uiBuilder.set(prefix + "Icon.ItemId", node.resourceItemId());

            if (eventBuilder != null && visual == NodeVisual.AVAILABLE) {
                eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, prefix,
                        EventData.of("Action", "start").append("Node", node.id()), false);
            }
        }
    }

    /**
     * Trois états visuels seulement, plus l'état transitoire d'une recherche en cours :
     * verrouillé (grisé), disponible, ou débloqué (terminé).
     */
    private enum NodeVisual { LOCKED, AVAILABLE, IN_PROGRESS, COMPLETED }

    private NodeVisual computeVisual(ResearchNode node, PlayerResearchState state) {
        if (state.completedNodeIds.contains(node.id())) {
            return NodeVisual.COMPLETED;
        }
        if (node.id().equals(state.inProgressNodeId)) {
            return NodeVisual.IN_PROGRESS;
        }
        return ResearchTree.isUnlockable(node, state.completedNodeIds)
                ? NodeVisual.AVAILABLE
                : NodeVisual.LOCKED;
    }

    /**
     * Ligne en bas à droite de la carte : durée de la recherche, ou temps restant quand
     * elle est en cours. Le statut lui-même est porté par la couleur de fond de la carte.
     */
    private String labelFor(ResearchNode node, NodeVisual visual, PlayerResearchState state, long now) {
        return switch (visual) {
            case IN_PROGRESS -> formatRemaining(state.completionEpochMs - now);
            case COMPLETED -> "";
            default -> formatRemaining(node.durationMillis());
        };
    }

    private String formatRemaining(long millis) {
        if (millis <= 0) {
            return "Prêt";
        }
        long totalMinutes = millis / 60000L;
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        return hours > 0 ? hours + "h" + String.format("%02d", minutes) : minutes + "min";
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull Data data) {
        super.handleDataEvent(ref, store, data);
        if (data.action == null) {
            return;
        }
        if ("start".equals(data.action) && data.node != null) {
            Player player = store.getComponent(ref, Player.getComponentType());
            if (player == null) {
                return;
            }
            ItemContainer storageC = InventoryCompat.getStorageContainer(player);
            ItemContainer hotbarC = InventoryCompat.getHotbarContainer(player);
            ResearchManager.Eligibility result = researchManager.startResearch(playerRef.getUuid(), data.node, storageC, hotbarC);
            if (result == ResearchManager.Eligibility.OK) {
                rebuild();
            } else {
                ResearchNode node = ResearchTree.byId(data.node);
                playerRef.sendMessage(
                        Message.raw(messageFor(result, node, storageC, hotbarC)).color(Color.RED));
            }
        }
    }

    private String messageFor(ResearchManager.Eligibility result, ResearchNode node,
                              ItemContainer storageC, ItemContainer hotbarC) {
        return switch (result) {
            case ALREADY_COMPLETED -> "Cette recherche est déjà débloquée.";
            case PREREQ_MISSING -> missingPrerequisitesMessage(node);
            case RESEARCH_IN_PROGRESS -> "Une recherche est déjà en cours.";
            case INSUFFICIENT_RESOURCES -> missingResourcesMessage(node, storageC, hotbarC);
            case UNKNOWN_NODE -> "Nœud de recherche inconnu.";
            case OK -> "";
        };
    }

    /** Détaille ce qu'il manque : item requis, quantité possédée et quantité restante à ramener. */
    private String missingResourcesMessage(ResearchNode node, ItemContainer storageC, ItemContainer hotbarC) {
        if (node == null) {
            return "Ressources insuffisantes pour cette recherche.";
        }
        int owned = researchManager.countOwned(node, storageC, hotbarC);
        int missing = Math.max(0, node.resourceQuantity() - owned);
        return "Il manque " + missing + "x " + ResearchItemNames.of(node.resourceItemId())
                + " (" + owned + "/" + node.resourceQuantity() + ").";
    }

    /** Nomme les recherches à débloquer avant celle-ci. */
    private String missingPrerequisitesMessage(ResearchNode node) {
        if (node == null) {
            return "Recherche verrouillée.";
        }
        PlayerResearchState state = researchManager.snapshot(playerRef.getUuid());
        List<String> missing = ResearchTree.missingPrerequisites(node, state.completedNodeIds);
        if (missing.isEmpty()) {
            return "Recherche verrouillée.";
        }
        StringBuilder sb = new StringBuilder("Recherche verrouillée. À débloquer d'abord : ");
        for (int i = 0; i < missing.size(); i++) {
            ResearchNode prereq = ResearchTree.byId(missing.get(i));
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(prereq == null ? missing.get(i)
                    : ResearchLabels.category(prereq.category()) + " Tier " + prereq.tier());
        }
        return sb.append('.').toString();
    }

    public static final class Data {
        public String action;
        public String node;

        public static final BuilderCodec<Data> CODEC = BuilderCodec.builder(Data.class, Data::new)
                .addField(new KeyedCodec<>("Action", Codec.STRING), (d, v) -> d.action = v, d -> d.action)
                .addField(new KeyedCodec<>("Node", Codec.STRING), (d, v) -> d.node = v, d -> d.node)
                .build();
    }
}
