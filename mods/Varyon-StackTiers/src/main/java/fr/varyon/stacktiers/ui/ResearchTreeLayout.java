package fr.varyon.stacktiers.ui;

import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;

import fr.varyon.stacktiers.research.ResearchNode;
import fr.varyon.stacktiers.research.ResearchTree;

import java.util.HashMap;
import java.util.Map;

/**
 * Positions des cartes de l'arbre et tracé des liens parent -> enfant.
 *
 * Les liens sont dessinés avec un pool de segments rectangulaires déclarés dans le .ui
 * (#ResearchEdgeSegN, invisibles par défaut) qu'on positionne à la volée — même technique
 * que TalentTreeEdgeLayout dans Varyon-RPG, le moteur UI n'ayant pas de primitive "ligne".
 */
final class ResearchTreeLayout {

    static final int CARD_W = 130;
    static final int CARD_H = 110;
    static final int EDGE_POOL = 320;

    private static final int RAIL = 3;
    private static final String EDGE_PREFIX = "#ResearchEdgeSeg";

    static final int SIDEBAR_W = 100;
    private static final int[] COL_X = {120, 280, 440, 600, 760};
    private static final int ROW_H = 180;
    private static final int TOP = 20;

    /** id de nœud (sans underscore) -> {x, y} du coin haut-gauche de sa carte. */
    private static final Map<String, int[]> POSITIONS = new HashMap<>();

    static {
        // La position d'un nœud est directement dérivée de son (column, row) : row n'est plus
        // recompacté par colonne (chaque valeur de row correspond à une bande de tier précise,
        // voir ResearchTree — recompacter casserait l'alignement des 3 bandes "Palier").
        for (ResearchNode n : ResearchTree.all()) {
            POSITIONS.put(uiId(n), new int[]{COL_X[n.column()], TOP + n.row() * ROW_H});
        }
    }

    /** Les sélecteurs #Id du moteur UI n'acceptent pas l'underscore : "wood_1" -> "wood1". */
    static String uiId(ResearchNode node) {
        return node.id().replace("_", "");
    }

    static int[] positionOf(ResearchNode node) {
        return POSITIONS.get(uiId(node));
    }

    /**
     * Trace les liens en deux couches distinctes plutôt qu'un coude par prérequis : ça évite
     * les trajectoires qui remontent/redescendent sur la même hauteur (liens dont le parent et
     * l'enfant partagent la même rangée) et qui donnaient l'impression de traits dédoublés.
     *
     * Couche verticale : un trait continu par colonne, reliant chaque nœud au nœud juste en
     * dessous dans la même colonne — toujours dessiné, indépendamment des prérequis réels,
     * pour la continuité visuelle de la colonne.
     *
     * Couche transversale, modèle "bus" : un nœud avec plusieurs prérequis/enfants transversaux
     * sur le même rail (même rangée d'origine) ne trace PAS un trait par lien — un empilement de
     * traits presque superposés, décalés de quelques pixels, donnait un effet de rayures dès
     * qu'une dizaine de liens partageaient le même rail (ex. fish_1 servant de prérequis à
     * plusieurs nœuds de la rangée suivante). À la place, chaque rail utilisé n'a qu'UN SEUL
     * trait horizontal, qui couvre toute la plage de colonnes touchée par les liens de ce rail ;
     * chaque colonne concernée se raccorde à ce bus par un unique connecteur vertical partagé.
     */
    static void drawEdges(UICommandBuilder ui) {
        int seg = 0;

        for (int col = 0; col < COL_X.length; col++) {
            java.util.List<ResearchNode> ordered = nodesInColumn(col);
            for (int i = 0; i + 1 < ordered.size(); i++) {
                int[] pp = positionOf(ordered.get(i));
                int[] cp = positionOf(ordered.get(i + 1));
                seg = drawVertical(ui, seg, pp, cp);
            }
        }

        // Regroupe tous les liens transversaux par rail (rangée d'origine du parent ; les liens
        // same-row utilisent leur propre rangée). Chaque rail devient un seul bus horizontal.
        Map<Integer, java.util.List<int[][]>> byRail = new HashMap<>();
        for (ResearchNode child : ResearchTree.all()) {
            int[] cp = positionOf(child);
            for (String parentId : child.prerequisiteIds()) {
                ResearchNode parent = ResearchTree.byId(parentId);
                if (parent == null || parent.column() == child.column()) {
                    continue;
                }
                int[] pp = positionOf(parent);
                if (pp == null || cp == null) {
                    continue;
                }
                int rail = pp[1] == cp[1] ? pp[1] : (pp[1] - TOP) / ROW_H;
                byRail.computeIfAbsent(rail, k -> new java.util.ArrayList<>()).add(new int[][]{pp, cp});
            }
        }

        for (Map.Entry<Integer, java.util.List<int[][]>> e : byRail.entrySet()) {
            seg = drawRailBus(ui, seg, e.getValue());
        }

        for (int i = seg; i < EDGE_POOL; i++) {
            ui.set(EDGE_PREFIX + i + ".Visible", false);
        }
    }

