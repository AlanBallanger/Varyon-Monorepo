package fr.varyon.vrpg.classes;

import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class WeaponDamageReader {

    private static final ConcurrentHashMap<String, Integer> CACHE = new ConcurrentHashMap<>();

    private static final Pattern RE_PHYSICAL      = Pattern.compile("\"Physical\"\\s*:\\s*(\\d+(?:\\.\\d+)?)");
    private static final Pattern RE_PROJECTILE    = Pattern.compile("\"Projectile\"\\s*:\\s*(\\d+(?:\\.\\d+)?)");
    private static final Pattern RE_ENTITY_DAMAGE = Pattern.compile("\"EntityDamage\"\\s*:\\s*(\\d+(?:\\.\\d+)?)");
    private static final Pattern RE_FIRE          = Pattern.compile("\"Fire\"\\s*:\\s*(\\d+(?:\\.\\d+)?)");
    private static final Pattern RE_ICE           = Pattern.compile("\"Ice\"\\s*:\\s*(\\d+(?:\\.\\d+)?)");
    private static final Pattern RE_MAGIC         = Pattern.compile("\"Magic\"\\s*:\\s*(\\d+(?:\\.\\d+)?)");

    private static final com.hypixel.hytale.logger.HytaleLogger LOG =
        com.hypixel.hytale.logger.HytaleLogger.forEnclosingClass();

    private WeaponDamageReader() {}

    public static int readHeldWeaponDamage(@Nullable PlayerRef playerRef) {
        if (playerRef == null) return -1;
        try {
            var hotbar = playerRef.getComponent(
                com.hypixel.hytale.server.core.inventory.InventoryComponent.Hotbar.getComponentType());
            if (hotbar == null) return -1;
            byte slot = hotbar.getActiveSlot();
            ItemStack held = hotbar.getInventory().getItemStack((short) slot);
            if (held == null || held.isEmpty()) return -1;
            String itemId = held.getItemId();
            if (itemId == null || itemId.isEmpty()) return -1;

            if (CACHE.containsKey(itemId)) {
                return CACHE.get(itemId);
            }

            int dmg = readFromDisk(itemId, 0);
            if (fr.varyon.vrpg.config.VrpgConfig.isDebugCombat())
                LOG.atInfo().log("[WeaponDamageReader] itemId=" + itemId + " dmg=" + dmg);
            if (dmg >= 0) CACHE.put(itemId, dmg);
            return dmg;
        } catch (Exception e) {
            LOG.atWarning().log("[WeaponDamageReader] exception: " + e.getMessage());
            return -1;
        }
    }

    private static int readFromDisk(String itemId, int depth) {
        if (itemId == null || itemId.isBlank() || depth > 8) return -1;
        try {
            Path path = Item.getAssetMap().getPath(itemId.trim());
            if (path != null && Files.isRegularFile(path)) {
                String json = Files.readString(path);
                int v = extractPhysicalFromJson(json);
                if (v >= 0) return v;
                String parent = extractStringField(json, "Parent");
                if (parent != null && !parent.isBlank()) return readFromDisk(parent.trim(), depth + 1);
                return -1;
            }
            // Item vanilla (pas sur disque) : lire depuis l'asset en mémoire
            Item asset = Item.getAssetMap().getAsset(itemId.trim());
            if (asset != null && asset.getWeapon() != null) {
                var breakdown = asset.getWeapon().getBasicDamageBreakdown();
                if (breakdown != null && !breakdown.entries().isEmpty()) {
                    float total = 0;
                    for (var entry : breakdown.entries()) total += entry.max();
                    return Math.round(total);
                }
            }
        } catch (Exception ignored) {}
        return -1;
    }

    private static int extractPhysicalFromJson(String json) {
        for (Pattern p : new Pattern[]{RE_PHYSICAL, RE_PROJECTILE, RE_FIRE, RE_ICE, RE_MAGIC, RE_ENTITY_DAMAGE}) {
            Matcher m = p.matcher(json);
            if (m.find()) {
                try { return Math.round(Float.parseFloat(m.group(1))); } catch (NumberFormatException ignored) {}
            }
        }
        return -1;
    }

    private static final Pattern RE_STRING_FIELD = Pattern.compile("\"Parent\"\\s*:\\s*\"([^\"]+)\"");

    private static String extractStringField(String json, String field) {
        Pattern p = Pattern.compile("\"" + field + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher m = p.matcher(json);
        return m.find() ? m.group(1) : null;
    }
}
