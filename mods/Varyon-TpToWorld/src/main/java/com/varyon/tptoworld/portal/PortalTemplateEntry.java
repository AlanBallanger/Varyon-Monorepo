package com.varyon.tptoworld.portal;

import javax.annotation.Nonnull;

public record PortalTemplateEntry(
        @Nonnull String dimension,
        int x,
        int y,
        int z,
        @Nonnull String type,
        @Nonnull String background,
        float scale,
        float centerOffsetY,
        boolean wide,
        float yaw
) {
    public static String key(@Nonnull String dimension, int x, int y, int z) {
        return dimension + ":" + x + ":" + y + ":" + z;
    }

    @Nonnull
    public String key() {
        return key(this.dimension, this.x, this.y, this.z);
    }

    public boolean hasBackground() {
        return background != null && !background.isBlank();
    }
}
