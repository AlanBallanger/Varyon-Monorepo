package fr.varyon.shop.util;

import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemQuality;

import java.util.Locale;

/** Resolves an item's rarity/quality id (e.g. "Common", "Rare") and its slot-background texture. */
public final class ItemRarityUtil {
    private ItemRarityUtil() {
    }

    public static String rarityOf(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return "Common";
        }
        try {
            Item asset = Item.getAssetMap().getAsset(itemId);
            if (asset == null) {
                int colon = itemId.indexOf(':');
                if (colon >= 0 && colon < itemId.length() - 1) {
                    asset = Item.getAssetMap().getAsset(itemId.substring(colon + 1));
                }
            }
            if (asset == null) {
                return "Common";
            }
            int qualityIndex = asset.getQualityIndex();
            ItemQuality quality = ItemQuality.getAssetMap().getAsset(qualityIndex);
            if (quality != null && quality.getId() != null && !quality.getId().isBlank()) {
                String id = quality.getId();
                return Character.toUpperCase(id.charAt(0)) + id.substring(1).toLowerCase(Locale.ROOT);
            }
        } catch (Throwable ignored) {
        }
        return "Common";
    }

    /** Built-in engine asset path (AssetImage.AssetPath), not a pack-local texture file. */
    public static String rarityTexturePath(String rarity) {
        String base = "UI/ItemQualities/Slots/";
        return switch (rarity == null ? "" : rarity.toLowerCase(Locale.ROOT)) {
            case "junk" -> base + "SlotJunk.png";
            case "uncommon" -> base + "SlotUncommon.png";
            case "rare" -> base + "SlotRare.png";
            case "epic" -> base + "SlotEpic.png";
            case "legendary" -> base + "SlotLegendary.png";
            case "developer" -> base + "SlotDeveloper.png";
            case "tool" -> base + "SlotTool.png";
            case "default" -> base + "SlotDefault.png";
            default -> base + "SlotCommon.png";
        };
    }
}
