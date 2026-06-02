package fr.varyon.holograms.hologram;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.regex.Pattern;

public enum HologramLineType {
    TEXT,
    IMAGE,
    ITEM;

    private static final Pattern DOUBLE_SIDED_PATTERN =
        Pattern.compile(":ds(?:$|:)|:doublesided(?:$|:)", Pattern.CASE_INSENSITIVE);

    @Nonnull
    public static HologramLineType fromLine(@Nonnull String line) {
        String special = extractSpecialLine(line);
        String lower = special.toLowerCase();
        if (lower.startsWith("image:")) return IMAGE;
        if (lower.startsWith("item:")) return ITEM;
        return TEXT;
    }

    @Nonnull
    public static String extractSpecialLine(@Nonnull String line) {
        String trimmed = line.trim();
        String lower = trimmed.toLowerCase();
        int imageIdx = lower.indexOf("image:");
        if (imageIdx >= 0) return trimmed.substring(imageIdx);
        int itemIdx = lower.indexOf("item:");
        if (itemIdx >= 0) return trimmed.substring(itemIdx);
        return trimmed;
    }

    public static boolean hasDoubleSidedFlag(@Nonnull String line) {
        return DOUBLE_SIDED_PATTERN.matcher(line.toLowerCase()).find();
    }

    @Nonnull
    public static String stripDoubleSidedFlag(@Nonnull String line) {
        String result = line.replaceAll("(?i):ds:", ":").replaceAll("(?i):doublesided:", ":");
        result = result.replaceAll("(?i):ds$", "").replaceAll("(?i):doublesided$", "");
        return result.trim();
    }

    public static class ImageLineData {
        public final String imageName;
        public final float scale;
        public final boolean billboard;
        public final boolean doubleSided;
        public final float trackingDistance;
        @Nullable public final String animationName;

        public ImageLineData(String imageName, float scale, boolean billboard, boolean doubleSided,
                             float trackingDistance, @Nullable String animationName) {
            this.imageName = imageName;
            this.scale = scale;
            this.billboard = billboard;
            this.doubleSided = doubleSided;
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
     * Parses: image:<name>[:<scale>][:<billboard>][:<dist>][:ds|:doublesided][:anim_<name>]
     */
    @Nonnull
    public static ImageLineData parseImageLine(@Nonnull String line) {
        String special = extractSpecialLine(line);
        boolean doubleSided = hasDoubleSidedFlag(special);
        String clean = stripDoubleSidedFlag(special);
        String body = clean.substring("image:".length());
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
            if (p.equalsIgnoreCase("ds") || p.equalsIgnoreCase("doublesided")) {
                doubleSided = true;
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
        return new ImageLineData(imageName, scale, billboard, doubleSided, trackingDistance, animationName);
    }

    /**
     * Parses: item:<id>[:<scale>][:anim_<name>]
     */
    @Nonnull
    public static ItemLineData parseItemLine(@Nonnull String line) {
        String body = extractSpecialLine(line).substring("item:".length());
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

    @Nonnull
    public static String stripAnimation(@Nonnull String line) {
        HologramLineType type = fromLine(line);
        if (type == TEXT) {
            return line;
        }
        if (type == IMAGE) {
            ImageLineData data = parseImageLine(line);
            StringBuilder sb = new StringBuilder("image:").append(data.imageName);
            if (data.scale != 1.0f) sb.append(":").append(trimNum(data.scale));
            if (data.billboard) sb.append(":true");
            if (data.trackingDistance >= 0) sb.append(":").append(trimNum(data.trackingDistance));
            if (data.doubleSided) sb.append(":ds");
            return sb.toString();
        }
        ItemLineData data = parseItemLine(line);
        StringBuilder sb = new StringBuilder("item:").append(data.itemId);
        if (data.scale != 1.0f) sb.append(":").append(trimNum(data.scale));
        return sb.toString();
    }

    private static String trimNum(float value) {
        if (value == (long) value) return String.valueOf((long) value);
        return String.valueOf(value);
    }
}
