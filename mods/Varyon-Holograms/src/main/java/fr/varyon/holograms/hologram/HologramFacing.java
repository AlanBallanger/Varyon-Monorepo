package fr.varyon.holograms.hologram;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public enum HologramFacing {

    NORTH,
    SOUTH,
    EAST,
    WEST;

    @Nonnull
    public static HologramFacing parse(@Nullable String raw) {
        if (raw == null || raw.isBlank()) return NORTH;
        return switch (raw.trim().toLowerCase()) {
            case "south", "sud", "s" -> SOUTH;
            case "east", "est", "e" -> EAST;
            case "west", "ouest", "o", "w" -> WEST;
            default -> NORTH;
        };
    }

    @Nonnull
    public String jsonValue() {
        return name().toLowerCase();
    }

    @Nonnull
    public String label() {
        return switch (this) {
            case NORTH -> "NORD";
            case SOUTH -> "SUD";
            case EAST -> "EST";
            case WEST -> "OUEST";
        };
    }

    public float yawRadians() {
        return switch (this) {
            case NORTH -> 0f;
            case SOUTH -> (float) Math.PI;
            case EAST -> (float) (-Math.PI / 2);
            case WEST -> (float) (Math.PI / 2);
        };
    }
}
