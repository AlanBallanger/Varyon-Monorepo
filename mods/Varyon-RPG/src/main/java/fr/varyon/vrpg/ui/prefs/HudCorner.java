package fr.varyon.vrpg.ui.prefs;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public enum HudCorner {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT;

    @Nonnull
    public String id() {
        return name().toLowerCase();
    }

    @Nonnull
    public static HudCorner fromId(@Nullable String id) {
        if (id == null) return TOP_LEFT;
        for (HudCorner c : values()) {
            if (c.id().equalsIgnoreCase(id) || c.name().equalsIgnoreCase(id)) return c;
        }
        return TOP_LEFT;
    }
}
