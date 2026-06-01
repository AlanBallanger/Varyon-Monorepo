package fr.varyon.holograms.animation;

import org.joml.Vector3d;
import org.joml.Vector3f;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Keyframe {

    private final float time;
    @Nullable private final Vector3d positionOffset;
    @Nullable private final Vector3f rotationDelta;
    private final float scale;
    @Nonnull private final EasingType easing;

    public Keyframe(float time, @Nullable Vector3d positionOffset, @Nullable Vector3f rotationDelta,
                    float scale, @Nonnull EasingType easing) {
        this.time = time;
        this.positionOffset = positionOffset != null ? new Vector3d(positionOffset) : null;
        this.rotationDelta = rotationDelta != null ? new Vector3f(rotationDelta) : null;
        this.scale = scale;
        this.easing = easing;
    }

    public float getTime() { return time; }
    @Nullable public Vector3d getPositionOffset() { return positionOffset; }
    @Nullable public Vector3f getRotationDelta() { return rotationDelta; }
    public float getScale() { return scale; }
    @Nonnull public EasingType getEasing() { return easing; }

    @Nonnull
    public Keyframe interpolate(@Nonnull Keyframe next, double rawT) {
        double t = easing.apply(rawT);
        Vector3d pos = null;
        if (positionOffset != null || next.positionOffset != null) {
            Vector3d a = positionOffset != null ? positionOffset : new Vector3d();
            Vector3d b = next.positionOffset != null ? next.positionOffset : new Vector3d();
            pos = new Vector3d(
                a.x + (b.x - a.x) * t,
                a.y + (b.y - a.y) * t,
                a.z + (b.z - a.z) * t
            );
        }
        Vector3f rot = null;
        if (rotationDelta != null || next.rotationDelta != null) {
            Vector3f a = rotationDelta != null ? rotationDelta : new Vector3f();
            Vector3f b = next.rotationDelta != null ? next.rotationDelta : new Vector3f();
            rot = new Vector3f(
                (float)(a.x + (b.x - a.x) * t),
                (float)(a.y + (b.y - a.y) * t),
                (float)(a.z + (b.z - a.z) * t)
            );
        }
        float sc = (float)(scale + (next.scale - scale) * t);
        return new Keyframe(time, pos, rot, sc, easing);
    }
}
