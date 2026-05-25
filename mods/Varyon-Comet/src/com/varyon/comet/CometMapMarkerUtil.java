package com.varyon.comet;

import com.hypixel.hytale.protocol.FormattedMessage;
import com.hypixel.hytale.protocol.Transform;
import com.hypixel.hytale.protocol.packets.worldmap.MapMarker;

import java.lang.reflect.Field;

public final class CometMapMarkerUtil {

    private CometMapMarkerUtil() {
    }

    public static MapMarker create(String id, FormattedMessage nameMsg, String iconPath, Transform transform) {
        MapMarker marker = new MapMarker();
        setField(marker, "id", id);
        setField(marker, "name", nameMsg);
        setField(marker, "markerImage", iconPath);
        setField(marker, "transform", transform);
        setField(marker, "contextMenuItems", null);
        setField(marker, "components", null);
        return marker;
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getField(fieldName);
            f.set(target, value);
        } catch (NoSuchFieldException | IllegalAccessException | IllegalArgumentException ignored) {
        }
    }
}
