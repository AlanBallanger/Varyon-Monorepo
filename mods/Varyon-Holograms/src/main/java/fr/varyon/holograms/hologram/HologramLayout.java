package fr.varyon.holograms.hologram;

import com.hypixel.hytale.math.vector.Rotation3f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public enum HologramLayout {

    WALL,
    FLOOR;

    @Nonnull
    public static HologramLayout parse(@Nullable String raw) {
        if (raw == null || raw.isBlank()) return WALL;
        return switch (raw.trim().toLowerCase()) {
            case "floor", "sol" -> FLOOR;
            default -> WALL;
        };
    }

    @Nonnull
    public String jsonValue() {
        return name().toLowerCase();
    }

    @Nonnull
    public String label() {
        return switch (this) {
            case WALL -> "MUR";
            case FLOOR -> "SOL";
        };
    }

    @Nonnull
    public Vector3d lineOffset(int lineIndex, double lineSpacing, @Nonnull HologramLineType type) {
        if (lineIndex <= 0) {
            return switch (this) {
                case WALL -> new Vector3d();
                case FLOOR -> new Vector3d(0, 0.02, 0);
            };
        }
        double mult = type == HologramLineType.TEXT ? 1.0 : 1.5;
        double step = lineIndex * lineSpacing * mult;
        return switch (this) {
            case WALL -> new Vector3d(0, -step, 0);
            case FLOOR -> new Vector3d(0, 0.02, -step);
        };
    }

    @Nonnull
    public Rotation3f spawnRotation() {
        return switch (this) {
            case WALL -> new Rotation3f();
            case FLOOR -> new Rotation3f((float) (-Math.PI / 2), 0f, 0f);
        };
    }

    @Nonnull
    public Vector3f animBaseRotationDegrees() {
        return switch (this) {
            case WALL -> new Vector3f();
            case FLOOR -> new Vector3f(-90f, 0f, 0f);
        };
    }
}
