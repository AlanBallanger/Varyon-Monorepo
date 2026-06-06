package fr.varyon.vrpg.combat;

import com.hypixel.hytale.server.npc.entities.NPCEntity;
import fr.varyon.vrpg.config.ClassXpConfig;
import fr.varyon.vrpg.config.TierMappingConfig;
import fr.varyon.vrpg.profession.chasseur.ChasseurXpTable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class MobKillXpResolver {

    private static volatile List<String> rolesLongestFirst;

    private MobKillXpResolver() {}

    public static double resolveBaseXp(@Nullable NPCEntity npc) {
        return resolveBaseXp(npcRoleKey(npc));
    }

    public static double resolveBaseXp(@Nullable String roleKey) {
        if (roleKey == null || roleKey.isBlank()) {
            return ClassXpConfig.getMinKillXp();
        }
        String tier = resolveTier(roleKey);
        if (tier == null) {
            return ClassXpConfig.getMinKillXp();
        }
        double weight = TierMappingConfig.TIER_WEIGHTS.getOrDefault(tier, -1.0);
        if (weight < 0) {
            return ClassXpConfig.getMinKillXp();
        }
        if (weight <= 0.0) {
            return 0.0;
        }
        double xp = weight * ClassXpConfig.getBaseKillMultiplier();
        return Math.max(ClassXpConfig.getMinKillXp(), xp);
    }

    @Nullable
    public static String antiFarmKey(@Nullable NPCEntity npc) {
        String key = npcRoleKey(npc);
        if (key == null || key.isBlank()) return null;
        return key.toLowerCase(Locale.ROOT);
    }

    @Nullable
    public static String npcRoleKey(@Nullable NPCEntity npc) {
        if (npc == null) return null;
        try {
            String typeId = npc.getNPCTypeId();
            if (typeId != null && !typeId.isBlank()) {
                return canonicalRole(typeId);
            }
        } catch (Exception ignored) {}
        try {
            String role = npc.getRoleName();
            if (role != null && !role.isBlank()) {
                return canonicalRole(role);
            }
        } catch (Exception ignored) {}
        return null;
    }

    @Nullable
    private static String resolveTier(@Nonnull String roleKey) {
        String canonical = canonicalRole(roleKey);
        if (canonical.isEmpty()) return null;
        Map<String, String> tiers = ChasseurXpTable.MOB_TIERS;
        String direct = tiers.get(canonical);
        if (direct != null) return direct.toLowerCase(Locale.ROOT);
        ensureRoleIndex();
        for (String role : rolesLongestFirst) {
            if (canonical.startsWith(role + "_")) {
                return tiers.get(role).toLowerCase(Locale.ROOT);
            }
        }
        return null;
    }

    private static void ensureRoleIndex() {
        if (rolesLongestFirst != null) return;
        synchronized (MobKillXpResolver.class) {
            if (rolesLongestFirst != null) return;
            List<String> sorted = new ArrayList<>(ChasseurXpTable.MOB_TIERS.keySet());
            sorted.sort(Comparator.comparing(String::length).reversed());
            rolesLongestFirst = List.copyOf(sorted);
        }
    }

    @Nonnull
    private static String canonicalRole(@Nonnull String raw) {
        String s = raw.trim().toLowerCase(Locale.ROOT);
        int slash = s.lastIndexOf('/');
        if (slash >= 0) s = s.substring(slash + 1);
        int colon = s.lastIndexOf(':');
        if (colon >= 0) s = s.substring(colon + 1);
        return s;
    }
}
