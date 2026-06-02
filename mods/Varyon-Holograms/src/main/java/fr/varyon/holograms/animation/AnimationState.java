package fr.varyon.holograms.animation;

import org.joml.Vector3d;
import org.joml.Vector3f;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public class AnimationState {

    @Nonnull private final UUID worldId;
    @Nonnull private final AnimationData animation;
    @Nonnull private Vector3d basePosition;
    @Nonnull private final Vector3f baseRotation;
    private final float baseScale;
    private float elapsed;
    @Nullable private Vector3d lastSetPosition;

    public AnimationState(@Nonnull UUID worldId, @Nonnull AnimationData animation,
                          @Nonnull Vector3d basePosition, @Nonnull Vector3f baseRotation, float baseScale) {
        this.worldId = worldId;
        this.animation = animation;
        this.basePosition = new Vector3d(basePosition);
        this.baseRotation = new Vector3f(baseRotation);
        this.baseScale = baseScale;
        this.elapsed = 0f;
    }

    public void tick(float deltaSeconds) {
        elapsed += deltaSeconds;
        if (animation.isLoop() && animation.getDuration() > 0) {
            elapsed %= animation.getDuration();
        }
    }

    @Nonnull public UUID getWorldId() { return worldId; }
    @Nonnull public AnimationData getAnimation() { return animation; }
    @Nonnull public Vector3d getBasePosition() { return new Vector3d(basePosition); }

    public void setBasePosition(@Nonnull Vector3d newBasePosition) {
        this.basePosition = new Vector3d(newBasePosition);
    }

    @Nullable
    public Vector3d getLastSetPosition() {
        return lastSetPosition != null ? new Vector3d(lastSetPosition) : null;
    }

    public void setLastSetPosition(@Nonnull Vector3d position) {
        this.lastSetPosition = new Vector3d(position);
    }

    @Nonnull public Vector3f getBaseRotation() { return baseRotation; }
    public float getBaseScale() { return baseScale; }
    public float getElapsed() { return elapsed; }

    @Nonnull
    public Vector3d getCurrentPosition() {
        Keyframe kf = animation.sample(elapsed);
        if (kf == null || kf.getPositionOffset() == null) return new Vector3d(basePosition);
        Vector3d off = kf.getPositionOffset();
        return new Vector3d(basePosition.x + off.x, basePosition.y + off.y, basePosition.z + off.z);
    }

    @Nonnull
    public Vector3f getCurrentRotation() {
        Keyframe kf = animation.sample(elapsed);
        if (kf == null || kf.getRotationDelta() == null) return new Vector3f(baseRotation);
        Vector3f delta = kf.getRotationDelta();
        return new Vector3f(
            (float) Math.toRadians(baseRotation.x + delta.x),
            (float) Math.toRadians(baseRotation.y + delta.y),
            (float) Math.toRadians(baseRotation.z + delta.z)
        );
    }

    public float getCurrentScale() {
        Keyframe kf = animation.sample(elapsed);
        return kf == null ? baseScale : baseScale * kf.getScale();
    }
}
