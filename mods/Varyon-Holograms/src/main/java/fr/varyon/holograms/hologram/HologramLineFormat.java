package fr.varyon.holograms.hologram;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class HologramLineFormat {

    public enum Mode { TEXT, IMAGE, ITEM }

    public static final class EditState {
        public Mode mode = Mode.TEXT;
        public String text = "";
        public String imageName = "";
        public String itemId = "";
        public float scale = 1.0f;
        public boolean billboard;
        public boolean doubleSided;

        @Nonnull
        public EditState copy() {
            EditState s = new EditState();
            s.mode = mode;
            s.text = text;
            s.imageName = imageName;
            s.itemId = itemId;
            s.scale = scale;
            s.billboard = billboard;
            s.doubleSided = doubleSided;
            return s;
        }
    }

    private HologramLineFormat() {}

    @Nonnull
    public static EditState parse(@Nonnull String line) {
        EditState state = new EditState();
        HologramLineType type = HologramLineType.fromLine(line);
        String special = HologramLineType.extractSpecialLine(line);

        switch (type) {
            case IMAGE -> {
                state.mode = Mode.IMAGE;
                HologramLineType.ImageLineData data = HologramLineType.parseImageLine(line);
                state.imageName = data.imageName;
                state.scale = data.scale;
                state.billboard = data.billboard;
                state.doubleSided = data.doubleSided;
            }
            case ITEM -> {
                state.mode = Mode.ITEM;
                HologramLineType.ItemLineData data = HologramLineType.parseItemLine(line);
                state.itemId = data.itemId;
                state.scale = data.scale;
            }
            default -> {
                state.mode = Mode.TEXT;
                state.text = special;
            }
        }
        return state;
    }

    @Nonnull
    public static String format(@Nonnull EditState state) {
        return switch (state.mode) {
            case IMAGE -> formatImage(state.imageName, state.scale, state.billboard, state.doubleSided);
            case ITEM -> formatItem(state.itemId, state.scale);
            default -> state.text != null ? state.text.trim() : "";
        };
    }

    @Nonnull
    public static String formatImage(@Nonnull String imageName, float scale, boolean billboard,
                                     boolean doubleSided) {
        String name = imageName.trim();
        if (name.isEmpty()) return "image:unknown";
        StringBuilder sb = new StringBuilder("image:").append(name);
        if (scale != 1.0f) sb.append(":").append(trimScale(scale));
        if (billboard) sb.append(":true");
        if (doubleSided) sb.append(":ds");
        return sb.toString();
    }

    @Nonnull
    public static String formatItem(@Nonnull String itemId, float scale) {
        String id = itemId.trim();
        if (id.isEmpty()) return "item:unknown";
        StringBuilder sb = new StringBuilder("item:").append(id);
        if (scale != 1.0f) sb.append(":").append(trimScale(scale));
        return sb.toString();
    }

    @Nonnull
    public static String preview(@Nonnull EditState state) {
        String formatted = format(state);
        return formatted.isBlank() ? "(vide)" : formatted;
    }

    @Nonnull
    public static String modeLabel(@Nonnull Mode mode) {
        return switch (mode) {
            case TEXT -> "TEXTE";
            case IMAGE -> "IMAGE";
            case ITEM -> "ITEM";
        };
    }

    private static String trimScale(float scale) {
        if (scale == (long) scale) return String.valueOf((long) scale);
        return String.format("%.2f", scale).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
