package fr.varyon.holograms.animation;

import org.joml.Vector3d;
import org.joml.Vector3f;
import javax.annotation.Nonnull;
import java.util.UUID;

public final class HologramAnimMember {

    @Nonnull private final UUID entityId;
    @Nonnull private final Vector3d lineOffset;
    @Nonnull private final Vector3f baseRotation;
    private final float baseScale;

    public HologramAnimMember(@Nonnull UUID entityId, @Nonnull Vector3d lineOffset,
                               @Nonnull Vector3f baseRotation, float baseScale) {
        this.entityId = entityId;
        this.lineOffset = new Vector3d(lineOffset);
        this.baseRotation = new Vector3f(baseRotation);
        this.baseScale = baseScale;
    }

    @Nonnull public UUID getEntityId() { return entityId; }
    @Nonnull public Vector3d getLineOffset() { return new Vector3d(lineOffset); }
    @Nonnull public Vector3f getBaseRotation() { return new Vector3f(baseRotation); }
    public float getBaseScale() { return baseScale; }
}
