package fr.varyon.holograms.animation;

import org.joml.Vector3d;
import org.joml.Vector3f;
import javax.annotation.Nonnull;

public class AnimationState {

    @Nonnull private final AnimationData animation;
    @Nonnull private final Vector3d basePosition;
    @Nonnull private final Vector3f baseRotation;
    private final float baseScale;
    private float elapsed;

    public AnimationState(@Nonnull AnimationData animation, @Nonnull Vector3d basePosition,
                          @Nonnull Vector3f baseRotation, float baseScale) {
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

    @Nonnull public AnimationData getAnimation() { return animation; }
    @Nonnull public Vector3d getBasePosition() { return basePosition; }
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
