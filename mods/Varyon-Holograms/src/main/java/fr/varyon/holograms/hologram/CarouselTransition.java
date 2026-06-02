package fr.varyon.holograms.hologram;

import org.joml.Vector3d;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public enum CarouselTransition {

    SLIDE_LEFT,
    SLIDE_UP,
    INSTANT;

    public static final float DURATION_SECONDS = 0.5f;
    private static final double SLIDE_DISTANCE = 2.0;

    @Nonnull
    public static CarouselTransition parse(@Nullable String raw) {
        if (raw == null || raw.isBlank()) return SLIDE_LEFT;
        return switch (raw.trim().toLowerCase()) {
            case "slide_up", "up", "haut" -> SLIDE_UP;
            case "instant", "cut" -> INSTANT;
            default -> SLIDE_LEFT;
        };
    }

    @Nonnull
    public String jsonValue() {
        return switch (this) {
            case SLIDE_UP -> "slide_up";
            case INSTANT -> "instant";
            default -> "slide_left";
        };
    }

    @Nonnull
    public String label() {
        return switch (this) {
            case SLIDE_UP -> "HAUT";
            case INSTANT -> "INSTANT";
            default -> "GAUCHE";
        };
    }

    @Nonnull
    public Vector3d incomingStart(@Nonnull HologramLayout layout) {
        return switch (this) {
            case SLIDE_LEFT -> layout == HologramLayout.FLOOR
                ? new Vector3d(0, 0, SLIDE_DISTANCE)
                : new Vector3d(SLIDE_DISTANCE, 0, 0);
            case SLIDE_UP -> new Vector3d(0, -SLIDE_DISTANCE, 0);
            case INSTANT -> new Vector3d();
        };
    }

    @Nonnull
    public Vector3d outgoingEnd(@Nonnull HologramLayout layout) {
        return switch (this) {
            case SLIDE_LEFT -> layout == HologramLayout.FLOOR
                ? new Vector3d(0, 0, -SLIDE_DISTANCE)
                : new Vector3d(-SLIDE_DISTANCE, 0, 0);
            case SLIDE_UP -> new Vector3d(0, SLIDE_DISTANCE, 0);
            case INSTANT -> new Vector3d();
        };
    }
}
