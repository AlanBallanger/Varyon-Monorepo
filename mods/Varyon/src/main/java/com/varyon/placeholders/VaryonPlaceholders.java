package com.varyon.placeholders;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.varyon.VaryonPlugin;
import com.varyon.config.ConfigManager;
import com.varyon.config.DifficultyZone;
import com.varyon.util.ZoneCalculator;

import javax.annotation.Nullable;

public final class VaryonPlaceholders {

    public static final String TOKEN_ZONE = "%varyon_zone%";
    public static final String TOKEN_ZONE_NAME = "%varyon_zone_name%";
    public static final String TOKEN_ZONE_ID = "%varyon_zone_id%";

    private VaryonPlaceholders() {
    }

    public static String replaceIn(@Nullable String text, @Nullable PlayerRef playerRef) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String zoneName = getZoneName(playerRef);
        String zoneIdStr = String.valueOf(getZoneId(playerRef));
        String out = text;
        out = out.replace(TOKEN_ZONE_NAME, zoneName);
        out = out.replace(TOKEN_ZONE_ID, zoneIdStr);
        out = out.replace(TOKEN_ZONE, zoneName);
        return out;
    }

    public static String getZoneName(@Nullable PlayerRef playerRef) {
        DifficultyZone z = resolveZone(playerRef);
        return z != null ? z.getName() : "";
    }

    public static int getZoneId(@Nullable PlayerRef playerRef) {
        DifficultyZone z = resolveZone(playerRef);
        return z != null ? z.getZoneId() : 0;
    }

    @Nullable
    private static DifficultyZone resolveZone(@Nullable PlayerRef playerRef) {
        if (playerRef == null) {
            return null;
        }
        ConfigManager cm = VaryonPlugin.getStaticConfigManager();
        if (cm == null) {
            return null;
        }
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return null;
        }
        Store<EntityStore> store = ref.getStore();
        return ZoneCalculator.getCurrentZone(store, ref, null, cm.getZoneConfig());
    }
}
