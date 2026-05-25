package com.varyon.config;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.moandjiezana.toml.Toml;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.cacheddata.CachedPermissionData;
import net.luckperms.api.query.QueryOptions;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class ZonePermissionsConfig {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String FILENAME          = "zone_permissions.toml";
    private static final String SECTION_PERMS     = "zone_permissions";
    private static final String SECTION_MAX       = "max_essence";

    private final Map<Integer, String>  permissionByZone;
    private final Map<Integer, Integer> maxEssenceByZone;
    private final int adminBase;
    private final int maxZone;

    public ZonePermissionsConfig(@Nonnull Map<Integer, String> permissionByZone,
                                 @Nonnull Map<Integer, Integer> maxEssenceByZone,
                                 int adminBase) {
        this.permissionByZone = new HashMap<>(permissionByZone);
        this.maxEssenceByZone = new HashMap<>(maxEssenceByZone);
        this.adminBase        = adminBase;
        this.maxZone          = permissionByZone.keySet().stream().mapToInt(i -> i).max().orElse(10);
    }

    @Nonnull
    public String getPermissionForZone(int zoneId) {
        return permissionByZone.getOrDefault(zoneId, "varyon.zone." + zoneId);
    }

    private int highestZoneGranted(@Nonnull CachedPermissionData permData) {
        for (int z = maxZone; z >= 1; z--) {
            if (permData.checkPermission(getPermissionForZone(z)).asBoolean()) {
                return z;
            }
        }
        return 1;
    }

    /**
     * Returns the highest zone ID the player can access.
     * Having varyon.zone.5 grants access to zones 1–5.
     */
    public int getMaxAccessibleZone(@Nonnull Player player) {
        int best = 1;
        try {
            PlayerRef playerRef = Universe.get().getPlayer(player.getUuid());
            if (playerRef != null) {
                LuckPerms lp = LuckPermsProvider.get();
                User user = lp.getPlayerAdapter(PlayerRef.class).getUser(playerRef);
                if (user != null) {
                    CachedPermissionData nonCtx = user.getCachedData().getPermissionData(QueryOptions.nonContextual());
                    best = Math.max(best, highestZoneGranted(nonCtx));
                    CachedPermissionData active = user.getCachedData().getPermissionData();
                    best = Math.max(best, highestZoneGranted(active));
                }
            }
        } catch (Throwable t) {
            LOGGER.at(Level.FINE).log("getMaxAccessibleZone: LuckPerms path failed ({0})", t.toString());
        }
        for (int z = maxZone; z >= 1; z--) {
            if (player.hasPermission(getPermissionForZone(z))) {
                return Math.max(best, z);
            }
        }
        return best;
    }

    /**
     * Returns true if the player may access the given zone.
     */
    public boolean canAccessZone(@Nonnull Player player, int zoneId) {
        return getMaxAccessibleZone(player) >= zoneId;
    }

    /**
     * Returns the configured max essence cap for the given zone level.
     */
    public int getMaxEssenceForZone(int zone) {
        return maxEssenceByZone.getOrDefault(zone, 1000);
    }

    /**
     * Returns the effective max essence cap for this player.
     * <ul>
     *   <li>Admins (varyon.admin): max(adminBase, currentEssence) — soft cap that grows with admin-given essence</li>
     *   <li>Others: hard cap from their highest zone permission</li>
     * </ul>
     */
    public int getEffectiveCap(@Nonnull Player player, double currentEssence) {
        if (player.hasPermission("varyon.admin")) {
            return (int) Math.max(adminBase, currentEssence);
        }
        int zone = getMaxAccessibleZone(player);
        return maxEssenceByZone.getOrDefault(zone, 1000);
    }

    @Nonnull
    public static ZonePermissionsConfig load(@Nonnull Path dataFolder) {
        File file = dataFolder.resolve(FILENAME).toFile();
        if (!file.exists()) {
            ZonePermissionsConfig def = createDefault();
            def.save(dataFolder);
            return def;
        }
        try {
            Toml toml = new Toml().read(file);

            // Zone permissions
            Map<Integer, String> perms = new HashMap<>();
            Toml permsSection = toml.getTable(SECTION_PERMS);
            if (permsSection != null) {
                for (Map.Entry<String, Object> e : permsSection.toMap().entrySet()) {
                    try {
                        int z = Integer.parseInt(e.getKey());
                        if (e.getValue() instanceof String s && !s.isBlank()) perms.put(z, s);
                    } catch (NumberFormatException ignored) {}
                }
            }

            // Max essence caps
            Map<Integer, Integer> caps = new HashMap<>();
            int adminBase = 3000;
            Toml maxSection = toml.getTable(SECTION_MAX);
            if (maxSection != null) {
                for (Map.Entry<String, Object> e : maxSection.toMap().entrySet()) {
                    if ("admin_base".equals(e.getKey())) {
                        if (e.getValue() instanceof Number n) adminBase = n.intValue();
                        continue;
                    }
                    try {
                        int z = Integer.parseInt(e.getKey());
                        if (e.getValue() instanceof Number n) caps.put(z, n.intValue());
                    } catch (NumberFormatException ignored) {}
                }
            }
            if (caps.isEmpty()) caps = defaultMaxEssenceCaps();

            LOGGER.at(Level.INFO).log("Loaded {0}: {1} zones, {2} caps", FILENAME, perms.size(), caps.size());
            return new ZonePermissionsConfig(perms, caps, adminBase);
        } catch (Exception e) {
            LOGGER.at(Level.SEVERE).log("Failed to load " + FILENAME + ", using defaults", e);
            return createDefault();
        }
    }

    public void save(@Nonnull Path dataFolder) {
        File file = dataFolder.resolve(FILENAME).toFile();
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(generateToml());
        } catch (IOException e) {
            LOGGER.at(Level.SEVERE).log("Failed to save " + FILENAME, e);
        }
    }

    @Nonnull
    private String generateToml() {
        StringBuilder sb = new StringBuilder();

        sb.append("# Zone Access Permissions\n");
        sb.append("# zoneId = \"permission.node\"\n");
        sb.append("# Having varyon.zone.5 grants access to zones 1–5 (not 6+).\n\n");
        sb.append("[").append(SECTION_PERMS).append("]\n");
        for (int z = 1; z <= 10; z++) {
            sb.append(z).append(" = \"").append(permissionByZone.getOrDefault(z, "varyon.zone." + z)).append("\"\n");
        }

        sb.append("\n# Plafonds de points de faction (max_essence)\n");
        sb.append("# admin_base : plafond par défaut pour varyon.admin (souple — suit les points ajoutés par commandes admin)\n");
        sb.append("# zoneId = max_essence : plafond pour les joueurs selon la permission de zone la plus haute\n\n");
        sb.append("[").append(SECTION_MAX).append("]\n");
        sb.append("admin_base = ").append(adminBase).append("\n");
        for (int z = 1; z <= 10; z++) {
            sb.append(z).append(" = ").append(maxEssenceByZone.getOrDefault(z, defaultCapFor(z))).append("\n");
        }

        return sb.toString();
    }

    @Nonnull
    public static ZonePermissionsConfig createDefault() {
        Map<Integer, String>  perms = new HashMap<>();
        for (int z = 1; z <= 10; z++) perms.put(z, "varyon.zone." + z);
        return new ZonePermissionsConfig(perms, defaultMaxEssenceCaps(), 3000);
    }

    @Nonnull
    private static Map<Integer, Integer> defaultMaxEssenceCaps() {
        Map<Integer, Integer> caps = new HashMap<>();
        for (int z = 1; z <= 10; z++) caps.put(z, defaultCapFor(z));
        return caps;
    }

    private static int defaultCapFor(int zone) {
        if (zone <= 1)  return 1000;
        if (zone >= 10) return 3000;
        return 1000 + (zone - 1) * 200;
    }
}
