package com.varyon.faction;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.query.QueryOptions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.logging.Level;

public class FactionManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public enum Faction {
        FRACTURE("Fracture",  1,  "group.fracture"),
        NOYAU   ("Noyau",   -1,  "group.noyau");

        private final String displayName;
        private final int    balanceMultiplier;
        private final String permission;

        Faction(String displayName, int balanceMultiplier, String permission) {
            this.displayName       = displayName;
            this.balanceMultiplier = balanceMultiplier;
            this.permission        = permission;
        }

        public String getDisplayName()    { return displayName; }
        public int getBalanceMultiplier() { return balanceMultiplier; }
        public String getPermission()     { return permission; }

        /**
         * Solde brut de la jauge ({@code rawBalance}, positif = Fracture) ramené au point de vue de cette faction.
         */
        public int perspectiveGlobalBalance(int rawBalance) {
            return rawBalance * balanceMultiplier;
        }

        @Nullable
        public static Faction fromString(String name) {
            for (Faction f : values()) {
                if (f.name().equalsIgnoreCase(name)) return f;
            }
            return null;
        }
    }

    @Nonnull
    public static String factionPointsAfterDepositLine(int rawGlobalBalance, int gaugeAbsMax, @Nonnull Faction faction) {
        int shown = faction.perspectiveGlobalBalance(rawGlobalBalance);
        return "Le nombre de points de ta faction est monté à " + shown + "/" + gaugeAbsMax;
    }

    /**
     * Primary method — uses LuckPerms API via PlayerRef.
     * Falls back to Player#hasPermission if LuckPerms is unavailable.
     */
    @Nullable
    public Faction getFaction(@Nullable PlayerRef playerRef) {
        if (playerRef == null) {
            LOGGER.at(Level.WARNING).log("[Faction] getFaction(PlayerRef) called with null");
            return null;
        }
        return getFactionDebug(playerRef, false);
    }

    /**
     * Same as getFaction but logs everything at INFO level — call from /varyon whois.
     */
    @Nullable
    public Faction getFactionVerbose(@Nullable PlayerRef playerRef) {
        if (playerRef == null) return null;
        return getFactionDebug(playerRef, true);
    }

    private Faction getFactionDebug(@Nullable PlayerRef playerRef, boolean verbose) {
        Level lvl = verbose ? Level.INFO : Level.FINE;
        String name = playerRef != null ? playerRef.getUsername() : "null";
        try {
            LuckPerms lp = LuckPermsProvider.get();
            var adapter  = lp.getPlayerAdapter(PlayerRef.class);
            User user    = adapter.getUser(playerRef);
            LOGGER.at(lvl).log("[Faction] User obtained for " + name);

            // Use NodeType.INHERITANCE to check actual group membership,
            // unaffected by wildcard (*) permissions.
            boolean directFracture = user.getNodes(NodeType.INHERITANCE).stream()
                .anyMatch(n -> "fracture".equalsIgnoreCase(n.getGroupName()) && n.getValue());
            boolean directNoyau = user.getNodes(NodeType.INHERITANCE).stream()
                .anyMatch(n -> "noyau".equalsIgnoreCase(n.getGroupName()) && n.getValue());

            LOGGER.at(lvl).log("[Faction] " + name + " direct nodes → fracture=" + directFracture + " noyau=" + directNoyau);

            if (directFracture && !directNoyau) return Faction.FRACTURE;
            if (directNoyau    && !directFracture) return Faction.NOYAU;

            // Transitive check: user may be in a parent group that inherits fracture/noyau
            boolean transFracture = user.resolveInheritedNodes(QueryOptions.nonContextual()).stream()
                .filter(NodeType.INHERITANCE::matches)
                .map(NodeType.INHERITANCE::cast)
                .anyMatch(n -> "fracture".equalsIgnoreCase(n.getGroupName()) && n.getValue());
            boolean transNoyau = user.resolveInheritedNodes(QueryOptions.nonContextual()).stream()
                .filter(NodeType.INHERITANCE::matches)
                .map(NodeType.INHERITANCE::cast)
                .anyMatch(n -> "noyau".equalsIgnoreCase(n.getGroupName()) && n.getValue());

            LOGGER.at(lvl).log("[Faction] " + name + " transitive → fracture=" + transFracture + " noyau=" + transNoyau);

            if (transFracture && !transNoyau) return Faction.FRACTURE;
            if (transNoyau    && !transFracture) return Faction.NOYAU;

            LOGGER.at(lvl).log("[Faction] " + name + " → aucune faction (direct=" + directFracture + "/" + directNoyau
                + " trans=" + transFracture + "/" + transNoyau + ")");
            return null;

        } catch (Exception e) {
            LOGGER.at(Level.WARNING).log("[Faction] Exception pour " + name
                + ": " + e.getClass().getSimpleName() + " — " + e.getMessage());
            return null;
        }
    }

    @Nullable
    public Faction getFaction(@Nullable Player player) {
        if (player == null) return null;
        // Universe.get().getPlayer() is thread-safe, no store access needed
        PlayerRef ref = Universe.get().getPlayer(player.getUuid());
        if (ref != null) return getFaction(ref);
        LOGGER.at(Level.WARNING).log("[Faction] Player UUID " + player.getUuid() + " not found in Universe");
        return null;
    }

    /** UUID-only fallback — cannot check permissions without a live entity. */
    @Nullable
    public Faction getFaction(@Nullable UUID uuid) {
        return null;
    }
}