    private static java.util.List<ResearchNode> nodesInColumn(int col) {
        java.util.List<ResearchNode> ordered = new java.util.ArrayList<>();
        for (ResearchNode n : ResearchTree.all()) {
            if (n.column() == col) {
                ordered.add(n);
            }
        }
        ordered.sort(java.util.Comparator.comparingInt(ResearchNode::row));
        return ordered;
    }

    private static int drawVertical(UICommandBuilder ui, int seg, int[] from, int[] to) {
        int cx = from[0] + CARD_W / 2;
        int top = from[1] + CARD_H;
        int bottom = to[1];
        return showEdge(ui, seg, cx - RAIL / 2, top, RAIL, bottom - top);
    }

    /**
     * Dessine tous les liens d'un même rail comme un seul bus horizontal : une barre continue
     * de la colonne la plus à gauche à la plus à droite parmi les liens de ce rail, plus un
     * connecteur vertical unique par colonne effectivement utilisée (dédupliqué — plusieurs
     * liens peuvent partager le même point d'ancrage). Distingue same-row (pas de rangée
     * intermédiaire, bus au centre vertical des cartes) et multi-row (bus à mi-hauteur entre
     * les deux rangées).
     */
    private static int drawRailBus(UICommandBuilder ui, int seg, java.util.List<int[][]> edges) {
        boolean sameRow = edges.get(0)[0][1] == edges.get(0)[1][1];
        int midY;
        if (sameRow) {
            midY = edges.get(0)[0][1] + CARD_H / 2 - RAIL / 2;
        } else {
            int fromRow = (edges.get(0)[0][1] - TOP) / ROW_H;
            int fromBottom = edges.get(0)[0][1] + CARD_H;
            int toTop = edges.get(0)[1][1];
            midY = TOP + fromRow * ROW_H + CARD_H + (ROW_H - CARD_H) / 2;
            midY = Math.max(midY, fromBottom + 8);
            midY = Math.min(midY, toTop - 8);
        }

        int minCx = Integer.MAX_VALUE;
        int maxCx = Integer.MIN_VALUE;
        // x -> plage verticale [yFrom,yTo] du connecteur à tracer pour cette colonne (un seul
        // par x, même si plusieurs liens du bus partagent cette colonne comme ancrage).
        Map<Integer, int[]> stubs = new java.util.TreeMap<>();
        for (int[][] edge : edges) {
            int fromCx = edge[0][0] + CARD_W / 2;
            int toCx = edge[1][0] + CARD_W / 2;
            minCx = Math.min(minCx, Math.min(fromCx, toCx));
            maxCx = Math.max(maxCx, Math.max(fromCx, toCx));
            if (sameRow) {
                continue;
            }
            int fromBottom = edge[0][1] + CARD_H;
            stubs.putIfAbsent(fromCx, new int[]{fromBottom, midY});
            int toTop = edge[1][1];
            stubs.putIfAbsent(toCx, new int[]{midY, toTop});
        }

        seg = showEdge(ui, seg, minCx - RAIL / 2, midY, maxCx - minCx + RAIL, RAIL);

        for (Map.Entry<Integer, int[]> e : stubs.entrySet()) {
            int cx = e.getKey();
            int yFrom = e.getValue()[0];
            int yTo = e.getValue()[1];
            seg = showEdge(ui, seg, cx - RAIL / 2, Math.min(yFrom, yTo), RAIL, Math.abs(yTo - yFrom));
        }
        return seg;
    }

    private static int showEdge(UICommandBuilder ui, int seg, int left, int top, int width, int height) {
        if (seg >= EDGE_POOL || width <= 0 || height <= 0) {
            return seg;
        }
        Anchor a = new Anchor();
        a.setLeft(Value.of(left));
        a.setTop(Value.of(top));
        a.setWidth(Value.of(width));
        a.setHeight(Value.of(height));
        ui.setObject(EDGE_PREFIX + seg + ".Anchor", a);
        ui.set(EDGE_PREFIX + seg + ".Visible", true);
        return seg + 1;
    }

    private ResearchTreeLayout() {
    }
}
