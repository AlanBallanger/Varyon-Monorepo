package fr.varyon.holograms.hologram;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public enum HologramLineType {
    TEXT,
    IMAGE,
    ITEM;

    @Nonnull
    public static HologramLineType fromLine(@Nonnull String line) {
        String lower = line.toLowerCase();
        if (lower.startsWith("image:")) return IMAGE;
        if (lower.startsWith("item:")) return ITEM;
        return TEXT;
    }

    public static class ImageLineData {
        public final String imageName;
        public final float scale;
        public final boolean billboard;
        public final float trackingDistance;
        @Nullable public final String animationName;

        public ImageLineData(String imageName, float scale, boolean billboard, float trackingDistance, @Nullable String animationName) {
            this.imageName = imageName;
            this.scale = scale;
            this.billboard = billboard;
            this.trackingDistance = trackingDistance;
            this.animationName = animationName;
        }

        public boolean hasAnimation() { return animationName != null; }
    }

    public static class ItemLineData {
        public final String itemId;
        public final float scale;
        @Nullable public final String animationName;

        public ItemLineData(String itemId, float scale, @Nullable String animationName) {
            this.itemId = itemId;
            this.scale = scale;
            this.animationName = animationName;
        }

        public boolean hasAnimation() { return animationName != null; }
    }

    /**
     * Parses: image:<name>[:<scale>][:<billboard>][:<dist>][:anim_<name>]
     * billboard = "true" token; dist = numeric after "true"; anim = token starting with "anim_"
     */
    @Nonnull
    public static ImageLineData parseImageLine(@Nonnull String line) {
        String body = line.substring("image:".length());
        String[] parts = body.split(":");
        String imageName = parts.length > 0 ? parts[0].trim() : "";
        float scale = 1.0f;
        boolean billboard = false;
        float trackingDistance = -1.0f;
        String animationName = null;

        for (int i = 1; i < parts.length; i++) {
            String p = parts[i].trim();
            if (p.startsWith("anim_")) {
                animationName = p.substring("anim_".length());
                continue;
            }
            if (p.equalsIgnoreCase("true")) {
                billboard = true;
                continue;
            }
            try {
                float v = Float.parseFloat(p);
                if (billboard && trackingDistance < 0) {
                    trackingDistance = v;
                } else {
                    scale = v;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return new ImageLineData(imageName, scale, billboard, trackingDistance, animationName);
    }

    /**
     * Parses: item:<id>[:<scale>][:anim_<name>]
     */
    @Nonnull
    public static ItemLineData parseItemLine(@Nonnull String line) {
        String body = line.substring("item:".length());
        String[] parts = body.split(":");
        String itemId = parts.length > 0 ? parts[0].trim() : "";
        float scale = 1.0f;
        String animationName = null;

        for (int i = 1; i < parts.length; i++) {
            String p = parts[i].trim();
            if (p.startsWith("anim_")) {
                animationName = p.substring("anim_".length());
                continue;
            }
            try {
                scale = Float.parseFloat(p);
            } catch (NumberFormatException ignored) {
            }
        }
        return new ItemLineData(itemId, scale, animationName);
    }

    @Nullable
    public static String extractAnimationName(@Nonnull String line) {
        for (String part : line.split(":")) {
            if (part.trim().startsWith("anim_")) {
                return part.trim().substring("anim_".length());
            }
        }
        return null;
    }
}
